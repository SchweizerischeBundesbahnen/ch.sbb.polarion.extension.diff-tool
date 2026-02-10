package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.report.MergeReport;
import ch.sbb.polarion.extension.diff_tool.report.MergeReportEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsContentMergePair;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class DocumentsContentMergeContext extends MergeContext implements IPreserveCommentsContext {
    final DocumentIdentifier leftDocumentIdentifier;
    final DocumentIdentifier rightDocumentIdentifier;
    @Getter
    final boolean preserveComments;

    protected DocumentsContentMergeContext(@NotNull DocumentIdentifier leftDocumentIdentifier, @NotNull DocumentIdentifier rightDocumentIdentifier, @NotNull MergeDirection direction, boolean preserveComments) {
        super(direction, false);

        this.leftDocumentIdentifier = leftDocumentIdentifier;
        this.rightDocumentIdentifier = rightDocumentIdentifier;
        this.preserveComments = preserveComments;
    }

    public DocumentIdentifier getSourceDocumentIdentifier() {
        return direction == MergeDirection.LEFT_TO_RIGHT ? leftDocumentIdentifier : rightDocumentIdentifier;
    }

    public DocumentIdentifier getTargetDocumentIdentifier() {
        return direction == MergeDirection.LEFT_TO_RIGHT ? rightDocumentIdentifier : leftDocumentIdentifier;
    }

    public String getSourceWorkItemId(DocumentsContentMergePair mergePair) {
        return direction == MergeDirection.LEFT_TO_RIGHT ? mergePair.getLeftWorkItemId() : mergePair.getRightWorkItemId();
    }

    public String getTargetWorkItemId(DocumentsContentMergePair mergePair) {
        return direction == MergeDirection.LEFT_TO_RIGHT ? mergePair.getRightWorkItemId() : mergePair.getLeftWorkItemId();
    }

    public void reportEntry(@NotNull MergeReport.OperationResultType operationResultType, @NotNull DocumentsContentMergePair mergePair, @NotNull String description) {
        mergeReport.addEntry(new MergeReportEntry(operationResultType, mergePair, description));
    }
}
