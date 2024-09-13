package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.*;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;

import javax.ws.rs.Path;

@Secured
@Path("/api")
public class DiffApiController extends DiffInternalController {
    @Override
    public DocumentsDiff getDocumentsDiff(DocumentsDiffParams documentsDiffParams) {
        return polarionService.callPrivileged(() -> super.getDocumentsDiff(documentsDiffParams));
    }

    @Override
    public WorkItemsDiff getWorkItemsDiff(WorkItemsDiffParams workItemsDiffParams) {
        return polarionService.callPrivileged(() -> super.getWorkItemsDiff(workItemsDiffParams));
    }

    @Override
    public StringsDiff diffHtml(String html1, String html2) {
        return polarionService.callPrivileged(() -> super.diffHtml(html1, html2));
    }

    @Override
    public StringsDiff diffText(String text1, String text2) {
        return polarionService.callPrivileged(() -> super.diffText(text1, text2));
    }
}
