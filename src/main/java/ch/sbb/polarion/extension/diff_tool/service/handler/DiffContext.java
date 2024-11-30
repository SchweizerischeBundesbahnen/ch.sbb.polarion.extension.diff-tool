package ch.sbb.polarion.extension.diff_tool.service.handler;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentWorkItemsPairDiffParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPairDiffParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.util.DiffModelCachedResource;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class DiffContext {
    @Nullable
    public final WorkItem workItemA;
    @NotNull
    public final WorkItem.Field fieldA;
    @Nullable
    public final WorkItem workItemB;
    @NotNull
    public final WorkItem.Field fieldB;
    @NotNull
    public final PolarionService polarionService;
    @NotNull
    public final String pairedWorkItemsLinkRole;
    public final boolean pairedWorkItemsDiffer;
    public final String leftProjectId;
    public final DiffModel diffModel;

    @Accessors(fluent = true)
    @Getter
    @Setter
    private boolean reverseStyles;

    public DiffContext(@Nullable WorkItem workItemA, @Nullable WorkItem workItemB, @NotNull String fieldId,
                       @NotNull WorkItemsPairDiffParams<?> workItemsPairDiffParams, @NotNull PolarionService polarionService) {
        WorkItem.Field stub = WorkItem.Field.builder().html("").build();

        this.workItemA = workItemA;
        this.fieldA = Optional.ofNullable(workItemA).map(w -> Objects.requireNonNullElse(w.getField(fieldId), stub)).orElse(stub);

        this.workItemB = workItemB;
        this.fieldB = Optional.ofNullable(workItemB).map(w -> Objects.requireNonNullElse(w.getField(fieldId), stub)).orElse(stub);

        this.polarionService = polarionService;

        this.pairedWorkItemsLinkRole = workItemsPairDiffParams.getPairedWorkItemsLinkRole();
        this.pairedWorkItemsDiffer = workItemsPairDiffParams.isPairedWorkItemsDiffer();

        leftProjectId = workItemsPairDiffParams.getLeftProjectId();
        diffModel = DiffModelCachedResource.get(workItemsPairDiffParams.getLeftProjectId(), workItemsPairDiffParams.getConfigName(), workItemsPairDiffParams.getConfigCacheBucketId());
    }

    public void addIssue(String issue) {
        fieldA.addIssue(issue);
        fieldB.addIssue(issue);
    }
}
