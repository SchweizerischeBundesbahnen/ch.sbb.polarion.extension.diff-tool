package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.service.MergeService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Hidden
@Path("/internal")
@Tag(name = "Merge")
public class MergeInternalController {
    protected final PolarionService polarionService = new PolarionService();
    protected final MergeService mergeService = new MergeService(polarionService);

    @POST
    @Path("/merge/workitems")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Merge WorkItems",
            parameters = {
                    @Parameter(
                            description = "Parameters for merging WorkItems",
                            required = true,
                            schema = @Schema(implementation = MergeParams.class)
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
    public MergeResult mergeWorkItems(@Parameter(required = true) MergeParams mergeParams) {
        if (mergeParams == null || mergeParams.getLeftDocument() == null || mergeParams.getRightDocument() == null || mergeParams.getDirection() == null) {
            throw new BadRequestException("Parameters 'leftDocument', 'rightDocument' and 'direction' should be provided");
        }
        return mergeService.mergeWorkItems(mergeParams.getLeftDocument(), mergeParams.getRightDocument(), mergeParams.getDirection(), mergeParams.getLinkRole(), mergeParams.getPairs(),
                mergeParams.getConfigName(), mergeParams.getConfigCacheBucketId(), mergeParams.getAllowReferencedWorkItemMerge());
    }
}
