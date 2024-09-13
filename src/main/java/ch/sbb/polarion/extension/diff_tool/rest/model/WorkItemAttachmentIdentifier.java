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
@Schema(description = "Unique identifier for a work item attachment")
public class WorkItemAttachmentIdentifier {

    @Schema(description = "ID of the project", example = "12345")
    private String projectId;

    @Schema(description = "ID of the WorkItem associated with the attachment")
    private String workItemId;

    @Schema(description = "ID of the specific attachment")
    private String attachmentId;
}