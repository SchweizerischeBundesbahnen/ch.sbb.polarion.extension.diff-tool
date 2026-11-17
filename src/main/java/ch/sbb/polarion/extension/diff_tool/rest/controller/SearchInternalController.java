package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.search.DocumentReference;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.ResolvedDocument;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchCollection;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchDocument;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchWorkItem;
import ch.sbb.polarion.extension.diff_tool.service.ItemsSearchService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import com.polarion.core.util.exceptions.IUserFriendlyException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

@Singleton
@Hidden
@Path("/internal")
@Tag(name = "Search")
public class SearchInternalController {

    private static final String MISSING_PROJECT_ID_MESSAGE = "'projectId' should be provided";
    private static final String MISSING_DOCUMENTS_MESSAGE = "Documents to resolve should be provided";

    protected final PolarionService polarionService;
    protected final ItemsSearchService itemsSearchService;

    public SearchInternalController() {
        this(new PolarionService());
    }

    public SearchInternalController(PolarionService polarionService) {
        this(polarionService, new ItemsSearchService(polarionService));
    }

    public SearchInternalController(PolarionService polarionService, ItemsSearchService itemsSearchService) {
        this.polarionService = polarionService;
        this.itemsSearchService = itemsSearchService;
    }

    @GET
    @Path("/projects/{projectId}/workitems/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Searches WorkItems of a project by a Lucene query and returns one page of them",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "One page of matching WorkItems",
                            useReturnTypeSchema = true
                    ),
                    @ApiResponse(responseCode = "400", description = "Missing project ID or malformed query")
            }
    )
    public SearchResult<SearchWorkItem> searchWorkItems(@Parameter(description = "Project to search in") @PathParam("projectId") String projectId,
                                                        @Parameter(description = "Lucene query, empty for all WorkItems") @QueryParam("query") String query,
                                                        @Parameter(description = "Lucene sort string, 'id' by default") @QueryParam("sortBy") String sortBy,
                                                        @Parameter(description = "1-based page number") @QueryParam("page") Integer page,
                                                        @Parameter(description = "Page size") @QueryParam("recordsPerPage") Integer recordsPerPage) {
        requireProjectId(projectId);
        return searchSafely(() -> itemsSearchService.searchWorkItems(projectId, query, sortBy, pageOrFirst(page), sizeOrDefault(recordsPerPage)));
    }

    @GET
    @Path("/projects/{projectId}/collections/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Searches baseline collections of a project by a Lucene query and returns one page of them",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "One page of matching collections",
                            useReturnTypeSchema = true
                    ),
                    @ApiResponse(responseCode = "400", description = "Missing project ID or malformed query")
            }
    )
    public SearchResult<SearchCollection> searchCollections(@Parameter(description = "Project to search in") @PathParam("projectId") String projectId,
                                                            @Parameter(description = "Lucene query, empty for all collections") @QueryParam("query") String query,
                                                            @Parameter(description = "1-based page number") @QueryParam("page") Integer page,
                                                            @Parameter(description = "Page size") @QueryParam("recordsPerPage") Integer recordsPerPage) {
        requireProjectId(projectId);
        return searchSafely(() -> itemsSearchService.searchCollections(projectId, query, pageOrFirst(page), sizeOrDefault(recordsPerPage)));
    }

    @GET
    @Path("/projects/{projectId}/documents/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Searches documents of a project by a Lucene query and returns one page of them, ordered by space and name",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "One page of matching documents",
                            useReturnTypeSchema = true
                    ),
                    @ApiResponse(responseCode = "400", description = "Missing project ID or malformed query")
            }
    )
    public SearchResult<SearchDocument> searchDocuments(@Parameter(description = "Project to search in") @PathParam("projectId") String projectId,
                                                        @Parameter(description = "Lucene query, empty for all documents") @QueryParam("query") String query,
                                                        @Parameter(description = "1-based page number") @QueryParam("page") Integer page,
                                                        @Parameter(description = "Page size") @QueryParam("recordsPerPage") Integer recordsPerPage) {
        requireProjectId(projectId);
        return searchSafely(() -> itemsSearchService.searchDocuments(projectId, query, pageOrFirst(page), sizeOrDefault(recordsPerPage)));
    }

    @POST
    @Path("/projects/{projectId}/documents/resolve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Tells for each given space and name whether such a document exists in the project and may be read",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "One entry per given document, in the same order",
                            useReturnTypeSchema = true
                    ),
                    @ApiResponse(responseCode = "400", description = "Missing project ID or documents")
            }
    )
    public List<ResolvedDocument> resolveDocuments(@Parameter(description = "Project to look in") @PathParam("projectId") String projectId,
                                                   List<DocumentReference> documents) {
        requireProjectId(projectId);
        if (documents == null) {
            throw new BadRequestException(MISSING_DOCUMENTS_MESSAGE);
        }
        return itemsSearchService.resolveDocuments(projectId, documents);
    }

    private void requireProjectId(String projectId) {
        if (StringUtils.isBlank(projectId)) {
            throw new BadRequestException(MISSING_PROJECT_ID_MESSAGE);
        }
    }

    private int pageOrFirst(Integer page) {
        return page == null ? 1 : page;
    }

    private int sizeOrDefault(Integer recordsPerPage) {
        return recordsPerPage == null ? 0 : recordsPerPage; // the service turns anything below 1 into its default
    }

    /**
     * The query comes from an input field, so it can be malformed. Polarion reports that with an exception whose
     * message is meant for the user, which as a 400 the picker shows next to the input instead of as an error
     * page. Any other failure is a backend problem: it propagates, so the generic mapper answers 500, keeps the
     * internal message out of the response and logs it under a correlation id.
     */
    private <T> T searchSafely(Supplier<T> search) {
        try {
            return search.get();
        } catch (RuntimeException e) {
            if (isCausedByTheQuery(e)) {
                throw new BadRequestException(e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Polarion marks the exceptions whose message may be shown to a user with {@link IUserFriendlyException},
     * and that is what a rejected query produces. The whole chain is inspected, since the persistence layer
     * wraps such an exception on its way out.
     */
    private boolean isCausedByTheQuery(@NotNull RuntimeException e) {
        return ExceptionUtils.getThrowableList(e).stream()
                .anyMatch(cause -> cause instanceof IUserFriendlyException || cause instanceof IllegalArgumentException);
    }
}
