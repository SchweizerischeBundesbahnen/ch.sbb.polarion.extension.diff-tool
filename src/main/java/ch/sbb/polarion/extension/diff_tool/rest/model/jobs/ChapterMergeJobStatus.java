package ch.sbb.polarion.extension.diff_tool.rest.model.jobs;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status of a chapter merge job")
public enum ChapterMergeJobStatus {

    @Schema(description = "The merge is currently in progress")
    IN_PROGRESS,

    @Schema(description = "The merge has finished and produced a result. The result itself states whether the chapter was merged")
    SUCCESSFULLY_FINISHED,

    @Schema(description = "The merge failed and produced no result of its own")
    FAILED
}
