package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comparison input data")
public class DocumentsFieldsDiffParams {
    @Schema(description = "Identifier for the left document in the comparison", implementation = DocumentIdentifier.class)
    private DocumentIdentifier leftDocument;

    @Schema(description = "Identifier for the right document in the comparison", implementation = DocumentIdentifier.class)
    private DocumentIdentifier rightDocument;

    @Schema(description = "Indicates if enums must be compared by their ID")
    private Boolean compareEnumsById;

    @Schema(description = "Indicates whether only mutual (declared in both documents) fields must be compared")
    private Boolean compareOnlyMutualFields;
}
