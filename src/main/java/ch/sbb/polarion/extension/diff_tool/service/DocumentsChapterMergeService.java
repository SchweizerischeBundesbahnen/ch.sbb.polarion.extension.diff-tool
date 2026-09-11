package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterInsertMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeInfo;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DiffField;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeWorkItemsPair;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ReferencedItemsHandling;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.internal.model.IInternalWorkItem;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.core.util.logging.Logger;
import com.polarion.core.util.types.Text;
import com.polarion.subterra.base.data.identification.IContextId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.sbb.polarion.extension.diff_tool.report.MergeReport.OperationResultType.CREATED;
import static ch.sbb.polarion.extension.diff_tool.report.MergeReport.OperationResultType.CREATION_FAILED;
import static ch.sbb.polarion.extension.diff_tool.report.MergeReport.OperationResultType.DETACHED;
import static ch.sbb.polarion.extension.diff_tool.report.MergeReport.OperationResultType.MOVED;
import static ch.sbb.polarion.extension.diff_tool.report.MergeReport.OperationResultType.PROHIBITED;
import static ch.sbb.polarion.extension.diff_tool.report.MergeReport.OperationResultType.WARNING;

/**
 * Copies or moves a chapter of one document, with everything below it, into another document.
 * <p>
 * A copy is never linked to its origin, which rules out every existing merge path of {@link MergeService}: they
 * seek counterparts by a link role. Work items are created the way {@link MergeService#fixReferencedWorkItem} creates
 * them - a blank work item of the same type, filled by {@link MergeService#merge} - and placed by
 * {@link MergeService#insertNode}, which lets this service control the position instead of deriving it from pairs.
 */
public class DocumentsChapterMergeService {

    private static final Logger logger = Logger.getLogger(DocumentsChapterMergeService.class);

    /**
     * Fields which are not part of what a work item contains, and are therefore not copied:
     * <ul>
     *     <li>where the work item lives, which is decided when the copy is created</li>
     *     <li>what happened to the original work item - approvals, signatures and work records, which are also the
     *     fields Polarion handles through an API of their own (see {@code ListCleanerProvider})</li>
     *     <li>comments, which are objects of their own: they are copied through the comment API, see
     *     {@link CommentsCopier#copyComments(IWorkItem, IWorkItem)}</li>
     * </ul>
     */
    private static final Set<String> FIELDS_NOT_TO_COPY = Set.of(
            IWorkItem.KEY_PROJECT, IWorkItem.KEY_MODULE, IWorkItem.KEY_TYPE, IWorkItem.KEY_OUTLINE_NUMBER, IWorkItem.KEY_ID,
            IWorkItem.KEY_COMMENTS, IWorkItem.KEY_APPROVALS, IWorkItem.KEY_WORK_RECORDS, IWorkflowObject.KEY_WORKFLOW_SIGNATURES);

    private final PolarionService polarionService;
    private final MergeService mergeService;
    private final DocumentsContentHandler documentsContentHandler;
    private final DocumentLayoutSyncService documentLayoutSyncService;
    private final CommentsCopier commentsCopier;

    public DocumentsChapterMergeService(@NotNull PolarionService polarionService, @NotNull MergeService mergeService) {
        this(polarionService, mergeService, new DocumentsContentHandler(), new DocumentLayoutSyncService(), new CommentsCopier(polarionService));
    }

    @VisibleForTesting
    DocumentsChapterMergeService(@NotNull PolarionService polarionService, @NotNull MergeService mergeService, @NotNull DocumentsContentHandler documentsContentHandler,
                                 @NotNull DocumentLayoutSyncService documentLayoutSyncService, @NotNull CommentsCopier commentsCopier) {
        this.polarionService = polarionService;
        this.mergeService = mergeService;
        this.documentsContentHandler = documentsContentHandler;
        this.documentLayoutSyncService = documentLayoutSyncService;
        this.commentsCopier = commentsCopier;
    }

    /**
     * A node of the source chapter, captured before the merge starts. In move mode the source document is modified
     * while the merge runs, so its structure tree cannot be walked twice.
     *
     * @param workItem work item of the node
     * @param parentId ID of the work item of the parent node, {@code null} for the chapter which is being merged
     * @param heading  whether this node is a heading, i.e. a chapter of the document
     * @param external whether this node references a work item instead of containing it
     */
    @VisibleForTesting
    record SourceNode(@NotNull IWorkItem workItem, @Nullable String parentId, boolean heading, boolean external) {
    }

    /**
     * Where the merged chapter is placed in the target document.
     *
     * @param parentWorkItem work item of the node the merged chapter becomes a child of
     * @param index          position among the children of that node
     */
    @VisibleForTesting
    record InsertionPoint(@Nullable IWorkItem parentWorkItem, int index) {
    }

    public @NotNull MergeResult mergeChapter(@NotNull ChapterMergeParams params) {
        return mergeChapter(params, null);
    }

    /**
     * Merges a chapter, as one write transaction: either the target document holds the whole chapter afterwards, or
     * it holds none of it. A single work item which cannot be merged is reported and passed over - that is a result,
     * not a failure of the merge.
     * <p>
     * The documents cache is not evicted here: it is keyed by the user of the request, which a merge running as a
     * job no longer has - the caller evicts it (see {@code ChapterMergeJobScheduler.schedule}).
     */
    public @NotNull MergeResult mergeChapter(@NotNull ChapterMergeParams params, @Nullable ProgressReporter progressReporter) {
        DocumentsChapterMergeContext context = new DocumentsChapterMergeContext(polarionService, params);

        // The revision is checked only if the caller provided it: a caller which doesn't track the document's
        // structure, like the Document Properties panel, has nothing to compare against.
        if (targetModuleChangedMeanwhile(context)) {
            return MergeResult.builder().success(false).targetModuleHasStructuralChanges(true).build();
        }
        if (!polarionService.userAuthorizedForMerge(context.getTargetModule().getProjectId())
                || (params.getMode() == ChapterMergeMode.MOVE && !polarionService.userAuthorizedForMerge(context.getSourceModule().getProjectId()))) {
            return MergeResult.builder().success(false).mergeNotAuthorized(true).build();
        }

        IModule.IStructureNode sourceChapterNode = resolveChapterNode(context.getSourceModule(), params.getSourceChapterOutlineNumber());
        IModule.IStructureNode targetChapterNode = resolveChapterNode(context.getTargetModule(), params.getTargetChapterOutlineNumber());
        String validationError = validate(context, sourceChapterNode, targetChapterNode);
        if (validationError != null) {
            context.reportChapterEntry(PROHIBITED, validationError);
            return result(context, false);
        }

        List<SourceNode> subtree = collectSubtree(context.getSourceModule(), sourceChapterNode);
        report(progressReporter, "Merging %d workitem(s) of chapter '%s'".formatted(subtree.size(), params.getSourceChapterOutlineNumber()));

        InsertionPoint insertionPoint = resolveInsertionPoint(targetChapterNode, params.getInsertMode());
        IWorkItem targetChapterWorkItem = targetChapterNode.getWorkItem();

        // One transaction for the whole merge: a merge which fails half way through would otherwise leave the
        // work items it already placed behind, and in move mode they would be gone from the source document too.
        TransactionalExecutor.executeInWriteTransaction(transaction -> {
            insertSubtree(context, subtree, insertionPoint, targetChapterWorkItem, progressReporter);
            detachMovedItems(context);
            // First the order of the merged work items on the page, then the text which goes between them
            placeMergedItemsUnderTargetChapter(context, targetChapterWorkItem);
            copyFreeContent(context, subtree);
            rewriteRichTextLinks(context);
            context.getTargetModule().save();
            return null;
        });

        return result(context, true);
    }

    // -------------------------------------------------------------------------------------------------------------
    // validation and source analysis
    // -------------------------------------------------------------------------------------------------------------

    /**
     * Finds the node of a chapter by its outline number. Only a heading can be a chapter: an outline number
     * containing '-' (eg. '2.1-1') belongs to a work item which is not a structural part of the document.
     */
    @VisibleForTesting
    @Nullable
    IModule.IStructureNode resolveChapterNode(@NotNull IModule module, @Nullable String outlineNumber) {
        if (outlineNumber == null || outlineNumber.isBlank() || outlineNumber.contains("-")) {
            return null;
        }
        IModule.IStructureNode node = mergeService.getNodeByOutlineNumber(module, outlineNumber.trim());
        return node != null && node.getWorkItem() != null && isHeading(module, node.getWorkItem()) ? node : null;
    }

    @VisibleForTesting
    @Nullable
    String validate(@NotNull DocumentsChapterMergeContext context, @Nullable IModule.IStructureNode sourceChapterNode, @Nullable IModule.IStructureNode targetChapterNode) {
        if (sourceChapterNode == null) {
            return "chapter '%s' could not be found in the source document, or it isn't a chapter".formatted(context.getSourceChapterOutlineNumber());
        }
        if (targetChapterNode == null) {
            return "chapter '%s' could not be found in the target document, or it isn't a chapter".formatted(context.getTargetChapterOutlineNumber());
        }
        if (context.getSourceDocumentIdentifier().pointsToSameDocumentAs(context.getTargetDocumentIdentifier())
                && isDescendantOrSelf(targetChapterNode, sourceChapterNode)) {
            return "chapter '%s' is a part of chapter '%s', a chapter cannot be merged into itself"
                    .formatted(context.getTargetChapterOutlineNumber(), context.getSourceChapterOutlineNumber());
        }
        return null;
    }

    @VisibleForTesting
    boolean isDescendantOrSelf(@NotNull IModule.IStructureNode node, @NotNull IModule.IStructureNode ancestor) {
        for (IModule.IStructureNode current = node; current != null; current = current.getParent()) {
            if (current == ancestor || (current.getWorkItem() != null && ancestor.getWorkItem() != null
                    && Objects.equals(current.getWorkItem().getId(), ancestor.getWorkItem().getId()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Collects the chapter and everything below it in document order (pre-order), so that a parent is always
     * processed before its children and can already be used as their parent in the target document.
     */
    @VisibleForTesting
    @NotNull
    List<SourceNode> collectSubtree(@NotNull IModule sourceModule, @NotNull IModule.IStructureNode chapterRoot) {
        List<SourceNode> nodes = new ArrayList<>();
        collectSubtree(sourceModule, chapterRoot, null, nodes);
        return nodes;
    }

    private void collectSubtree(@NotNull IModule sourceModule, @NotNull IModule.IStructureNode node, @Nullable String parentId, @NotNull List<SourceNode> nodes) {
        IWorkItem workItem = node.getWorkItem();
        if (workItem == null || workItem.isUnresolvable()) {
            return;
        }
        nodes.add(new SourceNode(workItem, parentId, isHeading(sourceModule, workItem), node.isExternal()));
        List<IModule.IStructureNode> children = node.getChildren();
        if (children != null) {
            children.forEach(child -> collectSubtree(sourceModule, child, workItem.getId(), nodes));
        }
    }

    @VisibleForTesting
    boolean isHeading(@NotNull IModule module, @NotNull IWorkItem workItem) {
        if (workItem instanceof IInternalWorkItem internalWorkItem) {
            return internalWorkItem.isHeading();
        }
        ITypeOpt headingType = module.getHeadingWorkItemType();
        return headingType != null && workItem.getType() != null && headingType.getId().equals(workItem.getType().getId());
    }

    // -------------------------------------------------------------------------------------------------------------
    // placing the chapter into the target document
    // -------------------------------------------------------------------------------------------------------------

    @VisibleForTesting
    @NotNull
    InsertionPoint resolveInsertionPoint(@NotNull IModule.IStructureNode targetChapterNode, @NotNull ChapterInsertMode insertMode) {
        if (insertMode == ChapterInsertMode.UNDER) {
            return new InsertionPoint(targetChapterNode.getWorkItem(), 0);
        }
        IModule.IStructureNode parentNode = targetChapterNode.getParent();
        if (parentNode == null) {
            return new InsertionPoint(null, 0);
        }
        List<IModule.IStructureNode> children = parentNode.getChildren();
        int index = children == null ? 0 : Math.min(children.indexOf(targetChapterNode) + 1, children.size());
        return new InsertionPoint(parentNode.getWorkItem(), index);
    }

    private void insertSubtree(@NotNull DocumentsChapterMergeContext context, @NotNull List<SourceNode> subtree,
                               @NotNull InsertionPoint insertionPoint, @NotNull IWorkItem targetChapterWorkItem, @Nullable ProgressReporter progressReporter) {
        if (context.isCopyWorkItemLayouts()) {
            copyLayouts(context, subtree);
        }
        takeMovedItemsOver(context, subtree);
        for (SourceNode sourceNode : subtree) {
            IModule.IStructureNode parentNode = resolveParentNode(context, sourceNode, insertionPoint, targetChapterWorkItem);
            if (sourceNode.parentId() != null && parentNode == null) {
                // Its parent didn't make it into the target document, so placing this item would tear the chapter apart
                context.reportChapterEntry(CREATION_FAILED, "workitem '%s' was skipped because its parent workitem '%s' could not be merged"
                        .formatted(sourceNode.workItem().getId(), sourceNode.parentId()));
                continue;
            }
            int index = sourceNode.parentId() == null ? insertionPoint.index() : childrenCount(parentNode);
            try {
                IWorkItem targetWorkItem = processNode(sourceNode, context, parentNode, index);
                if (targetWorkItem != null) {
                    context.bindCopy(sourceNode.workItem(), targetWorkItem);
                    mergeService.reloadModule(context.getTargetModule());
                }
            } catch (Exception e) {
                logger.error("Could not merge workitem '%s' into document '%s'".formatted(sourceNode.workItem().getId(), context.getTargetModule().getModuleName()), e);
                context.reportChapterEntry(CREATION_FAILED, "workitem '%s' could not be merged: %s".formatted(sourceNode.workItem().getId(), e.getMessage()));
            }
            report(progressReporter, "Merged workitem '%s'".formatted(sourceNode.workItem().getId()));
        }
    }

    /**
     * Takes the work items which are being moved into the target document, all of them in one call.
     * <p>
     * {@link IModule#moveIn} is a bulk operation and has to be used as one: it moves the work items in the
     * repository, and moving them one by one leaves Polarion rejecting every item after the first with
     * "The resource includes or located inside the previously copied (moved) resources". Their position in the
     * document is given to them afterwards, item by item, by {@link MergeService#placeNode}.
     */
    @VisibleForTesting
    void takeMovedItemsOver(@NotNull DocumentsChapterMergeContext context, @NotNull List<SourceNode> subtree) {
        if (context.getMode() != ChapterMergeMode.MOVE || !context.sameProject()) {
            return;
        }
        List<IWorkItem> itemsToMove = subtree.stream()
                .filter(sourceNode -> !sourceNode.heading() && !sourceNode.external())
                .map(SourceNode::workItem)
                .toList();
        if (!itemsToMove.isEmpty()) {
            context.getTargetModule().moveIn(itemsToMove);
            mergeService.reloadModule(context.getTargetModule());
        }
    }

    private void copyLayouts(@NotNull DocumentsChapterMergeContext context, @NotNull List<SourceNode> subtree) {
        Set<String> typeIds = subtree.stream()
                .filter(sourceNode -> !sourceNode.heading() && sourceNode.workItem().getType() != null)
                .map(sourceNode -> sourceNode.workItem().getType().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        DocumentLayoutSyncService.LayoutSyncResult syncResult = documentLayoutSyncService.copyMissingLayouts(context.getSourceModule(), context.getTargetModule(), typeIds);
        context.getCopiedLayoutTypeIds().addAll(syncResult.copiedLayoutTypeIds());
        syncResult.copiedLayoutTypeIds().forEach(typeId ->
                context.reportChapterEntry(CREATED, "layout of workitem type '%s' copied into the target document".formatted(typeId)));
        syncResult.allowedTypeIds().forEach(typeId ->
                context.reportChapterEntry(CREATED, "workitem type '%s' allowed in the target document".formatted(typeId)));
    }

    private @Nullable IModule.IStructureNode resolveParentNode(@NotNull DocumentsChapterMergeContext context, @NotNull SourceNode sourceNode,
                                                               @NotNull InsertionPoint insertionPoint, @NotNull IWorkItem targetChapterWorkItem) {
        if (sourceNode.parentId() == null) {
            IWorkItem parentWorkItem = insertionPoint.parentWorkItem() != null ? insertionPoint.parentWorkItem() : targetChapterWorkItem;
            return context.getTargetModule().getStructureNodeOfWI(parentWorkItem);
        }
        IWorkItem parentCopy = context.getItemMapping().get(sourceNode.parentId());
        return parentCopy == null ? null : context.getTargetModule().getStructureNodeOfWI(parentCopy);
    }

    private int childrenCount(@Nullable IModule.IStructureNode parentNode) {
        return parentNode == null || parentNode.getChildren() == null ? 0 : parentNode.getChildren().size();
    }

    /**
     * Places a single source node into the target document: a heading is always copied, a work item is copied
     * or moved depending on the mode, and a referenced work item is handled according to the chosen policy.
     *
     * @return the work item which represents this node in the target document, {@code null} when it was skipped
     */
    @VisibleForTesting
    @Nullable
    IWorkItem processNode(@NotNull SourceNode sourceNode, @NotNull DocumentsChapterMergeContext context, @Nullable IModule.IStructureNode parentNode, int index) {
        if (sourceNode.external()) {
            return processReferencedNode(sourceNode, context, parentNode, index);
        }
        if (sourceNode.heading() || context.getMode() == ChapterMergeMode.COPY) {
            return copyWorkItem(sourceNode.workItem(), context, parentNode, index);
        }
        return moveWorkItem(sourceNode.workItem(), context, parentNode, index);
    }

    private @Nullable IWorkItem processReferencedNode(@NotNull SourceNode sourceNode, @NotNull DocumentsChapterMergeContext context,
                                                      @Nullable IModule.IStructureNode parentNode, int index) {
        ReferencedItemsHandling handling = context.getReferencedItems();
        if (handling == ReferencedItemsHandling.SKIP) {
            context.reportChapterEntry(WARNING, "referenced workitem '%s' was skipped".formatted(sourceNode.workItem().getId()));
            return null;
        }
        if (handling == ReferencedItemsHandling.COPY_AS_NEW) {
            return copyWorkItem(sourceNode.workItem(), context, parentNode, index);
        }
        mergeService.insertNode(sourceNode.workItem(), context.getTargetModule(), parentNode, index, true);
        context.getReferencedItemsInTarget().add(sourceNode.workItem());
        context.reportChapterEntry(CREATED, "workitem '%s' referenced in the target document".formatted(sourceNode.workItem().getId()));
        if (context.getMode() == ChapterMergeMode.MOVE) {
            context.getDetachCandidates().add(sourceNode.workItem());
        }
        return sourceNode.workItem();
    }

    /**
     * Creates a work item in the target project out of the source one and fills it with the values of all its
     * writable fields. The copy is deliberately not linked to its origin.
     */
    @VisibleForTesting
    @NotNull
    IWorkItem copyWorkItem(@NotNull IWorkItem sourceWorkItem, @NotNull DocumentsChapterMergeContext context, @Nullable IModule.IStructureNode parentNode, int index) {
        IWorkItem createdWorkItem = polarionService.getTrackerProject(context.getTargetModule().getProjectId())
                .createWorkItem(resolveTargetTypeId(sourceWorkItem, context));
        createdWorkItem.setValue(IWorkItem.KEY_MODULE, context.getTargetModule());
        createdWorkItem.save();

        // The comments come first: the text copied right after them names them, and it can only name the copies
        context.setCommentIdMapping(commentsCopier.copyComments(sourceWorkItem, createdWorkItem));
        context.setCurrentDiffModel(fieldsToCopy(sourceWorkItem, context.getSourceModule()));
        mergeService.merge(sourceWorkItem, createdWorkItem, context, new MergeWorkItemsPair(sourceWorkItem, createdWorkItem, List.of()));
        mergeService.insertNode(createdWorkItem, context.getTargetModule(), parentNode, index, false);

        context.getCreatedItems().add(createdWorkItem);
        context.reportChapterEntry(CREATED, "workitem '%s' created out of workitem '%s'".formatted(createdWorkItem.getId(), sourceWorkItem.getId()));
        return createdWorkItem;
    }

    /**
     * A work item cannot change its project, so it can only be moved within one project. Between projects it is
     * referenced in the target document instead and taken out of the source one, which is reported as a warning.
     */
    @VisibleForTesting
    @NotNull
    IWorkItem moveWorkItem(@NotNull IWorkItem sourceWorkItem, @NotNull DocumentsChapterMergeContext context, @Nullable IModule.IStructureNode parentNode, int index) {
        if (context.sameProject()) {
            // Already taken over by takeMovedItemsOver, so it only has to be given its position
            mergeService.placeNode(sourceWorkItem, context.getTargetModule(), parentNode, index, false);
            context.getMovedItems().add(sourceWorkItem);
            context.reportChapterEntry(MOVED, "workitem '%s' moved into the target document".formatted(sourceWorkItem.getId()));
        } else {
            mergeService.insertNode(sourceWorkItem, context.getTargetModule(), parentNode, index, true);
            context.getMovedItems().add(sourceWorkItem);
            context.getReferencedItemsInTarget().add(sourceWorkItem);
            context.getDetachCandidates().add(sourceWorkItem);
            context.reportChapterEntry(WARNING, ("workitem '%s' stays in project '%s' because a workitem cannot change its project, " +
                    "so it is referenced in the target document of project '%s' and removed from the source document")
                    .formatted(sourceWorkItem.getId(), context.getSourceModule().getProjectId(), context.getTargetModule().getProjectId()));
        }
        return sourceWorkItem;
    }

    private @NotNull String resolveTargetTypeId(@NotNull IWorkItem sourceWorkItem, @NotNull DocumentsChapterMergeContext context) {
        if (isHeading(context.getSourceModule(), sourceWorkItem)) {
            ITypeOpt headingType = context.getTargetModule().getHeadingWorkItemType();
            if (headingType != null) {
                return headingType.getId();
            }
        }
        return sourceWorkItem.getType().getId();
    }

    /**
     * All writable fields of a work item are copied, so the model of fields to be merged depends on the type
     * of the item which is being copied right now. Custom fields are defined per project, hence the document
     * the item belongs to has to be given along with it.
     * <p>
     * A field the source work item has no value for is not copied at all. The copy is a new work item which
     * starts with the defaults of its type, and there is nothing to carry over - a chapter heading has no
     * severity or priority to give, and a required field cannot be set to an empty value anyway.
     */
    @VisibleForTesting
    @NotNull
    DiffModel fieldsToCopy(@NotNull IWorkItem workItem, @NotNull IModule module) {
        IContextId contextId = module.getProject().getContextId();
        List<WorkItemField> standardFields = polarionService.getStandardFields().stream().filter(PolarionService.deletableFieldsFilter).toList();
        List<DiffField> diffFields = polarionService.getDeletableFields(workItem, contextId, standardFields).stream()
                .map(WorkItemField::getKey)
                .filter(key -> !FIELDS_NOT_TO_COPY.contains(key))
                .filter(key -> polarionService.getFieldValue(workItem, key) != null)
                .map(key -> DiffField.builder().key(key).build())
                .toList();
        return DiffModel.builder().diffFields(diffFields).build();
    }

    /**
     * Takes moved work items out of the source document. Moving a node out takes its children with it, so items
     * are detached in reverse document order, i.e. children before their parents.
     */
    @VisibleForTesting
    void detachMovedItems(@NotNull DocumentsChapterMergeContext context) {
        if (context.getDetachCandidates().isEmpty()) {
            return;
        }
        List<IWorkItem> reversed = new ArrayList<>(context.getDetachCandidates());
        Collections.reverse(reversed);
        for (IWorkItem workItem : reversed) {
            if (context.getSourceModule().getExternalWorkItems().contains(workItem)) {
                context.getSourceModule().unreference(workItem);
            } else {
                context.getSourceModule().moveOut(List.of(workItem));
            }
            context.reportChapterEntry(DETACHED, "workitem '%s' removed from the source document".formatted(workItem.getId()));
        }
        mergeService.reloadModule(context.getSourceModule());
    }

    // -------------------------------------------------------------------------------------------------------------
    // page content of the target document
    // -------------------------------------------------------------------------------------------------------------

    /**
     * Copies the text which is placed between the work items of the merged chapter, i.e. the content of the
     * document page which doesn't belong to any work item.
     * <p>
     * This is the only thing a chapter merge writes into the page itself. Work items are put into the document
     * through Polarion's API ({@link MergeService#insertNode}), which maintains the page - including the level of
     * a merged chapter, which the API derives from the chapter it is attached to.
     */
    @VisibleForTesting
    void copyFreeContent(@NotNull DocumentsChapterMergeContext context, @NotNull List<SourceNode> subtree) {
        List<String> sourceWorkItemIds = subtree.stream().map(sourceNode -> sourceNode.workItem().getId()).toList();
        documentsContentHandler.copyFreeContent(context.getSourceHomePageContentSnapshot(), context.getTargetModule(),
                sourceWorkItemIds, context.getIdMapping(), copyDocumentComments(context, sourceWorkItemIds));
    }

    /**
     * Gives the target document the comments which were written on the text the merge copies into it.
     *
     * @return ID of a source comment mapped to the ID of its copy
     */
    @VisibleForTesting
    @NotNull
    Map<String, String> copyDocumentComments(@NotNull DocumentsChapterMergeContext context, @NotNull List<String> sourceWorkItemIds) {
        Set<String> commentIds = documentsContentHandler.freeContentCommentIds(context.getSourceHomePageContentSnapshot(), sourceWorkItemIds);
        if (commentIds.isEmpty()) {
            return Map.of();
        }
        return commentsCopier.copyComments(context.getSourceModule(), context.getTargetModule(),
                comment -> commentIds.contains(comment.getId()), context.getTargetModule().getAuthor());
    }

    /**
     * Puts everything merged under a chapter directly under that chapter, above what the chapter already held.
     * <p>
     * Where a merged work item lands on the page is decided by Polarion from the text which follows the item it is
     * added to, so a chapter which already holds text and work items scatters the merged ones among them. Their
     * order among themselves is the one the structure gives them; this is about where that block sits.
     */
    @VisibleForTesting
    void placeMergedItemsUnderTargetChapter(@NotNull DocumentsChapterMergeContext context, @NotNull IWorkItem targetChapterWorkItem) {
        if (context.getInsertMode() != ChapterInsertMode.UNDER || context.getIdMapping().isEmpty()) {
            return;
        }
        documentsContentHandler.moveAnchorsBelow(context.getTargetModule(),
                List.copyOf(context.getIdMapping().values()), targetChapterWorkItem.getId());
    }

    /**
     * Points the links of the copied work items to the copies of the items they link to. This cannot be done while
     * an item is being copied, because a link may point to an item which is copied later.
     */
    @VisibleForTesting
    void rewriteRichTextLinks(@NotNull DocumentsChapterMergeContext context) {
        if (context.getCreatedItems().isEmpty() || context.getIdMapping().isEmpty()) {
            return;
        }
        for (IWorkItem createdWorkItem : context.getCreatedItems()) {
            boolean modified = false;
            for (DiffField field : fieldsToCopy(createdWorkItem, context.getTargetModule()).getDiffFields()) {
                Object value = polarionService.getFieldValue(createdWorkItem, field.getKey());
                if (value instanceof Text text && text.getContent() != null) {
                    String rewritten = polarionService.rewriteWorkItemLinks(createdWorkItem, text.getContent(),
                            workItem -> context.getItemMapping().get(workItem.getId()));
                    if (!rewritten.equals(text.getContent())) {
                        polarionService.setFieldValue(createdWorkItem, field.getKey(), new Text(text.getType(), rewritten));
                        modified = true;
                    }
                }
            }
            if (modified) {
                createdWorkItem.save();
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // result
    // -------------------------------------------------------------------------------------------------------------

    private @NotNull MergeResult result(@NotNull DocumentsChapterMergeContext context, boolean success) {
        context.setInsertedOutlineNumber(insertedOutlineNumber(context));
        ChapterMergeInfo chapterMergeInfo = ChapterMergeInfo.builder()
                .insertedOutlineNumber(context.getInsertedOutlineNumber())
                .createdWorkItemIds(context.getCreatedItems().stream().map(IWorkItem::getId).toList())
                .movedWorkItemIds(context.getMovedItems().stream().map(IWorkItem::getId).toList())
                .referencedWorkItemIds(context.getReferencedItemsInTarget().stream().map(IWorkItem::getId).toList())
                .copiedLayoutTypeIds(context.getCopiedLayoutTypeIds())
                .build();
        return MergeResult.builder()
                .success(success)
                .mergeReport(context.getMergeReport())
                .chapterMergeInfo(chapterMergeInfo)
                .targetModuleHasStructuralChanges(targetModuleChangedMeanwhile(context))
                .build();
    }

    private boolean targetModuleChangedMeanwhile(@NotNull DocumentsChapterMergeContext context) {
        String moduleXmlRevision = context.getTargetDocumentIdentifier().getModuleXmlRevision();
        return moduleXmlRevision != null && !Objects.equals(moduleXmlRevision, context.getTargetModule().getLastRevision());
    }

    private @Nullable String insertedOutlineNumber(@NotNull DocumentsChapterMergeContext context) {
        IWorkItem chapterCopy = context.getItemMapping().values().stream().findFirst().orElse(null);
        return chapterCopy == null ? null : context.getTargetModule().getOutlineNumberOfWorkitem(chapterCopy);
    }

    private void report(@Nullable ProgressReporter progressReporter, @NotNull String message) {
        if (progressReporter != null) {
            progressReporter.report(message);
        }
    }

    /**
     * Reports the progress of a merge, so that a merge running as a background job can tell how far it got.
     */
    @FunctionalInterface
    public interface ProgressReporter {
        void report(@NotNull String message);
    }
}
