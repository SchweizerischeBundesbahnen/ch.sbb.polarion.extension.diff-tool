package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of WorkItem belonging to a project")
public class ProjectWorkItem {

    @Schema(description = "Unique identifier of the WorkItem")
    private String id;

    @Schema(description = "WorkItem's project ID, it can differ from document's project ID in case of referenced WorkItems")
    private String projectId;

    @Schema(description = "The revision of the WorkItem. Referenced WorkItem's can be frozen in document in certain revision")
    private String revision;

}
