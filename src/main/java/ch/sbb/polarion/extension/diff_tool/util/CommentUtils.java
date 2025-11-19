package ch.sbb.polarion.extension.diff_tool.util;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import com.polarion.core.util.StringUtils;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class CommentUtils {

    private static final String COMMENT_REGEX = "<span id=[\"']polarion-comment:(?<id>\\d+)[\"'][^>]*>(?:</span>)?";

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

}
