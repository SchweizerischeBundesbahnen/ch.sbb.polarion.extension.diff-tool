package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsMergeParams;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;

import javax.ws.rs.Path;

@Secured
@Path("/api")
public class MergeApiController extends MergeInternalController {
    @Override
    public MergeResult mergeDocuments(DocumentsMergeParams mergeParams) {
        return polarionService.callPrivileged(() -> super.mergeDocuments(mergeParams));
    }

    @Override
    public MergeResult mergeWorkItems(WorkItemsMergeParams mergeParams) {
        return polarionService.callPrivileged(() -> super.mergeWorkItems(mergeParams));
    }
}
