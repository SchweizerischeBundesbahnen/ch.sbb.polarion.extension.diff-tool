package ch.sbb.polarion.extension.diff_tool.velocity;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DetachedWorkItemsPairDiffParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ProjectWorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.StringsDiff;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPairDiff;
import ch.sbb.polarion.extension.diff_tool.service.DiffService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.util.DiffToolUtils;
import com.polarion.alm.tracker.model.IWorkItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

@SuppressWarnings("unused")
public class VelocityDiffTool {

    private static class DiffServiceHolder {
        private static final DiffService INSTANCE = new DiffService(new PolarionService());
    }

    @NotNull
    public StringsDiff diffText(@Nullable String text1, @Nullable String text2) {
        return DiffToolUtils.diffText(text1, text2);
    }

    @NotNull
    public StringsDiff diffHtml(@Nullable String html1, @Nullable String html2) {
        return DiffToolUtils.diffHtml(html1, html2);
    }

    @NotNull
    @SuppressWarnings("java:S3252") // false positive
    public WorkItemsPairDiff diffWorkItems(@NotNull String leftProjectId,
                                           @Nullable IWorkItem workItemA,
                                           @Nullable IWorkItem workItemB,
                                           @NotNull String configName,
                                           @Nullable String linkRole) {
        DetachedWorkItemsPairDiffParams params = DetachedWorkItemsPairDiffParams.builder()
                .leftProjectId(leftProjectId)
                .leftWorkItem(workItemA != null ? toProjectWorkItem(workItemA) : null)
                .rightWorkItem(workItemB != null ? toProjectWorkItem(workItemB) : null)
                .configName(configName)
                .pairedWorkItemsLinkRole(linkRole)
                .build();

        return DiffServiceHolder.INSTANCE.getDetachedWorkItemsPairDiff(params);
    }

    @NotNull
    @VisibleForTesting
    ProjectWorkItem toProjectWorkItem(@NotNull IWorkItem workItem) {
        String revision = workItem.getRevision();
        if (revision == null) {
            revision = workItem.getLastRevision();
        }

        return new ProjectWorkItem(
                workItem.getId(),
                workItem.getProjectId(),
                revision);
    }
}
