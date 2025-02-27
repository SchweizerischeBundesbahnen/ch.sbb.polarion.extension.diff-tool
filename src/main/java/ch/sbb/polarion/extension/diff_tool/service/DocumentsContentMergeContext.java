package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.report.MergeReport;
import ch.sbb.polarion.extension.diff_tool.report.MergeReportEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsContentMergePair;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import org.jetbrains.annotations.NotNull;

public class DocumentsContentMergeContext extends MergeContext {
    final DocumentIdentifier leftDocumentIdentifier;
    final DocumentIdentifier rightDocumentIdentifier;

    protected DocumentsContentMergeContext(@NotNull DocumentIdentifier leftDocumentIdentifier, @NotNull DocumentIdentifier rightDocumentIdentifier, @NotNull MergeDirection direction) {
        super(direction);

        this.leftDocumentIdentifier = leftDocumentIdentifier;
        this.rightDocumentIdentifier = rightDocumentIdentifier;
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
