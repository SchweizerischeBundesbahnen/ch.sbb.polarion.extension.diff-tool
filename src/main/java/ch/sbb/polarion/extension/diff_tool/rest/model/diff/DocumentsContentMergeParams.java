package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Documents content merge input data")
public class DocumentsContentMergeParams {

    @Schema(description = "Identifier for the left document", implementation = DocumentIdentifier.class)
    private DocumentIdentifier leftDocument;

    @Schema(description = "Identifier for the right document", implementation = DocumentIdentifier.class)
    private DocumentIdentifier rightDocument;

    @Schema(description = "The direction of the merge operation", implementation = MergeDirection.class)
    private MergeDirection direction;

    @Schema(description = "List of merge pairs to be considered in the merge operation", implementation = DocumentsContentMergePair.class)
    private List<DocumentsContentMergePair> pairs;
}
