package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Details of WorkItem contained in a document")
public class DocumentWorkItem extends ProjectWorkItem {
    @Schema(description = "The project ID of the document containing the WorkItem")
    private String documentProjectId;

    @Schema(description = "The location path of the document containing the WorkItem")
    private String documentLocationPath;

    @Schema(description = "The revision of the document containing the WorkItem")
    private String documentRevision;

    public DocumentWorkItem(String id, String projectId, String revision, String documentProjectId, String documentLocationPath, String documentRevision) {
        super(id, projectId, revision);
        this.documentProjectId = documentProjectId;
        this.documentLocationPath = documentLocationPath;
        this.documentRevision = documentRevision;
    }
}
