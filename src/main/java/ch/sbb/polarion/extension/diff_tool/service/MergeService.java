package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.*;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.util.DiffModelCachedResource;
import com.polarion.alm.shared.api.model.document.internal.InternalDocument;
import com.polarion.alm.shared.api.model.document.internal.InternalUpdatableDocument;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalWriteTransaction;
import com.polarion.alm.shared.dle.compare.*;
import com.polarion.alm.shared.dle.compare.merge.DuplicateWorkItemAction;
import com.polarion.alm.tracker.internal.model.IInternalWorkItem;
import com.polarion.alm.tracker.internal.model.LinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.logging.Logger;
import com.polarion.core.util.types.Text;
import com.polarion.platform.persistence.spi.EnumOption;
import com.polarion.subterra.base.data.model.IEnumType;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.IStructType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MergeService {

    private static final CompareOptions MERGE_OPTION = CompareOptions.create().forMerge(true).build();
    private static final Logger log = Logger.getLogger(MergeService.class);
    private final PolarionService polarionService;

    public MergeService(PolarionService polarionService) {
        this.polarionService = polarionService;
    }

    public MergeResult mergeWorkItems(@NotNull DocumentIdentifier documentIdentifier1, @NotNull DocumentIdentifier documentIdentifier2, @NotNull MergeDirection direction,
                                      @NotNull String linkRole, @NotNull List<WorkItemsPair> pairs, @Nullable String configName, @Nullable String configCacheBucketId) {
        // Evict cache of work items of specified documents before merging their work items, as after merge cache can become invalid
        polarionService.evictDocumentsCache(documentIdentifier1, documentIdentifier2);

        DiffModel diffModel = DiffModelCachedResource.get(documentIdentifier1.getProjectId(), configName, configCacheBucketId);
        MergeContext context = new MergeContext(polarionService, documentIdentifier1, documentIdentifier2, direction, linkRole);
        if (!Objects.equals(context.getTargetDocumentIdentifier().getModuleXmlRevision(), context.getTargetModule().getLastRevision())) {
            return MergeResult.builder().success(false).targetModuleHasStructuralChanges(true).build();
        }
        if (!polarionService.userAuthorizedForMerge(context.getTargetModule().getProjectId())) {
            return MergeResult.builder().success(false).mergeNotAuthorized(true).build();
        }
        List<WorkItemsPair> conflicted = new ArrayList<>();
        List<WorkItemsPair> prohibited = new ArrayList<>();
        List<WorkItemsPair> notPaired = new ArrayList<>();
        List<WorkItemsPair> created = new ArrayList<>();
        List<WorkItemsPair> modified = new ArrayList<>();
        List<WorkItemsPair> moved = new ArrayList<>();
        List<WorkItemsPair> notMoved = new ArrayList<>();
        TransactionalExecutor.executeInWriteTransaction(transaction -> {
            for (WorkItemsPair pair : pairs) {
                IWorkItem source = getWorkItem(context.getSourceWorkItem(pair));
                IWorkItem target = getWorkItem(context.getTargetWorkItem(pair));

                if (source != null && target != null) {
                    if (moveAction(pair)) { // Intentionally handled at first place. We first move items, and only then as an additional step merge content if needed
                        if (move(source, target, context)) {
                            target.save();
                            moved.add(pair);
                        } else {
                            notMoved.add(pair);
                        }
                    } else if (context.getTargetModule().getExternalWorkItems().contains(target)) { // merge into external work item
                        // TODO: insert a check if referenced WI in target project to be fixed in this case
                        throw new IllegalArgumentException("Cannot merge into referenced work item: (%s) -> (%s)".formatted(source.getId(), target.getId()));
                    } else {
                        if (!context.getTargetWorkItem(pair).getLastRevision().equals(target.getLastRevision())) {
                            conflicted.add(pair); // Don't allow merging if work item's revision was modified meanwhile
                        } else if (context.getSourceModule().getExternalWorkItems().contains(source)) {
                            prohibited.add(pair); // Don't allow merging referenced work item into included one
                        } else {
                            merge(source, target, diffModel.getDiffFields());
                            modified.add(pair);
                        }
                    }
                } else if (source != null) { // "target" is null in this case, so new item out of "source" to be created
                    if (context.getSourceModule().getExternalWorkItems().contains(source)) {
                        if (insertReferencedWorkItem(source, context) != null) {
                            created.add(pair);
                        } else {
                            notPaired.add(pair);
                        }
                    } else {
                        IWorkItem newWorkItem = copyWorkItemToDocument(source, (InternalWriteTransaction) transaction, context);
                        context.bindCounterpartItem(pair, WorkItem.of(newWorkItem, context.getTargetModule().getOutlineNumberOfWorkitem(newWorkItem), source.getId().equals(newWorkItem.getId())));
                        created.add(pair);
                    }
                } else { // "source" is null in this case, so "target" to be deleted
                    deleteWorkItemFromDocument(context.getTargetDocumentIdentifier(), target);
                }
            }
            reloadModule(context.getTargetModule());
            return null;
        });
        return MergeResult.builder().success(true)
                .createdPairs(created).modifiedPairs(modified).conflictedPairs(conflicted).prohibitedPairs(prohibited).notPaired(notPaired).movedPairs(moved).notMovedPairs(notMoved)
                .targetModuleHasStructuralChanges(!Objects.equals(context.getTargetDocumentIdentifier().getModuleXmlRevision(), context.getTargetModule().getLastRevision()))
                .build();
    }

    private boolean moveAction(@NotNull WorkItemsPair pair) {
        return pair.getLeftWorkItem().getMovedOutlineNumber() != null || pair.getRightWorkItem().getMovedOutlineNumber() != null;
    }

    private boolean move(@NotNull IWorkItem source, @NotNull IWorkItem target, @NotNull MergeContext mergeContext) {
        IModule.IStructureNode sourceNode = mergeContext.getSourceModule().getStructureNodeOfWI(source);
        IModule.IStructureNode targetNode = mergeContext.getTargetModule().getStructureNodeOfWI(target);
        if (sourceNode != null && targetNode != null) {
            if (mergeContext.parentsPaired(sourceNode, targetNode)) { // Moving to a different position within the same parent
                return move(sourceNode, targetNode, targetNode.getParent(), mergeContext);
            } else {
                IWorkItem targetDestinationParentWorkItem = mergeContext.getPairedWorkItem(sourceNode.getParent().getWorkItem(), mergeContext.getTargetModule());
                if (targetDestinationParentWorkItem != null) {
                    return move(sourceNode, targetNode, mergeContext.getTargetModule().getStructureNodeOfWI(targetDestinationParentWorkItem), mergeContext);
                }
                // Otherwise false will be returned, indicating move action is not possible
            }
        }

        return false;
    }

    private boolean move(@NotNull IModule.IStructureNode sourceNode, @NotNull IModule.IStructureNode targetNode,
                         @Nullable IModule.IStructureNode targetDestinationParentNode, @NotNull MergeContext mergeContext) {
        String targetDestinationNumber = getTargetDestinationNumber(sourceNode, targetDestinationParentNode);
        IModule.IStructureNode targetDestinationNode = getNodeByOutlineNumber(mergeContext.getTargetModule(), targetDestinationNumber);
        if (targetDestinationNode != null) {
            IModule.IStructureNode parent = targetDestinationNode.getParent();
            int destinationIndex = parent.getChildren().indexOf(targetDestinationNode);

            if (parent.getChildren().indexOf(targetNode) == 0 && parent.getChildren().size() > 1) {
                // Method 'addChild' has an issue moving from position 0 to any index below. So, as a workaround, we:
                // 1) move the second item to position 0 from below
                // 2) move first item to the required position (since it is now at position 1)
                parent.addChild(parent.getChildren().get(1), 0);
                if (destinationIndex > 1) {
                    parent.addChild(targetNode, destinationIndex);
                }
            } else {
                parent.addChild(targetNode, destinationIndex);
            }
            return true;
        } else if (targetDestinationParentNode != null) {
            targetDestinationParentNode.moveNodeAsLastChild(targetNode);
            return true;
        }
        return false;
    }

    private String getTargetDestinationNumber(IModule.IStructureNode sourceNode, IModule.IStructureNode targetDestinationParentNode) {
        String sourceNumberWithinParent = (sourceNode.getParent() == null || sourceNode.getParent().getOutlineNumber() == null)
                ? sourceNode.getOutlineNumber() : sourceNode.getOutlineNumber().substring(sourceNode.getParent().getOutlineNumber().length());
        return  (targetDestinationParentNode == null || targetDestinationParentNode.getOutlineNumber() == null)
                ? sourceNumberWithinParent : targetDestinationParentNode.getOutlineNumber() + sourceNumberWithinParent;
    }

    private IModule.IStructureNode getNodeByOutlineNumber(IModule module, String outlineNumber) {
        for (IWorkItem workItem : module.getAllWorkItems()) {
            if (outlineNumber.equals(module.getOutlineNumberOfWorkitem(workItem))) {
                return module.getStructureNodeOfWI(workItem);
            }
        }
        return null;
    }

    /**
     * Solution based on internal Polarion classes usage.
     */
    private IWorkItem copyWorkItemToDocument(IWorkItem sourceWorkItem, @NotNull InternalWriteTransaction transaction, MergeContext context) {
        // taken from DleWorkItemsCompareOther#initializeOutside
        InternalDocument leftDocument = (InternalDocument) context.getSourceDocumentReference().get(transaction);
        InternalDocument rightDocument = (InternalDocument) context.getTargetDocumentReference().get(transaction);

        DleWorkitemsMatcher leftMatcher = new DleWorkitemsMatcher(leftDocument, null, transaction);
        DleWorkitemsMatcher rightMatcher = new DleWorkitemsMatcher(rightDocument, null, transaction);
        DleWorkItemsComparator comparator = new DleWorkItemsComparator(transaction, leftMatcher, rightMatcher, MERGE_OPTION);

        // taken from DleWIsMergeSaveRequest#executeActions
        InternalUpdatableDocument targetDocument = (InternalUpdatableDocument) (context.getTargetDocumentReference()).getUpdatable(transaction);
        DleWIsMergeActionExecuter executer = new DleWIsMergeActionExecuter(targetDocument, comparator.getLeftMergedPartsOrder(), comparator.getRightMergedPartsOrder());

        WorkItemReference workItemReference = new WorkItemReference(sourceWorkItem.getProjectId(), sourceWorkItem.getId(), sourceWorkItem.getRevision(), null);
        DleWIsMergeAction action = new DuplicateWorkItemAction(workItemReference);
        action.execute(executer);
        executer.finish();
        // end of internal polarion code

        if (!executer.workItemsCreatedByDuplicateAction.isEmpty()) {
            // by default newly created work item linked using 'branched_from' link role, so we have to
            // remove this default role and add a link with the role which was used for comparing documents
            WorkItemReference createdWorkItemReference = executer.workItemsCreatedByDuplicateAction.get(0);
            IListType listType = (IListType) sourceWorkItem.getPrototype().getKeyType("linkedWorkItems");
            IStructType structType = (IStructType) listType.getItemType();
            String roleEnumId = ((IEnumType) structType.getKeyType("role")).getEnumerationId();
            IWorkItem createdWorkItem = polarionService.getWorkItem(createdWorkItemReference.projectId(), createdWorkItemReference.id());
            createdWorkItem.removeLinkedItem(sourceWorkItem, new EnumOption(roleEnumId, "branched_from"));
            // Link is added either to source or target item depending on merge direction
            if (context.getDirection() == MergeDirection.RIGHT_TO_LEFT) {
                sourceWorkItem.addLinkedItem(createdWorkItem, new LinkRoleOpt(new EnumOption(roleEnumId, context.linkRole)), null, false);
                sourceWorkItem.save();
            } else {
                createdWorkItem.addLinkedItem(sourceWorkItem, new LinkRoleOpt(new EnumOption(roleEnumId, context.linkRole)), null, false);
            }
            createdWorkItem.save();

            reloadModule(context.getTargetModule());
            move(sourceWorkItem, createdWorkItem, context); // this will move an item to the proper position, otherwise it'll be placed at the end of its chapter

            if (((IInternalWorkItem) createdWorkItem).isHeading()) {
                modifyHeaderTag(context, sourceWorkItem, createdWorkItem);
                reloadModule(context.getTargetModule());
            }

            return createdWorkItem; // return newly created WI
        } else {
            return sourceWorkItem; // return the same WI coz there is no simple way to get created WI when copying references
        }
    }

    private IWorkItem insertReferencedWorkItem(IWorkItem sourceWorkItem, MergeContext context) {
        final IWorkItem workItemToReference;
        if (sourceWorkItem.getProjectId().equals(context.getTargetModule().getProjectId())) {
            workItemToReference = sourceWorkItem;
        } else {
            workItemToReference = polarionService.getPairedWorkItems(sourceWorkItem, context.getTargetModule().getProjectId(), context.linkRole).stream().findFirst().orElse(null);
        }
        if (workItemToReference != null) {
            IModule.IStructureNode sourceNode = context.getSourceModule().getStructureNodeOfWI(sourceWorkItem);
            IModule.IStructureNode sourceParentNode = sourceNode.getParent();

            IWorkItem destinationParentWorkItem = polarionService.getPairedWorkItems(sourceParentNode.getWorkItem(), context.getTargetModule().getProjectId(), context.linkRole)
                    .stream().findFirst().orElse(null);
            IModule.IStructureNode destinationParentNode = context.getTargetModule().getStructureNodeOfWI(destinationParentWorkItem);
            int destinationIndex = sourceParentNode.getChildren().indexOf(sourceNode);

            polarionService.insertReferencedWorkItem(workItemToReference, context.getTargetModule(), destinationParentNode, destinationIndex);
        }
        return workItemToReference;
    }

    private void reloadModule(IModule module) {
        module.save();
        module.update();
    }

    private void modifyHeaderTag(MergeContext context, IWorkItem sourceWorkItem, IWorkItem createdWorkItem) {
        String sourceContent = context.getSourceModule().getHomePageContent().getContent();
        String headingLevel = "2";
        Matcher srcLevelMatcher = Pattern.compile("<h(?<level>\\d) [^>]+id=%s[\"|][^>]*></h\\d>".formatted(sourceWorkItem.getId())).matcher(sourceContent);
        if (srcLevelMatcher.find()) {
            headingLevel = srcLevelMatcher.group("level");
        }

        String targetContent = context.getTargetModule().getHomePageContent().getContent();

        Matcher matcher = Pattern.compile("<div [^>]+id=%s[\"|][^>]*></div>".formatted(createdWorkItem.getId())).matcher(targetContent);
        StringBuilder buf = new StringBuilder();
        while (matcher.find()) {
            String tag = matcher.group();
            String replacementTag = tag.replace("<div", "<h" + headingLevel)
                    .replace("</div", "</h" + headingLevel)
                    .replaceAll("\\|layout=\\d+", ""); // not sure why sometimes it adds layout entry and how it affects the result - so let's remove it
            matcher.appendReplacement(buf, replacementTag);
        }
        matcher.appendTail(buf);

        context.getTargetModule().setHomePageContent(Text.html(buf.toString()));
    }

    private void deleteWorkItemFromDocument(DocumentIdentifier documentIdentifier, IWorkItem workItem) {
        IModule module = polarionService.getModule(documentIdentifier.getProjectId(), documentIdentifier.getSpaceId(), documentIdentifier.getName());
        module.unreference(workItem);
        module.save();
    }

    private void merge(IWorkItem source, IWorkItem target, List<DiffField> fields) {
        for (DiffField field : fields) {
            polarionService.setFieldValue(target, field.getKey(), polarionService.getFieldValue(source, field.getKey()));
        }
        target.save();
    }

    private IWorkItem getWorkItem(@Nullable WorkItem workItem) {
        return workItem != null ? polarionService.getWorkItem(workItem.getProjectId(), workItem.getId(), workItem.getRevision()) : null;
    }

}
