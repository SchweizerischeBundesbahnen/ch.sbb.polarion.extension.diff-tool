package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentDuplicateParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DiffField;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.service.cleaners.FieldCleaner;
import ch.sbb.polarion.extension.diff_tool.service.cleaners.ListFieldCleaner;
import ch.sbb.polarion.extension.diff_tool.service.cleaners.NonListFieldCleaner;
import ch.sbb.polarion.extension.diff_tool.util.CommentUtils;
import ch.sbb.polarion.extension.diff_tool.util.DiffModelCachedResource;
import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import com.polarion.alm.projects.model.IFolder;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.internal.model.WorkItem;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.core.util.types.Text;
import com.polarion.core.util.xml.HTMLCleaner;
import com.polarion.subterra.base.data.identification.IContextId;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DocumentCopyService {

    private static final Logger logger = Logger.getLogger(DocumentCopyService.class);

    private final PolarionService polarionService;
    private final MergeService mergeService;
    private final CommentsCopier commentsCopier;

    public DocumentCopyService(PolarionService polarionService, MergeService mergeService) {
        this.polarionService = polarionService;
        this.mergeService = mergeService;
        this.commentsCopier = new CommentsCopier(polarionService);
    }

    /**
     * Creates a new document as a duplicate of source document
     */
    public DocumentIdentifier createDocumentDuplicate(@NotNull String sourceProjectId, @NotNull String sourceSpaceId, @NotNull String sourceDocumentName,
                                                      @Nullable String revision, @NotNull DocumentDuplicateParams documentDuplicateParams) {
        try {
            if (polarionService.getModule(new DocumentIdentifier(documentDuplicateParams.getTargetDocumentIdentifier())) != null) {
                throw new IllegalStateException(String.format("Document '%s' already exists in project '%s' and space '%s'",
                        documentDuplicateParams.getTargetDocumentIdentifier().getName(), documentDuplicateParams.getTargetDocumentIdentifier().getProjectId(),
                        documentDuplicateParams.getTargetDocumentIdentifier().getSpaceId()));
            }
        } catch (ObjectNotFoundException ex) {
            // Target Document doesn't exist, thus we can create it
        }
        if (StringUtils.isEmptyTrimmed(documentDuplicateParams.getTargetDocumentIdentifier().getSpaceId())) {
            throw new IllegalArgumentException("Target 'spaceId' should be provided");
        }
        if (StringUtils.isEmptyTrimmed(documentDuplicateParams.getTargetDocumentTitle())) {
            throw new IllegalArgumentException("Target 'documentName' should be provided");
        }

        if (!polarionService.getTrackerService().getFolderManager().existFolder(documentDuplicateParams.getTargetDocumentIdentifier().getProjectId(), documentDuplicateParams.getTargetDocumentIdentifier().getSpaceId())) {
            IFolder sourceSpace = polarionService.getTrackerService().getFolderManager().getFolder(sourceProjectId, sourceSpaceId);
            TransactionalExecutor.executeInWriteTransaction(transaction -> {
                polarionService.getTrackerService().getFolderManager().createFolder(documentDuplicateParams.getTargetDocumentIdentifier().getProjectId(),
                        documentDuplicateParams.getTargetDocumentIdentifier().getSpaceId(), sourceSpace.getTitleOrName());
                return null;
            });
        }

        IProject targetProject = polarionService.getProject(documentDuplicateParams.getTargetDocumentIdentifier().getProjectId());
        ITrackerProject targetTrackerProject = polarionService.getTrackerService().getTrackerProject(targetProject);
        IContextId targetProjectContextId = targetTrackerProject.getContextId();
        ILocation targetLocation = Location.getLocation(documentDuplicateParams.getTargetDocumentIdentifier().getSpaceId());

        // link role is optional
        String linkRoleId = documentDuplicateParams.getLinkRoleId();
        ILinkRoleOpt linkRole = StringUtils.isEmpty(linkRoleId) ? null :
                Optional.ofNullable(polarionService.getLinkRoleById(linkRoleId, targetTrackerProject)).orElseThrow(() -> new IllegalArgumentException(String.format("No link role could be found by ID '%s'", linkRoleId)));

        IModule sourceModule = polarionService.getModule(sourceProjectId, sourceSpaceId, sourceDocumentName, revision);
        DiffModel diffModel = DiffModelCachedResource.get(documentDuplicateParams.getTargetDocumentIdentifier().getProjectId(),
                documentDuplicateParams.getConfigName(), null);
        DocumentCopyContext context = new DocumentCopyContext(linkRoleId, null, diffModel);
        List<DiffField> allowedFields = diffModel.getDiffFields();

        // ---------
        // Following calls are intentionally split into separate transactions as list fields during documents duplication
        // are becoming visible in Polarion's API only as soon as document's creation transaction is closed, meaning that cleaning them up
        // should be done in a separate transaction

        // Taken before the references are handled, which can take referenced work items out of the duplicate's structure
        List<WorkItemCopy> workItemCopies = new ArrayList<>();
        IModule targetModule = Objects.requireNonNull(TransactionalExecutor.executeInWriteTransaction(transaction -> {
            IModule createdModule = polarionService.getTrackerService().getModuleManager().duplicate(sourceModule, targetProject, targetLocation,
                    documentDuplicateParams.getTargetDocumentIdentifier().getName(), linkRole, null, null, null, null);
            workItemCopies.addAll(pairWithCopies(sourceModule, createdModule));
            createdModule.setTitle(documentDuplicateParams.getTargetDocumentTitle());
            createdModule.updateTitleHeading(documentDuplicateParams.getTargetDocumentTitle());
            if (!createdModule.getExternalWorkItems().isEmpty()) {
                createdModule.getExternalWorkItems().forEach(workItem -> mergeService.fixReferencedWorkItem(workItem, createdModule, context, documentDuplicateParams.getHandleReferences()));
            }
            createdModule.save();
            return createdModule;
        }));

        Map<String, String> oldToNewCommentIdMap = documentDuplicateParams.isCopyDocumentComments()
                ? Objects.requireNonNull(TransactionalExecutor.executeInWriteTransaction(transaction -> copyModuleComments(sourceModule, targetModule)))
                : Map.of();

        TransactionalExecutor.executeInWriteTransaction(transaction -> {
            // The work items are written in this transaction only, which saves them all: a work item saved in a
            // transaction of its own before would be saved here once more from a state older than that save,
            // which Polarion refuses as a change conflict
            copyCommentMarkersIntoWorkItems(targetModule, oldToNewCommentIdMap, allowedFields, workItemCopies);
            if (linkRole != null && !sourceProjectId.equals(documentDuplicateParams.getTargetDocumentIdentifier().getProjectId())) {
                fixLinksInRichTextFields(targetModule, sourceProjectId, linkRole.getId(), allowedFields);
            }
            cleanUpFields(targetModule, targetProjectContextId, allowedFields, new NonListFieldCleaner());
            cleanUpFields(targetModule, targetProjectContextId, allowedFields, new ListFieldCleaner());
            return null;
        });

        // ----------

        return DocumentIdentifier.builder()
                .projectId(documentDuplicateParams.getTargetDocumentIdentifier().getProjectId())
                .spaceId(documentDuplicateParams.getTargetDocumentIdentifier().getSpaceId())
                .name(Objects.requireNonNull(targetModule).getModuleName())
                .moduleXmlRevision(getLastRevisionOfCreatedModule(targetModule))
                .build();
    }

    /**
     * Polarion can fail to find the history of a module created moments ago ({@code UnknownObjectException}).
     * The document is already created at this point, so the caller gets it without a revision instead of an error.
     * Polarion does the same in {@code com.polarion.alm.server.api.model.PObjectRevisionHandler#getLastRevision()}:
     * it catches the {@code WrapperException} of {@code IPObject.getLastRevision()} and returns {@code null}.
     */
    private @Nullable String getLastRevisionOfCreatedModule(@NotNull IModule module) {
        try {
            return module.getLastRevision();
        } catch (Exception e) {
            logger.warn(String.format("Cannot read the revision of the created document '%s'", module.getModuleName()), e);
            return null;
        }
    }

    @VisibleForTesting
    void fixLinksInRichTextFields(@NotNull IModule targetModule, @NotNull String sourceProjectId, @NotNull String linkRoleId, @NotNull List<DiffField> diffFields) {
        for (IWorkItem targetWorkItem : getWorkItemsForCleanUp(targetModule)) {
            for (DiffField field : diffFields) {
                Object fieldValue = polarionService.getFieldValue(targetWorkItem, field.getKey());
                if (fieldValue instanceof Text text) {
                    IWorkItem sourceWorkItem = polarionService.getPairedWorkItems(targetWorkItem, sourceProjectId, linkRoleId).stream().findFirst().orElse(null);
                    if (sourceWorkItem != null) {
                        fieldValue = new Text(text.getType(), polarionService.replaceLinksToPairedWorkItems(sourceWorkItem, targetWorkItem, linkRoleId, text.getContent()));
                        polarionService.setFieldValue(targetWorkItem, field.getKey(), fieldValue);
                    }
                }
            }
        }
    }

    @VisibleForTesting
    void cleanUpFields(@NotNull IModule module, @NotNull IContextId projectContextId, @NotNull List<DiffField> allowedFields, @NotNull FieldCleaner cleaner) {
        List<WorkItemField> standardFieldsDeletable = polarionService.getStandardFields().stream().filter(PolarionService.deletableFieldsFilter).toList();
        for (IWorkItem workItem : getWorkItemsForCleanUp(module)) {
            for (WorkItemField field : polarionService.getDeletableFields(workItem, projectContextId, standardFieldsDeletable)) {
                if (polarionService.isFieldToCleanUp(allowedFields, field, workItem.getType())) {
                    cleaner.clean(workItem, field);
                }
            }
            workItem.save();
        }
    }

    private List<IWorkItem> getWorkItemsForCleanUp(@NotNull IModule module) {
        List<IWorkItem> externalWorkItems = module.getExternalWorkItems();
        return module.getAllWorkItems().stream().filter(item -> {
            if (item.isUnresolvable() || externalWorkItems.contains(item)) {
                return false;
            }
            if (item instanceof WorkItem workItem) {
                return !workItem.isHeading();
            } else {
                return true;
            }
        }).toList();
    }

    /**
     * Copies the comments of the source document and the markers of its page. The markers in the work items are
     * pointed at the copied comments separately ({@link #copyCommentMarkersIntoWorkItems}).
     *
     * @return ID of a source comment mapped to the ID of its copy
     */
    @VisibleForTesting
    @NotNull
    Map<String, String> copyModuleComments(@NotNull IModule sourceModule, @NotNull IModule targetModule) {
        Map<String, String> oldToNewCommentIdMap = commentsCopier.copyComments(sourceModule, targetModule, c -> true, targetModule.getAuthor());
        if (!oldToNewCommentIdMap.isEmpty()) {
            copyCommentMarkers(sourceModule, targetModule, oldToNewCommentIdMap);
        }
        return oldToNewCommentIdMap;
    }

    /**
     * Points the comment markers in the fields of the duplicated work items at the copied comments.
     *
     * @param oldToNewCommentIdMap ID of a source comment mapped to the ID of its copy, empty when no comments were copied
     */
    @VisibleForTesting
    void copyCommentMarkersIntoWorkItems(@NotNull IModule targetModule, @NotNull Map<String, String> oldToNewCommentIdMap,
                                         @NotNull List<DiffField> fields, @NotNull List<WorkItemCopy> workItemCopies) {
        if (oldToNewCommentIdMap.isEmpty()) {
            return;
        }
        remapCommentMarkersInWorkItems(targetModule, oldToNewCommentIdMap, fields);
        // After the remapping: a description restored with markers which already name the copied comments
        // must not have them renumbered once more
        restoreDescriptionCommentMarkers(targetModule, workItemCopies, oldToNewCommentIdMap);
    }

    /**
     * Pairs the work items of a document with their duplicates. A duplicate is not linked to its origin, but it holds
     * the same position in the document structure, which Polarion's duplicate keeps. Referenced work items are not
     * paired: they are not duplicated. If the two structures differ in size, the positions do not correspond, and
     * nothing is paired.
     * <p>
     * To be taken right after the duplicate is made: handling its references can take nodes out of its structure.
     */
    @VisibleForTesting
    @NotNull
    List<WorkItemCopy> pairWithCopies(@NotNull IModule sourceModule, @NotNull IModule targetModule) {
        List<IModule.IStructureNode> sourceNodes = structureNodes(sourceModule);
        List<IModule.IStructureNode> targetNodes = structureNodes(targetModule);
        if (sourceNodes.size() != targetNodes.size()) {
            logger.warn("Work items of document '%s' were not paired with their duplicates in '%s': the documents differ in structure"
                    .formatted(sourceModule.getModuleName(), targetModule.getModuleName()));
            return List.of();
        }
        List<WorkItemCopy> workItemCopies = new ArrayList<>();
        for (int i = 0; i < sourceNodes.size(); i++) {
            IModule.IStructureNode sourceNode = sourceNodes.get(i);
            IModule.IStructureNode targetNode = targetNodes.get(i);
            if (!sourceNode.isExternal() && !targetNode.isExternal() && sourceNode.getWorkItem() != null && targetNode.getWorkItem() != null) {
                workItemCopies.add(new WorkItemCopy(sourceNode.getWorkItem(), targetNode.getWorkItem()));
            }
        }
        return workItemCopies;
    }

    /**
     * Gives the duplicated work items back the comment markers of their descriptions. Polarion's duplicate takes every
     * comment marker out of a description ({@code HTMLCleaner.cleanWiDesc}), while the other fields keep theirs.
     * <p>
     * Work items with the same description must not swap their markers, so a copy takes the markers of the work item
     * it was paired with by position ({@link #pairWithCopies}). It takes them only if both have the same type and
     * the cleaned description of the origin equals the copy's description exactly.
     * <p>
     * A copy is written through the instance the document holds, the one the clean-up of the copy reads and saves
     * in the same transaction. A change made through another instance of the work item could be lost when the
     * clean-up saves its own.
     * <p>
     * A copy is looked up by its project and ID among the document's own work items: a work item referenced from
     * another project can have the same ID.
     */
    @VisibleForTesting
    void restoreDescriptionCommentMarkers(@NotNull IModule targetModule, @NotNull List<WorkItemCopy> workItemCopies, @NotNull Map<String, String> oldToNewCommentIdMap) {
        List<IWorkItem> externalWorkItems = targetModule.getExternalWorkItems();
        Map<WorkItemKey, IWorkItem> currentWorkItems = new HashMap<>();
        for (IWorkItem workItem : targetModule.getAllWorkItems()) {
            if (!externalWorkItems.contains(workItem)) {
                currentWorkItems.putIfAbsent(WorkItemKey.of(workItem), workItem);
            }
        }
        for (WorkItemCopy workItemCopy : workItemCopies) {
            IWorkItem copy = currentWorkItems.get(WorkItemKey.of(workItemCopy.copy()));
            if (copy != null) {
                restoreDescriptionCommentMarkers(workItemCopy.origin(), copy, oldToNewCommentIdMap);
            }
        }
    }

    private void restoreDescriptionCommentMarkers(@NotNull IWorkItem origin, @NotNull IWorkItem copy, @NotNull Map<String, String> oldToNewCommentIdMap) {
        if (origin.isUnresolvable() || copy.isUnresolvable() || !Objects.equals(typeId(origin), typeId(copy))) {
            return;
        }
        Text originDescription = origin.getDescription();
        Text copyDescription = copy.getDescription();
        if (originDescription == null || copyDescription == null || CommentUtils.extractCommentIds(originDescription.getContent()).isEmpty()
                || !HTMLCleaner.cleanWiDesc(originDescription, true, true).getContent().equals(copyDescription.getContent())) {
            return;
        }
        copy.setDescription(new Text(originDescription.getType(), CommentUtils.remapComments(originDescription.getContent(), oldToNewCommentIdMap)));
        copy.save();
    }

    /**
     * The nodes of a document's structure in document order.
     */
    private @NotNull List<IModule.IStructureNode> structureNodes(@NotNull IModule module) {
        List<IModule.IStructureNode> nodes = new ArrayList<>();
        IModule.IStructureNode rootNode = module.getRootNode();
        if (rootNode != null) {
            rootNode.getChildren().forEach(child -> collectStructureNodes(child, nodes));
        }
        return nodes;
    }

    private void collectStructureNodes(@NotNull IModule.IStructureNode node, @NotNull List<IModule.IStructureNode> nodes) {
        nodes.add(node);
        node.getChildren().forEach(child -> collectStructureNodes(child, nodes));
    }

    private static @Nullable String typeId(@NotNull IWorkItem workItem) {
        return workItem.getType() == null ? null : workItem.getType().getId();
    }

    /**
     * Points the comment markers in the fields of the duplicated work items at the copied comments. A comment written
     * on the text of a work item is a document comment, with its marker in a field of that work item. A duplicated
     * work item keeps the markers of the source document. A referenced work item is left alone: its markers name the
     * comments of the document it belongs to.
     */
    @VisibleForTesting
    void remapCommentMarkersInWorkItems(@NotNull IModule targetModule, @NotNull Map<String, String> oldToNewCommentIdMap, @NotNull List<DiffField> fields) {
        List<IWorkItem> externalWorkItems = targetModule.getExternalWorkItems();
        for (IWorkItem workItem : targetModule.getAllWorkItems()) {
            if (workItem.isUnresolvable() || externalWorkItems.contains(workItem)) {
                continue;
            }
            boolean modified = false;
            for (String fieldKey : fieldKeysOf(workItem, fields)) {
                if (polarionService.getFieldValue(workItem, fieldKey) instanceof Text text) {
                    String remapped = CommentUtils.remapComments(text.getContent(), oldToNewCommentIdMap);
                    if (!remapped.equals(text.getContent())) {
                        polarionService.setFieldValue(workItem, fieldKey, new Text(text.getType(), remapped));
                        modified = true;
                    }
                }
            }
            if (modified) {
                workItem.save();
            }
        }
    }

    /**
     * The keys of the fields configured for the type of a work item, each key once: the markers of a field remapped
     * twice would be read the second time as markers of the source document, and taken out.
     */
    private @NotNull Set<String> fieldKeysOf(@NotNull IWorkItem workItem, @NotNull List<DiffField> fields) {
        String typeId = typeId(workItem);
        return fields.stream()
                .filter(field -> field.getWiTypeId() == null || field.getWiTypeId().equals(typeId))
                .map(DiffField::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void copyCommentMarkers(@NotNull IModule sourceModule, @NotNull IModule targetModule,
                                    @NotNull Map<String, String> oldToNewCommentIdMap) {
        if (sourceModule.getHomePageContent() == null || targetModule.getHomePageContent() == null) {
            return;
        }
        if (CommentUtils.extractCommentIds(sourceModule.getHomePageContent().getContent()).isEmpty()) {
            return;
        }
        String updatedContent = CommentUtils.copyCommentMarkers(sourceModule.getHomePageContent().getContent(), targetModule.getHomePageContent().getContent(), oldToNewCommentIdMap);
        targetModule.setHomePageContent(new Text(targetModule.getHomePageContent().getType(), updatedContent));
        targetModule.save();
    }

    /**
     * A work item of the source document and its duplicate.
     */
    @VisibleForTesting
    record WorkItemCopy(@NotNull IWorkItem origin, @NotNull IWorkItem copy) {
    }

    /**
     * Identifies a work item across projects: a work item ID is unique only within its project.
     */
    private record WorkItemKey(@Nullable String projectId, @Nullable String id) {
        static @NotNull WorkItemKey of(@NotNull IWorkItem workItem) {
            return new WorkItemKey(workItem.getProjectId(), workItem.getId());
        }
    }

}
