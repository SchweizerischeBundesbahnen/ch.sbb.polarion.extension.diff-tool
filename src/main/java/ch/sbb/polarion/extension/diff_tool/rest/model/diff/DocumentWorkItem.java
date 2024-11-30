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
@Schema(description = "Details of WorkItem contained in a document")
public class DocumentWorkItem {
    @Schema(description = "Unique identifier of the WorkItem")
    private String id;

    @Schema(description = "WorkItem's project ID, it can differ from document's project ID in case of referenced WorkItems")
    private String projectId;

    @Schema(description = "The revision of the WorkItem. Referenced WorkItem's can be frozen in document in certain revision")
    private String revision;

    @Schema(description = "The project ID of the document containing the WorkItem")
    private String documentProjectId;

    @Schema(description = "The location path of the document containing the WorkItem")
    private String documentLocationPath;

    @Schema(description = "The revision of the document containing the WorkItem")
    private String documentRevision;
}
