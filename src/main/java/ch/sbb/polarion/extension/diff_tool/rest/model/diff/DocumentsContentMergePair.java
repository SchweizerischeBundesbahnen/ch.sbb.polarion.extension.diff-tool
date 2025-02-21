package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Documents content merge pair definition")
public class DocumentsContentMergePair {
    @Schema(description = "Identifier of the left WorkItem", implementation = String.class)
    private String leftWorkItemId;

    @Schema(description = "Identifier of the right WorkItem", implementation = String.class)
    private String rightWorkItemId;

    @Schema(description = "Specifies if content above or below WorkItems to merge", implementation = DocumentContentAnchor.ContentSide.class)
    private DocumentContentAnchor.ContentSide contentSide;
}
