package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import com.polarion.alm.shared.api.model.document.DocumentReference;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Accessors(chain = true)
public final class DocumentsMergeContext extends SettingsAwareMergeContext implements IPreserveCommentsContext {
    final DocumentIdentifier leftDocumentIdentifier;
    final DocumentIdentifier rightDocumentIdentifier;
    final IModule leftModule;
    final IModule rightModule;
    final DocumentReference leftDocumentReference;
    final DocumentReference rightDocumentReference;
    final ILinkRoleOpt linkRoleObject;
    @Getter
    final List<AffectedModule> affectedModules = new ArrayList<>();
    @Getter
    @Setter
    boolean allowReferencedWorkItemMerge;
    @Getter
    @Setter
    boolean preserveComments;
    @Getter
    @Setter
    boolean copyMissingDocumentAttachments;

    public DocumentsMergeContext(@NotNull PolarionService polarionService, @NotNull DocumentIdentifier leftDocumentIdentifier, @NotNull DocumentIdentifier rightDocumentIdentifier,
                                 @NotNull MergeDirection direction, @NotNull String linkRole, @NotNull DiffModel diffModel) {
        super(direction, linkRole, diffModel);
        this.leftDocumentIdentifier = leftDocumentIdentifier;
        this.rightDocumentIdentifier = rightDocumentIdentifier;

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

    private boolean linked(IWorkItem workItemA, IWorkItem workItemB) {
        if (workItemA == null || workItemB == null) {
            return false;
        }
        AtomicBoolean linked = new AtomicBoolean(false);
        Stream.concat(workItemA.getLinkedWorkItemsStructsDirect().stream(), workItemA.getLinkedWorkItemsStructsBack().stream()).forEach(linkedWorkItemStruct -> {
            if (Objects.equals(linkedWorkItemStruct.getLinkedItem().getId(), workItemB.getId())
                    && Objects.equals(linkRole, linkedWorkItemStruct.getLinkRole().getId())) {
                linked.set(true);
            }
        });
        return linked.get();
    }

    public boolean isAllowedReferencedWorkItemMerge() {
        return Boolean.TRUE.equals(allowReferencedWorkItemMerge);
    }

    @Builder
    @Getter
    public static class AffectedModule {
        @NotNull private final IModule module;
        @NotNull private final IWorkItem workItem;
        @NotNull private final IModule.IStructureNode parentNode;
        private final int destinationIndex;
    }
}
