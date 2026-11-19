package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.search.DocumentReference;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.ResolvedDocument;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchCollection;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchDocument;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchWorkItem;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;

import java.util.List;

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

    @Override
    public SearchResult<SearchDocument> searchDocuments(String projectId, String query, Integer page, Integer recordsPerPage) {
        return polarionService.callPrivileged(() -> super.searchDocuments(projectId, query, page, recordsPerPage));
    }

    @Override
    public List<ResolvedDocument> resolveDocuments(String projectId, List<DocumentReference> documents) {
        return polarionService.callPrivileged(() -> super.resolveDocuments(projectId, documents));
    }
}
