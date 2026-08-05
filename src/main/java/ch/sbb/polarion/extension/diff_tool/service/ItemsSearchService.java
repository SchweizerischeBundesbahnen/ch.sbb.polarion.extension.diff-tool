package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.search.EnumOption;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchCollection;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchWorkItem;
import ch.sbb.polarion.extension.generic.util.EnumUtils;
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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * Backs the two selection tables of the Diff Tool navigation topics: it runs a Lucene query in a project
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
        String luceneQuery = StringUtils.defaultString(query);
        // IBaselineCollectionsManager offers no query method and searchInstances is repository wide, so the
        // project restriction is applied here instead of as a Lucene term. This keeps the caller's query
        // untouched and avoids guessing at index field names.
        List<IBaselineCollection> found = ((IPObjectList<IBaselineCollection>) polarionService.getTrackerService().getDataService()
                .searchInstances(IBaselineCollection.PROTO, luceneQuery, IBaselineCollection.KEY_NAME)).stream()
                .filter(collection -> projectId.equals(collection.getProjectId()))
                .toList();
        return toPage(found, effectiveQuery(projectId, luceneQuery), page, recordsPerPage, this::toSearchCollection);
    }

    /**
     * The query as it applies, project restriction included. This is what the table footer shows behind its
     * info icon, the way Polarion's rich page table showed {@code DataSet.queryToShow()}.
     */
    @VisibleForTesting
    @NotNull
    String effectiveQuery(@NotNull String projectId, @NotNull String query) {
        String scope = "%s:%s".formatted(IUniqueObject.KEY_PROJECT + ".id", projectId);
        return query.isBlank() ? scope : "%s AND (%s)".formatted(scope, query);
    }

    /**
     * Cuts the requested window out of the full result set, the way the widget renderers did: the page is
     * clamped into the available range instead of returning an empty page for an out-of-range request.
     */
    @VisibleForTesting
    @NotNull
    <S, T> SearchResult<T> toPage(@NotNull List<S> found, @NotNull String query, int page, int recordsPerPage, @NotNull Function<S, T> mapper) {
        int pageSize = recordsPerPage < 1 ? DEFAULT_RECORDS_PER_PAGE : recordsPerPage;
        int lastPage = (Math.max(0, found.size() - 1) / pageSize) + 1;
        int currentPage = Math.clamp(page, 1, lastPage);
        int from = (currentPage - 1) * pageSize;
        int to = Math.min(from + pageSize, found.size());
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

    @Nullable
    private Long toEpochMillis(@Nullable Date date) {
        return date == null ? null : date.getTime();
    }
}
