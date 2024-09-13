package ch.sbb.polarion.extension.diff_tool.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Unique document identifier data")
public class DocumentIdentifier {
    @Schema(description = "ID of the project")
    private String projectId;

    @Schema(description = "ID of the space")
    private String spaceId;

    @Schema(description = "Name of the document")
    private String name;

    @Schema(description = "Revision number of the document")
    private String revision;

    @Schema(description = "XML module revision number")
    private String moduleXmlRevision;
}

