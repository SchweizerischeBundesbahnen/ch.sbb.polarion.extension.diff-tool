package ch.sbb.polarion.extension.diff_tool.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Unique document identifier data with optional revision information, if revision is not specified this means document is in HEAD revision")
public class DocumentIdentifier extends BaseDocumentIdentifier{

    @Schema(description = "Revision number of the document")
    private String revision;

    @Schema(description = "XML module revision number")
    private String moduleXmlRevision;

    @Builder
    public DocumentIdentifier(String projectId, String spaceId, String name, String revision, String moduleXmlRevision) {
        super(projectId, spaceId, name);
        this.revision = revision;
        this.moduleXmlRevision = moduleXmlRevision;
    }

    public DocumentIdentifier(BaseDocumentIdentifier baseDocumentIdentifier) {
        super(baseDocumentIdentifier.getProjectId(), baseDocumentIdentifier.getSpaceId(), baseDocumentIdentifier.getName());
    }
}
