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

        IModule targetModule = Objects.requireNonNull(TransactionalExecutor.executeInWriteTransaction(transaction -> {
            IModule createdModule = polarionService.getTrackerService().getModuleManager().duplicate(sourceModule, targetProject, targetLocation,
                    documentDuplicateParams.getTargetDocumentIdentifier().getName(), linkRole, null, null, null, null);
            createdModule.setTitle(documentDuplicateParams.getTargetDocumentTitle());
            createdModule.updateTitleHeading(documentDuplicateParams.getTargetDocumentTitle());
            if (!createdModule.getExternalWorkItems().isEmpty()) {
                createdModule.getExternalWorkItems().forEach(workItem -> mergeService.fixReferencedWorkItem(workItem, createdModule, context, documentDuplicateParams.getHandleReferences()));
            }
            createdModule.save();
            return createdModule;
        }));

        if (documentDuplicateParams.isCopyDocumentComments()) {
            TransactionalExecutor.executeInWriteTransaction(transaction -> {
                copyModuleComments(sourceModule, targetModule, allowedFields);
                return null;
            });
        }

        TransactionalExecutor.executeInWriteTransaction(transaction -> {
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

    @VisibleForTesting
    void copyModuleComments(@NotNull IModule sourceModule, @NotNull IModule targetModule, @NotNull List<DiffField> fields) {
        Map<String, String> oldToNewCommentIdMap = commentsCopier.copyComments(sourceModule, targetModule, c -> true, targetModule.getAuthor());
        if (oldToNewCommentIdMap.isEmpty()) {
            return;
        }
        copyCommentMarkers(sourceModule, targetModule, oldToNewCommentIdMap);
        remapCommentMarkersInWorkItems(targetModule, oldToNewCommentIdMap, fields);
        // After the remapping: a description restored with markers which already name the copied comments
        // must not have them renumbered once more
        restoreDescriptionCommentMarkers(sourceModule, targetModule, oldToNewCommentIdMap);
    }

    /**
     * Gives the duplicated work items back the comment markers of their descriptions. Polarion's duplicate takes every
     * comment marker out of a description ({@code HTMLCleaner.cleanWiDesc}), while the other fields keep theirs.
     * <p>
     * A duplicate is not linked to its origin, but it holds the same position in the document structure, which the
     * duplicate keeps. Work items are therefore paired by position, those without comments included: two work items
     * with the same description must not swap their markers. A pair is restored only if neither is referenced, both
     * have the same type and the cleaned description of the origin equals the duplicate's description exactly. If
     * the two structures differ in size, the positions do not correspond, and nothing is restored.
     */
    @VisibleForTesting
    void restoreDescriptionCommentMarkers(@NotNull IModule sourceModule, @NotNull IModule targetModule, @NotNull Map<String, String> oldToNewCommentIdMap) {
        List<IModule.IStructureNode> sourceNodes = structureNodes(sourceModule);
        List<IModule.IStructureNode> targetNodes = structureNodes(targetModule);
        if (sourceNodes.size() != targetNodes.size()) {
            logger.warn("Comment markers of the work item descriptions were not restored: document '%s' and its duplicate '%s' differ in structure"
                    .formatted(sourceModule.getModuleName(), targetModule.getModuleName()));
            return;
        }
        for (int i = 0; i < sourceNodes.size(); i++) {
            restoreDescriptionCommentMarkers(sourceNodes.get(i), targetNodes.get(i), oldToNewCommentIdMap);
        }
    }

    private void restoreDescriptionCommentMarkers(@NotNull IModule.IStructureNode sourceNode, @NotNull IModule.IStructureNode targetNode,
                                                  @NotNull Map<String, String> oldToNewCommentIdMap) {
        IWorkItem sourceWorkItem = sourceNode.getWorkItem();
        IWorkItem targetWorkItem = targetNode.getWorkItem();
        if (sourceNode.isExternal() || targetNode.isExternal() || sourceWorkItem == null || targetWorkItem == null
                || sourceWorkItem.isUnresolvable() || targetWorkItem.isUnresolvable()
                || !Objects.equals(typeId(sourceWorkItem), typeId(targetWorkItem))) {
            return;
        }
        Text sourceDescription = sourceWorkItem.getDescription();
        Text targetDescription = targetWorkItem.getDescription();
        if (sourceDescription == null || targetDescription == null || CommentUtils.extractCommentIds(sourceDescription.getContent()).isEmpty()
                || !HTMLCleaner.cleanWiDesc(sourceDescription, true, true).getContent().equals(targetDescription.getContent())) {
            return;
        }
        targetWorkItem.setDescription(new Text(sourceDescription.getType(), CommentUtils.remapComments(sourceDescription.getContent(), oldToNewCommentIdMap)));
        targetWorkItem.save();
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

}
