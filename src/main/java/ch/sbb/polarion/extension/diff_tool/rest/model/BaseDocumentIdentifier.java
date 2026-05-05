package ch.sbb.polarion.extension.diff_tool.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Unique document identifier data without revision information, i.e. in HEAD revision")
public class BaseDocumentIdentifier {
    @Schema(description = "ID of the project")
    private String projectId;

    @Schema(description = "ID of the space")
    private String spaceId;

    @Schema(description = "Name of the document")
    private String name;

    /**
     * Returns true when both identifiers point to the same document (regardless of revision).
     */
    public boolean pointsToSameDocumentAs(BaseDocumentIdentifier other) {
        return other != null
                && Objects.equals(projectId, other.projectId)
                && Objects.equals(spaceId, other.spaceId)
                && Objects.equals(name, other.name);
    }
}
