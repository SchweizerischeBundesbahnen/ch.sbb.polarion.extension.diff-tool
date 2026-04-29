package ch.sbb.polarion.extension.diff_tool.rest.model.duplication;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Information about a project duplication job")
public class DuplicationJobInfo {
    @Schema(description = "Polarion job id")
    private String jobId;

    @Schema(description = "Human-readable job name")
    private String jobName;

    @Schema(description = "Job state: UNSCHEDULED, ACTIVATING, WAITING, RUNNING, FINISHED, CANCELED")
    private String state;

    @Schema(description = "Final job status type: OK, FAILED, CANCELED. Null while still running.")
    private String statusType;

    @Schema(description = "Status message (failure cause or success summary)")
    private String statusMessage;

    @Schema(description = "Creation time (epoch milliseconds)")
    private Long creationTime;

    @Schema(description = "Start time (epoch milliseconds, 0 if not started)")
    private Long startTime;

    @Schema(description = "Finish time (epoch milliseconds, 0 if still running)")
    private Long finishTime;

    @Schema(description = "Progress (0.0 to 1.0)")
    private Float completeness;

    @Schema(description = "Name of the current task within the job")
    private String currentTaskName;

    @Schema(description = "Relative URL of the Polarion job monitor page filtered to this job")
    private String monitorUrl;

    @Schema(description = "Relative URL of the Polarion job log/report")
    private String logUrl;
}
