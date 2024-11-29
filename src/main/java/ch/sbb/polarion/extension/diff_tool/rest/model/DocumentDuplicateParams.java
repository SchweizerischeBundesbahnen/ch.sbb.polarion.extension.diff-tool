package ch.sbb.polarion.extension.diff_tool.rest.model;

import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Parameters to create a document duplicated from another one")
public class DocumentDuplicateParams {
    @Schema(description = "Identification data of target document")
    private BaseDocumentIdentifier targetDocumentIdentifier;

    @Schema(description = "Title of target document")
    private String targetDocumentTitle;

    @Schema(description = "ID of link role by which work items in target document will be linked with counterparts from source document")
    private String linkRoleId;

    @Schema(description = "The configuration name to use for the duplication, which specifies which fields of work items to copy", defaultValue = NamedSettings.DEFAULT_NAME)
    private String configName;

    public String getConfigName() {
        return configName != null ? configName : NamedSettings.DEFAULT_NAME;
    }
}
