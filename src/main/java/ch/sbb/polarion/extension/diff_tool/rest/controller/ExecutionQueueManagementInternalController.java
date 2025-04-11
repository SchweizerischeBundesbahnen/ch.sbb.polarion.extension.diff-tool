package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentRevision;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.StatisticsParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.queue.ExecutionQueueMonitor;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Hidden
@Path("/internal")
@Tag(name = "Queued Execution Management")
public class ExecutionQueueManagementInternalController {
    protected final PolarionService polarionService = new PolarionService();
    private final ExecutionQueueMonitor executionMonitor;

    public ExecutionQueueManagementInternalController(ExecutionQueueMonitor executionMonitor) {
        this.executionMonitor = executionMonitor;
    }

    @POST
    @Path("/queueStatistics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets list of statistics items for each worker",
            parameters = {
                    @Parameter(
                            description = "Parameters for getting statistics",
                            required = true,
                            schema = @Schema(implementation = StatisticsParams.class)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of revisions for the specified document",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = DocumentRevision.class)
                            )
                    )
            }
    )
    public Map<String, Map<Feature, List<TimeframeStatisticsEntry>>> getStatistics(@Parameter StatisticsParams statisticsParams) {
        return executionMonitor.getHistory(statisticsParams == null ? new StatisticsParams() : statisticsParams);
    }

    @DELETE
    @Path("/queueStatistics")
    @Operation(summary = "Clear gathered statistics",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of revisions for the specified document",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = DocumentRevision.class)
                            )
                    )
            }
    )
    public void clearStatistics() {
        executionMonitor.clearHistory();
    }

    @POST
    @Path("/refreshQueueConfiguration")
    @Operation(summary = "Refresh configuration",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of revisions for the specified document",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = DocumentRevision.class)
                            )
                    )
            }
    )
    public void refreshConfiguration() {
        executionMonitor.refreshConfiguration();
    }

}
