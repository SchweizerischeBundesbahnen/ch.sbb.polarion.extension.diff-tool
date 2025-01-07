package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents a collection of live documents")
public class DocumentsCollection {
    @Schema(description = "Unique identifier of the collection")
    private String id;

    @Schema(description = "Name of the collection")
    private String name;

    @Schema(description = "ID of a project in which collection resides")
    private String projectId;

    @Schema(description = "Name of a project in which collection resides")
    private String projectName;

}
