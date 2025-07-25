package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentDuplicateParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.WorkItemAttachmentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DiffField;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentRevision;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.AuthorizationModel;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.LinkRole;
import ch.sbb.polarion.extension.diff_tool.service.cleaners.FieldCleaner;
import ch.sbb.polarion.extension.diff_tool.service.cleaners.ListFieldCleaner;
import ch.sbb.polarion.extension.diff_tool.service.cleaners.NonListFieldCleaner;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.LinksHandler;
import ch.sbb.polarion.extension.diff_tool.settings.AuthorizationSettings;
import ch.sbb.polarion.extension.diff_tool.util.DiffModelCachedResource;
import ch.sbb.polarion.extension.diff_tool.util.DocumentWorkItemsCache;
import ch.sbb.polarion.extension.diff_tool.util.RequestContextUtil;
import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IFolder;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.server.api.model.document.ProxyDocument;
import com.polarion.alm.server.rt.parts.Renderer;
import com.polarion.alm.shared.api.model.ModelObjectReference;
import com.polarion.alm.shared.api.model.document.DocumentReference;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.dle.document.DocumentRenderer;
import com.polarion.alm.shared.dle.document.DocumentRendererParameters;
import com.polarion.alm.shared.dle.document.WorkItemFieldReference;
import com.polarion.alm.shared.rt.RichTextRenderingContext;
import com.polarion.alm.shared.rt.parts.FieldRichTextRenderer;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.internal.baseline.BaseObjectBaselinesSearch;
import com.polarion.alm.tracker.internal.model.WorkItem;
import com.polarion.alm.tracker.model.IAttachment;
import com.polarion.alm.tracker.model.IBaseline;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IStatusOpt;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.model.ipi.IInternalBaselinesManager;
import com.polarion.alm.ui.shared.FieldRenderType;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.core.util.types.Text;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.persistence.ICustomFieldsService;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.persistence.model.IPrototype;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.data.identification.IContextId;
import com.polarion.subterra.base.data.model.ICustomField;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nullable;
import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("squid:S2176") // class named same as parent intentionally
public class PolarionService extends ch.sbb.polarion.extension.generic.service.PolarionService {

    private static final Logger logger = Logger.getLogger(PolarionService.class);
    static final Predicate<WorkItemField> deletableFieldsFilter = field -> !field.isReadOnly();
    private static final Set<String> fieldsNotToCleanUp = new HashSet<>(Arrays.asList(
            IUniqueObject.KEY_PROJECT,
            IWorkItem.KEY_MODULE,
            IWorkflowObject.KEY_TYPE,
            IWorkItem.KEY_OUTLINE_NUMBER,
            IWorkItem.KEY_RESOLUTION
    ));

    @Getter
    private final DocumentWorkItemsCache documentWorkItemsCache = DocumentWorkItemsCache.getInstance();

    public PolarionService() {
        super();
    }

    public PolarionService(ITrackerService trackerService, IProjectService projectService, ISecurityService securityService, IPlatformService platformService, IRepositoryService repositoryService) {
        super(trackerService, projectService, securityService, platformService, repositoryService);
    }

    @NotNull
    public List<IProject> getProjects() {
        return getProjectService().searchProjects("", "name").stream()
                .filter(project -> project.can().read())
                .toList();
    }

    @NotNull
    public List<IFolder> getSpaces(@NotNull String projectId) {
        return getTrackerService().getFolderManager().getFolders(projectId).stream()
                .filter(folder -> folder.can().read())
                .toList();
    }

    @NotNull
    public List<IModule> getDocuments(@NotNull String projectId, @NotNull String spaceId) {
        return getTrackerService().getModuleManager().getModules(getProject(projectId), Location.getLocation(spaceId)).stream()
                .filter(module -> module.can().read())
                .toList();
    }

    public List<DocumentRevision> getDocumentRevisions(IModule module) {

        IInternalBaselinesManager baselinesManager = (IInternalBaselinesManager) getTrackerService().getTrackerProject(module.getProject().getId()).getBaselinesManager();
        IPObjectList<IBaseline> projectBaselines = baselinesManager.getBaselines();
        // taken from 'BaselinesManager.getBaselines(@NotNull IPObject baseObject)' but 'includeProjectBaselines' set to 'false'
        IPObjectList<IBaseline> moduleBaselines = new BaseObjectBaselinesSearch(module, baselinesManager).includeProjectBaselines(false).resolve(true).execute();

        return ((com.polarion.alm.tracker.internal.model.module.Module) module).getObjectHistory().getItems().stream()
                .map(h -> DocumentRevision.builder()
                        .name(h.getRevision().getName())
                        .baselineName(getRevisionBaselineName(h.getRevision().getName(), projectBaselines, moduleBaselines))
                        .build())
                .sorted().toList();
    }

    private String getRevisionBaselineName(String revision, IPObjectList<IBaseline> projectBaselines, IPObjectList<IBaseline> moduleBaselines) {
        StringBuilder baselineNameBuilder = new StringBuilder();

        projectBaselines.stream().filter(b -> b.getBaseRevision().equals(revision)).findFirst()
                .ifPresent(projectBaseline -> baselineNameBuilder.append("pb. ").append(projectBaseline.getName()));

        moduleBaselines.stream().filter(b -> b.getBaseRevision().equals(revision)).findFirst()
                .ifPresent(moduleBaseline -> baselineNameBuilder.append(baselineNameBuilder.isEmpty() ? "" : " | ").append("db. ").append(moduleBaseline.getName()));

        return baselineNameBuilder.toString();
    }

    /**
     * Used in cases when we need current module revision explicitly set (otherwise looks like revision is null for HEAD revision items)
     */
    public @NotNull IModule getDocumentWithFilledRevision(@NotNull String projectId, @NotNull String spaceId, @NotNull String documentName, @Nullable String revision) {
        IModule module = super.getModule(projectId, spaceId, documentName, revision);
        return (revision == null || module.getRevision() != null) ? module : getModule(projectId, spaceId, documentName, getDocumentRevisions(module).get(0).getName());
    }

    /**
     * Creates a new document as a duplicate of source document
     */
    public DocumentIdentifier createDocumentDuplicate(@NotNull String sourceProjectId, @NotNull String sourceSpaceId, @NotNull String sourceDocumentName,
                                                      @Nullable String revision, @NotNull DocumentDuplicateParams documentDuplicateParams) {
        try {
            if (getModule(new DocumentIdentifier(documentDuplicateParams.getTargetDocumentIdentifier())) != null) {
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

        if (!trackerService.getFolderManager().existFolder(documentDuplicateParams.getTargetDocumentIdentifier().getProjectId(), documentDuplicateParams.getTargetDocumentIdentifier().getSpaceId())) {
            IFolder sourceSpace = trackerService.getFolderManager().getFolder(sourceProjectId, sourceSpaceId);
            TransactionalExecutor.executeInWriteTransaction(transaction -> {
                trackerService.getFolderManager().createFolder(documentDuplicateParams.getTargetDocumentIdentifier().getProjectId(),
                        documentDuplicateParams.getTargetDocumentIdentifier().getSpaceId(), sourceSpace.getTitleOrName());
                return null;
            });
        }

        IProject targetProject = getProject(documentDuplicateParams.getTargetDocumentIdentifier().getProjectId());
        ITrackerProject targetTrackerProject = trackerService.getTrackerProject(targetProject);
        IContextId targetProjectContextId = targetTrackerProject.getContextId();
        ILocation targetLocation = Location.getLocation(documentDuplicateParams.getTargetDocumentIdentifier().getSpaceId());

        // link role is optional
        String linkRoleId = documentDuplicateParams.getLinkRoleId();
        ILinkRoleOpt linkRole = StringUtils.isEmpty(linkRoleId) ? null :
                Optional.ofNullable(getLinkRoleById(linkRoleId, targetTrackerProject)).orElseThrow(() -> new IllegalArgumentException(String.format("No link role could be found by ID '%s'", linkRoleId)));

        IModule sourceModule = getModule(sourceProjectId, sourceSpaceId, sourceDocumentName, revision);
        List<DiffField> allowedFields = DiffModelCachedResource.get(documentDuplicateParams.getTargetDocumentIdentifier().getProjectId(),
                documentDuplicateParams.getConfigName(), null).getDiffFields();

        // ---------
        // Following calls are intentionally split into separate transactions as list fields during documents duplication
        // are becoming visible in Polarion's API only as soon as document's creation transaction is closed, meaning that cleaning them up
        // should be done in a separate transaction
        IModule targetModule = Objects.requireNonNull(TransactionalExecutor.executeInWriteTransaction(transaction -> {
            IModule createdModule = trackerService.getModuleManager().duplicate(sourceModule, targetProject, targetLocation,
                    documentDuplicateParams.getTargetDocumentIdentifier().getName(), linkRole, null, null, null, null);
            createdModule.setTitle(documentDuplicateParams.getTargetDocumentTitle());
            createdModule.updateTitleHeading(documentDuplicateParams.getTargetDocumentTitle());
            createdModule.save();

            cleanUpFields(createdModule, targetProjectContextId, allowedFields, new NonListFieldCleaner());

            return createdModule;
        }));

        TransactionalExecutor.executeInWriteTransaction(transaction -> {
            if (linkRole != null && !sourceProjectId.equals(documentDuplicateParams.getTargetDocumentIdentifier().getProjectId())) {
                fixLinksInRichTextFields(targetModule, sourceProjectId, linkRole.getId(), allowedFields);
            }
            cleanUpFields(targetModule, targetProjectContextId, allowedFields, new ListFieldCleaner());
            return null;
        });

        if (linkRole != null) {
            TransactionalExecutor.executeInWriteTransaction(transaction -> {
                for (IWorkItem workItem : targetModule.getExternalWorkItems()) {
                    fixReferencedWorkItem(workItem, targetModule, linkRole);
                }
                targetModule.save();
                return null;
            });
        }
        // ----------

        return DocumentIdentifier.builder()
                .projectId(documentDuplicateParams.getTargetDocumentIdentifier().getProjectId())
                .spaceId(documentDuplicateParams.getTargetDocumentIdentifier().getSpaceId())
                .name(Objects.requireNonNull(targetModule).getModuleName())
                .moduleXmlRevision(targetModule.getLastRevision())
                .build();
    }

    void cleanUpFields(@NotNull IModule module, @NotNull IContextId projectContextId, @NotNull List<DiffField> allowedFields, @NotNull FieldCleaner cleaner) {
        List<WorkItemField> standardFieldsDeletable = getStandardFields().stream().filter(deletableFieldsFilter).toList();
        for (IWorkItem workItem : getWorkItemsForCleanUp(module)) {
            for (WorkItemField field : getDeletableFields(workItem, projectContextId, standardFieldsDeletable)) {
                if (isFieldToCleanUp(allowedFields, field, workItem.getType())) {
                    cleaner.clean(workItem, field);
                }
            }
            workItem.save();
        }
    }

    @VisibleForTesting
    void fixLinksInRichTextFields(@NotNull IModule targetModule, @NotNull String sourceProjectId, @NotNull String linkRoleId, @NotNull List<DiffField> diffFields) {
        for (IWorkItem targetWorkItem : getWorkItemsForCleanUp(targetModule)) {
            for (DiffField field : diffFields) {
                Object fieldValue = getFieldValue(targetWorkItem, field.getKey());
                if (fieldValue instanceof Text text) {
                    IWorkItem sourceWorkItem = getPairedWorkItems(targetWorkItem, sourceProjectId, linkRoleId).stream().findFirst().orElse(null);
                    if (sourceWorkItem != null) {
                        fieldValue = new Text(text.getType(), replaceLinksToPairedWorkItems(sourceWorkItem, targetWorkItem, linkRoleId, text.getContent()));
                        setFieldValue(targetWorkItem, field.getKey(), fieldValue);
                    }
                }
            }
        }
    }

    /**
     * WARNING: this method just references/un-references work items in module, but does not persist module, which should be done in caller code which should run in write transaction!
     */
    public void fixReferencedWorkItem(@NotNull IWorkItem referencedWorkItem, @NotNull IModule targetModule, @NotNull ILinkRoleOpt linkRole) {
        // If a document contains a work item which is referenced (external) and belongs to a different project,
        // we are trying here to replace it by a reference of its counterpart item belonging to document's project, if possible.
        // If counterpart work item wasn't found such reference will be just removed
        if (!referencedWorkItem.getProjectId().equals(targetModule.getProjectId())) {
            IWorkItem workItemInHead = referencedWorkItem.getRevision() == null ? referencedWorkItem : getWorkItem(referencedWorkItem.getProjectId(), referencedWorkItem.getId());
            IWorkItem pairedWorkItem = getPairedWorkItems(workItemInHead, targetModule.getProjectId(), linkRole.getId()).stream().findFirst().orElse(null);
            if (pairedWorkItem != null) {
                IModule.IStructureNode nodeToBeRemoved = targetModule.getStructureNodeOfWI(referencedWorkItem);
                IModule.IStructureNode parentNode = nodeToBeRemoved.getParent();
                int destinationIndex = parentNode.getChildren().indexOf(nodeToBeRemoved);

                insertWorkItem(pairedWorkItem, targetModule, parentNode, destinationIndex, true);
            }
            targetModule.unreference(referencedWorkItem);
        }
    }

    public void insertWorkItem(@NotNull IWorkItem workItem, @NotNull IModule targetModule, @Nullable IModule.IStructureNode parentNode, int destinationIndex, boolean referenced) {
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

    List<WorkItemField> getDeletableFields(@NotNull IWorkItem workItem, @NotNull IContextId projectContextId, @NotNull List<WorkItemField> standardFieldsDeletable) {
        List<WorkItemField> deletableFields = new ArrayList<>(standardFieldsDeletable);
        if (workItem.getType() != null) {
            deletableFields.addAll(getCustomFields(projectContextId, workItem.getType()).stream().filter(deletableFieldsFilter).toList());
        }
        return deletableFields;
    }

    boolean isFieldToCleanUp(List<DiffField> allowedFields, WorkItemField fieldToCheck, @Nullable ITypeOpt workItemType) {
        if (fieldsNotToCleanUp.contains(fieldToCheck.getKey())) {
            return false; // Certain fields should never be cleaned up, eg. project, module, outline number etc.
        }
        for (DiffField allowedField : allowedFields) {
            if (allowedField.getWiTypeId() == null || (workItemType != null && workItemType.getId().equals(allowedField.getWiTypeId()))) {
                if (fieldToCheck.getKey().equals(allowedField.getKey())) {
                    return false; // If field is configured to be copied then it shouldn't be cleaned up
                }
            }
        }
        return true;
    }

    @NotNull
    public Collection<ILinkRoleOpt> getLinkRoles(@NotNull String projectId) {
        Set<ILinkRoleOpt> linkRoles = new LinkedHashSet<>();

        ITrackerProject trackerProject = getTrackerProject(projectId);
        List<ITypeOpt> wiTypes = trackerProject.getWorkItemTypeEnum().getAllOptions();
        for (ITypeOpt wiType : wiTypes) {
            Collection<ILinkRoleOpt> roles = trackerProject.getWorkItemLinkRoleEnum().getAvailableOptions(wiType.getId());
            linkRoles.addAll(roles);
        }

        return linkRoles;
    }

    public ILinkRoleOpt getLinkRoleById(@NotNull String linkRoleId, @NotNull ITrackerProject trackerProject) {
        List<ITypeOpt> wiTypes = trackerProject.getWorkItemTypeEnum().getAllOptions();
        for (ITypeOpt wiType : wiTypes) {
            Collection<ILinkRoleOpt> linkRoles = trackerProject.getWorkItemLinkRoleEnum().getAvailableOptions(wiType.getId());

            Optional<ILinkRoleOpt> matchedRole = linkRoles.stream().filter(linkRole -> linkRole.getId().equals(linkRoleId)).findFirst();
            if (matchedRole.isPresent()) {
                return matchedRole.get();
            }
        }
        return null;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public Collection<IStatusOpt> getWorkItemStatuses(@NotNull String projectId) {
        Set<IStatusOpt> statuses = new LinkedHashSet<>();

        ITrackerProject trackerProject = getTrackerProject(projectId);
        List<ITypeOpt> wiTypes = trackerProject.getWorkItemTypeEnum().getAllOptions();
        for (ITypeOpt wiType : wiTypes) {
            trackerProject.getStatusEnum().getAvailableOptions(wiType.getId()).forEach(status -> {
                if (status instanceof IStatusOpt statusOpt) {
                    statuses.add(statusOpt);
                }
            });
        }

        return statuses;
    }

    @NotNull
    public Collection<LinkRole> getHyperlinkRoles(@NotNull String projectId) {
        Set<LinkRole> roles = new LinkedHashSet<>();
        ITrackerProject trackerProject = getTrackerProject(projectId);
        for (ITypeOpt wiType : trackerProject.getWorkItemTypeEnum().getAllOptions()) {
            roles.addAll(trackerProject.getHyperlinkRoleEnum()
                    .getAvailableOptions(wiType.getId()).stream()
                    .map(e -> new LinkRole(e.getId(), e.getName(), wiType.getId(), wiType.getName())).toList());
        }
        return roles;
    }

    @NotNull
    public Collection<LinkRole> getLinkedWorkItemRoles(@NotNull String projectId) {
        ITrackerProject trackerProject = getTrackerProject(projectId);
        return trackerProject.getWorkItemLinkRoleEnum()
                .getAllOptions().stream()
                .map(e -> LinkRole.builder().id(e.getId()).name(e.getName()).build()).toList();
    }

    @SneakyThrows
    public List<WorkItemsPair> getPairedWorkItems(@NotNull IModule leftDocument, @NotNull IModule rightDocument, @NotNull ILinkRoleOpt linkRole, @NotNull List<String> statusesToIgnore) {
        CalculatePairsContext context = new CalculatePairsContext(leftDocument, rightDocument, linkRole, statusesToIgnore);

        // First we need to use sets for both direct and inverse pairs to exclude duplications
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CompletableFuture<Set<Pair<IWorkItem, IWorkItem>>> pairsFuture = CompletableFuture.supplyAsync(() -> leftDocument.getAllWorkItems().stream()
                .map(workItem -> selectOutlinePairedWorkItems(workItem, false, context))
                .flatMap(Set::stream)
                .collect(Collectors.toSet()), executor);

        CompletableFuture<Set<Pair<IWorkItem, IWorkItem>>> inversePairsFuture = CompletableFuture.supplyAsync(() -> rightDocument.getAllWorkItems().stream()
                .map(workItem -> selectOutlinePairedWorkItems(workItem, true, context))
                .flatMap(Set::stream)
                .collect(Collectors.toSet()), executor);

        CompletableFuture<Void> completableFuture = CompletableFuture.allOf(pairsFuture, inversePairsFuture);
        completableFuture.join();

        Set<Pair<IWorkItem, IWorkItem>> pairsSet = pairsFuture.get();
        Set<Pair<IWorkItem, IWorkItem>> inversePairsSet = inversePairsFuture.get();
        executor.shutdown();

        // Then we put those pairs from inverse set which have not empty left part to direct set
        inversePairsSet.stream().filter(pair -> pair.getLeft() != null).forEach(pairsSet::add);

        // When we have full collections of all pairs which left WorkItem is not empty - sort it by outline number of left WorkItem
        List<Pair<IWorkItem, IWorkItem>> pairs = new ArrayList<>(pairsSet);
        pairs.sort(Comparator.comparing(o -> context.getOutlineNumber(o.getLeft(), true), context.getOutlineNumberComparator()));

        // Then iterate over reversed pairs which left WorkItem is null and insert it into right position according to right WorkItems sequence
        inversePairsSet.stream().filter(pair -> pair.getLeft() == null).forEach(pair -> pairs.add(getIndex(pair, pairs, context), pair));

        // Sometimes extra 'orphan' pairs (who has empty left/right part) may occur
        // (e.g. the resulting set may contain pair {EL-1,null} even though {EL-1,EL-2} exists too),
        // so we have to check & remove that kind of data.
        List<Pair<IWorkItem, IWorkItem>> orphansToRemove = new ArrayList<>();
        for (Pair<IWorkItem, IWorkItem> pair : pairs) {
            if ((pair.getLeft() == null && pairs.stream().anyMatch(p -> p.getLeft() != null && Objects.equals(p.getRight(), pair.getRight()))) ||
                    (pair.getRight() == null && pairs.stream().anyMatch(p -> p.getRight() != null && Objects.equals(p.getLeft(), pair.getLeft())))) {
                orphansToRemove.add(pair);
            }
        }

        orphansToRemove.forEach(pairs::remove);

        return pairs.stream().map(pair -> WorkItemsPair.of(pair, context)).toList();
    }

    public String renderField(@NotNull String projectId, @NotNull String workItemId, @NotNull String revision, @NotNull String fieldId, ModelObjectReference mainObjectReference) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(trx -> {
            RichTextRenderTarget renderTarget = RichTextRenderTarget.PDF_EXPORT;
            InternalReadOnlyTransaction transaction = (InternalReadOnlyTransaction) trx;

            WorkItemReference workItemReference = new WorkItemReference(projectId, workItemId, revision, null);

            RichTextRenderingContext renderingContext = new RichTextRenderingContext(transaction.context(), renderTarget);
            renderingContext.setMainObjectReference(mainObjectReference != null ? mainObjectReference : workItemReference); //optional but sometimes it's important (e.g. links rendering inside rich texts)
            renderingContext.setTransaction(transaction);
            renderingContext.setDocumentOutlineNumber(true);

            FieldRichTextRenderer renderer = transaction.documents().createFieldRichTextRenderer();
            renderer.setRichTextRenderingContext(renderingContext);

            HtmlFragmentBuilder fragmentBuilder = renderTarget.selectBuilderTarget(transaction.context().createHtmlFragmentBuilderFor());
            renderer.renderField(fragmentBuilder.html(""), new WorkItemFieldReference(workItemReference, fieldId,
                    IWorkItem.KEY_LINKED_WORK_ITEMS.equals(fieldId) ? FieldRenderType.LINKED_WI_IN_DOC : FieldRenderType.IMGTXT));
            return fragmentBuilder.toString();
        });
    }

    public String renderField(@NotNull IModule module, @NotNull String fieldId) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(trx -> {
            RichTextRenderTarget renderTarget = RichTextRenderTarget.PDF_EXPORT;
            InternalReadOnlyTransaction transaction = (InternalReadOnlyTransaction) trx;

            RichTextRenderingContext renderingContext = new RichTextRenderingContext(transaction.context(), renderTarget);
            renderingContext.setMainObjectReference(DocumentReference.fromModuleLocation(module.getProjectId(), module.getModuleLocation().getLocationPath(), module.getRevision())); //optional but sometimes it's important (e.g. links rendering inside rich texts)
            renderingContext.setTransaction(transaction);
            renderingContext.setDocumentOutlineNumber(true);

            // create a temporary work item instance just to satisfy needs of Renderer constructor
            // this instance won't be saved because we do not call save() on it
            IWorkItem dummyInstance = trackerService.createWorkItem(module.getProject());

            HtmlFragmentBuilder fragmentBuilder = renderTarget.selectBuilderTarget(transaction.context().createHtmlFragmentBuilderFor());
            Renderer renderer = new Renderer(fragmentBuilder.html(""), renderingContext, dummyInstance, FieldRenderType.IMGTXT, "label", "fieldId", false);
            renderer.renderValue(module.getValue(fieldId));
            return fragmentBuilder.toString();
        });
    }

    public String renderDocumentContentBlock(@NotNull IModule module, @NotNull String contentBlock) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
            DocumentRendererParameters parameters = new DocumentRendererParameters(null, null);
            ProxyDocument document = new ProxyDocument(module, (InternalReadOnlyTransaction) transaction);
            DocumentRenderer documentRenderer = new DocumentRenderer((InternalReadOnlyTransaction) transaction, document, RichTextRenderTarget.PDF_EXPORT, parameters);
            return documentRenderer.render(contentBlock);
        });
    }

    private int getIndex(Pair<IWorkItem, IWorkItem> pairWithEmptyLeft, List<Pair<IWorkItem, IWorkItem>> pairs, CalculatePairsContext context) {
        String outlineNumber = context.getOutlineNumber(pairWithEmptyLeft.getRight(), false);
        for (int i = 0; i < pairs.size(); i++) {
            if (pairs.get(i).getRight() != null) {
                String nextOutlineNumber = context.getOutlineNumber(pairs.get(i).getRight(), false);
                if (context.getOutlineNumberComparator().compare(nextOutlineNumber, outlineNumber) > 0) {
                    return i;
                }
            }
        }
        return pairs.size();
    }

    @SuppressWarnings("squid:S1166") // Initial exception swallowed intentionally
    private Set<Pair<IWorkItem, IWorkItem>> selectOutlinePairedWorkItems(@NotNull IWorkItem workItem, boolean inversePair, @NotNull CalculatePairsContext context) {
        if (notDiffRelated(workItem, inversePair, context)) {
            return new HashSet<>();
        }

        List<IWorkItem> oppositeWorkItems = inversePair ? context.getLeftDocumentWorkItems() : context.getRightDocumentWorkItems();

        IWorkItem oppositeSameWorkItem = oppositeWorkItems.stream().filter(w ->
                !w.isUnresolvable() &&
                        w.getId().equals(workItem.getId()) &&
                        w.getProjectId().equals(workItem.getProjectId()) &&
                        context.getOutlineNumber(w, inversePair) != null).findFirst().orElse(null);

        // If we found same work item in the opposite document - use it
        // In case if we found nothing while comparing same document with different revision - use null as opposite item
        if (oppositeSameWorkItem != null || areEqual(context.getLeftDocument(), context.getRightDocument())) {
            return Set.of(pair(workItem, oppositeSameWorkItem, inversePair));
        }

        Set<Pair<IWorkItem, IWorkItem>> pairs = new HashSet<>();
        AtomicBoolean moduleLinksExist = new AtomicBoolean(false);
        Stream.concat(workItem.getLinkedWorkItemsStructsDirect().stream(), workItem.getLinkedWorkItemsStructsBack().stream()).forEach(linkedWorkItemStruct -> {
            IWorkItem linkedStructItem = linkedWorkItemStruct.getLinkedItem();
            IWorkItem linkedWorkItem = oppositeWorkItems.stream().filter(w -> Objects.equals(w.getId(), linkedStructItem.getId())).findFirst().orElse(null);

            if (linkedWorkItem != null && context.getOutlineNumber(linkedWorkItem, inversePair) != null) {
                moduleLinksExist.set(true);
                if (context.isSuitableLinkRole(linkedWorkItemStruct.getLinkRole()) && (!inversePair || !context.hasStatusToIgnore(linkedWorkItem))) {
                    pairs.add(pair(workItem, linkedWorkItem, inversePair));
                }
            }
        });
        return !moduleLinksExist.get() && pairs.isEmpty() ? Set.of(pair(workItem, null, inversePair)) : pairs;
    }

    private boolean notDiffRelated(@NotNull IWorkItem workItem, boolean inversePair, @NotNull CalculatePairsContext context) {
        return workItem.isUnresolvable() || context.getOutlineNumber(workItem, !inversePair) == null || (!inversePair && context.hasStatusToIgnore(workItem));
    }

    @NotNull
    public String replaceLinksToPairedWorkItems(@NotNull IWorkItem from, @NotNull IWorkItem to, @NotNull String linkRole, @NotNull String html) {
        if (Objects.equals(from.getProjectId(), to.getProjectId())) {
            return html; // do not modify links when copying data between same project work items
        }

        Pattern pattern = Pattern.compile(LinksHandler.LINK_REGEX);
        Matcher matcher = pattern.matcher(html);

        StringBuilder buf = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group();
            String workItemProjectId = ObjectUtils.firstNonNull(matcher.group("workItemProjectId"), from.getProjectId());
            String workItemId = matcher.group("workItemId");
            String revision = matcher.group("workItemRevision");
            IWorkItem workItem;
            try {
                workItem = getWorkItem(workItemProjectId, workItemId, revision);
            } catch (Exception e) {
                logger.error("Cannot get work item %s/%s/%s".formatted(workItemProjectId, workItemId, revision), e);
                continue;
            }
            IWorkItem pairedWorkItem = getPairedWorkItems(workItem, to.getProjectId(), linkRole).stream().findFirst().orElse(null);
            if (pairedWorkItem != null) {
                workItemProjectId = ""; // when we place link to the wi from target project there's no need to set 'data-scope' explicitly
                workItemId = pairedWorkItem.getId();
                revision = pairedWorkItem.getRevision();
            }

            // Even if we don't find paired item we must proceed at least for inserting
            // proper 'data-scope' - otherwise link will be completely broken
            String idEntry = "data-item-id=\"%s\"".formatted(workItemId);
            if (!StringUtils.isEmpty(workItemProjectId)) {
                idEntry = idEntry + " data-scope=\"%s\"".formatted(workItemProjectId);
            }
            if (!StringUtils.isEmpty(revision)) {
                idEntry = idEntry + " data-revision=\"%s\"".formatted(revision);
            }
            matcher.appendReplacement(buf, match
                    .replaceAll("data-scope=\"[^\"]+?\"", "")    // cleanup revision & scope, new attributes will be set below if needed
                    .replaceAll("data-revision=\"[^\"]+?\"", "")
                    .replaceAll("( )+", " ")                     // cleanup duplicated spaces
                    .replaceAll("data-item-id=\"[^\"]+?\"", idEntry));
        }
        matcher.appendTail(buf);
        return buf.toString();
    }

    public @NotNull List<IWorkItem> getWorkItems(@NotNull String projectId, @NotNull List<String> workItemIds) {
        ITrackerProject trackerProject = this.getTrackerProject(projectId);
        return workItemIds.stream()
                .filter(id -> !StringUtils.isEmptyTrimmed(id))
                .map(trackerProject::getWorkItem)
                .filter(workItem -> !workItem.isUnresolvable()).toList();
    }

    @SuppressWarnings("squid:S1166") // Initial exception swallowed intentionally
    public List<IWorkItem> getPairedWorkItems(@NotNull IWorkItem toWorkItem, @NotNull String targetProjectId, @NotNull String linkRoleId) {
        return Stream.concat(toWorkItem.getLinkedWorkItemsStructsDirect().stream(), toWorkItem.getLinkedWorkItemsStructsBack().stream())
                .filter(linkedWorkItemStruct -> linkedWorkItemStruct.getLinkRole() != null && linkedWorkItemStruct.getLinkRole().getId().equals(linkRoleId))
                .map(linkedWorkItemStruct -> {
                    Optional<IWorkItem> optionalLinkedWorkItem = Optional.empty();

                    IWorkItem linkedWorkItem = linkedWorkItemStruct.getLinkedItem();
                    if (linkedWorkItem.isUnresolvable()) {
                        // WorkItem can already be deleted or refer already deleted project, so we read it from certain revision
                        try {
                            optionalLinkedWorkItem = Optional.of(getWorkItem(linkedWorkItem.getProjectId(), linkedWorkItem.getId(), linkedWorkItemStruct.getRevision()));
                        } catch (Exception e) {
                            logger.info("Potential broken link: %s/%s:%s".formatted(linkedWorkItem.getProjectId(), linkedWorkItem.getId(), linkedWorkItemStruct.getRevision()));
                        }
                    } else {
                        optionalLinkedWorkItem = Optional.of(linkedWorkItem);
                    }
                    return optionalLinkedWorkItem;
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(linkedWorkItem -> Objects.equals(targetProjectId, linkedWorkItem.getProjectId()))
                .toList();
    }

    private Pair<IWorkItem, IWorkItem> pair(IWorkItem leftWorkItem, IWorkItem rightWorkItem, boolean inverse) {
        return inverse ? Pair.of(rightWorkItem, leftWorkItem) : Pair.of(leftWorkItem, rightWorkItem);
    }

    private boolean areEqual(@NotNull IModule leftDocument, IModule rightDocument) {
        return rightDocument != null && leftDocument.getProjectId().equals(rightDocument.getProjectId()) && leftDocument.getCreated().equals(rightDocument.getCreated());
    }

    public IAttachment getAttachment(WorkItemAttachmentIdentifier identifier) {
        if (Stream.of(identifier.getProjectId(), identifier.getWorkItemId(), identifier.getAttachmentId()).anyMatch(StringUtils::isEmptyTrimmed)) {
            throw new IllegalArgumentException("All 3 parameters should be provided to locate an attachment: 'projectId', 'workItemId', 'attachmentId'");
        }
        return TransactionalExecutor.executeInReadOnlyTransaction(transaction -> {
            IWorkItem workItem = getWorkItem(identifier.getProjectId(), identifier.getWorkItemId());
            return workItem.getAttachment(identifier.getAttachmentId());
        });
    }

    public List<WorkItemField> getAllWorkItemFields(@NotNull String projectId) {
        List<WorkItemField> fields = new ArrayList<>(getStandardFields());

        ITrackerProject trackerProject = getTrackerProject(projectId);
        IContextId contextId = trackerProject.getContextId();
        List<ITypeOpt> wiTypes = trackerProject.getWorkItemTypeEnum().getAllOptions();
        for (ITypeOpt wiType : wiTypes) {
            fields.addAll(getCustomFields(contextId, wiType));
        }

        Collections.sort(fields);
        return fields;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    List<WorkItemField> getStandardFields() {
        IPrototype workItemPrototype = getTrackerService().getDataService().getPrototype(IWorkItem.PROTO);
        IPObject workItemInstance = getTrackerService().getDataService().createInstance(IWorkItem.PROTO);

        List<String> keyNames = new ArrayList<>(workItemPrototype.getKeyNames());
        return keyNames.stream()
                .filter(workItemPrototype::isKeyDefined)
                .map(keyName -> WorkItemField.builder()
                        .key(keyName)
                        .name(workItemInstance.getFieldLabel(keyName)) // Field label can be obtained only via work item instance, attempt to obtain it via prototype throws UnsupportedOperationException
                        .required(workItemPrototype.isKeyRequired(keyName))
                        .readOnly(workItemPrototype.isKeyReadOnly(keyName))
                        .build())
                .toList();
    }

    List<WorkItemField> getCustomFields(@NotNull IContextId contextId, @NotNull ITypeOpt workItemType) {
        List<ICustomField> customFields = getCustomFieldsForType(contextId, workItemType);

        return customFields.stream()
                .map(customField -> WorkItemField.builder()
                        .key(customField.getId())
                        .name(customField.getName())
                        .required(customField.isRequired())
                        .customField(true)
                        .defaultValue(customField.getDefaultValue())
                        .wiTypeId(workItemType.getId())
                        .wiTypeName(workItemType.getName())
                        .build())
                .toList();
    }

    private @NotNull List<ICustomField> getCustomFieldsForType(@NotNull IContextId contextId, @NotNull ITypeOpt workItemType) {
        ICustomFieldsService customFieldsService = trackerService.getDataService().getCustomFieldsService();
        final Collection<ICustomField> customFieldsForAllTypes = customFieldsService.getCustomFields(IWorkItem.PROTO, contextId, null); // get custom fields for WorkItem with any type in the project (-- All Types --)
        final Collection<ICustomField> customFieldsForTypeId = customFieldsService.getCustomFields(IWorkItem.PROTO, contextId, workItemType.getId()); // get custom fields for WorkItem with specific type in the project

        return Stream.concat(customFieldsForAllTypes.stream(), customFieldsForTypeId.stream()).toList();
    }

    public void evictDocumentsCache(DocumentIdentifier... documents) {
        Subject userSubject = RequestContextUtil.getUserSubject();
        for (DocumentIdentifier documentIdentifier : documents) {
            IModule document = getDocumentWithFilledRevision(documentIdentifier.getProjectId(), documentIdentifier.getSpaceId(),
                    documentIdentifier.getName(), documentIdentifier.getRevision());
            documentWorkItemsCache.evictDocumentFromCache(document, userSubject);
        }
    }

    public IModule getModule(@NotNull DocumentIdentifier documentIdentifier) {
        return getModule(documentIdentifier.getProjectId(), documentIdentifier.getSpaceId(), documentIdentifier.getName(), documentIdentifier.getRevision());
    }

    public boolean userAuthorizedForMerge(@NotNull String projectId) {
        AuthorizationModel projectCustomFieldsSettingsModel = (AuthorizationModel)
                NamedSettingsRegistry.INSTANCE.getByFeatureName(AuthorizationSettings.FEATURE_NAME).read(
                        ScopeUtils.getScopeFromProject(projectId), SettingId.fromName(NamedSettings.DEFAULT_NAME), null);
        List<String> allowedRoles = projectCustomFieldsSettingsModel.getAllRoles();

        String currentUser = securityService.getCurrentUser();
        Collection<String> globalRoles = securityService.getRolesForUser(currentUser);
        if (globalRoles.stream().anyMatch(allowedRoles::contains)) {
            return true;
        } else {
            IContextId projectContext = getTrackerProject(projectId).getContextId();
            Collection<String> projectRoles = securityService.getRolesForUser(currentUser, projectContext);
            return projectRoles.stream().anyMatch(allowedRoles::contains);
        }
    }

}
