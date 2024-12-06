package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Input data to compare WorkItems contained in a document")
public class DocumentWorkItemsPairDiffParams extends WorkItemsPairDiffParams<DocumentWorkItem> {

    @Override
    @Schema(description = "Left WorkItem used in the comparison", implementation = DocumentWorkItem.class)
    public DocumentWorkItem getLeftWorkItem() {
        return super.getLeftWorkItem();
    }

    @Override
    @Schema(description = "Right WorkItem used in the comparison", implementation = DocumentWorkItem.class)
    public DocumentWorkItem getRightWorkItem() {
        return super.getRightWorkItem();
    }
}
