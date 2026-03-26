package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonFormat(shape = JsonFormat.Shape.STRING)
@Schema(description = "Enum representing the direction of a link role for newly created WorkItem")
public enum LinkRoleDirection {
    @Schema(description = "Direct link role")
    DIRECT,

    @Schema(description = "Reverse link role")
    REVERSE;

    @JsonCreator
    public static LinkRoleDirection fromValue(String value) {
        if (value == null || value.isBlank()) {
            return DIRECT;
        }
        return valueOf(value.toUpperCase());
    }
}
