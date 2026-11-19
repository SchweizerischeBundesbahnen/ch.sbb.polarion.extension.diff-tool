package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.search.DocumentReference;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.EnumOption;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.ResolvedDocument;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchCollection;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchDocument;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchWorkItem;
import ch.sbb.polarion.extension.generic.util.EnumUtils;
import ch.sbb.polarion.extension.generic.util.LuceneUtils;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.i18n.Localization;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.subterra.base.location.Location;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Backs the selection tables of the Diff Tool navigation topics: it runs a Lucene query in a project
 * and returns one page of plain values, which the React tables render themselves.
 * <p>
 * This replaces the former {@code widgets/} renderers, which reached Polarion's rich page query engine
 * through internal classes ({@code DataSetParameterImpl}, {@code RichPageWidgetRenderingContextImpl}) only
 * to produce server-rendered HTML.
 */
public class ItemsSearchService {

    private static final Logger logger = Logger.getLogger(ItemsSearchService.class);

    @VisibleForTesting
    static final int DEFAULT_RECORDS_PER_PAGE = 20;

    @VisibleForTesting
    static final String HEADING_TYPE_ID = "heading";

    /** The icon Polarion ships for a document heading, which the heading type option does not point at. */
    @VisibleForTesting
    static final String HEADING_TYPE_ICON_URL = "/polarion/ria/images/enums/type_heading.png";

    @VisibleForTesting
    static final String DOCUMENT_NOT_FOUND_MESSAGE = "Document not found";

    @VisibleForTesting
    static final String MISSING_DOCUMENT_MESSAGE = "Space and name of the document should be provided";

    /** Sorts after any space and name. */
    private static final String LAST = "\uffff";

    private final PolarionService polarionService;

    public ItemsSearchService(@NotNull PolarionService polarionService) {
        this.polarionService = polarionService;
    }

    @NotNull
    public SearchResult<SearchWorkItem> searchWorkItems(@NotNull String projectId, @Nullable String query, @Nullable String sortBy, int page, int recordsPerPage) {
        String luceneQuery = StringUtils.defaultString(query);
        String sortString = StringUtils.defaultIfBlank(sortBy, IUniqueObject.KEY_ID);
        IPObjectList<IWorkItem> found = polarionService.getTrackerProject(projectId).queryWorkItems(luceneQuery, sortString);
        return toPage(found, effectiveQuery(projectId, luceneQuery), page, recordsPerPage, this::toSearchWorkItem);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public SearchResult<SearchCollection> searchCollections(@NotNull String projectId, @Nullable String query, int page, int recordsPerPage) {
        // IBaselineCollectionsManager offers no query method and searchInstances is repository wide, so the
        // project restriction travels as a Lucene term: a collection carries the project in its 'project'
        // reference field (IBaselineCollectionsManager.createCollection sets it), which the index exposes as
        // 'project.id'. Restricting in the index keeps the search from resolving every collection of the
        // repository, and leaves an unresolvable hit to the guard in toSearchCollection.
        String luceneQuery = effectiveQuery(projectId, StringUtils.defaultString(query));
        List<IBaselineCollection> found = polarionService.getTrackerService().getDataService()
                .searchInstances(IBaselineCollection.PROTO, luceneQuery, IBaselineCollection.KEY_NAME);
        return toPage(found, luceneQuery, page, recordsPerPage, this::toSearchCollection);
    }

    /**
     * Documents of a project, ordered by space and name. Like the collections search, this one is repository wide,
     * so the project restriction travels as a Lucene term.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public SearchResult<SearchDocument> searchDocuments(@NotNull String projectId, @Nullable String query, int page, int recordsPerPage) {
        String luceneQuery = effectiveQuery(projectId, StringUtils.defaultString(query));
        List<IModule> found = polarionService.getTrackerService().getDataService()
                .searchInstances(IModule.PROTO, luceneQuery, IModule.KEY_MODULENAME);
        List<IModule> ordered = found.stream().sorted(Comparator.comparing(this::sortKey)).toList();
        Map<String, String> spaceTitles = new HashMap<>();
        return toPage(ordered, luceneQuery, page, recordsPerPage, module -> toSearchDocument(module, spaceTitles));
    }

    /**
     * Whether each of the documents exists in the project and the user may read it. The batch export pairs a
     * source document with the document of the same space and name in the target project, and asks this once per
     * page of the table instead of once per row.
     */
    @NotNull
    public List<ResolvedDocument> resolveDocuments(@NotNull String projectId, @NotNull List<DocumentReference> documents) {
        IProject project = polarionService.getProject(projectId);
        Map<String, String> spaceTitles = new HashMap<>();
        return documents.stream().map(document -> resolveDocument(project, document, spaceTitles)).toList();
    }

    @VisibleForTesting
    @NotNull
    ResolvedDocument resolveDocument(@NotNull IProject project, @NotNull DocumentReference document) {
        return resolveDocument(project, document, new HashMap<>());
    }

    @NotNull
    private ResolvedDocument resolveDocument(@NotNull IProject project, @NotNull DocumentReference document, @NotNull Map<String, String> spaceTitles) {
        ResolvedDocument.ResolvedDocumentBuilder builder = ResolvedDocument.builder()
                .spaceId(document.getSpaceId())
                .name(document.getName());
        if (StringUtils.isBlank(document.getSpaceId()) || StringUtils.isBlank(document.getName())) {
            return builder.found(false).unavailableMessage(MISSING_DOCUMENT_MESSAGE).build();
        }
        IModule module = polarionService.getModule(project, Location.getLocation(document.getSpaceId() + "/" + document.getName()));
        if (module == null || module.isUnresolvable()) {
            return builder.found(false).unavailableMessage(DOCUMENT_NOT_FOUND_MESSAGE).build();
        }
        if (!module.can().read()) {
            return builder.found(false).unavailableMessage(Localization.getString("security.cannotread")).build();
        }
        return builder.found(true)
                .spaceTitle(spaceTitle(project.getId(), document.getSpaceId(), spaceTitles))
                .title(module.getTitleOrName())
                .build();
    }

    /**
     * The title of a space, its ID when it has none or cannot be read. Remembered in the given map, because
     * the documents of one page of results usually share a few spaces.
     */
    @NotNull
    private String spaceTitle(@NotNull String projectId, @NotNull String spaceId, @NotNull Map<String, String> spaceTitles) {
        return spaceTitles.computeIfAbsent(projectId + "/" + spaceId, key -> polarionService.getSpaceTitle(projectId, spaceId));
    }

    /** Space and name of a document, for its order in the table. A document which cannot be read sorts last. */
    @NotNull
    private String sortKey(@NotNull IModule module) {
        try {
            return module.isUnresolvable() ? LAST : module.getModuleFolder() + "/" + module.getModuleName();
        } catch (RuntimeException e) {
            // The index may return a document the repository no longer holds, and then any getter can fail
            return LAST;
        }
    }

    @VisibleForTesting
    @NotNull
    SearchDocument toSearchDocument(@NotNull IModule module) {
        return toSearchDocument(module, new HashMap<>());
    }

    @NotNull
    private SearchDocument toSearchDocument(@NotNull IModule module, @NotNull Map<String, String> spaceTitles) {
        String id = safeId(module);
        SearchDocument.SearchDocumentBuilder builder = SearchDocument.builder().id(id);

        String unavailableMessage = unavailableMessage(module, id);
        if (unavailableMessage != null) {
            return builder.readable(false).unavailableMessage(unavailableMessage).build();
        }
        return builder.readable(true)
                .id(module.getModuleName())
                .projectId(module.getProjectId())
                .spaceId(module.getModuleFolder())
                .spaceTitle(spaceTitle(module.getProjectId(), module.getModuleFolder(), spaceTitles))
                .title(module.getTitleOrName())
                .type(toEnumOption(module.getType()))
                .status(toEnumOption(module.getStatus()))
                .updated(toEpochMillis(module.getUpdated()))
                .build();
    }

    /**
     * The query as it applies, project restriction included. The collections search sends exactly this string to
     * the index. The WorkItems search is scoped by Polarion itself, so there the string describes the same
     * restriction, the way Polarion's rich page table showed {@code DataSet.queryToShow()}. The table footer
     * shows it behind its info icon.
     */
    @VisibleForTesting
    @NotNull
    String effectiveQuery(@NotNull String projectId, @NotNull String query) {
        return LuceneUtils.and(LuceneUtils.projectTerm(projectId), query);
    }

    /**
     * Cuts the requested window out of the full result set, the way the widget renderers did: the page is
     * clamped into the available range instead of returning an empty page for an out-of-range request.
     * <p>
     * Both the page and the page size come from query parameters, so the window is computed in {@code long}
     * and capped at the result size: no requested value can overflow the {@code int} bounds.
     */
    @VisibleForTesting
    @NotNull
    <S, T> SearchResult<T> toPage(@NotNull List<S> found, @NotNull String query, int page, int recordsPerPage, @NotNull Function<S, T> mapper) {
        long pageSize = recordsPerPage < 1 ? DEFAULT_RECORDS_PER_PAGE : recordsPerPage;
        int lastPage = (int) (Math.max(0, found.size() - 1) / pageSize) + 1;
        int currentPage = Math.clamp(page, 1, lastPage);
        int from = (int) Math.min((currentPage - 1) * pageSize, found.size());
        int to = (int) Math.min(from + pageSize, found.size());
        return SearchResult.<T>builder()
                .totalCount(found.size())
                .page(currentPage)
                .lastPage(lastPage)
                .query(query)
                .items(found.subList(from, to).stream().map(mapper).toList())
                .build();
    }

    @VisibleForTesting
    @NotNull
    SearchWorkItem toSearchWorkItem(@NotNull IWorkItem workItem) {
        String id = safeId(workItem);
        SearchWorkItem.SearchWorkItemBuilder builder = SearchWorkItem.builder().id(id);

        String unavailableMessage = unavailableMessage(workItem, id);
        if (unavailableMessage != null) {
            return builder.readable(false).unavailableMessage(unavailableMessage).build();
        }
        return builder.readable(true)
                .projectId(workItem.getProjectId())
                .title(workItem.getTitle())
                .type(typeOf(workItem))
                .status(toEnumOption(workItem.getStatus()))
                .severity(toEnumOption(workItem.getSeverity()))
                .build();
    }

    /**
     * The WorkItem's type, or - for a document heading, which carries no type of its own - the heading type of
     * the document it belongs to. Polarion's own table shows "Heading" in that column, and
     * {@link IModule#getHeadingWorkItemType()} is where it takes that from.
     * <p>
     * The heading type is configured, not offered, so it carries no name either: that falls back to its
     * capitalized ID.
     */
    @Nullable
    private EnumOption typeOf(@NotNull IWorkItem workItem) {
        ITypeOpt type = workItem.getType();
        if (type == null) {
            IModule module = workItem.getModule();
            type = module == null ? null : module.getHeadingWorkItemType();
        }
        if (type == null) {
            return null;
        }
        return EnumOption.builder()
                .id(type.getId())
                .name(displayName(type))
                .iconUrl(typeIconUrl(type))
                .build();
    }

    /**
     * The icon of a WorkItem type. The heading type is recognised by its ID rather than by a missing icon URL:
     * whatever the enum resolves for it, the icon a document heading shows is Polarion's own.
     */
    @Nullable
    private String typeIconUrl(@NotNull ITypeOpt type) {
        return HEADING_TYPE_ID.equals(type.getId()) ? HEADING_TYPE_ICON_URL : EnumUtils.getIconUrl(type);
    }

    @VisibleForTesting
    @NotNull
    SearchCollection toSearchCollection(@NotNull IBaselineCollection collection) {
        String id = safeId(collection);
        SearchCollection.SearchCollectionBuilder builder = SearchCollection.builder().id(id);

        String unavailableMessage = unavailableMessage(collection, id);
        if (unavailableMessage != null) {
            return builder.readable(false).unavailableMessage(unavailableMessage).build();
        }
        IUser author = collection.getAuthor();
        return builder.readable(true)
                .projectId(collection.getProjectId())
                .name(collection.getName())
                .authorName(author == null ? null : author.getName())
                .created(toEpochMillis(collection.getCreated()))
                .updated(toEpochMillis(collection.getUpdated()))
                .build();
    }

    /**
     * The two rows the widget renderers showed instead of data, with the same Polarion message keys.
     */
    @VisibleForTesting
    @Nullable
    String unavailableMessage(@NotNull IPObject object, @Nullable String id) {
        if (object.isUnresolvable()) {
            return Localization.getString("richpages.widget.table.unresolvableItem", StringUtils.defaultString(id));
        }
        if (!object.can().read()) {
            return Localization.getString("security.cannotread");
        }
        return null;
    }

    /**
     * The ID of an object the index returned but the repository may no longer hold. Every other getter is only
     * read once the object turns out to be resolvable and readable, but the ID is needed for the message of
     * exactly those rows - and reading it is the one call that can still fail.
     */
    @Nullable
    private String safeId(@NotNull IUniqueObject object) {
        try {
            return object.getId();
        } catch (RuntimeException e) {
            logger.warn("Could not read the ID of a search result", e);
            return null;
        }
    }

    @Nullable
    private EnumOption toEnumOption(@Nullable IEnumOption option) {
        return option == null ? null : EnumOption.builder()
                .id(option.getId())
                .name(displayName(option))
                .iconUrl(EnumUtils.getIconUrl(option))
                .build();
    }

    /**
     * An option which is configured but not offered by the project's enum any more carries no name, and still
     * has to render as something: its capitalized ID, so `heading` reads as "Heading".
     */
    @NotNull
    private String displayName(@NotNull IEnumOption option) {
        return StringUtils.isNotBlank(option.getName()) ? option.getName() : StringUtils.capitalize(option.getId());
    }

    /**
     * Polarion returns the legacy {@link Date} and offers no {@link Instant} variant, so the value is bridged
     * into the {@code java.time} API here, at the only place which touches it.
     */
    @Nullable
    private Long toEpochMillis(@Nullable Date polarionDate) {
        return polarionDate == null ? null : polarionDate.toInstant().toEpochMilli();
    }
}
