package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchCollection;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchWorkItem;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;

@Singleton
@Secured
@Path("/api")
public class SearchApiController extends SearchInternalController {

    @Override
    public SearchResult<SearchWorkItem> searchWorkItems(String projectId, String query, String sortBy, Integer page, Integer recordsPerPage) {
        return polarionService.callPrivileged(() -> super.searchWorkItems(projectId, query, sortBy, page, recordsPerPage));
    }

    @Override
    public SearchResult<SearchCollection> searchCollections(String projectId, String query, Integer page, Integer recordsPerPage) {
        return polarionService.callPrivileged(() -> super.searchCollections(projectId, query, page, recordsPerPage));
    }
}
