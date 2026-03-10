package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents a field of WorkItem potentially to be merged")
public class MergeField {
    @Schema(description = "Field ID")
    private String id;

    @Schema(description = "If field is selected for merge")
    private boolean selected;
}
