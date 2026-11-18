package ch.sbb.polarion.extension.diff_tool.util;

import com.polarion.core.util.StringUtils;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@UtilityClass
public class CommentUtils {

    /**
     * A span start tag, with the end tag which may follow it right away. A comment marker is a span whose start tag
     * holds {@link #MARKER_ID_PATTERN}. The two patterns are kept apart so that neither has to backtrack.
     */
    private static final Pattern SPAN_PATTERN = Pattern.compile("(?i)(?<tag><span\\b[^>]*>)(?:</span>)?");
    /** Finds a marker ID the way Polarion does: in any letter case, at any position among the attributes. */
    private static final Pattern MARKER_ID_PATTERN = Pattern.compile("(?i)\\sid=[\"']polarion-comment:(?<id>\\d+)[\"']");
    /** The ID a comment marker carries as the ID of its span element. */
    private static final Pattern COMMENT_MARKER_ID_PATTERN = Pattern.compile("polarion-comment:\\d+");
    private static final int CONTEXT_WINDOW = 50;
    private static final String POLARION_COMMENT = "<span id=\"polarion-comment:%s\"></span>";

    public List<String> extractCommentIds(@Nullable String content) {
        List<String> commentIds = new ArrayList<>();
        Matcher matcher = SPAN_PATTERN.matcher(StringUtils.getEmptyIfNull(content));
        while (matcher.find()) {
            String commentId = markerId(matcher);
            if (commentId != null) {
                commentIds.add(commentId);
            }
        }
        return commentIds;
    }

    public List<String> extractCommentIds(Element element) {
        List<String> result = new ArrayList<>();
        if ("span".equals(element.tagName()) && COMMENT_MARKER_ID_PATTERN.matcher(element.id()).matches()) {
            result.add(element.id().substring("polarion-comment:".length()));
        } else {
            for (Element child : element.children()) {
                result.addAll(extractCommentIds(child));
            }
        }
        return result;
    }

    public String appendComments(@NotNull String content, List<String> commentIds) {
        return content + commentIds.stream().map(POLARION_COMMENT::formatted).collect(Collectors.joining());
    }

    public void appendComments(@NotNull List<Element> elements, @NotNull List<String> commentIds) {
        elements.addAll(commentIds.stream().map(id -> new Element("span").attr("id", "polarion-comment:%s".formatted(id))).toList());
    }

    public String removeComments(String content) {
        return replaceMarkers(content, commentId -> "");
    }

    /**
     * Points the markers which anchor a comment to a piece of text at the copies of those comments. A marker naming
     * a comment which was not copied is removed: it would otherwise anchor to a comment the copy does not have.
     *
     * @param idMapping ID of a copied comment mapped to the ID of its copy
     */
    public String remapComments(@NotNull String content, @NotNull Map<String, String> idMapping) {
        return replaceMarkers(content, commentId -> {
            String newId = idMapping.get(commentId);
            return newId == null ? "" : POLARION_COMMENT.formatted(newId);
        });
    }

    /**
     * Replaces each comment marker in one pass over the content. Spans which are not comment markers stay as they are.
     *
     * @param replacement gives the text which takes the place of the marker with the given comment ID
     */
    private String replaceMarkers(@NotNull String content, @NotNull UnaryOperator<String> replacement) {
        return SPAN_PATTERN.matcher(content).replaceAll(span -> {
            String commentId = markerId(span);
            return Matcher.quoteReplacement(commentId == null ? span.group() : replacement.apply(commentId));
        });
    }

    /**
     * @return ID of the comment the matched span is a marker of, {@code null} if it is no comment marker
     */
    private @Nullable String markerId(@NotNull MatchResult span) {
        Matcher markerId = MARKER_ID_PATTERN.matcher(span.group("tag"));
        return markerId.find() ? markerId.group("id") : null;
    }

    public Element removeComments(Element element) {
        List<Element> toRemove = new ArrayList<>();
        for (Element child : element.children()) {
            if ("span".equals(child.tagName()) && COMMENT_MARKER_ID_PATTERN.matcher(child.id()).matches()) {
                toRemove.add(child);
            } else {
                removeComments(child);
            }
        }
        toRemove.forEach(Element::remove);
        return element;
    }

    /**
     * Copies comment markers from source HTML into target HTML using surrounding context matching.
     * For each marker in the source, captures surrounding text context and finds the matching position
     * in the target to insert the corresponding marker. Falls back to appending at the end if no match is found.
     * <p>
     * The context is taken from the source without any of its markers: the target has none, and a context window
     * which cut a neighboring marker in two would match nothing.
     */
    public String copyCommentMarkers(@NotNull String sourceHtmlWithMarkers, @NotNull String targetHtmlWithoutMarkers, @NotNull Map<String, String> oldToNewIdMap) {
        StringBuilder cleanSource = new StringBuilder();
        List<SourceMarker> sourceMarkers = new ArrayList<>();
        Matcher matcher = SPAN_PATTERN.matcher(sourceHtmlWithMarkers);
        int copiedUpTo = 0;
        while (matcher.find()) {
            String commentId = markerId(matcher);
            if (commentId != null) {
                cleanSource.append(sourceHtmlWithMarkers, copiedUpTo, matcher.start());
                copiedUpTo = matcher.end();
                sourceMarkers.add(new SourceMarker(commentId, cleanSource.length()));
            }
        }
        cleanSource.append(sourceHtmlWithMarkers, copiedUpTo, sourceHtmlWithMarkers.length());

        List<MarkerInsert> inserts = new ArrayList<>();
        List<String> fallbackIds = new ArrayList<>();
        for (SourceMarker sourceMarker : sourceMarkers) {
            String newId = oldToNewIdMap.get(sourceMarker.commentId());
            if (newId == null) {
                continue;
            }
            int position = sourceMarker.position();
            String cleanBefore = cleanSource.substring(Math.max(0, position - CONTEXT_WINDOW), position);
            String cleanAfter = cleanSource.substring(position, Math.min(cleanSource.length(), position + CONTEXT_WINDOW));

            int insertPosition = findInsertPosition(targetHtmlWithoutMarkers, cleanBefore, cleanAfter);
            if (insertPosition >= 0) {
                inserts.add(new MarkerInsert(insertPosition, inserts.size(), POLARION_COMMENT.formatted(newId)));
            } else if (cleanBefore.isEmpty() && cleanAfter.isEmpty()) {
                fallbackIds.add(newId);
            }
        }

        return applyInserts(targetHtmlWithoutMarkers, inserts, fallbackIds);
    }

    /**
     * Finds the insert position in the target HTML by matching surrounding context.
     * Tries both contexts combined first, then falls back to individual context matching. When both contexts exist
     * but not together, one side may name a source work item, which the target names differently: then a single
     * context is taken only where it occurs exactly once in the target.
     *
     * @return insert position, or -1 if no match found
     */
    private int findInsertPosition(@NotNull String target, @NotNull String cleanBefore, @NotNull String cleanAfter) {
        if (!cleanBefore.isEmpty() && !cleanAfter.isEmpty()) {
            int idx = target.indexOf(cleanBefore + cleanAfter);
            if (idx >= 0) {
                return idx + cleanBefore.length();
            }
            idx = indexOfSingleOccurrence(target, cleanAfter);
            if (idx >= 0) {
                return idx;
            }
            idx = indexOfSingleOccurrence(target, cleanBefore);
            if (idx >= 0) {
                return idx + cleanBefore.length();
            }
        } else if (!cleanAfter.isEmpty()) {
            int idx = target.indexOf(cleanAfter);
            if (idx >= 0) {
                return idx;
            }
        } else if (!cleanBefore.isEmpty()) {
            int idx = target.indexOf(cleanBefore);
            if (idx >= 0) {
                return idx + cleanBefore.length();
            }
        }
        return -1;
    }

    private int indexOfSingleOccurrence(@NotNull String target, @NotNull String text) {
        int idx = target.indexOf(text);
        return idx >= 0 && target.indexOf(text, idx + 1) < 0 ? idx : -1;
    }

    /**
     * Inserts from the end of the target backwards, so that a position is not shifted by an insert before it.
     * Markers which go to the same position keep their order in the source.
     */
    private String applyInserts(@NotNull String target, @NotNull List<MarkerInsert> inserts, @NotNull List<String> fallbackIds) {
        inserts.sort(Comparator.comparingInt(MarkerInsert::position).thenComparingInt(MarkerInsert::order).reversed());
        StringBuilder result = new StringBuilder(target);
        for (MarkerInsert insert : inserts) {
            result.insert(insert.position(), insert.marker());
        }
        return appendComments(result.toString(), fallbackIds);
    }

    /**
     * @param position where the marker stood in the source text without any markers
     */
    private record SourceMarker(String commentId, int position) {
    }

    private record MarkerInsert(int position, int order, String marker) {
    }

}
