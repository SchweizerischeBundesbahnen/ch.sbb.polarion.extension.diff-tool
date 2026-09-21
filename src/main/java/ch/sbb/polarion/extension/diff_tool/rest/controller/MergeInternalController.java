package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.properties.DiffToolExtensionConfiguration;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsContentMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsFieldsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.jobs.ChapterMergeJobDetails;
import ch.sbb.polarion.extension.diff_tool.rest.model.jobs.ChapterMergeJobStatus;
import ch.sbb.polarion.extension.diff_tool.service.MergeService;
import ch.sbb.polarion.extension.diff_tool.service.job.ChapterMergeJobsService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Hidden
@Path("/internal")
@Tag(name = "Merge")
public class MergeInternalController {
    protected final PolarionService polarionService;
    protected final MergeService mergeService;
    protected final ChapterMergeJobsService chapterMergeJobsService;

    @Context
    private UriInfo uriInfo;

    public MergeInternalController() {
        this(new PolarionService());
    }

    public MergeInternalController(@NotNull PolarionService polarionService) {
        this(polarionService, new ChapterMergeJobsService(polarionService));
    }

    @VisibleForTesting
    MergeInternalController(@NotNull PolarionService polarionService, @NotNull ChapterMergeJobsService chapterMergeJobsService) {
        this.polarionService = polarionService;
        this.mergeService = new MergeService(polarionService);
        this.chapterMergeJobsService = chapterMergeJobsService;
    }

    @VisibleForTesting
    void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    @POST
    @Path("/merge/documents")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Merge Documents and their WorkItems",
            parameters = {
                    @Parameter(
                            description = "Parameters for merging Documents and their WorkItems",
                            required = true,
                            schema = @Schema(implementation = DocumentsMergeParams.class)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Merge result",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = MergeResult.class)
                            )
                    )
            }
    )
    public MergeResult mergeDocuments(@Parameter(required = true) DocumentsMergeParams mergeParams) {
        if (mergeParams == null || mergeParams.getLeftDocument() == null || mergeParams.getRightDocument() == null || mergeParams.getMergeDirection() == null) {
            throw new BadRequestException("Parameters 'leftDocument', 'rightDocument' and 'mergeDirection' should be provided");
        }
        return mergeService.mergeDocuments(mergeParams);
    }

    @POST
    @Path("/merge/documents-fields")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Merge fields of documents",
            parameters = {
                    @Parameter(
                            description = "Parameters for merging documents fields",
                            required = true,
                            schema = @Schema(implementation = DocumentsFieldsMergeParams.class)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Merge result",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = MergeResult.class)
                            )
                    )
            }
    )
    public MergeResult mergeDocumentsFields(@Parameter(required = true) DocumentsFieldsMergeParams mergeParams) {
        if (mergeParams == null || mergeParams.getLeftDocument() == null || mergeParams.getRightDocument() == null || mergeParams.getMergeDirection() == null
                || mergeParams.getFieldIds() == null || mergeParams.getFieldIds().isEmpty()) {
            throw new BadRequestException("Parameters 'leftDocument', 'rightDocument', 'mergeDirection' and 'fieldIds' should be provided");
        }
        return mergeService.mergeDocumentsFields(mergeParams);
    }

    @POST
    @Path("/merge/documents-content")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Merge inline content of documents",
            parameters = {
                    @Parameter(
                            description = "Parameters for merging documents content",
                            required = true,
                            schema = @Schema(implementation = DocumentsContentMergeParams.class)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Merge result",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = MergeResult.class)
                            )
                    )
            }
    )
    public MergeResult mergeDocumentsContent(@Parameter(required = true) DocumentsContentMergeParams mergeParams) {
        if (mergeParams == null || mergeParams.getLeftDocument() == null || mergeParams.getRightDocument() == null || mergeParams.getMergeDirection() == null
                || mergeParams.getPairs() == null || mergeParams.getPairs().isEmpty()) {
            throw new BadRequestException("Parameters 'leftDocument', 'rightDocument', 'mergeDirection' and 'pairs' should be provided");
        }
        return mergeService.mergeDocumentsContent(mergeParams);
    }

    @POST
    @Path("/merge/workitems")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Merge WorkItems out of documents scope",
            parameters = {
                    @Parameter(
                            description = "Parameters for merging WorkItems out of documents scope",
                            required = true,
                            schema = @Schema(implementation = WorkItemsMergeParams.class)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Merge result",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = MergeResult.class)
                            )
                    )
            }
    )
    public MergeResult mergeWorkItems(@Parameter(required = true) WorkItemsMergeParams mergeParams) {
        if (mergeParams == null || mergeParams.getLeftProject() == null || mergeParams.getRightProject() == null || mergeParams.getMergeDirection() == null) {
            throw new BadRequestException("Parameters 'leftProject', 'rightProject' and 'mergeDirection' should be provided");
        }
        return mergeService.mergeWorkItems(mergeParams);
    }

    @POST
    @Path("/merge/chapter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Copies or moves a chapter of one Document into another one",
            description = "Merging a chapter can take long, so it is carried out in the background: this method returns as soon as "
                    + "the merge is started, and the Location header names the job which delivers its result",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = ChapterMergeParams.class))
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "202",
                            description = "The merge is started, the job URI is returned in the Location header"
                    ),
                    @ApiResponse(responseCode = "400", description = "Mandatory parameters are missing"),
                    @ApiResponse(responseCode = "403", description = "The current user is not authorized to merge into these documents"),
                    @ApiResponse(responseCode = "429", description = "Too many chapter merges are running or waiting for their turn")
            }
    )
    public Response mergeChapter(ChapterMergeParams mergeParams) {
        checkMergeParams(mergeParams);
        checkAuthorizedForMerge(mergeParams);

        String jobId = chapterMergeJobsService.startJob(mergeParams, DiffToolExtensionConfiguration.getInstance().getChapterMergeTimeout());

        URI jobUri = UriBuilder.fromUri(uriInfo.getRequestUri().getPath()).path("jobs").path(jobId).build();
        return Response.accepted().location(jobUri).build();
    }

    @GET
    @Path("/merge/chapter/jobs")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns the states of all chapter merge jobs of the current user",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Chapter merge jobs, by job ID",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(type = "object", additionalPropertiesSchema = ChapterMergeJobDetails.class))
                    )
            }
    )
    public Response getAllChapterMergeJobs() {
        Map<String, ChapterMergeJobDetails> jobsDetails = chapterMergeJobsService.getAllJobsStates().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> toJobDetails(entry.getValue())));
        return Response.ok(jobsDetails).build();
    }

    @GET
    @Path("/merge/chapter/jobs/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns the state of a chapter merge job",
            description = "This is what a caller of a chapter merge polls to learn that the merge has finished: "
                    + "a merge which is still running is answered with 202, a finished one redirects to its result",
            responses = {
                    // The media types of the 303 and 202 responses are generic, so that SwaggerUI follows the redirect
                    @ApiResponse(
                            responseCode = "303",
                            description = "The merge has finished, the Location header contains the URL of its result",
                            content = @Content(mediaType = "application/*", schema = @Schema(implementation = ChapterMergeJobDetails.class))
                    ),
                    @ApiResponse(
                            responseCode = "202",
                            description = "The merge is still running",
                            content = @Content(mediaType = "application/*", schema = @Schema(implementation = ChapterMergeJobDetails.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "The merge failed and produced no result of its own",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ChapterMergeJobDetails.class))
                    ),
                    @ApiResponse(responseCode = "404", description = "There is no chapter merge job with this ID")
            }
    )
    public Response getChapterMergeJob(@Parameter(description = "ID of the chapter merge job") @PathParam("jobId") String jobId) {
        ChapterMergeJobsService.JobState jobState = chapterMergeJobsService.getJobState(jobId);
        ChapterMergeJobDetails jobDetails = toJobDetails(jobState);

        Response.ResponseBuilder responseBuilder = switch (jobDetails.getStatus()) {
            case IN_PROGRESS -> Response.accepted();
            case SUCCESSFULLY_FINISHED -> Response.status(Response.Status.SEE_OTHER)
                    .location(UriBuilder.fromUri(uriInfo.getRequestUri().getPath()).path("result").build());
            case FAILED -> Response.status(Response.Status.CONFLICT);
        };
        return responseBuilder.entity(jobDetails).build();
    }

    @GET
    @Path("/merge/chapter/jobs/{jobId}/result")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns the result of a chapter merge job",
            description = "A merge which did not do what was asked of it reports that in its result too, so an "
                    + "unsuccessful merge result is answered with 200 like any other",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The merge result",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MergeResult.class))
                    ),
                    @ApiResponse(responseCode = "204", description = "The merge is still running"),
                    @ApiResponse(responseCode = "409", description = "The merge failed and produced no result of its own"),
                    @ApiResponse(responseCode = "404", description = "There is no chapter merge job with this ID")
            }
    )
    public Response getChapterMergeJobResult(@Parameter(description = "ID of the chapter merge job") @PathParam("jobId") String jobId) {
        return chapterMergeJobsService.getJobResult(jobId)
                .map(mergeResult -> Response.ok(mergeResult).build())
                .orElseGet(() -> Response.noContent().build());
    }

    /**
     * Refuses a merge which does not say what to merge where.
     */
    private void checkMergeParams(ChapterMergeParams mergeParams) {
        if (mergeParams == null || mergeParams.getSourceDocument() == null || mergeParams.getTargetDocument() == null
                || mergeParams.getMode() == null || mergeParams.getInsertMode() == null) {
            throw new BadRequestException("Parameters 'sourceDocument', 'targetDocument', 'mode' and 'insertMode' should be provided");
        }
        if (StringUtils.isBlank(mergeParams.getSourceChapterOutlineNumber()) || StringUtils.isBlank(mergeParams.getTargetChapterOutlineNumber())) {
            throw new BadRequestException("Parameters 'sourceChapterOutlineNumber' and 'targetChapterOutlineNumber' should be provided");
        }
    }

    /**
     * Turns a caller away who may not merge into the documents they named, before their merge takes a place in the
     * queue of the merges waiting for a thread. The merge itself refuses them too, but only once it has that thread
     * and has read both documents - by then the work of an unauthorized caller has already been queued.
     */
    private void checkAuthorizedForMerge(@NotNull ChapterMergeParams mergeParams) {
        String targetProjectId = mergeParams.getTargetDocument().getProjectId();
        if (!polarionService.userAuthorizedForMerge(targetProjectId)) {
            throw new ForbiddenException("You are not authorized to merge into project '%s'".formatted(targetProjectId));
        }
        // a move takes the work items out of the source document, which changes that document too
        String sourceProjectId = mergeParams.getSourceDocument().getProjectId();
        if (mergeParams.getMode() == ChapterMergeMode.MOVE && !polarionService.userAuthorizedForMerge(sourceProjectId)) {
            throw new ForbiddenException("You are not authorized to move work items out of project '%s'".formatted(sourceProjectId));
        }
    }

    private @NotNull ChapterMergeJobDetails toJobDetails(ChapterMergeJobsService.@NotNull JobState jobState) {
        ChapterMergeJobStatus status;
        if (!jobState.isDone()) {
            status = ChapterMergeJobStatus.IN_PROGRESS;
        } else if (jobState.isFailed()) {
            status = ChapterMergeJobStatus.FAILED;
        } else {
            status = ChapterMergeJobStatus.SUCCESSFULLY_FINISHED;
        }
        return ChapterMergeJobDetails.builder()
                .status(status)
                .progressMessage(status == ChapterMergeJobStatus.IN_PROGRESS ? jobState.progressMessage() : null)
                .errorMessage(jobState.errorMessage())
                .build();
    }

}
