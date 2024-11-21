package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.report.MergeReport;
import ch.sbb.polarion.extension.diff_tool.report.MergeReportEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import com.google.common.collect.Streams;
import com.polarion.alm.shared.api.model.document.DocumentReference;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MergeContext {
    final DocumentIdentifier leftDocumentIdentifier;
    final DocumentIdentifier rightDocumentIdentifier;
    final IModule leftModule;
    final IModule rightModule;
    final DocumentReference leftDocumentReference;
    final DocumentReference rightDocumentReference;
    @Getter
    final MergeDirection direction;
    @Getter
    final MergeReport mergeReport = new MergeReport();
    final String linkRole;
    final ILinkRoleOpt linkRoleObject;
    @Getter
    final DiffModel diffModel;
    @Getter
    final List<ModifiedModule> modifiedModules = new ArrayList<>();
    @Getter
    final boolean allowReferencedWorkItemMerge;

    public MergeContext(@NotNull PolarionService polarionService, @NotNull DocumentIdentifier leftDocumentIdentifier, @NotNull DocumentIdentifier rightDocumentIdentifier,
                        @NotNull MergeDirection direction, @NotNull String linkRole, DiffModel diffModel, boolean allowReferencedWorkItemMerge) {
        this.leftDocumentIdentifier = leftDocumentIdentifier;
        this.rightDocumentIdentifier = rightDocumentIdentifier;
        this.direction = direction;
        this.linkRole = linkRole;
        this.diffModel = diffModel;

        leftModule = polarionService.getModule(leftDocumentIdentifier);
        rightModule = polarionService.getModule(rightDocumentIdentifier);

        linkRoleObject = polarionService.getLinkRoleById(linkRole, getTargetModule().getProject());
        if (linkRoleObject == null) {
            throw new IllegalArgumentException(String.format("No link role could be found by ID '%s'", linkRole));
        }

        leftDocumentReference = DocumentReference.fromModuleLocation(leftDocumentIdentifier.getProjectId(), leftModule.getModuleLocation().getLocationPath(),
                leftDocumentIdentifier.getRevision() == null ? leftModule.getLastRevision() : leftDocumentIdentifier.getRevision());
        rightDocumentReference = DocumentReference.fromModuleLocation(rightDocumentIdentifier.getProjectId(), rightModule.getModuleLocation().getLocationPath(),
                rightDocumentIdentifier.getRevision() == null ? rightModule.getLastRevision() : rightDocumentIdentifier.getRevision());
        this.allowReferencedWorkItemMerge = allowReferencedWorkItemMerge;
    }

    public IModule getSourceModule() {
        return direction == MergeDirection.LEFT_TO_RIGHT ? leftModule : rightModule;
    }

    public IModule getTargetModule() {
        return direction == MergeDirection.LEFT_TO_RIGHT ? rightModule : leftModule;
    }

    public DocumentIdentifier getTargetDocumentIdentifier() {
        return direction == MergeDirection.LEFT_TO_RIGHT ? rightDocumentIdentifier : leftDocumentIdentifier;
    }

    public DocumentReference getSourceDocumentReference() {
        return direction == MergeDirection.LEFT_TO_RIGHT ? leftDocumentReference : rightDocumentReference;
    }

    public DocumentReference getTargetDocumentReference() {
        return direction == MergeDirection.LEFT_TO_RIGHT ? rightDocumentReference : leftDocumentReference;
    }

    public WorkItem getSourceWorkItem(WorkItemsPair pair) {
        return direction == MergeDirection.LEFT_TO_RIGHT ? pair.getLeftWorkItem() : pair.getRightWorkItem();
    }

    public WorkItem getTargetWorkItem(WorkItemsPair pair) {
        return direction == MergeDirection.LEFT_TO_RIGHT ? pair.getRightWorkItem() : pair.getLeftWorkItem();
    }

    public void bindCounterpartItem(WorkItemsPair pairWithMissingCounterpart, WorkItem newCounterpartItem) {
        if (pairWithMissingCounterpart.getLeftWorkItem() == null) {
            pairWithMissingCounterpart.setLeftWorkItem(newCounterpartItem);
        } else {
            pairWithMissingCounterpart.setRightWorkItem(newCounterpartItem);
        }
    }

    public boolean parentsPaired(@NotNull IModule.IStructureNode sourceNode, @NotNull IModule.IStructureNode targetNode) {
        return (sourceNode.getParent() == null && targetNode.getParent() == null)
                || linked(sourceNode.getParent().getWorkItem(), targetNode.getParent().getWorkItem());
    }

    public IWorkItem getPairedWorkItem(@NotNull IWorkItem workItem, @NotNull IModule oppositeDocument) {
        for (IWorkItem oppositeWorkItem : oppositeDocument.getAllWorkItems()) {
            if (linked(workItem, oppositeWorkItem)) {
                return oppositeWorkItem;
            }
        }
        return null;
    }

    public void reportEntry(@NotNull MergeReport.OperationResultType operationResultType, @NotNull WorkItemsPair workItemsPair, @NotNull String description) {
        mergeReport.addEntry(new MergeReportEntry(operationResultType, workItemsPair, description));
    }

    private boolean linked(IWorkItem workItemA, IWorkItem workItemB) {
        if (workItemA == null || workItemB == null) {
            return false;
        }
        AtomicBoolean linked = new AtomicBoolean(false);
        Streams.concat(workItemA.getLinkedWorkItemsStructsDirect().stream(), workItemA.getLinkedWorkItemsStructsBack().stream()).forEach(linkedWorkItemStruct -> {
            if (Objects.equals(linkedWorkItemStruct.getLinkedItem().getId(), workItemB.getId())
                    && Objects.equals(linkRole, linkedWorkItemStruct.getLinkRole().getId())) {
                linked.set(true);
            }
        });
        return linked.get();
    }

    @Builder
    @Getter
    public static class ModifiedModule {
        @NotNull private final IModule module;
        @NotNull private final IWorkItem workItem;
        @NotNull private final IModule.IStructureNode parentNode;
        private final int destinationIndex;
    }
}
