package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.*;
import ch.sbb.polarion.extension.diff_tool.util.OutlineNumberComparator;
import ch.sbb.polarion.extension.diff_tool.util.DiffToolUtils;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import ch.sbb.polarion.extension.generic.util.ObjectUtils;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffLifecycleHandler;
import ch.sbb.polarion.extension.diff_tool.util.DiffModelCachedResource;
import ch.sbb.polarion.extension.diff_tool.util.RequestContextUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.model.document.DocumentReference;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollectionElement;
import com.polarion.core.util.types.Text;
import com.polarion.core.util.xml.HTMLCleaner;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.ITypedList;
import com.polarion.subterra.base.data.model.IType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.security.auth.Subject;
import java.util.*;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DiffService {

    private enum DiffSide {
        LEFT, RIGHT
    }

    private static final CharSequence DAISY_DIFF_CHANGED_FRAGMENT = "class=\"diff-html-";
    private final PolarionService polarionService;

    public DiffService(PolarionService polarionService) {
        this.polarionService = polarionService;
    }

    public DocumentsDiff getDocumentsDiff(DocumentIdentifier leftDocumentIdentifier, DocumentIdentifier rightDocumentIdentifier, String linkRoleId, String configName, String configCacheBucketId) {
        IModule leftDocument = polarionService.getDocumentWithFilledRevision(leftDocumentIdentifier.getProjectId(), leftDocumentIdentifier.getSpaceId(),
                leftDocumentIdentifier.getName(), leftDocumentIdentifier.getRevision());
        ILinkRoleOpt linkRole = polarionService.getLinkRoleById(linkRoleId, leftDocument.getProject());
        if (linkRole == null) {
            throw new IllegalArgumentException(String.format("No link role could be found by ID '%s'", linkRoleId));
        }

        IModule rightDocument = polarionService.getDocumentWithFilledRevision(rightDocumentIdentifier.getProjectId(), rightDocumentIdentifier.getSpaceId(),
                rightDocumentIdentifier.getName(), rightDocumentIdentifier.getRevision());

        DiffModel diffModel = DiffModelCachedResource.get(leftDocumentIdentifier.getProjectId(), configName, configCacheBucketId);

        List<WorkItemsPair> pairedWorkItems = handleMovedItems(polarionService.getPairedWorkItems(leftDocument, rightDocument, linkRole, diffModel.getStatusesToIgnore()));

        // Refresh workItems cache for both documents
        Subject userSubject = RequestContextUtil.getUserSubject();
        polarionService.getDocumentWorkItemsCache().cacheWorkItemsFromDocument(leftDocument, userSubject);
        polarionService.getDocumentWorkItemsCache().cacheWorkItemsFromDocument(rightDocument, userSubject);

        return DocumentsDiff.builder()
                .leftDocument(Document.from(leftDocument).authorizedForMerge(polarionService.userAuthorizedForMerge(leftDocumentIdentifier.getProjectId())))
                .rightDocument(Document.from(rightDocument).authorizedForMerge(polarionService.userAuthorizedForMerge(rightDocumentIdentifier.getProjectId())))
                .pairedWorkItems(pairedWorkItems)
                .build();
    }

    List<WorkItemsPair> handleMovedItems(@NotNull List<WorkItemsPair> pairedWorkItems) {
        // To show items as moved on UI we use kind of a trick - we artificially create duplicates (surrogates) for each such pair of work items.
        // One of it to show left item as it is (on its position in document) and indicating that move happened on right hand side,
        // another vice versa - to show right item as it is (on its position in document) and indicating that move happened on left hand side.

        List<WorkItemsPair> movedItemSurrogates = new ArrayList<>();

        OutlineNumberComparator outlineNumberComparator = new OutlineNumberComparator();

        for (WorkItemsPair pairedWorkItem : pairedWorkItems) {
            if (isMoveRelevant(pairedWorkItem)) {
                String parentOutlineNumberLeft = getParentOutlineNumber(pairedWorkItem.getLeftWorkItem());
                String parentOutlineNumberRight = getParentOutlineNumber(pairedWorkItem.getRightWorkItem());

                MergeMoveDirection moveDirection = getParentMoveDirection(movedItemSurrogates, parentOutlineNumberRight);

                // If pairs' parent is moved, consider this pair as moved as well and stop other checks
                if (moveDirection == null) {
                    // ...otherwise calculate direction by pair itself
                    moveDirection = getMoveDirection(pairedWorkItem, pairedWorkItems, parentOutlineNumberLeft, parentOutlineNumberRight, outlineNumberComparator);
                }

                if (moveDirection != null) {
                    pairedWorkItem.getLeftWorkItem().setMovedOutlineNumber(pairedWorkItem.getRightWorkItem().getOutlineNumber());
                    pairedWorkItem.getLeftWorkItem().setMoveDirection(moveDirection);

                    WorkItem leftSurrogate = WorkItem.of(pairedWorkItem.getLeftWorkItem());
                    WorkItem rightSurrogate = WorkItem.of(pairedWorkItem.getRightWorkItem());
                    rightSurrogate.setMovedOutlineNumber(leftSurrogate.getOutlineNumber());
                    rightSurrogate.setMoveDirection(moveDirection.getOpposite());

                    movedItemSurrogates.add(new WorkItemsPair(leftSurrogate, rightSurrogate));
                }
            }
        }

        // Surrogates created above are used to show right items in their position in document, so mainly we need to place them in resulting list
        // using right hand side sorting, but in some cases left nodes should also be taken into account, that's why pre-sort surrogates
        // in reverse order prior to placing in resulting list
        movedItemSurrogates.sort((pair1, pair2) -> outlineNumberComparator.compare(pair1.getRightWorkItem().getOutlineNumber(), pair2.getRightWorkItem().getOutlineNumber()));

        List<WorkItemsPair> result = new ArrayList<>(pairedWorkItems);
        movedItemSurrogates.forEach(pair -> result.add(getIndex(pair, result, outlineNumberComparator), pair));
        return result;
    }

    private boolean isMoveRelevant(@NotNull WorkItemsPair pairedWorkItem) {
        // Item can be considered as moved only if pair is full (left and right parts exist)
        return pairedWorkItem.getLeftWorkItem() != null && pairedWorkItem.getRightWorkItem() != null;
    }

    private boolean isStructuralItem(@NotNull WorkItem workItem) {
        return !workItem.getOutlineNumber().contains("-"); // "-" (eg. "2.1-1.1") means that item is not structural (i.e. not a header)
    }

    private String getParentOutlineNumber(@NotNull WorkItem workItem) {
        if (workItem.getOutlineNumber().contains("-")) {
            return workItem.getOutlineNumber().substring(0, workItem.getOutlineNumber().indexOf("-"));
        } else {
            return workItem.getOutlineNumber().contains(".") ? workItem.getOutlineNumber().substring(0, workItem.getOutlineNumber().lastIndexOf(".")) : null;
        }
    }

    private WorkItemsPair getParentPair(@NotNull List<WorkItemsPair> pairedWorkItems, @NotNull String leftOutlineNumber, @NotNull String rightOutlineNumber) {
        for (WorkItemsPair pairedWorkItem : pairedWorkItems) {
            if (pairedWorkItem.getLeftWorkItem() != null && pairedWorkItem.getRightWorkItem() != null) {
                if (pairedWorkItem.getLeftWorkItem().getOutlineNumber().equals(leftOutlineNumber)) {
                    return pairedWorkItem.getRightWorkItem().getOutlineNumber().equals(rightOutlineNumber) ? pairedWorkItem : null;
                } else if (pairedWorkItem.getRightWorkItem().getOutlineNumber().equals(rightOutlineNumber)) {
                    return null;
                }
            }
        }
        return null;
    }

    private MergeMoveDirection getParentMoveDirection(@NotNull List<WorkItemsPair> movedItemSurrogates, @Nullable String parentOutlineNumberRight) {
        for (WorkItemsPair pairedWorkItem : movedItemSurrogates) {
            if (pairedWorkItem.getRightWorkItem().getOutlineNumber().equals(parentOutlineNumberRight)) {
                // Surrogates always contain opposite direction to original items
                return pairedWorkItem.getRightWorkItem().getMoveDirection() != null ? pairedWorkItem.getRightWorkItem().getMoveDirection().getOpposite() : null;
            }
        }
        return null;
    }

    private MergeMoveDirection getMoveDirection(@NotNull WorkItemsPair pairedWorkItem, @NotNull List<WorkItemsPair> allPairedWorkItems,
                                                @Nullable String parentOutlineNumberLeft, @Nullable String parentOutlineNumberRight, @NotNull OutlineNumberComparator outlineNumberComparator) {
        MergeMoveDirection moveDirection = null;

        if (parentOutlineNumberLeft != null && parentOutlineNumberRight != null) {
            if (getParentPair(allPairedWorkItems, parentOutlineNumberLeft, parentOutlineNumberRight) == null) {
                // Items considered as moved if their parent nodes are not paired...
                moveDirection = outlineNumberComparator.compare(parentOutlineNumberLeft, parentOutlineNumberRight) < 0 ? MergeMoveDirection.DOWN : MergeMoveDirection.UP;
            } else {
                // ...or if they have different position within their parent hierarchy
                String leftPosition = positionWithinParent(parentOutlineNumberLeft, pairedWorkItem.getLeftWorkItem());
                String rightPosition = positionWithinParent(parentOutlineNumberRight, pairedWorkItem.getRightWorkItem());
                if (!leftPosition.equals(rightPosition)) {
                    moveDirection = outlineNumberComparator.compare(leftPosition, rightPosition) < 0 ? MergeMoveDirection.DOWN : MergeMoveDirection.UP;
                }
            }
        } else if (parentOutlineNumberLeft != null || parentOutlineNumberRight != null) {
            // ...or if one of comparing items is a root node and another is not a root node (which also means their parent nodes are not paired)
            String outlineNumberLeft = parentOutlineNumberLeft != null ? parentOutlineNumberLeft : pairedWorkItem.getLeftWorkItem().getOutlineNumber();
            String outlineNumberRight = parentOutlineNumberRight != null ? parentOutlineNumberRight : pairedWorkItem.getRightWorkItem().getOutlineNumber();

            moveDirection = outlineNumberComparator.compare(outlineNumberLeft, outlineNumberRight) < 0 ? MergeMoveDirection.DOWN : MergeMoveDirection.UP;
        } else if (!pairedWorkItem.getLeftWorkItem().getOutlineNumber().equals(pairedWorkItem.getRightWorkItem().getOutlineNumber())) {
            // ...or if comparing items are root nodes and their outline numbers differ
            moveDirection = outlineNumberComparator.compare(pairedWorkItem.getLeftWorkItem().getOutlineNumber(), pairedWorkItem.getRightWorkItem().getOutlineNumber()) < 0
                    ? MergeMoveDirection.DOWN : MergeMoveDirection.UP;
        }
        return moveDirection;
    }

    private String positionWithinParent(@NotNull String parentOutlineNumber, @NotNull WorkItem childWorkItem) {
        String position = childWorkItem.getOutlineNumber().substring(parentOutlineNumber.length());
        return position.startsWith("-") || position.startsWith(".") ? position.substring(1) : position;
    }

    private int getIndex(@NotNull WorkItemsPair movedItemSurrogate, @NotNull List<WorkItemsPair> pairedWorkItems, @NotNull OutlineNumberComparator outlineNumberComparator) {
        int rightIndex = getIndex(movedItemSurrogate, pairedWorkItems, outlineNumberComparator, DiffSide.RIGHT);
        if (rightIndex >= 0) {
            return rightIndex;
        }
        // Didn't find appropriate position, placing at last index
        return pairedWorkItems.size();
    }

    // 'side' parameter can be removed in future if right-hand side check will be enough
    private int getIndex(@NotNull WorkItemsPair movedItemSurrogate, @NotNull List<WorkItemsPair> pairedWorkItems,
                         @NotNull OutlineNumberComparator outlineNumberComparator, @NotNull DiffSide side) {
        int index = 0;
        WorkItem leftItemByIndex = null;
        WorkItem rightItemByIndex = null;
        for (int i = 0; i < pairedWorkItems.size(); i++) {
            WorkItem leftItem = pairedWorkItems.get(i).getLeftWorkItem();
            WorkItem rightItem = pairedWorkItems.get(i).getRightWorkItem();

            WorkItem baseItem = side == DiffSide.LEFT ? leftItem : rightItem;
            WorkItem pairedItem = side == DiffSide.LEFT ? rightItem : leftItem;

            if (isInlined(leftItemByIndex, leftItem) || isInlined(rightItemByIndex, rightItem)) {
                if (isInlined(rightItemByIndex, movedItemSurrogate.getRightWorkItem()) && rightItem != null
                        && outlineNumberComparator.compare(rightItem.getOutlineNumber(), movedItemSurrogate.getRightWorkItem().getOutlineNumber()) > 0) {
                    // If both current right item and surrogate right item are inline items of indexed pair
                    // and at the same time surrogate right item is less than current right item, then return current index
                    return i;
                } else {
                    // Either left or right items are children of last indexed pair, redefine index variable to index of current pair,
                    // considering it as a whole with its parent
                    index = i;
                }
            } else {
                // If surrogate is an inline item of last indexed one and current item is not, then place surrogate by current index
                if (isInlined(rightItemByIndex, movedItemSurrogate.getRightWorkItem())) {
                    return i;
                }

                // Continue checks only if there's a base item (usually it's a right item in pair) and it's structural,
                // inline items should always be treated as a whole with their parent structural items
                if (baseItem != null && isStructuralItem(baseItem)) {
                    // Only take into account real items, not placeholders for movement indication
                    if (pairedItem == null || pairedItem.getMovedOutlineNumber() == null) {
                        if (outlineNumberComparator.compare(baseItem.getOutlineNumber(), movedItemSurrogate.getRightWorkItem().getOutlineNumber()) > 0) {
                            // MATCH! Surrogate is less than base item, found appropriate location
                            return index + 1;
                        } else {
                            // Surrogate is bigger than base item, remember it and its index as next indexed pair
                            index = i;
                            leftItemByIndex = leftItem;
                            rightItemByIndex = rightItem;
                        }
                    } else if (outlineNumberComparator.compare(pairedItem.getOutlineNumber(), movedItemSurrogate.getRightWorkItem().getOutlineNumber()) > 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private boolean isInlined(WorkItem potentialParent, WorkItem potentialInlined) {
        return (potentialParent == null && potentialInlined == null)
                || (potentialParent != null && potentialInlined != null && potentialInlined.getOutlineNumber().contains("-")
                    && potentialParent.getOutlineNumber().equals(potentialInlined.getOutlineNumber().substring(0, potentialInlined.getOutlineNumber().indexOf("-"))));
    }

    public WorkItemsPairs findWorkItemsPairs(@NotNull WorkItemsPairsParams workItemsPairsParams) {
        ILinkRoleOpt linkRole = polarionService.getLinkRoleById(workItemsPairsParams.getLinkRole(), polarionService.getTrackerProject(workItemsPairsParams.getLeftProjectId()));
        if (linkRole == null) {
            throw new IllegalArgumentException(String.format("No link role could be found by ID '%s'", workItemsPairsParams.getLinkRole()));
        }
        IProject leftProject = polarionService.getProject(workItemsPairsParams.getLeftProjectId());
        IProject rightProject = polarionService.getProject(workItemsPairsParams.getRightProjectId());

        List<IWorkItem> leftWorkItems = polarionService.getWorkItems(workItemsPairsParams.getLeftProjectId(), workItemsPairsParams.getLeftWorkItemIds());
        Collection<WorkItemsPair> pairedWorkItems = new ArrayList<>();
        Collection<String> leftWorkItemIdsWithRedundancy = new ArrayList<>();
        leftWorkItems.forEach(leftWorkItem -> {
            List<IWorkItem> paired = polarionService.getPairedWorkItems(leftWorkItem, workItemsPairsParams.getRightProjectId(), workItemsPairsParams.getLinkRole());
            if (CollectionUtils.isEmpty(paired)) {
                pairedWorkItems.add(WorkItemsPair.of(leftWorkItem, null));
            } else if (paired.size() == 1) {
                pairedWorkItems.add(WorkItemsPair.of(leftWorkItem, paired.get(0)));
            } else {
                leftWorkItemIdsWithRedundancy.add(leftWorkItem.getId());
            }
        });

        return WorkItemsPairs.builder()
                .leftProject(Project.builder()
                        .id(leftProject.getId())
                        .name(leftProject.getName())
                        .authorizedForMerge(polarionService.userAuthorizedForMerge(leftProject.getId()))
                        .build())
                .rightProject(Project.builder()
                        .id(rightProject.getId())
                        .name(rightProject.getName())
                        .authorizedForMerge(polarionService.userAuthorizedForMerge(rightProject.getId()))
                        .build())
                .pairedWorkItems(pairedWorkItems)
                .leftWorkItemIdsWithRedundancy(leftWorkItemIdsWithRedundancy)
                .build();
    }

    public CollectionsDiff getCollectionsDiff(@NotNull CollectionsDiffParams diffParams) {
        IBaselineCollection leftCollection = polarionService.getCollection(diffParams.getLeftCollection().getProjectId(), diffParams.getLeftCollection().getId());
        IBaselineCollection rightCollection = polarionService.getCollection(diffParams.getRightCollection().getProjectId(), diffParams.getRightCollection().getId());

        List<IModule> leftDocuments = leftCollection.getElements().stream()
                .map(IBaselineCollectionElement::getObjectWithRevision)
                .filter(IModule.class::isInstance)
                .map(IModule.class::cast)
                .toList();

        List<IModule> rightDocuments = rightCollection.getElements().stream()
                .map(IBaselineCollectionElement::getObjectWithRevision)
                .filter(IModule.class::isInstance)
                .map(IModule.class::cast)
                .toList();

        Collection<DocumentsPair> pairedDocuments = new ArrayList<>();
        leftDocuments.forEach(leftDocument -> {
            // We first look for the document with the same name and in same space...
            IModule counterpartDocument = rightDocuments.stream()
                    .filter(rightDocument -> Objects.equals(rightDocument.getId(), leftDocument.getId()) && Objects.equals(rightDocument.getModuleFolder(), leftDocument.getModuleFolder()))
                    .findFirst().orElse(null);
            if (counterpartDocument == null) {
                // ... If not found, we look for the document with the same name regardless of it's space
                counterpartDocument = rightDocuments.stream()
                        .filter(rightDocument -> Objects.equals(rightDocument.getId(), leftDocument.getId()))
                        .findFirst().orElse(null);
            }
            pairedDocuments.add(DocumentsPair.of(leftDocument, counterpartDocument));
        });

        return CollectionsDiff.builder()
                .leftCollection(DocumentsCollection.builder()
                        .projectId(leftCollection.getProjectId())
                        .projectName(leftCollection.getProject().getName())
                        .id(leftCollection.getId())
                        .name(leftCollection.getName())
                        .build())
                .rightCollection(DocumentsCollection.builder()
                        .projectId(rightCollection.getProjectId())
                        .projectName(rightCollection.getProject().getName())
                        .id(rightCollection.getId())
                        .name(rightCollection.getName())
                        .build())
                .pairedDocuments(pairedDocuments)
                .build();
    }

    public WorkItemsPairDiff getDocumentWorkItemsPairDiff(@NotNull DocumentWorkItemsPairDiffParams documentWorkItemsPairDiffParams) {
        DocumentReference leftDocumentReference = getDocumentReference(documentWorkItemsPairDiffParams.getLeftWorkItem());
        DocumentReference rightDocumentReference = getDocumentReference(documentWorkItemsPairDiffParams.getRightWorkItem());

        Pair<IModule, IModule> modules = ObjectUtils.requireNotNull(TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> Pair.of(
                Optional.ofNullable(leftDocumentReference).map(reference -> reference.get(transaction).getOldApi()).orElse(null),
                Optional.ofNullable(rightDocumentReference).map(reference -> reference.get(transaction).getOldApi()).orElse(null))
        ));

        IWorkItem leftWorkItem = getWorkItem(documentWorkItemsPairDiffParams.getLeftWorkItem(), modules.getLeft());
        IWorkItem rightWorkItem = getWorkItem(documentWorkItemsPairDiffParams.getRightWorkItem(), modules.getRight());

        Set<String> fieldIds = getFieldsToDiff(documentWorkItemsPairDiffParams.getConfigName(), documentWorkItemsPairDiffParams.getConfigCacheBucketId(), documentWorkItemsPairDiffParams.getLeftProjectId(), leftWorkItem, rightWorkItem);

        WorkItemsPair workItemsPair = new WorkItemsPair();
        if (leftWorkItem != null) {
            workItemsPair.setLeftWorkItem(buildWorkItem(leftWorkItem, fieldIds,
                    modules.getLeft().getOutlineNumberOfWorkitem(leftWorkItem),
                    modules.getLeft().getExternalWorkItems().contains(leftWorkItem),
                    leftDocumentReference, documentWorkItemsPairDiffParams.isCompareEnumsById()));
        }
        if (rightWorkItem != null) {
            workItemsPair.setRightWorkItem(buildWorkItem(rightWorkItem, fieldIds,
                    modules.getRight().getOutlineNumberOfWorkitem(rightWorkItem),
                    modules.getRight().getExternalWorkItems().contains(rightWorkItem),
                    rightDocumentReference, documentWorkItemsPairDiffParams.isCompareEnumsById()));
        }

        fillHtmlDiffs(workItemsPair, fieldIds, documentWorkItemsPairDiffParams);

        return WorkItemsPairDiff.of(workItemsPair, fieldIds);
    }

    public WorkItemsPairDiff getDetachedWorkItemsPairDiff(@NotNull DetachedWorkItemsPairDiffParams detachedWorkItemsPairDiffParams) {
        IWorkItem leftWorkItem = getWorkItem(detachedWorkItemsPairDiffParams.getLeftWorkItem());
        IWorkItem rightWorkItem = getWorkItem(detachedWorkItemsPairDiffParams.getRightWorkItem());

        Set<String> fieldIds = getFieldsToDiff(detachedWorkItemsPairDiffParams.getConfigName(), detachedWorkItemsPairDiffParams.getConfigCacheBucketId(),
                detachedWorkItemsPairDiffParams.getLeftProjectId(), leftWorkItem, rightWorkItem);

        WorkItemsPair workItemsPair = new WorkItemsPair();
        if (leftWorkItem != null) {
            workItemsPair.setLeftWorkItem(buildWorkItem(leftWorkItem, fieldIds, null, false, null, detachedWorkItemsPairDiffParams.isCompareEnumsById()));
        }
        if (rightWorkItem != null) {
            workItemsPair.setRightWorkItem(buildWorkItem(rightWorkItem, fieldIds, null, false, null, detachedWorkItemsPairDiffParams.isCompareEnumsById()));
        }

        fillHtmlDiffs(workItemsPair, fieldIds, detachedWorkItemsPairDiffParams);

        return WorkItemsPairDiff.of(workItemsPair, fieldIds);
    }

    public DocumentsFieldsDiff getDocumentsFieldsDiff(DocumentIdentifier leftDocumentIdentifier, DocumentIdentifier rightDocumentIdentifier, boolean compareEnumsById, boolean compareOnlyMutualFields) {
        IModule leftDocument = polarionService.getDocumentWithFilledRevision(leftDocumentIdentifier.getProjectId(), leftDocumentIdentifier.getSpaceId(),
                leftDocumentIdentifier.getName(), leftDocumentIdentifier.getRevision());
        IModule rightDocument = polarionService.getDocumentWithFilledRevision(rightDocumentIdentifier.getProjectId(), rightDocumentIdentifier.getSpaceId(),
                rightDocumentIdentifier.getName(), rightDocumentIdentifier.getRevision());

        return DocumentsFieldsDiff.builder()
                .leftDocument(Document.from(leftDocument).authorizedForMerge(polarionService.userAuthorizedForMerge(leftDocumentIdentifier.getProjectId())))
                .rightDocument(Document.from(rightDocument).authorizedForMerge(polarionService.userAuthorizedForMerge(rightDocumentIdentifier.getProjectId())))
                .pairedFields(getFieldsDiff(leftDocument, rightDocument, compareEnumsById, compareOnlyMutualFields))
                .build();
    }

    private IWorkItem getWorkItem(@Nullable DocumentWorkItem workItemInDocument, IModule document) {
        IWorkItem workItem = null;
        if (workItemInDocument != null) {
            if (workItemInDocument.getProjectId() != null && workItemInDocument.getRevision() != null) {
                workItem = polarionService.getWorkItem(workItemInDocument.getProjectId(), workItemInDocument.getId(), workItemInDocument.getRevision());
            } else if (document != null) {
                // Note that 'projectId' of the document isn't always the same as all its
                // work items (e.g. it may contain referenced work item from another project) - this is
                // why we do not use it during work item search.
                Subject userSubject = RequestContextUtil.getUserSubject();
                workItem = polarionService.getDocumentWorkItemsCache().getWorkItem(document, userSubject, workItemInDocument.getId());
            }
        }

        return workItem;
    }

    private IWorkItem getWorkItem(@Nullable ProjectWorkItem detachedWorkItem) {
        IWorkItem workItem = null;
        if (detachedWorkItem != null) {
            workItem = polarionService.getWorkItem(detachedWorkItem.getProjectId(), detachedWorkItem.getId(), detachedWorkItem.getRevision());
        }
        return workItem;
    }

    private Set<String> getFieldsToDiff(String configName, String configCacheBucketId, String leftProjectId, IWorkItem leftWorkItem, IWorkItem rightWorkItem) {
        DiffModel diffModel = DiffModelCachedResource.get(leftProjectId, configName, configCacheBucketId);

        Set<DiffField> fieldsToDiff = new HashSet<>(diffModel.getDiffFields());
        // -----
        // Outline numbers are minimally required to show structure difference
        fieldsToDiff.add(DiffField.OUTLINE_NUMBER);
        fieldsToDiff.add(DiffField.EXTERNAL_PROJECT_WORK_ITEM);
        // -----

        IWorkItem baseWorkItem = leftWorkItem != null ? leftWorkItem : rightWorkItem;
        // Exclude fields which are not supposed for underlying WorkItem type
        fieldsToDiff.removeIf(field -> field.getWiTypeId() != null
                && (baseWorkItem.getType() == null || !field.getWiTypeId().equals(baseWorkItem.getType().getId())));
        return fieldsToDiff.stream().map(DiffField::getKey).collect(Collectors.toSet());
    }

    private DocumentReference getDocumentReference(DocumentWorkItem workItemInDocument) {
        return workItemInDocument == null ? null :
                DocumentReference.fromModuleLocation(workItemInDocument.getDocumentProjectId(), workItemInDocument.getDocumentLocationPath(), workItemInDocument.getDocumentRevision());
    }

    private WorkItem buildWorkItem(@NotNull IWorkItem iWorkItem, @NotNull Set<String> fieldIds, @Nullable String outlineNumber,
                                   boolean referenced, @Nullable DocumentReference documentReference, boolean compareEnumsById) {
        WorkItem workItem = WorkItem.of(iWorkItem, outlineNumber, referenced, (documentReference != null && !iWorkItem.getProjectId().equals(documentReference.projectId())));
        Set<FieldMetadata> fields = Sets.union(polarionService.getGeneralFields(IWorkItem.PROTO, iWorkItem.getContextId()),
                polarionService.getCustomFields(IWorkItem.PROTO, iWorkItem.getContextId(), Optional.ofNullable(iWorkItem.getType()).map(IEnumOption::getId).orElse(null)));
        for (String fieldId : fieldIds) {
            IType fieldType = fields.stream().filter(f -> f.getId().equals(fieldId)).map(FieldMetadata::getType).findFirst().orElse(null);
            workItem.addField(
                    WorkItem.Field.builder()
                            .id(fieldId)
                            .name(iWorkItem.getFieldLabel(fieldId))
                            .type(fieldType)
                            .value(getFieldValue(iWorkItem, fieldId, workItem, fieldType, compareEnumsById))
                            .html(renderHtml(iWorkItem, fieldId, documentReference, workItem))
                            .build()
            );
        }
        return workItem;
    }

    private String renderHtml(IWorkItem iWorkItem, String fieldId, DocumentReference documentReference, WorkItem workItem) {
        if (IWorkItem.KEY_OUTLINE_NUMBER.equals(fieldId)) {
            // Work item may either do not contain outline number or it may be invalid in case when it is referenced (for referenced work items its outline number stored on document level).
            // So in order to cover all cases it is better to request it from outer document.
            return workItem.getOutlineNumber();
        } else if (DiffField.EXTERNAL_PROJECT_WORK_ITEM.getKey().equals(fieldId) && workItem.isExternalProjectWorkItem()) {
            return Boolean.TRUE.toString();
        }
        return polarionService.renderField(iWorkItem.getProjectId(), iWorkItem.getId(), workItem.getRevision(), fieldId, documentReference);
    }

    private Object getFieldValue(IPObject ipObject, String fieldId, WorkItem workItem, IType fieldType, boolean compareEnumsById) {
        boolean enumIdComparisonMode = compareEnumsById && DiffToolUtils.isEnumContainingType(fieldType);
        Object value = polarionService.getFieldValue(ipObject, fieldId, enumIdComparisonMode ? Object.class : String.class);
        if (IWorkItem.KEY_DESCRIPTION.equals(fieldId)) {
            // Sometimes during work item duplication the description is 'cleaned' (see WorkItem#setValue method).
            // This results in slight differences between initial & copied description values.
            // Using the code below we try to 'harmonize' both descriptions reducing non-important diffs count,
            // also this code will remove comments because they shouldn't be treated as difference
            value = HTMLCleaner.cleanWiDesc(Text.html((String) value), true, true).getContent();
        } else if (IWorkItem.KEY_OUTLINE_NUMBER.equals(fieldId) && workItem != null) {
            // Work item may either do not contain outline number or it may be invalid in case when it is referenced (for referenced work items its outline number stored on document level).
            // So in order to cover all cases it is better to request it from outer document.
            value = workItem.getOutlineNumber();
        } else if (fieldId.equals(DiffField.EXTERNAL_PROJECT_WORK_ITEM.getKey()) && workItem != null) {
            value = workItem.isExternalProjectWorkItem();
        } else if (enumIdComparisonMode) {
            if (value instanceof IEnumOption) {
                // single enum - just take enum ID
                value = ((IEnumOption) value).getId();
            } else if (value instanceof ITypedList<?>) {
                // enum list - get all IDs in order to later compare them properly using Objects.equals()
                // (note that we are sorting list in order to ignore the values order)
                value = ((ITypedList<?>) value).stream().filter(i -> i instanceof IEnumOption).map(e -> ((IEnumOption) e).getId()).sorted().toList();
            } else if (value != null) {
                // theoretically we shouldn't reach this statement, but just in case we convert it to string otherwise we get jackson serialization exception
                value = String.valueOf(value);
            }
        }
        return value;
    }

    @VisibleForTesting
    void fillHtmlDiffs(@NotNull WorkItemsPair workItemsPair, @NotNull Set<String> fieldIds, @NotNull WorkItemsPairDiffParams<?> workItemsPairDiffParams) {
        // We will execute diff operation twice: using direct & reverse order.
        // this allows to show better visual result in case of style changes.
        // IMPORTANT: as a required non-obvious step for this approach - we have to swap
        // styles in the reversed diff to display it properly (this swap implemented using ReverseStylesHandler).
        Consumer<DiffContext> differ = context -> {
            if (Objects.equals(context.fieldA.getValue(), context.fieldB.getValue())) {
                // Skip fields with identical content in both WorkItems
                return;
            }

            List<DiffLifecycleHandler> handlers = DiffLifecycleHandler.getHandlers();
            Pair<String, String> pairToDiff = preparePairToDiff(context, handlers);
            String diff = DiffToolUtils.computeDiff(pairToDiff);
            diff = postProcessDiff(diff, context, handlers);
            // Note that the resulted diff is placed to the second/B field only (coz opposite field will be filled by the reversed operation)
            context.fieldB.setHtmlDiff(diff);
        };

        fieldIds.forEach(fieldId -> {
            DiffContext contextA = new DiffContext(workItemsPair.getLeftWorkItem(), workItemsPair.getRightWorkItem(), fieldId, workItemsPairDiffParams, polarionService);
            DiffContext contextB = new DiffContext(workItemsPair.getRightWorkItem(), workItemsPair.getLeftWorkItem(), fieldId, workItemsPairDiffParams, polarionService).reverseStyles(true);
            differ.accept(contextA);
            differ.accept(contextB);

            //Sometimes values aren't the same by calling equals() method but the resulting diff doesn't contain any changes.
            //Workaround below is pretty simple - we just check whether diff contains specific piece of html which DaisyDiff
            //adds for changed parts and if there is no such things - we treat situation as "no diff".
            if (Stream.of(contextA.fieldA.getHtmlDiff(), contextA.fieldB.getHtmlDiff(), contextB.fieldA.getHtmlDiff(), contextB.fieldB.getHtmlDiff())
                    .filter(Objects::nonNull).noneMatch(diff -> diff.contains(DAISY_DIFF_CHANGED_FRAGMENT))) {
                contextA.fieldA.setHtmlDiff(null);
                contextA.fieldB.setHtmlDiff(null);
                contextB.fieldA.setHtmlDiff(null);
                contextB.fieldB.setHtmlDiff(null);
            }
        });
    }

    @VisibleForTesting
    Collection<DocumentsFieldsPair> getFieldsDiff(@NotNull IModule leftDocument, @NotNull IModule rightDocument, boolean compareEnumsById, boolean compareOnlyMutualFields) {
        // We will execute diff operation twice: using direct & reverse order.
        // this allows to show better visual result in case of style changes.
        // IMPORTANT: as a required non-obvious step for this approach - we have to swap
        // styles in the reversed diff to display it properly (this swap implemented using ReverseStylesHandler).
        Consumer<DiffContext> differ = context -> {
            if (Objects.equals(context.fieldA.getValue(), context.fieldB.getValue())) {
                // Skip fields with identical content in both WorkItems
                return;
            }

            List<DiffLifecycleHandler> handlers = DiffLifecycleHandler.getHandlers();
            Pair<String, String> pairToDiff = preparePairToDiff(context, handlers);
            String diff = DiffToolUtils.computeDiff(pairToDiff);
            diff = postProcessDiff(diff, context, handlers);
            // Note that the resulted diff is placed to the second/B field only (coz opposite field will be filled by the reversed operation)
            context.fieldB.setHtmlDiff(diff);
        };

        Set<String> fieldIds = new LinkedHashSet<>(leftDocument.getCustomFieldsList());
        if (compareOnlyMutualFields) {
            fieldIds.retainAll(rightDocument.getCustomFieldsList());
        } else {
            fieldIds.addAll(rightDocument.getCustomFieldsList());
        }
        return fieldIds.stream().map(fieldId -> {

            WorkItem.Field leftField = WorkItem.Field.builder()
                    .id(fieldId)
                    .name(leftDocument.getFieldLabel(fieldId))
                    .type(leftDocument.getCustomFieldPrototype(fieldId).getType())
                    .value(getFieldValue(leftDocument, fieldId, null, leftDocument.getCustomFieldPrototype(fieldId).getType(), compareEnumsById))
                    .html(polarionService.renderField(leftDocument, fieldId))
                    .build();

            WorkItem.Field rightField = WorkItem.Field.builder()
                    .id(fieldId)
                    .name(rightDocument.getFieldLabel(fieldId))
                    .type(rightDocument.getCustomFieldPrototype(fieldId).getType())
                    .value(getFieldValue(rightDocument, fieldId, null, rightDocument.getCustomFieldPrototype(fieldId).getType(), compareEnumsById))
                    .html(polarionService.renderField(rightDocument, fieldId))
                    .build();

            DiffContext contextA = new DiffContext(leftDocument, rightDocument, leftField, rightField, fieldId, leftDocument.getProjectId(), polarionService);
            DiffContext contextB = new DiffContext(rightDocument, leftDocument, rightField, leftField, fieldId, leftDocument.getProjectId(), polarionService).reverseStyles(true);
            differ.accept(contextA);
            differ.accept(contextB);

            //Sometimes values aren't the same by calling equals() method but the resulting diff doesn't contain any changes.
            //Workaround below is pretty simple - we just check whether diff contains specific piece of html which DaisyDiff
            //adds for changed parts and if there is no such things - we treat situation as "no diff".
            if (Stream.of(contextA.fieldA.getHtmlDiff(), contextA.fieldB.getHtmlDiff(), contextB.fieldA.getHtmlDiff(), contextB.fieldB.getHtmlDiff())
                    .filter(Objects::nonNull).noneMatch(diff -> diff.contains(DAISY_DIFF_CHANGED_FRAGMENT))) {
                contextA.fieldA.setHtmlDiff(null);
                contextA.fieldB.setHtmlDiff(null);
                contextB.fieldA.setHtmlDiff(null);
                contextB.fieldB.setHtmlDiff(null);
            }
            return new DocumentsFieldsPair(leftField, rightField);
        }).toList();
    }

    private Pair<String, String> preparePairToDiff(DiffContext context, List<DiffLifecycleHandler> handlers) {
        Pair<String, String> pair = Pair.of(context.fieldA.getHtml(), context.fieldB.getHtml());
        for (DiffLifecycleHandler handler : handlers) {
            pair = handler.preProcess(pair, context);
        }
        return pair;
    }

    private String postProcessDiff(@NotNull String diff, DiffContext context, List<DiffLifecycleHandler> handlers) {
        for (DiffLifecycleHandler handler : Lists.reverse(handlers)) {
            diff = handler.postProcess(diff, context);
        }
        return diff;
    }
}
