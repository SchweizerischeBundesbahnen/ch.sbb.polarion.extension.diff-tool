package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.CpuLoadEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.StatisticsParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.guice.internal.GuicePlatform;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry.MAX_HISTORY_ENTRIES;

public class ExecutionQueueMonitor {
    private static final Logger logger = Logger.getLogger(ExecutionQueueMonitor.class);
    private static final String EXECUTOR_COMMON = "COMMON"; // extra executor used for secondary activities (e.g., CPU Load)
    private final ExecutionQueueService executionService;
    private final CircularFifoQueue<CpuLoadEntry> cpuHistory = new CircularFifoQueue<>(MAX_HISTORY_ENTRIES);

    public ExecutionQueueMonitor(ExecutionQueueService executionService) {
        this.executionService = executionService;
        GuicePlatform.tryInjectMembers(this);
        clearHistory();

        Executors.newSingleThreadScheduledExecutor(new NamedDaemonThreadFactory("ExecutionQueueMonitor-Scheduler"))
                .scheduleAtFixedRate(this::collectStatistics, 0, 1, TimeUnit.SECONDS);
    }

    private void collectStatistics() {
        try {
            cpuHistory.add(new CpuLoadEntry());
            executionService.snapshotHistory();
        } catch (Exception e) {
            logger.error("Cannot collect statistics", e);
        }
    }

    public Map<String, Map<Feature, List<TimeframeStatisticsEntry>>> getHistory(StatisticsParams statisticsParams) {
        Map<String, Map<Feature, List<TimeframeStatisticsEntry>>> result = executionService.getHistory(statisticsParams);
        result.put(EXECUTOR_COMMON, Map.of(Feature.CPU_LOAD, statisticsParams.filterQueue(Feature.CPU_LOAD.name(), cpuHistory)));
        return result;
    }

    public void clearHistory() {
        cpuHistory.clear();
        executionService.clearHistory();
    }

    public void refreshConfiguration() {
        executionService.refreshConfiguration();
    }
}
