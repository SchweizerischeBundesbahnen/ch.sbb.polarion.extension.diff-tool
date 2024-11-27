package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Input data for diffing multiple work items")
public class MultipleWorkItemsDiffParams {
    @Schema(description = "Identifier for the left project in the comparison", implementation = DocumentIdentifier.class)
    private String leftProjectId;

    @Schema(description = "Identifier for the right project in the comparison", implementation = DocumentIdentifier.class)
    private String rightProjectId;

    private List<String> workItemIds;

    @Schema(description = "The role of the link connecting work item pairs")
    private String linkRole;

    @Schema(description = "The configuration name to use for the comparison")
    private String configName;

    @Schema(description = "The ID of the configuration cache bucket")
    private String configCacheBucketId;
}
