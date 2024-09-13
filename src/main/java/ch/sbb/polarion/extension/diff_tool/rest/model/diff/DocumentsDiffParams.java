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
public class DocumentsDiffParams {
    @Schema(description = "Identifier for the left document in the comparison", implementation = DocumentIdentifier.class)
    private DocumentIdentifier leftDocument;

    @Schema(description = "Identifier for the right document in the comparison", implementation = DocumentIdentifier.class)
    private DocumentIdentifier rightDocument;

    @Schema(description = "The role of the link connecting the two documents")
    private String linkRole;

    @Schema(description = "The configuration name to use for the comparison")
    private String configName;

    @Schema(description = "The ID of the configuration cache bucket")
    private String configCacheBucketId;
}
