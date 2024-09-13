package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Represents the result of comparing two strings")
public class StringsDiff {

    @Schema(description = "Indicates whether the two strings are different")
    private boolean different;

    @Schema(description = "The result after comparing the strings")
    private String result;
}
