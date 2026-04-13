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
import com.polarion.alm.tracker.model.ICommentBase;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IModuleComment;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.core.util.types.Text;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.subterra.base.data.identification.IContextId;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DocumentCopyService {

    private static final Logger log = Logger.getLogger(DocumentCopyService.class);

    private final PolarionService polarionService;
    private final MergeService mergeService;

    public DocumentCopyService(PolarionService polarionService, MergeService mergeService) {
        this.polarionService = polarionService;
        this.mergeService = mergeService;
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
                copyModuleComments(sourceModule, targetModule);
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
                .moduleXmlRevision(targetModule.getLastRevision())
                .build();
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
    void copyModuleComments(@NotNull IModule sourceModule, @NotNull IModule targetModule) {
        IPObjectList<IModuleComment> sourceRootComments = sourceModule.getRootComments(true);
        if (sourceRootComments.isEmpty()) {
            return;
        }

        Map<String, String> oldToNewCommentIdMap = new LinkedHashMap<>();

        for (IModuleComment sourceRootComment : sourceRootComments) {
            IModuleComment newRootComment = targetModule.createComment(sourceRootComment.getText());
            copyCommentMetadata(sourceRootComment, newRootComment, targetModule);
            newRootComment.save();
            oldToNewCommentIdMap.put(sourceRootComment.getId(), newRootComment.getId());
            copyChildComments(sourceRootComment, newRootComment, targetModule, oldToNewCommentIdMap);
            if (sourceRootComment.isResolvedComment()) {
                // The resolved flag can be set only on a root comment.
                // Set it only after all child comments have been created, which requires an additional save.
                newRootComment.setResolvedComment(true);
                newRootComment.save();
            }
        }

        copyCommentMarkers(sourceModule, targetModule, oldToNewCommentIdMap);
    }

    private void copyChildComments(@NotNull IModuleComment sourceParent, @NotNull IModuleComment targetParent, @NotNull IModule targetModule,
                                   @NotNull Map<String, String> oldToNewCommentIdMap) {
        IPObjectList<IModuleComment> children = sourceParent.getChildComments();
        for (IModuleComment sourceChild : children) {
            IModuleComment newChild = targetParent.createChildComment(sourceChild.getText());
            copyCommentMetadata(sourceChild, newChild, targetModule);
            newChild.save();
            oldToNewCommentIdMap.put(sourceChild.getId(), newChild.getId());
            copyChildComments(sourceChild, newChild, targetModule, oldToNewCommentIdMap);
        }
    }

    private void copyCommentMetadata(@NotNull IModuleComment source, @NotNull IModuleComment target, @NotNull IModule targetModule) {
        target.setValue(ICommentBase.KEY_CREATED, source.getCreated());
        try {
            polarionService.getSecurityService().doAsSystemUser((java.security.PrivilegedAction<Void>) () -> {
                target.setValue(ICommentBase.KEY_AUTHOR, source.getAuthor());
                return null;
            });
        } catch (Exception e) {
            String authorName = source.getAuthor() != null ? source.getAuthor().getName() : "<null>";
            log.warn(String.format("Could not assign source comment author [%s] in target module [%s]: %s", authorName, targetModule.getModuleLocation(), e.getMessage()));
            try {
                target.setValue(ICommentBase.KEY_AUTHOR, targetModule.getAuthor());
                log.warn("Fallback: target module author assigned as a comment author: " + e.getMessage());
            } catch (Exception ex) {
                log.warn("Could not assign target module author as a comment author (as a fallback): " + ex.getMessage());
            }
        }
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
