package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.generic.util.HtmlUtils;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffLifecycleHandler;
import com.polarion.alm.tracker.model.IWorkItem;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor
public class LinksHandler implements DiffLifecycleHandler {
    private static final String DATA_PAIRED_ITEM_ID = "data-paired-item-id";
    private static final String SPAN_START = "<span";
    private static final String SPAN_END = "</span>";

    private final Map<String, String> placeholdersMapping = new HashMap<>();

    @Override
    public @NotNull Pair<String, String> preProcess(@NotNull Pair<String, String> initialPair, @NotNull DiffContext context) {
        String left = initialPair.getLeft();
        String right = initialPair.getRight();
        if (!context.pairedWorkItemsDiffer && context.workItemA != null && context.workItemB != null
                && !context.workItemA.sameDocument(context.workItemB)) { // Pairs of WIs from same document should be compared as is
            left = appendPairedWorkItemId(left, context.workItemA.getProjectId(), context.workItemB.getProjectId(), context);
            right = appendPairedWorkItemId(right, context.workItemB.getProjectId(), context.workItemA.getProjectId(), context);

            left = replaceItemsContentByPlaceholders(left);
            right = replaceItemsContentByPlaceholders(right);
        }

        return Pair.of(left, right);
    }

    String appendPairedWorkItemId(String html, @NotNull String sourceProjectId, @NotNull String targetProjectId, @NotNull DiffContext context) {
        Pattern pattern = Pattern.compile("(?<match><span class=\"polarion-rte-link\" data-type=\"workItem\"([^>]+?data-scope=\"(?<workItemProjectId>[^\"]+?)\")*[^>]+?data-item-id=\"(?<workItemId>[^\"]+?)\"" +
                "([^>]+?data-revision=\"(?<workItemRevision>[^\"]+?)\")*)");
        Matcher matcher = pattern.matcher(html);

        StringBuilder buf = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group("match");
            String workItemProjectId = StringUtils.defaultString(matcher.group("workItemProjectId"), sourceProjectId); //data-scope is rendered in case of another project reference
            String workItemId = matcher.group("workItemId");
            String revision = matcher.group("workItemRevision");
            IWorkItem workItem = null;
            try {
                workItem = context.polarionService.getWorkItem(workItemProjectId, workItemId, revision);
            } catch (Exception e) {
                context.addIssue(e.getMessage());
            }
            IWorkItem pairedWorkItem = workItem != null ? context.polarionService.getPairedWorkItems(workItem, targetProjectId, context.pairedWorkItemsLinkRole).stream().findFirst().orElse(null) : null;
            if (pairedWorkItem != null) {
                matcher.appendReplacement(buf, String.format("%s %s=\"%s\"", match, DATA_PAIRED_ITEM_ID, pairedWorkItem.getId()));
            } else {
                matcher.appendReplacement(buf, String.format("%s", match));
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
}
