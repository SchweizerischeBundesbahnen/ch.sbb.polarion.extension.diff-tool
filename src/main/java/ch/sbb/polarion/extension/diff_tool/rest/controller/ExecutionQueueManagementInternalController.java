package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.DiffToolRestApplication;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.FeatureInfo;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.QueueConfigurationMeta;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.StatisticsParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry;
import ch.sbb.polarion.extension.diff_tool.service.queue.ExecutionQueueMonitor;
import ch.sbb.polarion.extension.diff_tool.service.queue.ExecutionWorker;
import ch.sbb.polarion.extension.diff_tool.util.OSUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Singleton
@Hidden
@Path("/internal")
@Tag(name = "Queued Execution Management")
public class ExecutionQueueManagementInternalController {
    private final ExecutionQueueMonitor executionMonitor;

    public ExecutionQueueManagementInternalController() {
        this.executionMonitor = DiffToolRestApplication.getExecutionMonitor();
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
                            description = "List of statistics items for each worker",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = TimeframeStatisticsEntry.class)
                            )
                    )
            }
    )
    public Map<String, Map<Feature, List<TimeframeStatisticsEntry>>> getStatistics(@Parameter StatisticsParams statisticsParams) {
        return executionMonitor.getHistory(statisticsParams == null ? new StatisticsParams() : statisticsParams);
    }

    @GET
    @Path("/queue/configuration-meta")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets the static metadata needed to render the execution queue configuration",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Assignable features with their labels, worker count, thread limit and queue capacity",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = QueueConfigurationMeta.class)
                            )
                    )
            }
    )
    public QueueConfigurationMeta getConfigurationMeta() {
        List<Feature> workerFeatures = Feature.workerFeatures();
        return QueueConfigurationMeta.builder()
                .features(workerFeatures.stream().map(FeatureInfo::of).toList())
                .cpuLoad(FeatureInfo.of(Feature.CPU_LOAD))
                // One worker per assignable feature, which is what the settings model allocates.
                .workerCount(workerFeatures.size())
                .maxRecommendedThreads(OSUtils.getMaxRecommendedParallelThreads())
                .queueCapacity(ExecutionWorker.DEFAULT_MAX_QUEUE_CAPACITY)
                .build();
    }

    @DELETE
    @Path("/queueStatistics")
    @Operation(summary = "Clear gathered statistics",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Statistics cleared successfully"
                    )
            }
    )
    public void clearStatistics() {
        executionMonitor.clearHistory();
    }

}
