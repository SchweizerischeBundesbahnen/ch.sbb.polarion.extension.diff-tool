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
@Schema(description = "One row of the document selection table")
public class SearchDocument {
    @Schema(description = "Name of the document, unique in its space")
    private String id;

    @Schema(description = "Project ID of the document")
    private String projectId;

    @Schema(description = "Space (folder) of the document")
    private String spaceId;

    @Schema(description = "Title of the space, its ID if it has no title")
    private String spaceTitle;

    @Schema(description = "Title of the document, its name if it has no title")
    private String title;

    @Schema(description = "Type of the document")
    private EnumOption type;

    @Schema(description = "Status of the document")
    private EnumOption status;

    @Schema(description = "Last update time in epoch milliseconds")
    private Long updated;

    @Schema(description = "False when the document is unresolvable or the user may not read it")
    private boolean readable;

    @Schema(description = "Reason why the document is not readable, null otherwise")
    private String unavailableMessage;
}
