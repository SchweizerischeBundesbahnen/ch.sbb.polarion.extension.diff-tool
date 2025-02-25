package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentContentAnchor;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsContentMergePair;
import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import com.polarion.alm.tracker.model.IModule;
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

import static ch.sbb.polarion.extension.diff_tool.report.MergeReport.OperationResultType.MODIFIED;

class DocumentsContentHandler {

    private static final Set<String> HTML_HEADER_TAGS = new HashSet<>(Arrays.asList("h1", "h2", "h3", "h4", "h5"));

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
        return contentAnchors;
    }

    public void merge(@NotNull IModule sourceModule, @NotNull IModule targetModule, @NotNull DocumentsContentMergeContext mergeContext, @NotNull List<DocumentsContentMergePair> mergePairs) {
        Document sourceDocument = Jsoup.parse(sourceModule.getHomePageContent().getContent());
        Document targetDocument = Jsoup.parse(targetModule.getHomePageContent().getContent());
        for (DocumentsContentMergePair mergePair : mergePairs) {
            List<Element> sourceContent = getContent(sourceDocument, mergeContext.getSourceWorkItemId(mergePair), mergePair.getContentSide());
            boolean contentModified = insertContent(targetDocument, mergeContext.getTargetWorkItemId(mergePair), mergePair.getContentSide(), sourceContent);
            if (contentModified) {
                String contentSide = mergePair.getContentSide() == DocumentContentAnchor.ContentSide.ABOVE ? "above" : "below";
                mergeContext.reportEntry(MODIFIED, mergePair, "content %s workitem '%s' modified with content %s workitem '%s'".formatted(
                        contentSide, mergeContext.getTargetWorkItemId(mergePair), contentSide, mergeContext.getSourceWorkItemId(mergePair)));
            }
        }
        if (!mergeContext.getMergeReport().getModified().isEmpty()) {
            targetModule.setHomePageContent(Text.html(targetDocument.body().html()));
        }
    }

    @VisibleForTesting
    List<Element> getContent(@NotNull Document sourceDocument, @NotNull String contentAnchorId, @NotNull DocumentContentAnchor.ContentSide contentSide) {
        List<Element> content = new ArrayList<>();
        boolean elementPassed = false;
        for (Element element : sourceDocument.body().children()) {
            String extractedWorkItemId = extractWorkItemId(element);
            if (extractedWorkItemId == null) {
                content.add(element);
            } else {
                if (elementPassed) {
                    return content;
                }
                if (contentAnchorId.equals(extractedWorkItemId)) {
                    if (contentSide == DocumentContentAnchor.ContentSide.ABOVE) {
                        return content;
                    } else {
                        elementPassed = true;
                    }
                }
                content.clear();
            }
        }
        return Collections.emptyList();
    }

    @VisibleForTesting
    boolean insertContent(@NotNull Document targetDocument, @NotNull String contentAnchorId, @NotNull DocumentContentAnchor.ContentSide contentSide, @NotNull List<Element> contentToMerge) {
        final Pair<Element, Element> contentBoundaries;
        if (contentSide == DocumentContentAnchor.ContentSide.ABOVE) {
            contentBoundaries = getContentBoundariesAbove(targetDocument, contentAnchorId);
        } else {
            contentBoundaries = getContentBoundariesBelow(targetDocument, contentAnchorId);
        }
        boolean removedOldContent = removeContent(targetDocument, contentBoundaries.getLeft(), contentBoundaries.getRight());
        boolean insertedNewContent = insertContent(targetDocument, contentBoundaries.getLeft(), contentToMerge);
        return removedOldContent || insertedNewContent;
    }

    private Pair<Element, Element> getContentBoundariesAbove(@NotNull Document targetDocument, @NotNull String contentAnchorId) {
        Element fromAnchor = null;
        Element toAnchor = null;
        for (Element element : targetDocument.body().children()) {
            String extractedWorkItemId = extractWorkItemId(element);
            if (extractedWorkItemId == null) {
                continue; // We are looking for anchors, skip content
            }
            if (contentAnchorId.equals(extractedWorkItemId)) {
                toAnchor = element;
                break;
            } else {
                fromAnchor = element;
            }
        }
        return Pair.of(fromAnchor, toAnchor);
    }

    private Pair<Element, Element> getContentBoundariesBelow(@NotNull Document targetDocument, @NotNull String contentAnchorId) {
        Element fromAnchor = null;
        Element toAnchor = null;
        for (Element element : targetDocument.body().children()) {
            String extractedWorkItemId = extractWorkItemId(element);
            if (extractedWorkItemId == null) {
                continue; // We are looking for anchors, skip content
            }
            if (contentAnchorId.equals(extractedWorkItemId)) {
                fromAnchor = element;
            } else {
                if (fromAnchor != null) {
                    toAnchor = element;
                    break;
                }
            }
        }
        return Pair.of(fromAnchor, toAnchor);
    }

    @VisibleForTesting
    boolean removeContent(@NotNull Document document, @Nullable Element fromElement, @Nullable Element toElement) {
        boolean removed = false;
        Element current = fromElement != null ? fromElement.nextElementSibling() : document.body().child(0);
        while (current != null && !current.equals(toElement)) {
            Element next = current.nextElementSibling();
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
