package ch.sbb.polarion.extension.diff_tool.service.handler;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPairDiffParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.util.DiffModelCachedResource;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.subterra.base.data.model.ICustomField;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

@Getter
public class DiffContext {
    public IModule docA;
    public WorkItem workItemA;
    public WorkItem.Field fieldA;
    public ICustomField docFieldA;
    public IModule docB;
    public WorkItem workItemB;
    public WorkItem.Field fieldB;
    public ICustomField docFieldB;
    public final PolarionService polarionService;
    public String pairedWorkItemsLinkRole;
    public boolean pairedWorkItemsDiffer;
    public final String leftProjectId;
    public DiffModel diffModel;

    @Accessors(fluent = true)
    @Getter
    @Setter
    private boolean reverseStyles;

    public DiffContext(@NotNull IModule docA, @NotNull IModule docB, @NotNull WorkItem.Field fieldA, @NotNull WorkItem.Field fieldB, @NotNull String fieldId, @NotNull String leftProjectId, @NotNull PolarionService polarionService) {

        this.docA = docA;
        this.fieldA = fieldA;
        this.docFieldA = docA.getCustomFieldPrototype(fieldId);

        this.docB = docB;
        this.fieldB = fieldB;
        this.docFieldB = docB.getCustomFieldPrototype(fieldId);

        this.leftProjectId = leftProjectId;
        this.polarionService = polarionService;
    }

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

    public boolean isFieldsDiff() {
        return pairedWorkItemsLinkRole == null;
    }
}
