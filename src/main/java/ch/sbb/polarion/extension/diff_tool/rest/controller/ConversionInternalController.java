package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.conversion.PrintOrientation;
import ch.sbb.polarion.extension.diff_tool.rest.model.conversion.PrintPaperSize;
import ch.sbb.polarion.extension.diff_tool.service.HtmlToPdfConverterService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Hidden
@Path("/internal")
@Tag(name = "Conversion")
public class ConversionInternalController {
    protected final PolarionService polarionService = new PolarionService();
    private final HtmlToPdfConverterService htmlToPdfConverterService = new HtmlToPdfConverterService();

    @POST
    @Path("/conversion/html-to-pdf")
    @Consumes(MediaType.TEXT_HTML)
    @Produces("application/pdf")
    @Operation(summary = "Converts input HTML to PDF",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Content of PDF document as a byte array",
                            content = {@Content(mediaType = "application/pdf")}
                    )
            })
    public Response convertHtmlToPdf(@Parameter(description = "input html (must include html and body elements)") String html,
                                     @Parameter(description = "default value: landscape") @QueryParam("orientation") PrintOrientation orientation,
                                     @Parameter(description = "default value: A4") @QueryParam("paperSize") PrintPaperSize paperSize) {
        byte[] pdfBytes = htmlToPdfConverterService.convertHtmlToPdf(html, orientation, paperSize);
        return Response.ok(pdfBytes).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=DiffResult.pdf").build();
    }
}
