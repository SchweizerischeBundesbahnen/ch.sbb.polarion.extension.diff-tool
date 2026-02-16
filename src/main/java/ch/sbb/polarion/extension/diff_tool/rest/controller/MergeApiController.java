package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsContentMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsFieldsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsMergeParams;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;

import javax.inject.Singleton;
import javax.ws.rs.Path;

@Singleton
@Secured
@Path("/api")
public class MergeApiController extends MergeInternalController {
    @Override
    public MergeResult mergeDocuments(DocumentsMergeParams mergeParams) {
        return polarionService.callPrivileged(() -> super.mergeDocuments(mergeParams));
    }

    @Override
    public MergeResult mergeDocumentsFields(DocumentsFieldsMergeParams mergeParams) {
        return polarionService.callPrivileged(() -> super.mergeDocumentsFields(mergeParams));
    }

    @Override
    public MergeResult mergeDocumentsContent(DocumentsContentMergeParams mergeParams) {
        return polarionService.callPrivileged(() -> super.mergeDocumentsContent(mergeParams));
    }

    @Override
    public MergeResult mergeWorkItems(WorkItemsMergeParams mergeParams) {
        return polarionService.callPrivileged(() -> super.mergeWorkItems(mergeParams));
    }
}
