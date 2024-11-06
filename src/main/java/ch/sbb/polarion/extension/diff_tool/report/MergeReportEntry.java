package ch.sbb.polarion.extension.diff_tool.report;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class MergeReportEntry {
    private WorkItemsPair workItemsPair;
    private String description;
    private LocalDateTime operationTime;

    public MergeReportEntry(WorkItemsPair workItemsPair, String description) {
        this.workItemsPair = workItemsPair;
        this.description = description;
        this.operationTime = LocalDateTime.now();
    }
}
