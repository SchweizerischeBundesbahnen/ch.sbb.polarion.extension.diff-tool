package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.conversion.PrintOrientation;
import ch.sbb.polarion.extension.diff_tool.rest.model.conversion.PrintPaperSize;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Secured
@Path("/api")
public class ConversionApiController extends ConversionInternalController {
    @Override
    public Response convertHtmlToPdf(String html, PrintOrientation orientation, PrintPaperSize paperSize) {
        return polarionService.callPrivileged(() -> super.convertHtmlToPdf(html, orientation, paperSize));
    }
}
