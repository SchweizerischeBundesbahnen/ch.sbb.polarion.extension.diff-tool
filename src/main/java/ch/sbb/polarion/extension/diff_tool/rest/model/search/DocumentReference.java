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
@Schema(description = "A document of a project, named by its space and name")
public class DocumentReference {
    @Schema(description = "Space (folder) of the document")
    private String spaceId;

    @Schema(description = "Name of the document")
    private String name;
}
