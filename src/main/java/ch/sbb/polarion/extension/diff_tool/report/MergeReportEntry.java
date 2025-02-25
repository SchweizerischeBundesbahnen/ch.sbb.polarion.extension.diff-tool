package ch.sbb.polarion.extension.diff_tool.report;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsContentMergePair;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.LocalDateTime;

@Getter
@EqualsAndHashCode
public class MergeReportEntry {
    @Schema(description = "Type of the operation result")
    private final @NotNull MergeReport.OperationResultType operationResultType;
    @Schema(description = "WorkItem pairs", implementation = WorkItemsPair.class)
    private WorkItemsPair workItemsPair;
    @Schema(description = "Field id")
    private String fieldId;
    @Schema(description = "Document content pair")
    private DocumentsContentMergePair documentsContentPair;
    @Schema(description = "Extended description of the operation")
    @JsonIgnore
    private final @NotNull String description;
    @Schema(description = "Date and time of the operation")
    @JsonIgnore
    private final @NotNull LocalDateTime operationTime;

    public MergeReportEntry(@NotNull MergeReport.OperationResultType operationResultType, @NotNull WorkItemsPair workItemsPair, @NotNull String description) {
        this.operationResultType = operationResultType;
        this.workItemsPair = workItemsPair;
        this.description = description;
        this.operationTime = LocalDateTime.now();
    }

    public MergeReportEntry(@NotNull MergeReport.OperationResultType operationResultType, @NotNull String fieldId, @NotNull String description) {
        this.operationResultType = operationResultType;
        this.fieldId = fieldId;
        this.description = description;
        this.operationTime = LocalDateTime.now();
    }

    @VisibleForTesting
    MergeReportEntry(@NotNull MergeReport.OperationResultType operationResultType, WorkItemsPair workItemsPair, String fieldId, @NotNull String description, @NotNull LocalDateTime operationTime) {
        this.operationResultType = operationResultType;
        this.workItemsPair = workItemsPair;
        this.fieldId = fieldId;
        this.description = description;
        this.operationTime = operationTime;
    }

    public MergeReportEntry(@NotNull MergeReport.OperationResultType operationResultType, @NotNull DocumentsContentMergePair documentsContentPair, @NotNull String description) {
        this.operationResultType = operationResultType;
        this.documentsContentPair = documentsContentPair;
        this.description = description;
        this.operationTime = LocalDateTime.now();
    }

}
