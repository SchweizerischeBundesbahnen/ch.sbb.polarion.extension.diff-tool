package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonFormat(shape = JsonFormat.Shape.NUMBER)
@Schema(description = "Enum representing the direction in which a merge move operation can occur")
public enum MergeMoveDirection {

    @Schema(description = "Move up in the merge direction")
    UP,

    @Schema(description = "Move down in the merge direction")
    DOWN;

    public MergeMoveDirection getOpposite() {
        return this == UP ? DOWN : UP;
    }
}
