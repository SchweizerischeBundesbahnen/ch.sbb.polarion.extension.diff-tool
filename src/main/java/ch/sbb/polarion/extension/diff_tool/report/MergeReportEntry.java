package ch.sbb.polarion.extension.diff_tool.report;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
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
    private @NotNull MergeReport.OperationResultType operationResultType;
    private @NotNull WorkItemsPair workItemsPair;
    private @NotNull String description;
    private @NotNull LocalDateTime operationTime;

    public MergeReportEntry(@NotNull MergeReport.OperationResultType operationResultType, @NotNull WorkItemsPair workItemsPair, @NotNull String description) {
        this.operationResultType = operationResultType;
        this.workItemsPair = workItemsPair;
        this.description = description;
        this.operationTime = LocalDateTime.now();
    }
}
