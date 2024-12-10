package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Schema(description = "Documents fields merge input data")
public class DocumentsFieldsMergeParams {

    @Schema(description = "Identifier for the left document", implementation = DocumentIdentifier.class)
    private DocumentIdentifier leftDocument;

    @Schema(description = "Identifier for the right document", implementation = DocumentIdentifier.class)
    private DocumentIdentifier rightDocument;

    @Schema(description = "The direction of the merge operation", implementation = MergeDirection.class)
    private MergeDirection direction;

    @Schema(description = "List of fields IDs to be considered in the merge operation")
    private List<String> fieldIds;

}
