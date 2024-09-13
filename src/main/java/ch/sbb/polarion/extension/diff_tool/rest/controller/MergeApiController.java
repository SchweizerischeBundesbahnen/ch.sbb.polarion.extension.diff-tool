package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;

import javax.ws.rs.Path;

@Secured
@Path("/api")
public class MergeApiController extends MergeInternalController {
    @Override
    public MergeResult mergeWorkItems(MergeParams mergeParams) {
        return polarionService.callPrivileged(() -> super.mergeWorkItems(mergeParams));
    }
}
