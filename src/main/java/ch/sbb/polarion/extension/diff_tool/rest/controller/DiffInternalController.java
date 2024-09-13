package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsDiff;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsDiffParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.StringsDiff;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsDiff;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsDiffParams;
import ch.sbb.polarion.extension.diff_tool.service.DiffService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.util.DiffToolUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Hidden
@Path("/internal")
@Tag(name = "Difference")
public class DiffInternalController {
    protected final PolarionService polarionService = new PolarionService();
    protected final DiffService diffService = new DiffService(polarionService);

    @POST
    @Path("/diff/documents")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets difference of two live documents",
            parameters = {
                    @Parameter(
                            description = "Parameters for getting the document differences",
                            required = true,
                            schema = @Schema(implementation = DocumentsDiffParams.class)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the differences between the provided documents",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = DocumentsDiff.class)
                            )
                    )
            }
    )
    public DocumentsDiff getDocumentsDiff(@Parameter(required = true) DocumentsDiffParams documentsDiffParams) {
        if (documentsDiffParams == null || documentsDiffParams.getLeftDocument() == null || documentsDiffParams.getRightDocument() == null || documentsDiffParams.getLinkRole() == null) {
            throw new BadRequestException("Parameters 'leftDocument', 'rightDocument' and 'linkRole' should be provided");
        }
        return diffService.getDocumentsDiff(documentsDiffParams.getLeftDocument(), documentsDiffParams.getRightDocument(), documentsDiffParams.getLinkRole(),
                documentsDiffParams.getConfigName(), documentsDiffParams.getConfigCacheBucketId());
    }

    @POST
    @Path("/diff/workitems")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets difference of two WorkItems",
            parameters = {
                    @Parameter(
                            description = "Parameters for getting the WorkItem differences",
                            required = true,
                            schema = @Schema(implementation = WorkItemsDiffParams.class)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the differences between the provided WorkItems",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = WorkItemsDiff.class)
                            )
                    )
            }
    )
    public WorkItemsDiff getWorkItemsDiff(@Parameter(required = true) WorkItemsDiffParams workItemsDiffParams) {
        if (workItemsDiffParams == null || (workItemsDiffParams.getLeftWorkItem() == null && workItemsDiffParams.getRightWorkItem() == null)) {
            throw new BadRequestException("Either parameter 'leftWorkItem' or 'rightWorkItem' should be provided");
        }
        return diffService.getWorkItemsDiff(workItemsDiffParams);
    }

    @POST
    @Path("/diff/html")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets difference of two strings which contain HTML tags",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the differences between the provided HTML strings",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = StringsDiff.class)
                            )
                    )
            }
    )
    public StringsDiff diffHtml(@FormDataParam("html1") String html1, @FormDataParam("html2") String html2) {
        return DiffToolUtils.diffHtml(html1, html2);
    }

    @POST
    @Path("/diff/text")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets difference of two strings which contain plain text",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the differences between the provided plain text strings",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = StringsDiff.class)
                            )
                    )
            }
    )
    public StringsDiff diffText(@FormDataParam("text1") String text1, @FormDataParam("text2") String text2) {
        return DiffToolUtils.diffText(text1, text2);
    }
}
