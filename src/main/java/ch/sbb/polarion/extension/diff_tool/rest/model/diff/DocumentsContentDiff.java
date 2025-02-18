package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
@Schema(description = "Represents the difference between inline content of two documents")
public class DocumentsContentDiff {

    @Schema(description = "The left document in the comparison", implementation = Document.class)
    private Document leftDocument;

    @Schema(description = "The right document in the comparison", implementation = Document.class)
    private Document rightDocument;

    @Schema(description = "A collection of paired anchors of inline content of two documents", implementation = DocumentContentAnchorsPair.class)
    private Collection<DocumentContentAnchorsPair> pairedContentAnchors;

}
