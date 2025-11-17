package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.report.MergeReportEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.HandleReferencesType;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DiffField;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsContentMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsFieldsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.service.cleaners.FieldCleaner;
import ch.sbb.polarion.extension.diff_tool.service.cleaners.ListFieldCleaner;
import ch.sbb.polarion.extension.diff_tool.service.cleaners.NonListFieldCleaner;
import ch.sbb.polarion.extension.diff_tool.util.DiffModelCachedResource;
import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import com.polarion.alm.shared.api.model.document.internal.InternalDocument;
import com.polarion.alm.shared.api.model.document.internal.InternalUpdatableDocument;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.WriteTransaction;
import com.polarion.alm.shared.api.transaction.internal.InternalWriteTransaction;
import com.polarion.alm.shared.dle.compare.CompareOptions;
import com.polarion.alm.shared.dle.compare.DleWIsMergeAction;
import com.polarion.alm.shared.dle.compare.DleWIsMergeActionExecuter;
import com.polarion.alm.shared.dle.compare.DleWorkItemsComparator;
import com.polarion.alm.shared.dle.compare.DleWorkitemsMatcher;
import com.polarion.alm.shared.dle.compare.merge.DuplicateWorkItemAction;
import com.polarion.alm.tracker.internal.model.HyperlinkStruct;
import com.polarion.alm.tracker.internal.model.IInternalWorkItem;
import com.polarion.alm.tracker.internal.model.LinkRoleOpt;
import com.polarion.alm.tracker.internal.model.TestSteps;
import com.polarion.alm.tracker.internal.model.module.Module;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITestStepKeyOpt;
import com.polarion.alm.tracker.model.ITestSteps;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.types.Text;
import com.polarion.platform.persistence.ICustomFieldsService;
import com.polarion.platform.persistence.spi.EnumOption;
import com.polarion.subterra.base.data.identification.IContextId;
import com.polarion.subterra.base.data.model.ICustomField;
import com.polarion.subterra.base.data.model.IEnumType;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.IStructType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ch.sbb.polarion.extension.diff_tool.report.MergeReport.OperationResultType.*;
import static ch.sbb.polarion.extension.diff_tool.util.DiffToolUtils.*;

public class MergeService {

    private static final String TEST_STEPS = "steps";
    private static final String TEST_STEP_KEYS = "keys";
    private static final String TEST_STEP_VALUES = "values";

    private static final CompareOptions MERGE_OPTION = CompareOptions.create().forMerge(true).build();
    private final PolarionService polarionService;

    public MergeService(PolarionService polarionService) {
        this.polarionService = polarionService;
    }

    public MergeResult mergeDocuments(@NotNull DocumentsMergeParams mergeParams) {
        DocumentIdentifier documentIdentifier1 = mergeParams.getLeftDocument();
        DocumentIdentifier documentIdentifier2 = mergeParams.getRightDocument();
        // Evict cache of work items of specified documents before merging their work items, as after merge cache can become invalid
        polarionService.evictDocumentsCache(documentIdentifier1, documentIdentifier2);

        DiffModel diffModel = DiffModelCachedResource.get(documentIdentifier1.getProjectId(), mergeParams.getConfigName(), mergeParams.getConfigCacheBucketId());
        DocumentsMergeContext context = new DocumentsMergeContext(polarionService, documentIdentifier1, documentIdentifier2, mergeParams.getDirection(), mergeParams.getLinkRole(), diffModel, mergeParams.isAllowedReferencedWorkItemMerge());
        if (!Objects.equals(context.getTargetDocumentIdentifier().getModuleXmlRevision(), context.getTargetModule().getLastRevision())) {
            return MergeResult.builder().success(false).targetModuleHasStructuralChanges(true).build();
        }
        if (!polarionService.userAuthorizedForMerge(context.getTargetModule().getProjectId())) {
            return MergeResult.builder().success(false).mergeNotAuthorized(true).build();
        }

        List<WorkItemsPair> pairs = mergeParams.getPairs();
        removeDuplicatedOrConflictingPairs(pairs, context);

        TransactionalExecutor.executeInWriteTransaction(transaction -> {
            // Here we separate some operations based on their type attempting to reduce merge steps to achieve desired document state.
            pairs.removeIf(pair -> createOrDeleteItem(pair, context, transaction));
            for (WorkItemsPair pair : pairs) {
                updateAndMoveItem(pair, context);
            }

            if (mergeParams.isAllowedReferencedWorkItemMerge()) {
                pairs.removeIf(pair -> moveWorkItemOutOfDocument(pair, context));
            }
            reloadModule(context.getTargetModule());
            return null;
        });

        if (!pairs.isEmpty()) {
            TransactionalExecutor.executeInWriteTransaction(transaction -> {
                context.getTargetModule().update();
                pairs.removeIf(pair -> createOrDeleteItem(pair, context, transaction));
                reloadModule(context.getTargetModule());
                return null;
            });
        }

        if (!context.getAffectedModules().isEmpty()) {
            TransactionalExecutor.executeInWriteTransaction(transaction -> {
                context.getAffectedModules().forEach(module -> {
                    insertNode(module.getWorkItem(), module.getModule(), module.getParentNode(), module.getDestinationIndex(), true);
                    reloadModule(module.getModule());
                });
                return null;
            });
        }

        updateModifiedItemsRevisions(context);

        return MergeResult.builder()
                .success(true)
                .mergeReport(context.getMergeReport())
                .targetModuleHasStructuralChanges(!Objects.equals(context.getTargetDocumentIdentifier().getModuleXmlRevision(), context.getTargetModule().getLastRevision()))
                .build();
    }

    public MergeResult mergeDocumentsFields(@NotNull DocumentsFieldsMergeParams mergeParams) {

        DocumentsFieldsMergeContext context = new DocumentsFieldsMergeContext(mergeParams.getDirection(), mergeParams.getLeftDocument(), mergeParams.getRightDocument());

        IModule source = polarionService.getModule(context.getSourceDocumentIdentifier());
        IModule target = polarionService.getModule(context.getTargetDocumentIdentifier());

        if (!Objects.equals(context.getTargetDocumentIdentifier().getModuleXmlRevision(), target.getLastRevision())) {
            return MergeResult.builder().success(false).targetModuleHasStructuralChanges(true).build();
        }
        if (!polarionService.userAuthorizedForMerge(target.getProjectId())) {
            return MergeResult.builder().success(false).mergeNotAuthorized(true).build();
        }

        TransactionalExecutor.executeInWriteTransaction(transaction -> {
            for (String fieldId : mergeParams.getFieldIds()) {
                try {
                    target.setValue(fieldId, source.getValue(fieldId));
                    context.reportEntry(COPIED, fieldId, "value successfully copied");
                } catch (Exception e) {
                    context.reportEntry(WARNING, fieldId, "cannot copy value (%s)".formatted(e.getMessage()));
                }
            }
            if (!context.getMergeReport().getCopied().isEmpty()) {
                target.save();
            }
            return null;
        });

        return MergeResult.builder()
                .success(!context.getMergeReport().getCopied().isEmpty())
                .mergeReport(context.getMergeReport())
                .build();
    }

    public MergeResult mergeDocumentsContent(@NotNull DocumentsContentMergeParams mergeParams) {
        DocumentsContentMergeContext context = new DocumentsContentMergeContext(mergeParams.getLeftDocument(), mergeParams.getRightDocument(), mergeParams.getDirection());

        IModule sourceModule = polarionService.getModule(context.getSourceDocumentIdentifier());
        IModule targetModule = polarionService.getModule(context.getTargetDocumentIdentifier());

        if (!Objects.equals(context.getTargetDocumentIdentifier().getModuleXmlRevision(), targetModule.getLastRevision())) {
            return MergeResult.builder().success(false).targetModuleHasStructuralChanges(true).build();
        }
        if (!polarionService.userAuthorizedForMerge(targetModule.getProjectId())) {
            return MergeResult.builder().success(false).mergeNotAuthorized(true).build();
        }

        DocumentsContentHandler documentsContentHandler = new DocumentsContentHandler();
        TransactionalExecutor.executeInWriteTransaction(transaction -> {
            documentsContentHandler.merge(sourceModule, targetModule, context, mergeParams.getPairs());
            if (!context.getMergeReport().getModified().isEmpty()) {
                targetModule.save();
            }
            return null;
        });

        return MergeResult.builder()
                .success(!context.getMergeReport().getModified().isEmpty())
                .mergeReport(context.getMergeReport())
                .build();
    }

    public MergeResult mergeWorkItems(@NotNull WorkItemsMergeParams mergeParams) {
        String leftProjectId = mergeParams.getLeftProject().getId();
        DiffModel diffModel = DiffModelCachedResource.get(leftProjectId, mergeParams.getConfigName(), mergeParams.getConfigCacheBucketId());
        WorkItemsMergeContext context = new WorkItemsMergeContext(mergeParams.getLeftProject(), mergeParams.getRightProject(), mergeParams.getDirection(), mergeParams.getLinkRole(), diffModel);

        if (!polarionService.userAuthorizedForMerge(context.getTargetProject().getId())) {
            return MergeResult.builder().success(false).mergeNotAuthorized(true).build();
        }

        List<WorkItemsPair> pairs = mergeParams.getPairs();
        removeDuplicatedOrConflictingPairs(pairs, context);

        ILinkRoleOpt linkRoleObj = polarionService.getLinkRoleById(context.linkRole, polarionService.getTrackerProject(context.getTargetProject().getId()));

        List<String> createdWorkItemIds = new ArrayList<>();
        TransactionalExecutor.executeInWriteTransaction(transaction -> {
            for (WorkItemsPair pair : pairs) {
                IWorkItem createdTarget = createItem(pair, linkRoleObj, context);
                if (createdTarget != null) {
                    context.putTargetWorkItem(pair, createdTarget);
                    createdWorkItemIds.add(createdTarget.getId());
                }
            }

            pairs.removeIf(pair -> deleteItem(pair, context));

            return null;
        });

        TransactionalExecutor.executeInWriteTransaction(transaction -> {
            for (WorkItemsPair pair : pairs) {
                WorkItem target = context.getTargetWorkItem(pair);
                boolean reportModification = !createdWorkItemIds.contains(target.getId());
                updateItem(pair, context, reportModification);
            }
            return null;
        });

        updateModifiedItemsRevisions(context);

        return MergeResult.builder()
                .success(true)
                .mergeReport(context.getMergeReport())
                .build();
    }

    @VisibleForTesting
    @SuppressWarnings("java:S3776")
        // ignore cognitive complexity complaint
    boolean createOrDeleteItem(WorkItemsPair pair, DocumentsMergeContext context, WriteTransaction transaction) {
        IWorkItem source = getWorkItem(context.getSourceWorkItem(pair));
        IWorkItem target = getWorkItem(context.getTargetWorkItem(pair));

        if (source != null && target == null) { // "target" is null, so new item out of "source" to be created
            if (context.getSourceModule().getExternalWorkItems().contains(source)) {
                IWorkItem createdWorkItem = insertWorkItem(source, context, true);
                if (createdWorkItem != null) {
                    context.reportEntry(CREATED, pair, "new workitem '%s' based on source workitem '%s' created".formatted(createdWorkItem.getId(), source.getId()));
                    reloadModule(context.getTargetModule());
                } else {
                    context.reportEntry(CREATION_FAILED, pair, "new workitem based on source workitem '%s' NOT created".formatted(source.getId()));
                }
            } else {
                IWorkItem newWorkItem;
                IWorkItem pairedWorkItem = polarionService.getPairedWorkItems(source, context.getTargetModule().getProjectId(), context.linkRole).stream().findFirst().orElse(null);
                if (pairedWorkItem != null) {
                    newWorkItem = insertWorkItem(source, context, false);
                    if (newWorkItem != null) {
                        context.reportEntry(CREATED, pair, "workitem '%s' moved from '%s'".formatted(newWorkItem.getId(), pairedWorkItem.getModule() != null ? pairedWorkItem.getModule().getTitleWithSpace() : "tracker"));
                    } else {
                        context.reportEntry(CREATION_FAILED, pair, "new workitem based on source workitem '%s' NOT created".formatted(pairedWorkItem.getModule() != null ? pairedWorkItem.getModule().getTitleWithSpace() : "tracker"));
                    }
                } else {
                    newWorkItem = copyWorkItemToDocument(source, (InternalWriteTransaction) transaction, context, pair);
                    merge(source, newWorkItem, context, pair);
                    context.reportEntry(CREATED, pair, "new workitem '%s' based on source workitem '%s' created".formatted(newWorkItem.getId(), source.getId()));
                }
                context.bindCounterpartItem(pair,
                        WorkItem.of(newWorkItem,
                                context.getTargetModule().getOutlineNumberOfWorkitem(newWorkItem),
                                Objects.equals(source.getId(), newWorkItem == null ? "" : newWorkItem.getId()),
                                Objects.equals(newWorkItem == null ? "" : newWorkItem.getProjectId(), context.getTargetModule().getProjectId())
                        )
                );
                reloadModule(context.getTargetModule());
            }
            return true;
        } else if (source == null && target != null) { // "source" is null, so "target" to be deleted
            deleteWorkItemFromDocument(context.getTargetDocumentIdentifier(), target);
            context.reportEntry(DELETED, pair, "workitem '%s' deleted in target document".formatted(target.getId()));
            reloadModule(context.getTargetModule());
            return true;
        }
        return false;
    }

    @VisibleForTesting
    IWorkItem createItem(WorkItemsPair pair, ILinkRoleOpt linkRoleObj, WorkItemsMergeContext context) {
        if (context.getSourceWorkItem(pair) != null && context.getTargetWorkItem(pair) == null) {
            IWorkItem source = getWorkItem(context.getSourceWorkItem(pair));
            IWorkItem newWorkItem = polarionService.getTrackerProject(context.getTargetProject().getId()).createWorkItem(Objects.requireNonNull(source.getType()).getId());
            newWorkItem.addLinkedItem(source, linkRoleObj, null, false);
            newWorkItem.save();
            context.reportEntry(CREATED, pair, "new workitem '%s' based on source workitem '%s' created".formatted(newWorkItem.getId(), source.getId()));
            return newWorkItem;
        }
        return null;
    }

    @VisibleForTesting
    boolean deleteItem(WorkItemsPair pair, WorkItemsMergeContext context) {
        if (context.getSourceWorkItem(pair) == null && context.getTargetWorkItem(pair) != null) {
            IWorkItem target = getWorkItem(context.getTargetWorkItem(pair));
            if (target != null) {
                target.delete();
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    @SuppressWarnings("java:S3776")
        // ignore cognitive complexity complaint
    void updateAndMoveItem(WorkItemsPair pair, DocumentsMergeContext context) {
        IWorkItem source = getWorkItem(context.getSourceWorkItem(pair));
        IWorkItem target = getWorkItem(context.getTargetWorkItem(pair));
        boolean moveRequested = pair.getLeftWorkItem().getMovedOutlineNumber() != null || pair.getRightWorkItem().getMovedOutlineNumber() != null;

        if (!context.getTargetModule().getExternalWorkItems().contains(target)) {
            if (!context.getTargetWorkItem(pair).getLastRevision().equals(target.getLastRevision())) {
                context.reportEntry(CONFLICTED, pair, "merge is not allowed: target workitem '%s' revision '%s' has been already changed to '%s'"
                        .formatted(target.getId(), context.getTargetWorkItem(pair).getLastRevision(), target.getLastRevision()));
            } else if (context.getSourceModule().getExternalWorkItems().contains(source)) {
                context.reportEntry(PROHIBITED, pair, "don't allow merging referenced work item into included one");
            } else {
                merge(source, target, context, pair);
                context.reportEntry(MODIFIED, pair, "workitem '%s' modified with info from '%s'".formatted(target.getId(), source.getId()));
            }
        } else {
            if (!target.getProjectId().equals(context.getTargetModule().getProjectId())) {
                fixReferencedWorkItem(target, context.getTargetModule(), context, HandleReferencesType.DEFAULT);
                reloadModule(context.getTargetModule());
            } else if (!moveRequested && !context.isAllowedReferencedWorkItemMerge()) {
                context.reportEntry(PROHIBITED, pair, "can't merge into referenced workitem '%s' in target document '%s'".formatted(target.getId(), context.getTargetModule().getTitleWithSpace()));
            }
        }

        if (moveRequested) {
            if (Objects.equals(context.getSourceModule().getOutlineNumberOfWorkitem(source), context.getTargetModule().getOutlineNumberOfWorkitem(target))) {
                context.reportEntry(MOVE_SKIPPED, pair, "move skipped the item is already at the desired position");
            } else if (move(source, target, context)) {
                context.reportEntry(MOVED, pair, "workitem '%s' moved".formatted(target.getId()));
            } else {
                context.reportEntry(MOVE_FAILED, pair, "workitem '%s' NOT moved".formatted(target.getId()));
            }
        }
    }

    @VisibleForTesting
    void updateItem(WorkItemsPair pair, WorkItemsMergeContext context, boolean reportModification) {
        IWorkItem source = getWorkItem(context.getSourceWorkItem(pair));
        IWorkItem target = getWorkItem(context.getTargetWorkItem(pair));

        String targetLastRevision = context.getTargetWorkItem(pair).getLastRevision();
        if (targetLastRevision != null && !targetLastRevision.equals(target.getLastRevision())) {
            context.reportEntry(CONFLICTED, pair, "merge is not allowed: target workitem '%s' revision '%s' has been already changed to '%s'"
                    .formatted(target.getId(), targetLastRevision, target.getLastRevision()));
        } else {
            merge(source, target, context, pair);
            if (reportModification) {
                context.reportEntry(MODIFIED, pair, "workitem '%s' modified with info from '%s'".formatted(target.getId(), source.getId()));
            }
        }
    }

    @VisibleForTesting
    boolean moveWorkItemOutOfDocument(@NotNull WorkItemsPair pair, @NotNull DocumentsMergeContext context) {
        IWorkItem source = getWorkItem(context.getSourceWorkItem(pair));
        IWorkItem target = getWorkItem(context.getTargetWorkItem(pair));

        if (source != null && target != null && source.getLinkedWorkItems().contains(target) && hasReferenceMismatch(pair, context)) {
            if (context.getTargetWorkItem(pair).isReferenced()) {
                deleteWorkItemFromDocument(context.getTargetDocumentIdentifier(), target);
            } else {
                moveWorkItemOutOfDocument(context.getTargetDocumentIdentifier(), target);
            }
            context.reportEntry(DETACHED, pair, "workitem '%s' moved out of document".formatted(target.getId()));
            if (context.getDirection() == MergeDirection.LEFT_TO_RIGHT) {
                pair.setRightWorkItem(null);
            } else {
                pair.setLeftWorkItem(null);
            }
        }
        return false;
    }

    @VisibleForTesting
    boolean hasReferenceMismatch(@NotNull WorkItemsPair pair, @NotNull DocumentsMergeContext context) {
        boolean sourceReferenced = context.getSourceWorkItem(pair).isReferenced();
        boolean targetReferenced = context.getTargetWorkItem(pair).isReferenced();
        return sourceReferenced != targetReferenced;
    }

    private void updateModifiedItemsRevisions(SettingsAwareMergeContext context) {
        for (MergeReportEntry mergeReportEntry : context.getMergeReport().getModified()) {
            WorkItem targetWorkItem = context.getTargetWorkItem(mergeReportEntry.getWorkItemsPair());
            targetWorkItem.setLastRevision(getWorkItem(targetWorkItem).getLastRevision());
        }
    }

    @VisibleForTesting
    boolean move(@NotNull IWorkItem source, @NotNull IWorkItem target, @NotNull DocumentsMergeContext mergeContext) {
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

    @VisibleForTesting
    boolean move(@NotNull IModule.IStructureNode sourceNode, @NotNull IModule.IStructureNode targetNode,
                 @Nullable IModule.IStructureNode targetDestinationParentNode, @NotNull DocumentsMergeContext mergeContext) {
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

    /**
     * Client can accidentally (or not) send:
     * a) duplicated pairs
     * b) several pairs which have particular work item as a source/target but different value as a counterpart
     * I these cases we must leave only the first pair in the list.
     */
    private void removeDuplicatedOrConflictingPairs(List<WorkItemsPair> pairs, SettingsAwareMergeContext context) {
        Set<String> processedSourceIds = new HashSet<>();
        Set<String> processedTargetIds = new HashSet<>();
        pairs.removeIf(pair -> {
            String sourceId = Optional.ofNullable(context.getSourceWorkItem(pair)).map(WorkItem::getId).orElse("");
            String targetId = Optional.ofNullable(context.getTargetWorkItem(pair)).map(WorkItem::getId).orElse("");
            if (!StringUtils.isEmpty(sourceId) && processedSourceIds.contains(sourceId) || !StringUtils.isEmpty(targetId) && processedTargetIds.contains(targetId)) {
                context.reportEntry(DUPLICATE_SKIPPED, pair, "pair processing will be skipped because it duplicates entry from another pair");
                return true;
            }
            processedSourceIds.add(sourceId);
            processedTargetIds.add(targetId);
            return false;
        });
    }

    @VisibleForTesting
    String getTargetDestinationNumber(IModule.IStructureNode sourceNode, IModule.IStructureNode targetDestinationParentNode) {
        String sourceNumberWithinParent = (sourceNode.getParent() == null || sourceNode.getParent().getOutlineNumber() == null)
                ? sourceNode.getOutlineNumber() : sourceNode.getOutlineNumber().substring(sourceNode.getParent().getOutlineNumber().length());
        return (targetDestinationParentNode == null || targetDestinationParentNode.getOutlineNumber() == null)
                ? sourceNumberWithinParent : targetDestinationParentNode.getOutlineNumber() + sourceNumberWithinParent;
    }

    @VisibleForTesting
    IModule.IStructureNode getNodeByOutlineNumber(IModule module, String outlineNumber) {
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
    IWorkItem copyWorkItemToDocument(IWorkItem sourceWorkItem, @NotNull InternalWriteTransaction transaction, DocumentsMergeContext context, WorkItemsPair pair) {
        DleWIsMergeActionExecuter executer = getDleWIsMergeActionExecuter(sourceWorkItem, transaction, context);

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
            removeWrongHyperlinks(createdWorkItem, context, pair);
            cleanUpFields(createdWorkItem, context.getTargetModule().getProject().getContextId(), context.getDiffModel().getDiffFields(), new NonListFieldCleaner());
            cleanUpFields(createdWorkItem, context.getTargetModule().getProject().getContextId(), context.getDiffModel().getDiffFields(), new ListFieldCleaner());
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

    void cleanUpFields(@NotNull IWorkItem workItem, @NotNull IContextId projectContextId, @NotNull List<DiffField> allowedFields, @NotNull FieldCleaner cleaner) {
        List<WorkItemField> standardFieldsDeletable = polarionService.getStandardFields().stream().filter(PolarionService.deletableFieldsFilter).toList();
        for (WorkItemField field : polarionService.getDeletableFields(workItem, projectContextId, standardFieldsDeletable)) {
            if (polarionService.isFieldToCleanUp(allowedFields, field, workItem.getType())) {
                cleaner.clean(workItem, field);
            }
        }
    }

    @VisibleForTesting
    @NotNull
    public DleWIsMergeActionExecuter getDleWIsMergeActionExecuter(IWorkItem sourceWorkItem, @NotNull InternalWriteTransaction transaction, DocumentsMergeContext context) {
        // taken from DleWorkItemsCompareOther#initializeOutside
        InternalDocument leftDocument = (InternalDocument) context.getSourceDocumentReference().get(transaction);
        InternalDocument rightDocument = (InternalDocument) context.getTargetDocumentReference().get(transaction);

        DleWorkItemsComparator comparator = createComparator(transaction, leftDocument, rightDocument);

        // taken from DleWIsMergeSaveRequest#executeActions
        InternalUpdatableDocument targetDocument = (InternalUpdatableDocument) context.getTargetDocumentReference().getUpdatable(transaction);

        DleWIsMergeActionExecuter executer = new DleWIsMergeActionExecuter(targetDocument, comparator.getLeftMergedPartsOrder(), comparator.getRightMergedPartsOrder());

        DleWIsMergeAction action = getAction(sourceWorkItem);

        action.execute(executer);
        executer.finish();
        return executer;
    }

    @VisibleForTesting
    DleWIsMergeAction getAction(IWorkItem sourceWorkItem) {
        WorkItemReference workItemReference = new WorkItemReference(sourceWorkItem.getProjectId(), sourceWorkItem.getId(), sourceWorkItem.getRevision(), null);
        return new DuplicateWorkItemAction(workItemReference);
    }

    @VisibleForTesting
    DleWorkItemsComparator createComparator(InternalWriteTransaction transaction, InternalDocument leftDocument, InternalDocument rightDocument) {
        DleWorkitemsMatcher leftMatcher = new DleWorkitemsMatcher(leftDocument, null, transaction);
        DleWorkitemsMatcher rightMatcher = new DleWorkitemsMatcher(rightDocument, null, transaction);
        return new DleWorkItemsComparator(transaction, leftMatcher, rightMatcher, MERGE_OPTION);
    }

    /**
     * Newly created work item will have all the hyperlinks from the source but the allowed roles
     * may be limited by settings therefore we have to check and remove forbidden links
     */
    @VisibleForTesting
    void removeWrongHyperlinks(IWorkItem workItem, DocumentsMergeContext context, WorkItemsPair pair) {
        if (!workItem.getHyperlinks().isEmpty()) {
            ((Collection<?>) workItem.getHyperlinks()).removeIf(link -> {
                if (link instanceof HyperlinkStruct linkStruct) {
                    if (!isHyperlinkRoleAllowedInConfiguration(linkStruct, workItem.getType(), context)) {
                        return true;
                    } else if (isHyperlinkRoleForbiddenForType(linkStruct, workItem.getType(), workItem.getProjectId())) {
                        context.reportEntry(WARNING, pair, "Cannot create hyperlink in the workitem '%s' because project '%s' doesn't support role '%s' for type '%s'"
                                .formatted(workItem.getId(), workItem.getProjectId(), linkStruct.getRole().getId(), workItem.getType()));
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private boolean isHyperlinkRoleForbiddenForType(HyperlinkStruct link, ITypeOpt type, String projectId) {
        return polarionService.getHyperlinkRoles(projectId).stream().noneMatch(role ->
                Objects.equals(role.getWorkItemTypeId(), (type == null ? "" : type.getId())) && Objects.equals(role.getId(), link.getRole().getId()));
    }

    private boolean isHyperlinkRoleAllowedInConfiguration(HyperlinkStruct link, ITypeOpt type, SettingsAwareMergeContext context) {
        return context.getDiffModel().getHyperlinkRoles().isEmpty() ||
                context.getDiffModel().getHyperlinkRoles().contains((type == null ? "" : type.getId()) + "#" + link.getRole().getId());
    }

    @VisibleForTesting
    IWorkItem insertWorkItem(IWorkItem sourceWorkItem, DocumentsMergeContext context, boolean referenced) {
        final IWorkItem workItemToInsert;
        if (sourceWorkItem.getProjectId().equals(context.getTargetModule().getProjectId())) {
            workItemToInsert = sourceWorkItem;
        } else {
            workItemToInsert = polarionService.getPairedWorkItems(sourceWorkItem, context.getTargetModule().getProjectId(), context.linkRole).stream().findFirst().orElse(null);
        }
        if (workItemToInsert != null) {
            IModule.IStructureNode sourceNode = context.getSourceModule().getStructureNodeOfWI(sourceWorkItem);
            IModule.IStructureNode sourceParentNode = sourceNode.getParent();

            IWorkItem destinationParentWorkItem = polarionService.getPairedWorkItems(sourceParentNode.getWorkItem(), context.getTargetModule().getProjectId(), context.linkRole)
                    .stream().findFirst().orElse(null);
            IModule.IStructureNode destinationParentNode = context.getTargetModule().getStructureNodeOfWI(destinationParentWorkItem);
            int destinationIndex = sourceParentNode.getChildren().indexOf(sourceNode);

            if (workItemToInsert.getModule() != null && !workItemToInsert.getModule().equals(context.getTargetModule())) {
                IModule.IStructureNode sourceExternalNode = workItemToInsert.getModule().getStructureNodeOfWI(workItemToInsert);
                IModule.IStructureNode sourceExternalParentNode = sourceExternalNode.getParent();

                DocumentsMergeContext.AffectedModule affectedModule = DocumentsMergeContext.AffectedModule.builder()
                        .module(workItemToInsert.getModule())
                        .workItem(workItemToInsert)
                        .parentNode(sourceExternalParentNode)
                        .destinationIndex(sourceExternalParentNode.getChildren().indexOf(sourceExternalNode))
                        .build();
                context.getAffectedModules().add(affectedModule);
            }
            insertNode(workItemToInsert, context.getTargetModule(), destinationParentNode, destinationIndex, referenced);
        }
        return workItemToInsert;
    }

    @VisibleForTesting
    void reloadModule(IModule module) {
        ((Module) module).save(false);
        module.update();
    }

    private void modifyHeaderTag(DocumentsMergeContext context, IWorkItem sourceWorkItem, IWorkItem createdWorkItem) {
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

    @VisibleForTesting
    void deleteWorkItemFromDocument(DocumentIdentifier documentIdentifier, IWorkItem workItem) {
        IModule module = polarionService.getModule(documentIdentifier.getProjectId(), documentIdentifier.getSpaceId(), documentIdentifier.getName());
        module.unreference(workItem);
        module.save();
    }

    @VisibleForTesting
    void moveWorkItemOutOfDocument(DocumentIdentifier documentIdentifier, IWorkItem workItem) {
        IModule module = polarionService.getModule(documentIdentifier.getProjectId(), documentIdentifier.getSpaceId(), documentIdentifier.getName());
        module.moveOut(Collections.singletonList(workItem));
        module.save();
    }

    @VisibleForTesting
    void merge(IWorkItem source, IWorkItem target, SettingsAwareMergeContext context, WorkItemsPair pair) {
        for (DiffField field : context.getDiffModel().getDiffFields()) {
            Object fieldValue = polarionService.getFieldValue(source, field.getKey());
            if (IWorkItem.KEY_HYPERLINKS.equals(field.getKey()) && (fieldValue == null || fieldValue instanceof Collection<?>)) {
                mergeHyperlinks(target, (Collection<?>) fieldValue, context, pair);
            } else if (IWorkItem.KEY_LINKED_WORK_ITEMS.equals(field.getKey())) {
                mergeLinkedWorkItems(source, target, context, pair);
            } else {
                validateCustomFieldTypesAccordance(source, target, field);

                if (fieldValue instanceof TestSteps testSteps) {
                    fieldValue = polarionService.getTrackerService().getDataService().createStructureForTypeId(target, ITestSteps.STRUCTURE_ID, getTestStepsData(testSteps));
                } else if (fieldValue instanceof Text text) {
                    fieldValue = new Text(text.getType(), preProcessRichText(source, target, context.getLinkRole(), text.getContent()));
                }
                polarionService.setFieldValue(target, field.getKey(), fieldValue);
            }
        }
        target.save();
    }

    private String preProcessRichText(IWorkItem source, IWorkItem target, String linkRole, String richTextContent) {
        return polarionService.replaceLinksToPairedWorkItems(source, target, linkRole, removeComments(richTextContent));
    }

    private String removeComments(String richTextContent) {
        return RegexMatcher.get("<span id=[\"']polarion-comment:\\d+[\"'][^>]*>(?:</span>)?").removeAll(richTextContent);
    }

    @VisibleForTesting
    void validateCustomFieldTypesAccordance(IWorkItem source, IWorkItem target, DiffField field) {
        if (!source.getPrototype().isKeyDefined(field.getKey())) { // If field is a custom field in source item...
            ICustomFieldsService customFieldsService = polarionService.getTrackerService().getDataService().getCustomFieldsService();
            ICustomField sourceCustomField = customFieldsService.getCustomField(source, field.getKey());
            ICustomField targetCustomField = customFieldsService.getCustomField(target, field.getKey());
            if (!sourceCustomField.getType().equals(targetCustomField.getType())) { // ...and if types of this custom field in source and in target items are not equal then throw an error
                throw new IllegalStateException(String.format("Can't merge fields with different types, check that fields with key '%s' have the same type in source and target work item", field.getKey()));
            }
        }
    }

    @VisibleForTesting
    @SuppressWarnings("java:S1452") // wildcard usage is appropriate here
    Map<String, List<?>> getTestStepsData(TestSteps testSteps) {
        Map<String, List<?>> testStepsData = new HashMap<>();

        testStepsData.put(TEST_STEP_KEYS, testSteps.getKeys().stream().map(ITestStepKeyOpt::getId).toList());
        testStepsData.put(TEST_STEPS, testSteps.getSteps().stream().map(testStep -> Map.of(TEST_STEP_VALUES, testStep.getValues())).toList());

        return testStepsData;
    }

    @VisibleForTesting
    void mergeHyperlinks(IWorkItem workItem, @Nullable Collection<?> linksList, SettingsAwareMergeContext context, WorkItemsPair pair) {
        List<HyperlinkStruct> existingList = ((Collection<?>) workItem.getHyperlinks()).stream()
                .filter(HyperlinkStruct.class::isInstance).map(HyperlinkStruct.class::cast)
                .filter(h -> isHyperlinkRoleAllowedInConfiguration(h, workItem.getType(), context))
                .collect(Collectors.toCollection(ArrayList::new));
        List<HyperlinkStruct> newList = linksList == null ? List.of() : linksList.stream()
                .filter(HyperlinkStruct.class::isInstance).map(HyperlinkStruct.class::cast)
                .collect(Collectors.toCollection(ArrayList::new));
        for (HyperlinkStruct link : existingList) {
            HyperlinkStruct sameLink = newList.stream()
                    .filter(l -> Objects.equals(l.getRole().getId(), link.getRole().getId()) &&
                            Objects.equals(l.getUri(), link.getUri())).findFirst().orElse(null);
            if (sameLink != null) {
                newList.remove(sameLink);
            } else {
                workItem.getHyperlinks().remove(link);
            }
        }
        for (HyperlinkStruct link : newList) {
            if (isHyperlinkRoleAllowedInConfiguration(link, workItem.getType(), context)) {
                if (isHyperlinkRoleForbiddenForType(link, workItem.getType(), workItem.getProjectId())) {
                    context.reportEntry(WARNING, pair, "Cannot create hyperlink in the workitem '%s' because project '%s' doesn't support role '%s' for type '%s'"
                            .formatted(workItem.getId(), workItem.getProjectId(), link.getRole().getId(), workItem.getType()));
                } else {
                    workItem.addHyperlink(link.getUri(), link.getRole());
                }
            }
        }
    }

    @VisibleForTesting
    void mergeLinkedWorkItems(IWorkItem source, IWorkItem target, SettingsAwareMergeContext context, WorkItemsPair pair) {

        Collection<ILinkedWorkItemStruct> srcLinks = getLinks(source, false);
        Collection<ILinkedWorkItemStruct> targetLinks = getLinks(target, false);
        if (!context.getDiffModel().getLinkedWorkItemRoles().isEmpty()) {
            srcLinks = srcLinks.stream()
                    .filter(link -> context.getDiffModel().getLinkedWorkItemRoles().contains(link.getLinkRole().getId())).toList();
            targetLinks = targetLinks.stream()
                    .filter(link -> context.getDiffModel().getLinkedWorkItemRoles().contains(link.getLinkRole().getId())).toList();
        }

        List<ILinkedWorkItemStruct> sameLinks = new ArrayList<>();
        List<String> interlinkedSrcIds = new ArrayList<>();
        for (ILinkedWorkItemStruct targetLink : targetLinks) {

            // first, skip links to source work item
            if (sameWorkItem(targetLink, source)) {
                continue;
            }

            // next, attempt to find links to the same work item from source
            ILinkedWorkItemStruct sameLink = srcLinks.stream()
                    .filter(link -> Objects.equals(targetLink.getLinkRole().getId(), link.getLinkRole().getId()) && sameWorkItem(targetLink, link.getLinkedItem())).findFirst().orElse(null);
            if (sameLink != null) {
                if (sameProjectItems(source, target) || (!sameProjectItems(target, targetLink.getLinkedItem()) && !sameProjectItems(source, targetLink.getLinkedItem()))) {
                    // either both work items are from one project or both links lead to 3rd project work item
                    sameLinks.add(sameLink);
                    if (!Objects.equals(targetLink.getRevision(), sameLink.getRevision())) { // if they differ only by revision - fix it
                        target.getLinkedWorkItemsStructsDirect().remove(targetLink);
                        target.addLinkedItem(sameLink.getLinkedItem(), sameLink.getLinkRole(), sameLink.getRevision(), sameLink.isSuspect());
                    }
                    continue;
                }
            }

            // if there are some links left to 3rd projects - remove them
            if (!sameProjectItems(target, targetLink.getLinkedItem())) {
                target.getLinkedWorkItemsStructsDirect().remove(targetLink);
                continue;
            }

            // now we attempt to find counterparts (items which are linked to the interlinked work items)
            Set<String> oppositeWorkItems = srcLinks.stream()
                    .filter(l -> notFromModule(l.getLinkedItem(), target.getModule())) // filter out direct links to the target work item
                    .map(l -> l.getLinkedItem().getId()).collect(Collectors.toSet());
            List<IWorkItem> pairedWorkItems = polarionService.getPairedWorkItems(targetLink.getLinkedItem(), source.getProjectId(), context.getLinkRole());
            interlinkedSrcIds = pairedWorkItems.stream().map(IWorkItem::getId).filter(oppositeWorkItems::contains).toList();
            if (interlinkedSrcIds.isEmpty()) {
                target.getLinkedWorkItemsStructsDirect().remove(targetLink); // remove non-interlinked
            }
        }

        for (ILinkedWorkItemStruct srcLink : srcLinks) {
            if (sameWorkItem(srcLink, target) || sameLinks.contains(srcLink)) {
                continue;
            }

            if (sameProjectItems(source, srcLink.getLinkedItem())) {
                if (!interlinkedSrcIds.contains(srcLink.getLinkedItem().getId())) {
                    // attempt to find interlinked work item
                    IWorkItem found = polarionService.getPairedWorkItems(srcLink.getLinkedItem(), target.getProjectId(), context.getLinkRole()).stream().findFirst().orElse(null);
                    if (found != null) {
                        target.addLinkedItem(found, srcLink.getLinkRole(), found.getRevision(), srcLink.isSuspect());
                    } else {
                        context.reportEntry(WARNING, pair, "linkedWorkItems merge: cannot find opposite pair for the workitem '%s' in the project '%s'"
                                .formatted(srcLink.getLinkedItem().getId(), target.getProjectId()));
                    }
                }
            } else {
                target.addLinkedItem(srcLink.getLinkedItem(), srcLink.getLinkRole(), srcLink.getRevision(), srcLink.isSuspect());
            }
        }
    }

    /**
     * If a document contains a work item which is referenced (external) and belongs to a different project, depending on the 'handleReferences'.
     * WARNING: this method modifies module data (like references/un-references work items),
     * but does not persist module, which should be done in caller code which should run in write transaction!
     */
    public void fixReferencedWorkItem(@NotNull IWorkItem referencedWorkItem, @NotNull IModule targetModule, @NotNull SettingsAwareMergeContext context, HandleReferencesType handleReferences) {
        if (!referencedWorkItem.getProjectId().equals(targetModule.getProjectId())) {
            IWorkItem workItemInHead = referencedWorkItem.getRevision() == null ? referencedWorkItem : polarionService.getWorkItem(referencedWorkItem.getProjectId(), referencedWorkItem.getId());
            IWorkItem pairedWorkItem = context.getLinkRole().isEmpty() ? null : polarionService.getPairedWorkItems(workItemInHead, targetModule.getProjectId(), context.getLinkRole()).stream().findFirst().orElse(null);

            IModule.IStructureNode nodeToBeRemoved = targetModule.getStructureNodeOfWI(referencedWorkItem);
            IModule.IStructureNode parentNode = nodeToBeRemoved.getParent();
            int destinationIndex = parentNode.getChildren().indexOf(nodeToBeRemoved);
            if (HandleReferencesType.ALWAYS_OVERWRITE.equals(handleReferences) || (pairedWorkItem == null && HandleReferencesType.CREATE_MISSING.equals(handleReferences))) {
                IWorkItem newWorkItem = polarionService.getTrackerProject(targetModule.getProjectId()).createWorkItem(Objects.requireNonNull(referencedWorkItem.getType()).getId());
                ILinkRoleOpt linkRole = polarionService.getLinkRoleById(context.getLinkRole(), targetModule.getProject());
                if (linkRole != null) {
                    newWorkItem.addLinkedItem(workItemInHead, linkRole, null, false);
                }
                newWorkItem.setValue("module", targetModule);
                newWorkItem.save();

                merge(referencedWorkItem, newWorkItem, context, null);
                insertNode(newWorkItem, targetModule, parentNode, destinationIndex, false);
            } else if (pairedWorkItem != null) {
                insertNode(pairedWorkItem, targetModule, parentNode, destinationIndex, true);
            } else if (HandleReferencesType.KEEP.equals(handleReferences)) {
                return;
            }
            targetModule.unreference(referencedWorkItem);
        }
    }

    @VisibleForTesting
    void insertNode(@NotNull IWorkItem workItem, @NotNull IModule targetModule, @Nullable IModule.IStructureNode parentNode, int destinationIndex, boolean referenced) {
        if (referenced) {
            targetModule.addExternalWorkItem(workItem);
        } else {
            targetModule.moveIn(List.of(workItem));
        }

        if (parentNode == null) {
            parentNode = targetModule.getRootNode().getChildren().get(0);
        }

        // getStructureNodeOfWI may return null for items which are placed in recycle bin.
        // the problem basically is that a workitem is still bound to module but not a single node use it
        // so in this case we're adding a new node for it
        IModule.IStructureNode insertedWorkItemNode = Optional.ofNullable(targetModule.getStructureNodeOfWI(workItem)).orElse(createNode(targetModule, workItem, referenced));
        parentNode.addChild(insertedWorkItemNode, destinationIndex); // Placing inserted work item at required position in document
    }

    private IModule.IStructureNode createNode(@NotNull IModule targetModule, @NotNull IWorkItem workitem, boolean referenced) {
        // taken from Module#createStructureNode(IModule.IStructureNode parent, IWorkItem workitem)
        IModule.IStructureNode node = (IModule.IStructureNode) targetModule.getDataSvc().createStructureForTypeId(targetModule, "ModuleStructureNode", new HashMap<>());
        node.setValue("workItem", workitem);
        node.setValue("external", referenced);
        return node;
    }

    @Nullable
    @VisibleForTesting
    IWorkItem getWorkItem(@Nullable WorkItem workItem) {
        return workItem != null ? polarionService.getWorkItem(workItem.getProjectId(), workItem.getId(), workItem.getRevision()) : null;
    }

}
