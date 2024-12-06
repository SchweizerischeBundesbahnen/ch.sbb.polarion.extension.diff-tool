package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.report.MergeReport;
import ch.sbb.polarion.extension.diff_tool.report.MergeReportEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class DocumentsFieldsMergeContext extends MergeContext {
    protected final DocumentIdentifier sourceDocumentIdentifier;
    protected final DocumentIdentifier targetDocumentIdentifier;

    protected DocumentsFieldsMergeContext(@NotNull MergeDirection direction, DocumentIdentifier leftDocument, DocumentIdentifier rightDocument) {
        super(direction);
        sourceDocumentIdentifier = direction.equals(MergeDirection.LEFT_TO_RIGHT) ? leftDocument : rightDocument;
        targetDocumentIdentifier = direction.equals(MergeDirection.LEFT_TO_RIGHT) ? rightDocument : leftDocument;
    }

    public void reportEntry(@NotNull MergeReport.OperationResultType operationResultType, @NotNull String fieldId, @NotNull String description) {
        mergeReport.addEntry(new MergeReportEntry(operationResultType, fieldId, description));
    }
}
