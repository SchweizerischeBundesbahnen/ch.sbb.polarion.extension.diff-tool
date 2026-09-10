package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Input data of a chapter merge, i.e. copying or moving a part of one document into another one")
public class ChapterMergeParams {

    @Schema(description = "Identifier of the document content is taken from", implementation = DocumentIdentifier.class)
    private DocumentIdentifier sourceDocument;

    @Schema(description = "Identifier of the document content is placed into", implementation = DocumentIdentifier.class)
    private DocumentIdentifier targetDocument;

    @Schema(description = "Declares whether work items are copied or moved", implementation = ChapterMergeMode.class)
    private ChapterMergeMode mode;

    @Schema(description = "Declares where content is placed relatively to the target chapter", implementation = ChapterInsertMode.class)
    private ChapterInsertMode insertMode;

    @Schema(description = "Outline number of the chapter to be merged, eg. '2.1.1'")
    private String sourceChapterOutlineNumber;

    @Schema(description = "Outline number of the chapter in the target document which is used as an anchor, eg. '3.1'")
    private String targetChapterOutlineNumber;

    @Schema(description = "Declares how referenced work items should be handled", implementation = ReferencedItemsHandling.class, defaultValue = "KEEP_REFERENCE")
    private ReferencedItemsHandling referencedItems;

    @Schema(description = "Indicates whether work item layouts missing in the target document should be copied from the source one", defaultValue = "true")
    private boolean copyWorkItemLayouts;

    public ReferencedItemsHandling getReferencedItems() {
        return referencedItems != null ? referencedItems : ReferencedItemsHandling.KEEP_REFERENCE;
    }
}
