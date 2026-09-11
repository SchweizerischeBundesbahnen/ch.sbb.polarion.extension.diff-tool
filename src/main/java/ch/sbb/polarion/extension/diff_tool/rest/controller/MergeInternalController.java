package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeJobInfo;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsContentMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsFieldsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsMergeParams;
import ch.sbb.polarion.extension.diff_tool.service.MergeService;
import ch.sbb.polarion.extension.diff_tool.service.job.ChapterMergeJobScheduler;
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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Singleton
@Hidden
@Path("/internal")
@Tag(name = "Merge")
public class MergeInternalController {
    protected final PolarionService polarionService = new PolarionService();
    protected final MergeService mergeService = new MergeService(polarionService);
    protected final ChapterMergeJobScheduler chapterMergeJobScheduler;

    public MergeInternalController() {
        this(new ChapterMergeJobScheduler());
    }

    public MergeInternalController(ChapterMergeJobScheduler chapterMergeJobScheduler) {
        this.chapterMergeJobScheduler = chapterMergeJobScheduler;
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
            description = "Merging a chapter can take long, so it is executed as a Polarion job: this method returns as soon as "
                    + "the job is scheduled, its result is delivered by GET /merge/chapter/jobs/{jobId}",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = ChapterMergeParams.class))
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Information about the scheduled chapter merge job",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ChapterMergeJobInfo.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Mandatory parameters are missing")
            }
    )
    public ChapterMergeJobInfo mergeChapter(ChapterMergeParams mergeParams) {
        if (mergeParams == null || mergeParams.getSourceDocument() == null || mergeParams.getTargetDocument() == null
                || mergeParams.getMode() == null || mergeParams.getInsertMode() == null) {
            throw new BadRequestException("Parameters 'sourceDocument', 'targetDocument', 'mode' and 'insertMode' should be provided");
        }
        if (StringUtils.isBlank(mergeParams.getSourceChapterOutlineNumber()) || StringUtils.isBlank(mergeParams.getTargetChapterOutlineNumber())) {
            throw new BadRequestException("Parameters 'sourceChapterOutlineNumber' and 'targetChapterOutlineNumber' should be provided");
        }
        return chapterMergeJobScheduler.schedule(mergeParams);
    }

    @GET
    @Path("/merge/chapter/jobs")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Lists chapter merge jobs, the most recent one first",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Chapter merge jobs", useReturnTypeSchema = true)
            }
    )
    public List<ChapterMergeJobInfo> listChapterMergeJobs() {
        return chapterMergeJobScheduler.listJobs();
    }

    @GET
    @Path("/merge/chapter/jobs/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns a certain chapter merge job, with its merge result as soon as it has one",
            description = "This is what a caller of a chapter merge polls to learn that the merge has finished: "
                    + "a job which is still running is answered with its state and no merge result",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The chapter merge job",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ChapterMergeJobInfo.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "There is no chapter merge job with this ID")
            }
    )
    public ChapterMergeJobInfo getChapterMergeJob(@Parameter(description = "ID of the chapter merge job") @PathParam("jobId") String jobId) {
        ChapterMergeJobInfo jobInfo = chapterMergeJobScheduler.getJob(jobId);
        if (jobInfo == null) {
            throw new NotFoundException("No chapter merge job '%s' could be found".formatted(jobId));
        }
        return jobInfo;
    }

}
