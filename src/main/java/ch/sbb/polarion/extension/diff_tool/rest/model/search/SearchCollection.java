package ch.sbb.polarion.extension.diff_tool.rest.model.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "One row of the baseline collection selection table")
public class SearchCollection {
    @Schema(description = "Unique identifier of the collection")
    private String id;

    @Schema(description = "Project ID of the collection")
    private String projectId;

    @Schema(description = "Name of the collection")
    private String name;

    @Schema(description = "Name of the user who created the collection")
    private String authorName;

    @Schema(description = "Creation time in epoch milliseconds")
    private Long created;

    @Schema(description = "Last update time in epoch milliseconds")
    private Long updated;

    @Schema(description = "False when the collection is unresolvable or the user may not read it")
    private boolean readable;

    @Schema(description = "Reason why the collection is not readable, null otherwise")
    private String unavailableMessage;
}
