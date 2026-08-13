package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchCollection;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchWorkItem;
import ch.sbb.polarion.extension.diff_tool.service.ItemsSearchService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import com.polarion.core.util.exceptions.IUserFriendlyException;
import com.polarion.platform.persistence.UnresolvableObjectException;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchInternalControllerTest {

    @Mock
    private PolarionService polarionService;

    @Mock
    private ItemsSearchService itemsSearchService;

    private SearchInternalController controller;

    @BeforeEach
    void setUp() {
        controller = new SearchInternalController(polarionService, itemsSearchService);
    }

    @Test
    void testSearchWorkItemsRequiresAProject() {
        assertThrows(BadRequestException.class, () -> controller.searchWorkItems(null, null, null, null, null));
        assertThrows(BadRequestException.class, () -> controller.searchWorkItems("  ", null, null, null, null));
    }

    @Test
    void testSearchCollectionsRequiresAProject() {
        assertThrows(BadRequestException.class, () -> controller.searchCollections(null, null, null, null));
        assertThrows(BadRequestException.class, () -> controller.searchCollections("  ", null, null, null));
    }

    @Test
    void testSearchWorkItemsPassesTheParametersThrough() {
        SearchResult<SearchWorkItem> expected = SearchResult.<SearchWorkItem>builder().items(List.of()).build();
        when(itemsSearchService.searchWorkItems("elibrary", "type:task", "title", 2, 50)).thenReturn(expected);

        assertEquals(expected, controller.searchWorkItems("elibrary", "type:task", "title", 2, 50));
    }

    @Test
    void testSearchWorkItemsDefaultsThePageAndLetsTheServiceDefaultThePageSize() {
        controller.searchWorkItems("elibrary", null, null, null, null);

        // 0 is "not given": the service turns anything below 1 into its own default page size
        verify(itemsSearchService).searchWorkItems("elibrary", null, null, 1, 0);
    }

    @Test
    void testSearchCollectionsPassesTheParametersThrough() {
        SearchResult<SearchCollection> expected = SearchResult.<SearchCollection>builder().items(List.of()).build();
        when(itemsSearchService.searchCollections("elibrary", "name:release*", 3, 10)).thenReturn(expected);

        assertEquals(expected, controller.searchCollections("elibrary", "name:release*", 3, 10));
    }

    @Test
    void testSearchCollectionsDefaultsThePage() {
        controller.searchCollections("elibrary", null, null, null);

        verify(itemsSearchService).searchCollections("elibrary", null, 1, 0);
    }

    @Test
    void testAMalformedQueryBecomesABadRequest() {
        when(itemsSearchService.searchWorkItems("elibrary", "type:(", null, 1, 0))
                .thenThrow(new RejectedQueryException("Cannot parse 'type:('"));

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> controller.searchWorkItems("elibrary", "type:(", null, null, null));
        assertEquals("Cannot parse 'type:('", thrown.getMessage());
    }

    @Test
    void testAWrappedMalformedQueryBecomesABadRequest() {
        // The persistence layer wraps what the index rejected, so the marker sits in the cause
        when(itemsSearchService.searchCollections("elibrary", "name:(", 1, 0))
                .thenThrow(new UnresolvableObjectException("Query failed", new RejectedQueryException("Cannot parse 'name:('")));

        assertThrows(BadRequestException.class, () -> controller.searchCollections("elibrary", "name:(", null, null));
    }

    @Test
    void testAnIllegalArgumentBecomesABadRequest() {
        when(itemsSearchService.searchCollections("elibrary", "??", 1, 0))
                .thenThrow(new IllegalArgumentException("Bad query"));

        assertThrows(BadRequestException.class, () -> controller.searchCollections("elibrary", "??", null, null));
    }

    @Test
    void testABackendFailureIsNotReportedAsABadRequest() {
        // Nothing here points at the query, so the generic mapper has to turn this into a 500 with a message
        // which does not carry Polarion internals
        when(itemsSearchService.searchWorkItems("elibrary", null, null, 1, 0))
                .thenThrow(new UnresolvableObjectException("repository/collection/1 is gone"));

        assertThrows(UnresolvableObjectException.class, () -> controller.searchWorkItems("elibrary", null, null, null, null));
    }

    /**
     * Stands in for what Polarion's index throws for a query it cannot parse: the real class
     * ({@code com.polarion.subterra.index.QueryIndexException}) is not on this module's classpath, and only its
     * {@link IUserFriendlyException} marker matters here.
     */
    private static class RejectedQueryException extends RuntimeException implements IUserFriendlyException {
        RejectedQueryException(String message) {
            super(message);
        }
    }
}
