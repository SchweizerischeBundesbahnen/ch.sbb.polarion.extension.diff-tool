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
    private static final Pattern COMMENT_PATTERN = Pattern.compile(COMMENT_REGEX);
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
            String newId = oldToNewIdMap.get(matcher.group("id"));
            if (newId == null) {
                continue;
            }

            String cleanBefore = extractCleanContext(sourceHtmlWithMarkers, Math.max(0, matcher.start() - CONTEXT_WINDOW), matcher.start());
            String cleanAfter = extractCleanContext(sourceHtmlWithMarkers, matcher.end(), Math.min(sourceHtmlWithMarkers.length(), matcher.end() + CONTEXT_WINDOW));
            String newMarker = "<span id=\"polarion-comment:%s\"></span>".formatted(newId);

            int position = findInsertPosition(targetHtmlWithoutMarkers, cleanBefore, cleanAfter);
            if (position >= 0) {
                inserts.add(new MarkerInsert(position, newMarker));
            } else if (cleanBefore.isEmpty() && cleanAfter.isEmpty()) {
                fallbackIds.add(newId);
            }
        }

        return applyInserts(targetHtmlWithoutMarkers, inserts, fallbackIds);
    }

    private String extractCleanContext(@NotNull String source, int from, int to) {
        return COMMENT_PATTERN.matcher(source.substring(from, to)).replaceAll("");
    }

    /**
     * Finds the insert position in the target HTML by matching surrounding context.
     * Tries both contexts combined first, then falls back to individual context matching.
     *
     * @return insert position, or -1 if no match found
     */
    private int findInsertPosition(@NotNull String target, @NotNull String cleanBefore, @NotNull String cleanAfter) {
        if (!cleanBefore.isEmpty() && !cleanAfter.isEmpty()) {
            int idx = target.indexOf(cleanBefore + cleanAfter);
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

    private String applyInserts(@NotNull String target, @NotNull List<MarkerInsert> inserts, @NotNull List<String> fallbackIds) {
        inserts.sort((a, b) -> Integer.compare(b.position, a.position));
        StringBuilder result = new StringBuilder(target);
        for (MarkerInsert insert : inserts) {
            result.insert(insert.position, insert.marker);
        }
        return appendComments(result.toString(), fallbackIds);
    }

    private record MarkerInsert(int position, String marker) {
    }

}
