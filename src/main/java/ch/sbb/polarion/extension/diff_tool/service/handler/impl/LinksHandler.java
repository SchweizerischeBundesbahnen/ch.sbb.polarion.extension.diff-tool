package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffLifecycleHandler;
import ch.sbb.polarion.extension.generic.util.HtmlUtils;
import com.polarion.alm.tracker.model.IWorkItem;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor
public class LinksHandler implements DiffLifecycleHandler {

    /*
     * Link must have specific class attribute, data-type="workItem" and data-item-id which contains work item ID.
     * Note that all those attributes may come in random order.
     */
    public static final String LINK_REGEX = "<span\\s+" +
            "(?=[^>]*class=\"polarion-rte-link\")" +
            "(?=[^>]*data-type=\"workItem\")" +
            "(?=[^>]*data-item-id=\"(?<workItemId>[^\"]+)\")" +
            "[^>]*?>";

    // Optional attributes extracted separately to reduce main regex complexity
    public static final Pattern DATA_SCOPE_PATTERN = Pattern.compile("data-scope=\"([^\"]+)\"");
    public static final Pattern DATA_REVISION_PATTERN = Pattern.compile("data-revision=\"([^\"]+)\"");

    private static final String DATA_PAIRED_ITEM_ID = "data-paired-item-id";
    private static final String SPAN_START = "<span";
    private static final String SPAN_END = "</span>";

    private final Map<String, String> placeholdersMapping = new HashMap<>();

    @Override
    public @NotNull Pair<String, String> preProcess(@NotNull Pair<String, String> initialPair, @NotNull DiffContext context) {
        String left = initialPair.getLeft();
        String right = initialPair.getRight();
        if (!context.isPairedWorkItemsDiffer() && context.getWorkItemA() != null && context.getWorkItemB() != null
                && !context.getWorkItemA().sameDocument(context.getWorkItemB())) { // Pairs of WIs from same document should be compared as is
            left = appendPairedWorkItemId(left, context.getWorkItemA().getProjectId(), context.getWorkItemB().getProjectId(), context);
            right = appendPairedWorkItemId(right, context.getWorkItemB().getProjectId(), context.getWorkItemA().getProjectId(), context);

            left = replaceItemsContentByPlaceholders(left);
            right = replaceItemsContentByPlaceholders(right);
        }

        return Pair.of(left, right);
    }

    String appendPairedWorkItemId(String html, @NotNull String sourceProjectId, @NotNull String targetProjectId, @NotNull DiffContext context) {
        Pattern pattern = Pattern.compile(LINK_REGEX);
        Matcher matcher = pattern.matcher(html);

        StringBuilder buf = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group();
            String workItemId = matcher.group("workItemId");

            // Extract optional attributes from the matched span tag
            String projectId = extractAttribute(DATA_SCOPE_PATTERN, match, sourceProjectId);
            String revision = extractAttribute(DATA_REVISION_PATTERN, match, null);
            IWorkItem workItem = null;
            try {
                workItem = context.getPolarionService().getWorkItem(projectId, workItemId, revision);
            } catch (Exception e) {
                context.addIssue(e.getMessage());
            }
            IWorkItem pairedWorkItem = workItem != null ? context.getPolarionService().getPairedWorkItems(workItem, targetProjectId, context.getPairedWorkItemsLinkRole()).stream().findFirst().orElse(null) : null;
            if (pairedWorkItem != null) {
                matcher.appendReplacement(buf, match.replace(">", " %s=\"%s\">".formatted(DATA_PAIRED_ITEM_ID, pairedWorkItem.getId())));
            }
        }
        matcher.appendTail(buf);
        return buf.toString();
    }

    String replaceItemsContentByPlaceholders(@NotNull String html) {
        StringBuilder result = new StringBuilder();
        int pairedItemId = html.indexOf(DATA_PAIRED_ITEM_ID);
        int itemStart = pairedItemId == -1 ? -1 : html.substring(0, pairedItemId).lastIndexOf(SPAN_START);
        int itemEnd = itemStart == -1 ? -1 : HtmlUtils.getEnding(html, itemStart, SPAN_START, SPAN_END);
        if (itemStart >= 0 && itemEnd > 0) {
            result.append(html, 0, itemStart);
        } else {
            return html;
        }

        while (itemStart >= 0 && itemEnd > 0) {
            result.append(replaceItemContentByPlaceholder(html.substring(itemStart, Math.min(itemEnd, html.length()))));

            pairedItemId = html.indexOf(DATA_PAIRED_ITEM_ID, itemEnd);
            itemStart = pairedItemId == -1 ? -1 : html.substring(0, pairedItemId).lastIndexOf(SPAN_START);
            if (itemEnd < (html.length() - 1)) {
                result.append(html, itemEnd, itemStart < 0 ? html.length() : itemStart);
            }
            itemEnd = HtmlUtils.getEnding(html, itemStart, SPAN_START, SPAN_END);
        }

        return result.toString();
    }

    String replaceItemContentByPlaceholder(@NotNull String itemHtml) {
        if (!itemHtml.contains("data-option-id=\"custom\"")) {
            String itemContent = itemHtml.substring(itemHtml.indexOf(">") + 1, itemHtml.length() - SPAN_END.length());
            String placeholder = UUID.randomUUID().toString().replace("-", "");
            placeholdersMapping.put(placeholder, itemContent);
            return itemHtml.replace(itemContent, placeholder);
        } else {
            return itemHtml;
        }
    }

    @Override
    public @NotNull String postProcess(@NotNull String diff, @NotNull DiffContext context) {
        String result = diff;
        for (Map.Entry<String, String> entry : placeholdersMapping.entrySet()) {
            if (result.contains(entry.getKey())) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public static String extractAttribute(Pattern pattern, String text, String defaultValue) {
        Matcher attrMatcher = pattern.matcher(text);
        return attrMatcher.find() ? attrMatcher.group(1) : defaultValue;
    }
}
