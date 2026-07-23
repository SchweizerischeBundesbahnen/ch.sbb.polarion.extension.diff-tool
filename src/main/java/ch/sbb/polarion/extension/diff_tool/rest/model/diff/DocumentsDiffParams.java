package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Schema(description = "Boolean value which specifies if documents to be compared are a branched documents")
    private boolean branchedDocuments;

    @Schema(description = "The role of the link connecting the two documents")
    private String linkRole;

    @Schema(description = "The configuration name to use for the comparison", defaultValue = NamedSettings.DEFAULT_NAME)
    private String configName;

    @Schema(description = "The ID of the configuration cache bucket")
    private String configCacheBucketId;

    @Schema(description = "Indicates whether structural changes (moved / added / deleted items) are included; when false only content changes of items present on both sides are returned. Defaults to true.")
    private Boolean syncStructure;

    public String getConfigName() {
        return configName != null ? configName : NamedSettings.DEFAULT_NAME;
    }

    @JsonIgnore
    public boolean isSyncStructureEnabled() {
        return !Boolean.FALSE.equals(syncStructure); // default true when null
    }
}
