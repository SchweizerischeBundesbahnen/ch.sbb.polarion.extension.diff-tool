package ch.sbb.polarion.extension.diff_tool.rest.model.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "One page of a search result")
public class SearchResult<T> {
    @Schema(description = "Size of the whole result set, not of this page")
    private int totalCount;

    @Schema(description = "Number of the returned page, 1-based and clamped to the available pages")
    private int page;

    @Schema(description = "Number of the last available page, at least 1")
    private int lastPage;

    @Schema(description = "The Lucene query which produced this result, as executed")
    private String query;

    @Schema(description = "Items of the requested page")
    private List<T> items;
}
