package ch.sbb.polarion.extension.diff_tool.rest.model.jobs;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "State of a chapter merge job: where it stands, and what went wrong if it failed")
public class ChapterMergeJobDetails {

    @Schema(description = "Current status of the merge job",
            example = "IN_PROGRESS",
            implementation = ChapterMergeJobStatus.class
    )
    private ChapterMergeJobStatus status;

    @Schema(description = "What the merge is doing right now, as long as it is running")
    private String progressMessage;

    @Schema(description = "Error message if the merge failed")
    private String errorMessage;
}
