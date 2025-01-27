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
@Schema(description = "Represents a pair of documents fields for comparison")
public class DocumentsFieldsPair {

    @Schema(description = "The left field in the pair", implementation = WorkItem.Field.class)
    private WorkItem.Field leftField;

    @Schema(description = "The right field in the pair", implementation = WorkItem.Field.class)
    private WorkItem.Field rightField;
}
