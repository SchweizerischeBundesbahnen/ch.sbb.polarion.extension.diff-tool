package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.util.DiffToolUtils;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffLifecycleHandler;
import com.polarion.alm.tracker.model.IWorkItem;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.polarion.alm.tracker.model.IWorkItem.KEY_LINKED_WORK_ITEMS;

/**
 * Provides possibility to display linked work item as non-changed if the diff is only in its ID.
 */
@NoArgsConstructor
public class LinkedWorkItemsHandler implements DiffLifecycleHandler {

    private final Map<String, String> placeholdersMapping = new HashMap<>();
    private final Map<String, String> linkToPlaceholderMapping = new HashMap<>();

    @Override
    public @NotNull Pair<String, String> preProcess(@NotNull Pair<String, String> initialPair, @NotNull DiffContext context) {
        if (isInappropriateCaseForHandler(context)) {
            return initialPair;
        }

        String left = initialPair.getLeft();
        String right = initialPair.getRight();
        if (context.workItemA != null && context.workItemB != null) {
            left = replacePairedLinks(left, context.workItemB.getProjectId(), context, false);
            right = replacePairedLinks(right, context.workItemA.getProjectId(), context, true);
        }

        return Pair.of(left, right);
    }

    @VisibleForTesting
    String replacePairedLinks(String html, @NotNull String targetProjectId, @NotNull DiffContext context, boolean rightItem) {
        Pattern pattern = Pattern.compile("(?s)(?<match>(<img[^>]*?title=\"Revision[^>]+? (?<workItemRevision>\\d+)\"[^>]*?/>)" +
                "*<a[^>]+?href=\"/polarion/#/project/(?<projectId>[^/]+?)/[^?]+?\\?[^>]*?selection=(?<workItemId>[^\"]+?)\"" +
                ".*?(?<titleSpan><span style=\"overflow-wrap: anywhere.*?</span>)" +
                ".*?</a>)");//NOSONAR
        Matcher matcher = pattern.matcher(html);

        StringBuilder buf = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group("match");
            String workItemProjectId = matcher.group("projectId");
            String workItemId = matcher.group("workItemId");
            String revision = matcher.group("workItemRevision");
            IWorkItem workItem = null;
            try {
                workItem = context.polarionService.getWorkItem(workItemProjectId, workItemId, revision);
            } catch (Exception e) {
                context.addIssue(e.getMessage());
            }
            WorkItem oppositeModuleWorkItem = rightItem ? context.workItemA : context.workItemB;
            //there may be several paired WI with the same role, so we have to peek the one which is bound to the opposite module
            IWorkItem pairedWorkItem = workItem != null ? getPairedWorkItem(context, workItem, oppositeModuleWorkItem, targetProjectId) : null;
            if (pairedWorkItem != null) {
                //----- experimental feature: compare titles. Note that this requires style reversion.
                String titleSpan = matcher.group("titleSpan");
                if (titleSpan != null && rightItem && !Objects.equals(workItem.getTitle(), pairedWorkItem.getTitle())) {
                    String titleDiff = DiffToolUtils.computeDiff(Pair.of(pairedWorkItem.getTitle(), workItem.getTitle()));
                    String reversedStyleDiff = new ReverseStylesHandler().postProcess(titleDiff, context);
                    match = match.replace(titleSpan, "<span style=\"overflow-wrap: anywhere\">%s</span>".formatted(reversedStyleDiff));
                }
                //-----

                String link = Stream.of(workItemId, pairedWorkItem.getId()).sorted().collect(Collectors.joining("x"));
                matcher.appendReplacement(buf, replaceLinkContentByPlaceholder(match, link, rightItem));
            } else {
                matcher.appendReplacement(buf, String.format("%s", match));
            }
        }
        matcher.appendTail(buf);
        return buf.toString();
    }

    private IWorkItem getPairedWorkItem(@NotNull DiffContext context, @NotNull IWorkItem workItem, @Nullable WorkItem oppositeModuleWorkItem, @NotNull String targetProjectId) {
        if (oppositeModuleWorkItem != null) {
            return context.polarionService.getPairedWorkItems(workItem, targetProjectId, context.pairedWorkItemsLinkRole)
                    .stream().filter(wi -> wi.getModule() != null && oppositeModuleWorkItem.getModule() != null &&
                            wi.getModule().getModuleLocation().removeRevision().equals(oppositeModuleWorkItem.getModule().getModuleLocation().removeRevision()))
                    .findFirst().orElse(null);
        } else {
            return null;
        }

    }

    @Override
    public @NotNull String postProcess(@NotNull String diff, @NotNull DiffContext context) {
        if (isInappropriateCaseForHandler(context)) {
            return diff;
        }

        String result = diff;
        for (Map.Entry<String, String> entry : placeholdersMapping.entrySet()) {
            if (result.contains(entry.getKey())) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private String replaceLinkContentByPlaceholder(@NotNull String itemHtml, String link, boolean rememberReplacement) {
        String placeholder = linkToPlaceholderMapping.getOrDefault(link, UUID.randomUUID().toString().replace("-", ""));
        linkToPlaceholderMapping.put(link, placeholder);
        if (rememberReplacement || placeholdersMapping.get(placeholder) == null) {
            placeholdersMapping.put(placeholder, itemHtml);
        }
        return itemHtml.replace(itemHtml, placeholder);
    }

    private boolean isInappropriateCaseForHandler(DiffContext context) {
        return context.pairedWorkItemsDiffer || !Objects.equals(context.fieldA.getId(), KEY_LINKED_WORK_ITEMS) || !Objects.equals(context.fieldB.getId(), KEY_LINKED_WORK_ITEMS);
    }
}
