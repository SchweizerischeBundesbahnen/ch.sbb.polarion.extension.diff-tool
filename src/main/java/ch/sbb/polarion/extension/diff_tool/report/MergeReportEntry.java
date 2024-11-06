package ch.sbb.polarion.extension.diff_tool.report;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class MergeReportEntry {
    @Schema(description = "Type of the operation result", allowableValues = {"CONFLICTED", "PROHIBITED", "NOT_PAIRED", "CREATED", "MODIFIED", "MOVED", "NOT_MOVED"})
    private @NotNull MergeReport.OperationResultType operationResultType;
    @Schema(description = "WorkItem pairs", implementation = WorkItemsPair.class)
    private @NotNull WorkItemsPair workItemsPair;
    @Schema(description = "Extended description of the operation")
    private @NotNull String description;
    @Schema(description = "Date and time of the operation")
    private @NotNull LocalDateTime operationTime;

    public MergeReportEntry(@NotNull MergeReport.OperationResultType operationResultType, @NotNull WorkItemsPair workItemsPair, @NotNull String description) {
        this.operationResultType = operationResultType;
        this.workItemsPair = workItemsPair;
        this.description = description;
        this.operationTime = LocalDateTime.now();
    }
}
