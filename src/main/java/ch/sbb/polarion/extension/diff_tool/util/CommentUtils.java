package ch.sbb.polarion.extension.diff_tool.util;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import com.polarion.core.util.StringUtils;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@UtilityClass
public class CommentUtils {

    private static final String COMMENT_REGEX = "<span id=[\"']polarion-comment:(?<id>\\d+)[\"'][^>]*>(?:</span>)?";
    private static Pattern COMMENT_PATTERN = Pattern.compile(COMMENT_REGEX);
    private static final int CONTEXT_WINDOW = 50;

    public List<String> extractCommentIds(@Nullable String content) {
        List<String> commentIds = new ArrayList<>();
        RegexMatcher.get(COMMENT_REGEX)
                .processEntry(StringUtils.getEmptyIfNull(content), engine -> commentIds.add(engine.group("id")));
        return commentIds;
    }

    public List<String> extractCommentIds(Element element) {
        List<String> result = new ArrayList<>();
        if ("span".equals(element.tagName()) && element.id().matches("polarion-comment:\\d+")) {
            result.add(element.id().substring("polarion-comment:".length()));
        } else {
            for (Element child : element.children()) {
                result.addAll(extractCommentIds(child));
            }
        }
        return result;
    }

    public String appendComments(@NotNull String content, List<String> commentIds) {
        return content + commentIds.stream().map("<span id=\"polarion-comment:%s\"></span>"::formatted).collect(Collectors.joining());
    }

    public void appendComments(@NotNull List<Element> elements, @NotNull List<String> commentIds) {
        elements.addAll(commentIds.stream().map(id -> new Element("span").attr("id", "polarion-comment:%s".formatted(id))).toList());
    }

    public String removeComments(String content) {
        return RegexMatcher.get(COMMENT_REGEX).removeAll(content);
    }

    public Element removeComments(Element element) {
        List<Element> toRemove = new ArrayList<>();
        for (Element child : element.children()) {
            if ("span".equals(child.tagName()) && child.id().matches("polarion-comment:\\d+")) {
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
     */
    public String copyCommentMarkers(@NotNull String sourceHtmlWithMarkers, @NotNull String targetHtmlWithoutMarkers, @NotNull Map<String, String> oldToNewIdMap) {
        Matcher matcher = COMMENT_PATTERN.matcher(sourceHtmlWithMarkers);

        List<MarkerInsert> inserts = new ArrayList<>();
        List<String> fallbackIds = new ArrayList<>();

        while (matcher.find()) {
            String oldId = matcher.group("id");
            String newId = oldToNewIdMap.get(oldId);
            if (newId == null) {
                continue;
            }

            int markerStart = matcher.start();
            int markerEnd = matcher.end();

            String before = sourceHtmlWithMarkers.substring(Math.max(0, markerStart - CONTEXT_WINDOW), markerStart);
            String after = sourceHtmlWithMarkers.substring(markerEnd, Math.min(sourceHtmlWithMarkers.length(), markerEnd + CONTEXT_WINDOW));

            // Strip any other comment markers from context strings to get clean search patterns
            String cleanBefore = COMMENT_PATTERN.matcher(before).replaceAll("");
            String cleanAfter = COMMENT_PATTERN.matcher(after).replaceAll("");

            String newMarker = "<span id=\"polarion-comment:%s\"></span>".formatted(newId);

            if (!cleanBefore.isEmpty() && !cleanAfter.isEmpty()) {
                String searchPattern = cleanBefore + cleanAfter;
                int idx = targetHtmlWithoutMarkers.indexOf(searchPattern);
                if (idx >= 0) {
                    inserts.add(new MarkerInsert(idx + cleanBefore.length(), newMarker));
                }
            } else if (!cleanAfter.isEmpty()) { // Try matching with just the "after" context
                int idx = targetHtmlWithoutMarkers.indexOf(cleanAfter);
                if (idx >= 0) {
                    inserts.add(new MarkerInsert(idx, newMarker));
                }
            } else if (!cleanBefore.isEmpty()) { // Try matching with just the "before" context
                int idx = targetHtmlWithoutMarkers.indexOf(cleanBefore);
                if (idx >= 0) {
                    inserts.add(new MarkerInsert(idx + cleanBefore.length(), newMarker));
                }
            } else {
                fallbackIds.add(newId);
            }
        }

        // Sort inserts by position descending to avoid offset shifting
        inserts.sort((a, b) -> Integer.compare(b.position, a.position));
        StringBuilder result = new StringBuilder(targetHtmlWithoutMarkers);
        for (MarkerInsert insert : inserts) {
            result.insert(insert.position, insert.marker);
        }

        // Append any markers that couldn't be positioned
        return appendComments(result.toString(), fallbackIds);
    }

    private record MarkerInsert(int position, String marker) {
    }

}
