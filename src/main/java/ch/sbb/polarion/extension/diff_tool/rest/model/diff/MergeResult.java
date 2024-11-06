package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.diff_tool.report.MergeReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of a merge operation")
public class MergeResult {

    @Schema(description = "Indicates whether the merge operation was successful")
    private boolean success;

    @Schema(description = "Indicates whether the target module has structural changes")
    private boolean targetModuleHasStructuralChanges;

    @Schema(description = "Indicates whether the merge was not authorized")
    private boolean mergeNotAuthorized;

    @Schema(description = "Merge report with detailed information about the merge operation", implementation = MergeReport.class)
    private MergeReport mergeReport;
}
