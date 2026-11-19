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
@Schema(description = "Whether a document exists in a project and the user may read it")
public class ResolvedDocument {
    @Schema(description = "Space (folder) of the document, as asked for")
    private String spaceId;

    @Schema(description = "Title of the space, its ID if it has no title; null when not found")
    private String spaceTitle;

    @Schema(description = "Name of the document, as asked for")
    private String name;

    @Schema(description = "True when the document exists and the user may read it")
    private boolean found;

    @Schema(description = "Title of the document, its name if it has no title; null when not found")
    private String title;

    @Schema(description = "Reason why the document cannot be used, null when found")
    private String unavailableMessage;
}
