package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentRevision;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import ch.sbb.polarion.extension.diff_tool.rest.model.WorkItemAttachmentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.AuthorizationModel;
import ch.sbb.polarion.extension.diff_tool.settings.AuthorizationSettings;
import ch.sbb.polarion.extension.diff_tool.util.DocumentWorkItemsCache;
import ch.sbb.polarion.extension.diff_tool.util.RequestContextUtil;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.google.common.collect.Streams;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IFolder;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.model.ModelObjectReference;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.dle.document.WorkItemFieldReference;
import com.polarion.alm.shared.rt.RichTextRenderingContext;
import com.polarion.alm.shared.rt.parts.FieldRichTextRenderer;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.internal.baseline.BaseObjectBaselinesSearch;
import com.polarion.alm.tracker.internal.diff.RichTextDiffGenerator;
import com.polarion.alm.tracker.model.IAttachment;
import com.polarion.alm.tracker.model.IBaseline;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IStatusOpt;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.ipi.IInternalBaselinesManager;
import com.polarion.alm.ui.shared.FieldRenderType;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.persistence.model.IPrototype;
import com.polarion.platform.security.IPermission;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.data.identification.IContextId;
import com.polarion.subterra.base.data.model.ICustomField;
import com.polarion.subterra.base.location.Location;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("squid:S2176") // class named same as parent intentionally
public class PolarionService extends ch.sbb.polarion.extension.generic.service.PolarionService {

    private static final Logger logger = Logger.getLogger(PolarionService.class);

    @Getter
    private final DocumentWorkItemsCache documentWorkItemsCache = new DocumentWorkItemsCache();

    public PolarionService() {
        super();
    }

    public PolarionService(ITrackerService trackerService, IProjectService projectService, ISecurityService securityService, IPlatformService platformService, IRepositoryService repositoryService) {
        super(trackerService, projectService, securityService, platformService, repositoryService);
    }

    @NotNull
    public List<IProject> getAuthorizedProjects() {
        final IPermission projectReadPermission = getSecurityService().constructPermission("com.polarion.persistence.object.Project.read");
        return getProjectService().searchProjects("", "name").stream()
                .filter(project -> getSecurityService().hasPermission(getSecurityService().getCurrentUser(), projectReadPermission, project.getContextId()))
                .toList();
    }

    @NotNull
    public List<IFolder> getSpaces(@NotNull String projectId) {
        return getTrackerService().getFolderManager().getFolders(projectId);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public IPObjectList<IModule> getDocuments(@NotNull String projectId) {
        IDataService dataService = getTrackerService().getDataService();
        return dataService.searchInstances(dataService.getPrototype("Module"), String.format("project.id:%s", projectId), "title");
    }

    @NotNull
    public List<IModule> getDocuments(@NotNull String projectId, @NotNull String spaceId) {
        return getTrackerService().getModuleManager().getModules(getProject(projectId), Location.getLocation(spaceId));
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

            RichTextRenderingContext renderingContext = new RichTextRenderingContext(transaction.context(), renderTarget);
            renderingContext.setMainObjectReference(mainObjectReference); //optional but sometimes it's important (e.g. links rendering inside rich texts)
            renderingContext.setTransaction(transaction);
            renderingContext.setDocumentOutlineNumber(true);

            FieldRichTextRenderer renderer = transaction.documents().createFieldRichTextRenderer();
            renderer.setRichTextRenderingContext(renderingContext);

            HtmlFragmentBuilder fragmentBuilder = renderTarget.selectBuilderTarget(transaction.context().createHtmlFragmentBuilderFor());
            renderer.renderField(fragmentBuilder.html(""), new WorkItemFieldReference(new WorkItemReference(projectId, workItemId, revision, null), fieldId,
                    IWorkItem.KEY_LINKED_WORK_ITEMS.equals(fieldId) ? FieldRenderType.LINKED_WI_IN_DOC : FieldRenderType.IMGTXT));
            return fragmentBuilder.toString();
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
        Streams.concat(workItem.getLinkedWorkItemsStructsDirect().stream(), workItem.getLinkedWorkItemsStructsBack().stream()).forEach(linkedWorkItemStruct -> {
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

    @SuppressWarnings("squid:S1166") // Initial exception swallowed intentionally
    public List<IWorkItem> getPairedWorkItems(@NotNull IWorkItem toWorkItem, @NotNull String targetProjectId, @NotNull String linkRoleId) {
        return Streams.concat(toWorkItem.getLinkedWorkItemsStructsDirect().stream(), toWorkItem.getLinkedWorkItemsStructsBack().stream())
                .filter(linkedWorkItemStruct -> linkedWorkItemStruct.getLinkRole().getId().equals(linkRoleId))
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<WorkItemField> getAllWorkItemFields(@NotNull String projectId) {
        IPrototype workItemPrototype = getTrackerService().getDataService().getPrototype(IWorkItem.PROTO);
        IPObject workItemInstance = getTrackerService().getDataService().createInstance(IWorkItem.PROTO);

        List<WorkItemField> fields = new ArrayList<>();

        List<String> keyNames = new ArrayList<>(workItemPrototype.getKeyNames());
        keyNames.stream().filter(workItemPrototype::isKeyDefined).forEach(keyName -> fields.add(WorkItemField.builder().key(keyName).name(workItemInstance.getFieldLabel(keyName)).build()));

        ITrackerProject trackerProject = getTrackerProject(projectId);
        IContextId contextId = trackerProject.getContextId();
        List<ITypeOpt> wiTypes = trackerProject.getWorkItemTypeEnum().getAllOptions();
        for (ITypeOpt wiType : wiTypes) {
            Collection<ICustomField> customFields = getTrackerService().getDataService().getCustomFieldsService().getCustomFields(IWorkItem.PROTO, contextId, wiType.getId());
            customFields.forEach(customField -> fields.add(WorkItemField.builder().key(customField.getId()).name(customField.getName()).wiTypeId(wiType.getId()).wiTypeName(wiType.getName()).build()));
        }

        Collections.sort(fields);
        return fields;
    }

    @SuppressWarnings("unchecked")
    public List<String> getGeneralFields(String proto) {
        return getTrackerService().getDataService().getPrototype(proto).getKeyNames().stream().toList();
    }

    public List<String> getCustomFields(@NotNull String projectId, @NotNull String proto, @Nullable String subType) {
        final IContextId contextId = getTrackerProject(projectId).getContextId();
        final Collection<ICustomField> customFields = getTrackerService().getDataService().getCustomFieldsService().getCustomFields(proto, contextId, subType);
        return customFields.stream().map(ICustomField::getId).toList();
    }

    public RichTextDiffGenerator getRichTextDiffGenerator() {
        return new RichTextDiffGenerator(getTrackerService());
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
        String currentUser = securityService.getCurrentUser();
        Collection<String> userRoles = securityService.getRolesForUser(currentUser);

        AuthorizationModel projectCustomFieldsSettingsModel = (AuthorizationModel)
                NamedSettingsRegistry.INSTANCE.getByFeatureName(AuthorizationSettings.FEATURE_NAME).read(
                        ScopeUtils.getScopeFromProject(projectId), SettingId.fromName(NamedSettings.DEFAULT_NAME), null);

        List<String> allowedRoles = projectCustomFieldsSettingsModel.getAllRoles();

        return userRoles.stream()
                .anyMatch(allowedRoles::contains);
    }


}
