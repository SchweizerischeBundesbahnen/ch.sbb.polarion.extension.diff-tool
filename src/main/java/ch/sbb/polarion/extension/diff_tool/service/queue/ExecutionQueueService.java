package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.StatisticsParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry;
import ch.sbb.polarion.extension.diff_tool.settings.ExecutionQueueSettings;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A service that queues tasks and executes them using a configurable number
 * of worker threads while blocking calling threads until completion.
 */
@SuppressWarnings("unused")
public class ExecutionQueueService {

    public static final int WORKERS_COUNT = 9;
    private final Map<Integer, ExecutionWorker> workers = new ConcurrentHashMap<>();

    public ExecutionQueueService() {
        for (int i = 1; i <= WORKERS_COUNT; i++) {
            workers.put(i, new ExecutionWorker(i));
        }
    }

    public void refreshConfiguration() {
        for (int i = 1; i <= WORKERS_COUNT; i++) {
            workers.get(i).refreshConfiguration();
        }
    }

    public void snapshotHistory() {
        for (int i = 1; i <= WORKERS_COUNT; i++) {
            workers.get(i).snapshotHistory();
        }
    }

    public void clearHistory() {
        for (int i = 1; i <= WORKERS_COUNT; i++) {
            workers.get(i).clearHistory();
        }
    }

    @SneakyThrows
    public <T> T executeAndWait(FeatureExecutionTask<T> task) {
        Integer workerId = ExecutionQueueSettings.readAsSystemUser().getWorkers().get(task.getFeature());
        if (workerId == 0) {
            return task.getTask().call();
        }
        return workers.get(workerId).executeAndWait(task);
    }

    public Map<String, Map<Feature, List<TimeframeStatisticsEntry>>> getHistory(StatisticsParams statisticsParams) {
        Map<String, Map<Feature, List<TimeframeStatisticsEntry>>> result = new HashMap<>();
        for (int i = 1; i <= WORKERS_COUNT; i++) {
            result.put(String.valueOf(i), workers.get(i).getHistory(statisticsParams));
        }
        return result;
    }
}
