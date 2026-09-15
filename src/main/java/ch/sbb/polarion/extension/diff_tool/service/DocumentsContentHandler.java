package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentContentAnchor;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsContentMergePair;
import ch.sbb.polarion.extension.diff_tool.util.CommentUtils;
import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.core.util.logging.Logger;
import com.polarion.core.util.types.Text;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ch.sbb.polarion.extension.diff_tool.report.MergeReport.OperationResultType.MODIFIED;
import static ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentContentAnchor.ContentPosition.ABOVE;
import static ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentContentAnchor.ContentPosition.BELOW;

class DocumentsContentHandler {

    private static final Logger logger = Logger.getLogger(DocumentsContentHandler.class);

    private static final Set<String> HTML_HEADER_TAGS = new HashSet<>(Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6"));

    /**
     * The element a work item takes on a document page, eg.
     * {@code <div id="polarion_wiki macro name=module-workitem;params=id=EL-1|layout=0"></div>}, whose ID is
     * followed either by the closing quote of the attribute or by the next parameter.
     * <p>
     * The element carries no markup, but it does carry text: Polarion writes the work item ID into a snippet it
     * builds itself (see {@code ModulePageModifier.createWorkItemPart}), while its editor writes the element empty.
     */
    private static final String ANCHOR_REGEX = "<(?<tag>[a-zA-Z][a-zA-Z0-9]*)\\s[^>]+params=id=%s[\"|][^>]*>[^<]*</\\k<tag>>";



    public Map<String, DocumentContentAnchor> parse(@NotNull String documentContent) {
        DocumentContentAnchor lastAnchor = null;
        StringBuilder contentBuffer = new StringBuilder();
        Map<String, DocumentContentAnchor> contentAnchors = new HashMap<>();

        Document parsedDocument = Jsoup.parse(documentContent);
        for (Element element : parsedDocument.body().children()) {
            DocumentContentAnchor anchor = anchor(element);
            if (anchor != null) {
                if (!contentBuffer.isEmpty()) {
                    if (lastAnchor == null) {
                        anchor.setContentAbove(contentBuffer.toString());
                    } else if (isChapter(element)) {
                        lastAnchor.setContentBelow(contentBuffer.toString());
                    } else {
                        anchor.setContentAbove(contentBuffer.toString());
                    }
                    contentBuffer = new StringBuilder();
                }
                contentAnchors.put(anchor.getId(), anchor);
                lastAnchor = anchor;
            } else {
                contentBuffer.append(element.outerHtml());
            }
        }
        // put the rest content to the last anchor
        if (!contentBuffer.isEmpty() && lastAnchor != null) {
            lastAnchor.setContentBelow(contentBuffer.toString());
        }
        return contentAnchors;
    }

    public void merge(@NotNull IModule sourceModule, @NotNull IModule targetModule, @NotNull DocumentsContentMergeContext mergeContext, @NotNull List<DocumentsContentMergePair> mergePairs) {
        Document sourceDocument = Jsoup.parse(sourceModule.getHomePageContent().getContent());
        Document targetDocument = Jsoup.parse(targetModule.getHomePageContent().getContent());
        for (DocumentsContentMergePair mergePair : mergePairs) {
            List<Element> sourceContent = getContent(sourceDocument, mergeContext.getSourceWorkItemId(mergePair), mergePair.getContentPosition());
            boolean contentModified = insertContent(targetDocument, mergeContext.getTargetWorkItemId(mergePair), mergePair.getContentPosition(), sourceContent, mergeContext.isPreserveComments());
            if (contentModified) {
                String contentPosition = mergePair.getContentPosition().toString();
                mergeContext.reportEntry(MODIFIED, mergePair, "content %s workitem '%s' modified with content %s workitem '%s'".formatted(
                        contentPosition, mergeContext.getTargetWorkItemId(mergePair), contentPosition, mergeContext.getSourceWorkItemId(mergePair)));
            }
        }
        if (!mergeContext.getMergeReport().getModified().isEmpty()) {
            targetModule.setHomePageContent(Text.html(targetDocument.body().html()));
        }
    }

    @SuppressWarnings("java:S3776") // ignore cognitive complexity complaint
    @VisibleForTesting
    List<Element> getContent(@NotNull Document sourceDocument, @NotNull String contentAnchorId, @NotNull DocumentContentAnchor.ContentPosition contentPosition) {
        List<Element> content = new ArrayList<>();
        boolean elementPassed = false;
        Element body = preProcessSourceDocument(sourceDocument.body());
        for (Element element : body.children()) {
            String extractedWorkItemId = extractWorkItemId(element);
            if (extractedWorkItemId == null) {
                content.add(element);
            } else {
                if (elementPassed) {
                    return content;
                }
                if (contentAnchorId.equals(extractedWorkItemId)) {
                    if (contentPosition == DocumentContentAnchor.ContentPosition.ABOVE) {
                        return content;
                    } else {
                        elementPassed = true;
                    }
                }
                content.clear();
            }
        }
        // even if there were no more work items below the anchor, we have to return the rest content
        return elementPassed && contentPosition == DocumentContentAnchor.ContentPosition.BELOW && !content.isEmpty() ? content : Collections.emptyList();
    }

    private Element preProcessSourceDocument(Element body) {
        return CommentUtils.removeComments(body);
    }

    /**
     * Copies into the target document the text which is placed between the work items of a merged chapter, i.e. the
     * content of the source document page which doesn't belong to any work item.
     * <p>
     * The content is spliced into the page around the anchors of the merged work items, the way
     * {@code MergeService.modifyHeaderTag} rewrites a heading tag: every other byte of the page is left as it is.
     * A document page is written by Polarion's own editor and read back by its own parser, and this method has no
     * business normalizing the parts of it which the merge does not touch.
     *
     * The markers which anchor a comment to a piece of that text are pointed at the comments copied along with it:
     * a marker refers to a comment of the document it was written in, so it has to name the copy of that comment.
     *
     * @param sourceContent      home page content of the source document, as it was before the merge started
     * @param sourceWorkItemIds  IDs of the merged work items, in document order
     * @param idMapping          maps the ID of a copied work item to the ID of its copy, empty for a moved item
     * @param commentIdMapping   maps the ID of a copied comment to the ID of its copy
     * @return whether the content of the target document was modified
     */
    public boolean copyFreeContent(@NotNull String sourceContent, @NotNull IModule targetModule, @NotNull List<String> sourceWorkItemIds,
                                   @NotNull Map<String, String> idMapping, @NotNull Map<String, String> commentIdMapping) {
        Map<String, DocumentContentAnchor> sourceAnchors = parse(sourceContent);
        String targetContent = targetModule.getHomePageContent() == null ? "" : targetModule.getHomePageContent().getContent();

        String newContent = targetContent;
        for (String sourceWorkItemId : sourceWorkItemIds) {
            DocumentContentAnchor anchor = sourceAnchors.get(sourceWorkItemId);
            if (anchor == null) {
                continue;
            }
            String targetWorkItemId = idMapping.getOrDefault(sourceWorkItemId, sourceWorkItemId);
            newContent = insertAtAnchor(newContent, targetWorkItemId, contentToInsert(anchor.getContentAbove(), commentIdMapping), ABOVE);
            newContent = insertAtAnchor(newContent, targetWorkItemId, contentToInsert(anchor.getContentBelow(), commentIdMapping), BELOW);
        }

        if (newContent.equals(targetContent)) {
            return false;
        }
        targetModule.setHomePageContent(Text.html(newContent));
        return true;
    }

    /**
     * Puts the work items of a merge directly under the chapter they were merged into, in the order they were
     * merged, leaving everything the chapter already had below them.
     * <p>
     * Polarion places a work item on the page at the end of the parsed state of the item it is added to, and that
     * state covers the text which follows that item. A chapter which already holds text and work items therefore
     * decides where a merged item lands, and merged items end up interleaved with what was already there. Their
     * order among themselves comes from the structure; where the whole block sits is fixed here.
     *
     * @return whether the content of the target document was modified
     */
    public boolean moveAnchorsBelow(@NotNull IModule targetModule, @NotNull List<String> workItemIds, @NotNull String belowWorkItemId) {
        String documentContent = targetModule.getHomePageContent() == null ? "" : targetModule.getHomePageContent().getContent();

        StringBuilder mergedBlock = new StringBuilder();
        String remainingContent = documentContent;
        for (String workItemId : workItemIds) {
            Matcher anchorMatcher = Pattern.compile(ANCHOR_REGEX.formatted(Pattern.quote(workItemId))).matcher(remainingContent);
            if (!anchorMatcher.find()) {
                continue;
            }
            mergedBlock.append(anchorMatcher.group());
            remainingContent = remainingContent.substring(0, anchorMatcher.start()) + remainingContent.substring(anchorMatcher.end());
        }

        if (mergedBlock.isEmpty()) {
            return false;
        }
        String newContent = insertAtAnchor(remainingContent, belowWorkItemId, mergedBlock.toString(), BELOW);
        if (newContent.equals(remainingContent)) {
            // The chapter itself has no anchor on the page, so the block was taken out of the page and put nowhere.
            // Leaving the page as it is keeps the merged work items visible where Polarion placed them.
            logger.warn("Work items %s were left where they are: work item '%s' has no anchor on the page of document '%s'"
                    .formatted(workItemIds, belowWorkItemId, targetModule.getModuleName()));
            return false;
        }
        if (newContent.equals(documentContent)) {
            return false;
        }
        targetModule.setHomePageContent(Text.html(newContent));
        return true;
    }

    /**
     * IDs of the comments which are written on the text between the given work items, i.e. the comments a merge of
     * these work items takes along with that text.
     */
    public @NotNull Set<String> freeContentCommentIds(@NotNull String sourceContent, @NotNull List<String> sourceWorkItemIds) {
        Map<String, DocumentContentAnchor> sourceAnchors = parse(sourceContent);
        Set<String> commentIds = new HashSet<>();
        for (String sourceWorkItemId : sourceWorkItemIds) {
            DocumentContentAnchor anchor = sourceAnchors.get(sourceWorkItemId);
            if (anchor != null) {
                commentIds.addAll(CommentUtils.extractCommentIds(anchor.getContentAbove()));
                commentIds.addAll(CommentUtils.extractCommentIds(anchor.getContentBelow()));
            }
        }
        return commentIds;
    }

    /**
     * The text to put into the target document: the text of the source document with its comment markers pointed at
     * the comments which were copied along with it. The markers are rewritten in the text itself, where the comment
     * they belong to is known - matching them into a whole page by their surroundings would not find them, because
     * the text around them names the work items of the source document.
     */
    @VisibleForTesting
    @Nullable
    String contentToInsert(@Nullable String content, @NotNull Map<String, String> commentIdMapping) {
        if (content == null || content.isBlank()) {
            return null;
        }
        return CommentUtils.remapComments(content, commentIdMapping);
    }

    /**
     * Puts content right before or right after the anchor of a certain work item. Content whose anchor is not on the
     * page is not placed anywhere: there is nothing it could be anchored to, and a page is never rewritten blindly.
     */
    @VisibleForTesting
    @NotNull
    String insertAtAnchor(@NotNull String documentContent, @NotNull String workItemId, @Nullable String contentToInsert,
                          @NotNull DocumentContentAnchor.ContentPosition position) {
        if (contentToInsert == null) {
            return documentContent;
        }
        Matcher anchorMatcher = Pattern.compile(ANCHOR_REGEX.formatted(Pattern.quote(workItemId))).matcher(documentContent);
        if (!anchorMatcher.find()) {
            return documentContent;
        }
        String anchorElement = anchorMatcher.group();
        String replacement = position == ABOVE ? contentToInsert + anchorElement : anchorElement + contentToInsert;
        return new StringBuilder(documentContent).replace(anchorMatcher.start(), anchorMatcher.end(), replacement).toString();
    }

    @VisibleForTesting
    boolean insertContent(@NotNull Document targetDocument, @NotNull String contentAnchorId, @NotNull DocumentContentAnchor.ContentPosition contentPosition, @NotNull List<Element> contentToMerge, boolean preserveComments) {
        final Pair<Element, Element> contentBoundaries;
        if (contentPosition == DocumentContentAnchor.ContentPosition.ABOVE) {
            contentBoundaries = getContentBoundariesAbove(targetDocument, contentAnchorId);
        } else {
            contentBoundaries = getContentBoundariesBelow(targetDocument, contentAnchorId);
        }
        List<String> commentIdsToPreserve = new ArrayList<>();
        boolean removedOldContent = removeContent(targetDocument, contentBoundaries.getLeft(), contentBoundaries.getRight(), commentIdsToPreserve);
        if (preserveComments) {
            CommentUtils.appendComments(contentToMerge, commentIdsToPreserve);
        }
        boolean insertedNewContent = insertContent(targetDocument, contentBoundaries.getLeft(), contentToMerge);
        return removedOldContent || insertedNewContent;
    }

    private Pair<Element, Element> getContentBoundariesAbove(@NotNull Document targetDocument, @NotNull String contentAnchorId) {
        Element fromAnchor = null;
        Element toAnchor = null;
        for (Element element : targetDocument.body().children()) {
            String extractedWorkItemId = extractWorkItemId(element);
            if (extractedWorkItemId != null) { // We are looking for anchors, skip content
                if (contentAnchorId.equals(extractedWorkItemId)) {
                    toAnchor = element;
                    break;
                } else {
                    fromAnchor = element;
                }
            }
        }
        return Pair.of(fromAnchor, toAnchor);
    }

    private Pair<Element, Element> getContentBoundariesBelow(@NotNull Document targetDocument, @NotNull String contentAnchorId) {
        Element fromAnchor = null;
        Element toAnchor = null;
        for (Element element : targetDocument.body().children()) {
            String extractedWorkItemId = extractWorkItemId(element);
            if (extractedWorkItemId != null) { // We are looking for anchors, skip content
                if (contentAnchorId.equals(extractedWorkItemId)) {
                    fromAnchor = element;
                } else {
                    if (fromAnchor != null) {
                        toAnchor = element;
                        break;
                    }
                }
            }
        }
        return Pair.of(fromAnchor, toAnchor);
    }

    @VisibleForTesting
    boolean removeContent(@NotNull Document document, @Nullable Element fromElement, @Nullable Element toElement, List<String> commentIds) {
        boolean removed = false;
        Element current = fromElement != null ? fromElement.nextElementSibling() : document.body().child(0);
        while (current != null && !current.equals(toElement)) {
            Element next = current.nextElementSibling();
            commentIds.addAll(CommentUtils.extractCommentIds(current));
            current.remove();
            removed = true;
            current = next;
        }
        return removed;
    }

    @VisibleForTesting
    boolean insertContent(@NotNull Document document, @Nullable Element fromElement, @NotNull List<Element> elementsToInsert) {
        for (Element element : elementsToInsert) {
            if (fromElement != null) {
                fromElement.after(element);
            } else {
                document.body().prependChild(element);
            }
            fromElement = element;  // Update reference to continue inserting after the newly added element
        }
        return !elementsToInsert.isEmpty();
    }

    @VisibleForTesting
    DocumentContentAnchor anchor(@NotNull Element element) {
        String workItemId = extractWorkItemId(element);
        if (workItemId != null) {
            return DocumentContentAnchor.builder().id(workItemId).build();
        } else {
            return null;
        }
    }

    @VisibleForTesting
    boolean isChapter(@NotNull Element element) {
        return HTML_HEADER_TAGS.contains(element.tagName().toLowerCase());
    }

    @VisibleForTesting
    String extractWorkItemId(@NotNull Element element) {
        if (element.id().contains("name=module-workitem;")) {
            RegexMatcher idMatcher = RegexMatcher.get(".*;params=id=(?<id>[A-Za-z]+-\\d+)");
            return idMatcher.findFirst(element.id(), regexEngine -> regexEngine.group("id")).orElse(null);
        } else {
            return null;
        }
    }

}
