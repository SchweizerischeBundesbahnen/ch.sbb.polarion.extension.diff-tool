package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.CollectionsDiff;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.CollectionsDiffParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DetachedWorkItemsPairDiffParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsDiff;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsDiffParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsFieldsDiff;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsFieldsDiffParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPairs;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPairsParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.StringsDiff;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPairDiff;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentWorkItemsPairDiffParams;
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
import org.apache.commons.collections4.CollectionUtils;
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
    @Path("/diff/workitems-pairs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Finds pairs to specified WorkItems, for later diff",
            parameters = {
                    @Parameter(
                            description = "Parameters for finding WorkItems pairs",
                            required = true,
                            schema = @Schema(implementation = WorkItemsPairsParams.class)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved pairs of specified WorkItems to be later diffed",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = WorkItemsPairs.class)
                            )
                    )
            }
    )
    public WorkItemsPairs findWorkItemsPairs(@Parameter(required = true) WorkItemsPairsParams workItemsPairsParams) {
        if (workItemsPairsParams == null || workItemsPairsParams.getLeftProjectId() == null
                || workItemsPairsParams.getRightProjectId() == null || CollectionUtils.isEmpty(workItemsPairsParams.getLeftWorkItemIds())) {
            throw new BadRequestException("Parameters 'leftProjectId', 'rightProjectId' and 'workItemIds' should be provided");
        }
        return diffService.findWorkItemsPairs(workItemsPairsParams);
    }

    @POST
    @Path("/diff/collections")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets difference of two live document collections",
            parameters = {
                    @Parameter(
                            description = "Parameters for getting collections differences",
                            required = true,
                            schema = @Schema(implementation = CollectionsDiffParams.class)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the differences between the provided collections",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = CollectionsDiff.class)
                            )
                    )
            }
    )
    public CollectionsDiff getCollectionsDiff(@Parameter(required = true) CollectionsDiffParams collectionsDiffParams) {
        if (collectionsDiffParams == null || collectionsDiffParams.getLeftCollection() == null || collectionsDiffParams.getRightCollection() == null) {
            throw new BadRequestException("Parameters 'leftCollection' and 'rightCollection' should be provided");
        }
        return diffService.getCollectionsDiff(collectionsDiffParams);
    }

    @POST
    @Path("/diff/document-workitems")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets difference of two WorkItems contained in LiveDoc",
            parameters = {
                    @Parameter(
                            description = "Parameters for getting differences of two WorkItems",
                            required = true,
                            schema = @Schema(implementation = DocumentWorkItemsPairDiffParams.class)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the differences between the provided WorkItems",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = WorkItemsPairDiff.class)
                            )
                    )
            }
    )
    public WorkItemsPairDiff getDocumentWorkItemsPairDiff(@Parameter(required = true) DocumentWorkItemsPairDiffParams documentWorkItemsPairDiffParams) {
        if (documentWorkItemsPairDiffParams == null || (documentWorkItemsPairDiffParams.getLeftWorkItem() == null && documentWorkItemsPairDiffParams.getRightWorkItem() == null)) {
            throw new BadRequestException("Either parameter 'leftWorkItem' or 'rightWorkItem' should be provided");
        }
        return diffService.getDocumentWorkItemsPairDiff(documentWorkItemsPairDiffParams);
    }

    @POST
    @Path("/diff/detached-workitems")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets difference of two WorkItems not necessarily contained in a document",
            parameters = {
                    @Parameter(
                            description = "Parameters for getting differences of two WorkItems",
                            required = true,
                            schema = @Schema(implementation = DocumentWorkItemsPairDiffParams.class)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the differences between the provided WorkItems",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = WorkItemsPairDiff.class)
                            )
                    )
            }
    )
    public WorkItemsPairDiff getDetachedWorkItemsPairDiff(@Parameter(required = true) DetachedWorkItemsPairDiffParams detachedWorkItemsPairDiffParams) {
        if (detachedWorkItemsPairDiffParams == null || detachedWorkItemsPairDiffParams.getLeftWorkItem() == null) {
            throw new BadRequestException("'leftWorkItem' is required");
        }
        return diffService.getDetachedWorkItemsPairDiff(detachedWorkItemsPairDiffParams);
    }

    @POST
    @Path("/diff/documents-fields")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets difference between fields of two live documents",
            parameters = {
                    @Parameter(
                            description = "Parameters for getting the documents fields differences",
                            required = true,
                            schema = @Schema(implementation = DocumentsFieldsDiffParams.class)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the differences between fields of the provided documents",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = DocumentsFieldsDiff.class)
                            )
                    )
            }
    )
    public DocumentsFieldsDiff getDocumentsFieldsDiff(@Parameter(required = true) DocumentsFieldsDiffParams params) {
        if (params == null || params.getLeftDocument() == null || params.getRightDocument() == null) {
            throw new BadRequestException("Parameters 'leftDocument' and 'rightDocument' should be provided");
        }
        return diffService.getDocumentsFieldsDiff(params.getLeftDocument(), params.getRightDocument(),
                Boolean.TRUE.equals(params.getCompareEnumsById()), Boolean.TRUE.equals(params.getCompareOnlyMutualFields()));
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
