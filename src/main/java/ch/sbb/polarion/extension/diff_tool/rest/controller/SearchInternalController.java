package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchCollection;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchWorkItem;
import ch.sbb.polarion.extension.diff_tool.service.ItemsSearchService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import com.polarion.platform.persistence.PException;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Supplier;

@Singleton
@Hidden
@Path("/internal")
@Tag(name = "Search")
public class SearchInternalController {

    private static final String MISSING_PROJECT_ID_MESSAGE = "'projectId' should be provided";

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
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = SearchResult.class)
                            )
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
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = SearchResult.class)
                            )
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
     * The query comes from an input field, so it can be malformed. Polarion answers that with an unchecked
     * persistence exception, which would otherwise surface as a 500 and an error page. As a 400 the picker can
     * show the message next to the input instead.
     */
    private <T> T searchSafely(Supplier<T> search) {
        try {
            return search.get();
        } catch (PException | IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }
}
