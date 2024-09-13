package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonFormat(shape = JsonFormat.Shape.NUMBER)
@Schema(description = "Enum representing the direction of a merge operation")
public enum MergeDirection {

    @Schema(description = "Merge from left to right")
    LEFT_TO_RIGHT,

    @Schema(description = "Merge from right to left")
    RIGHT_TO_LEFT
}
