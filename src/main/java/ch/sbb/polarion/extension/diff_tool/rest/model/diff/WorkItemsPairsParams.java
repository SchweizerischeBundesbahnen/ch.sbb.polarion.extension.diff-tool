package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Input data for finding work items pairs")
public class WorkItemsPairsParams {
    @Schema(description = "Identifier for the left project")
    private String leftProjectId;

    @Schema(description = "Identifier for the right project")
    private String rightProjectId;

    @Schema(description = "IDs of work items in left project which counterparts from right project to be found")
    private List<String> leftWorkItemIds;

    @Schema(description = "The role of the link connecting work item pairs")
    private String linkRole;
}
