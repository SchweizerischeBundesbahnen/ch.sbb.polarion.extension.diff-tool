package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.StatisticsParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.queue.ExecutionQueueMonitor;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;

import javax.ws.rs.Path;
import java.util.List;
import java.util.Map;

@Secured
@Path("/api")
public class ExecutionQueueQueueManagementApiController extends ExecutionQueueManagementInternalController {
    private final PolarionService polarionService = new PolarionService();

    public ExecutionQueueQueueManagementApiController(ExecutionQueueMonitor executionQueueMonitor) {
        super(executionQueueMonitor);
    }

    @Override
    public Map<String, Map<Feature, List<TimeframeStatisticsEntry>>> getStatistics(StatisticsParams statisticsParams) {
        return polarionService.callPrivileged(() -> super.getStatistics(statisticsParams));
    }

    @Override
    public void clearStatistics() {
        polarionService.callPrivileged(super::clearStatistics);
    }

}
