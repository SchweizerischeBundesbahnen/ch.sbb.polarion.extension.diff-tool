package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.EndpointCallEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.StatisticsParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.ExecutionQueueModel;
import com.polarion.core.util.logging.Logger;
import lombok.SneakyThrows;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static ch.sbb.polarion.extension.diff_tool.service.queue.ExecutionQueueMonitor.MAX_ENTRIES;

public class ExecutionWorker {

    private static final Logger logger = Logger.getLogger(ExecutionWorker.class);
    private static final int DEFAULT_MAX_QUEUE_CAPACITY = 1000;

    private final ThreadPoolExecutor executor;
    private final CountersRegistry countersRegistry = new CountersRegistry();

    private final Map<Feature, CircularFifoQueue<TimeframeStatisticsEntry>> history = new ConcurrentHashMap<>();
    private final Set<Feature> features = new HashSet<>();
    private final int workerId;

    public ExecutionWorker(int workerId) {
        this(workerId, DEFAULT_MAX_QUEUE_CAPACITY);
    }

    @VisibleForTesting
    public ExecutionWorker(int workerId, int queueCapacity) {
        this.workerId = workerId;
        clearHistory();
        refreshConfiguration();

        Integer workerThreads = ExecutionQueueModel.readAsSystemUser().getThreads().get(workerId);
        this.executor = new ThreadPoolExecutor(
                0, workerThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new NamedDaemonThreadFactory("ExecutionThread")
        );
    }

    public void refreshConfiguration() {
        ExecutionQueueModel settings = ExecutionQueueModel.readAsSystemUser();
        for (Map.Entry<Feature, Integer> entry : settings.getWorkers().entrySet()) {
            if (entry.getValue().equals(workerId)) {
                features.add(entry.getKey());
            } else {
                features.remove(entry.getKey());
            }
        }
        if (executor != null) {
            Integer workerThreads = settings.getThreads().get(workerId);
            if (getWorkerThreadCount() != workerThreads) {
                setWorkerThreadCount(workerThreads);
            }
        }
    }

    /**
     * Gets the current number of worker threads.
     *
     * @return the current number of worker threads
     */
    public int getWorkerThreadCount() {
        return executor.getCorePoolSize();
    }

    /**
     * Adjusts the number of worker threads processing the queue.
     *
     * @param newThreadCount the new number of worker threads
     */
    public void setWorkerThreadCount(int newThreadCount) {
        if (newThreadCount < 1) {
            throw new IllegalArgumentException("Worker thread count must be at least 1");
        }

        int currentCoreSize = executor.getCorePoolSize();

        // Update both core and max pool size
        if (newThreadCount > currentCoreSize) {
            executor.setMaximumPoolSize(newThreadCount);
            executor.setCorePoolSize(newThreadCount);
        } else {
            executor.setCorePoolSize(newThreadCount);
            executor.setMaximumPoolSize(newThreadCount);
        }
    }

    public void snapshotHistory() {
        for (Feature feature : features) {
            long queued = getQueuedCount(feature);
            long executing = getExecutingCount(feature);
            history.get(feature).add(new EndpointCallEntry(queued, executing));
        }
    }

    @SneakyThrows
    public <T> T executeAndWait(FeatureExecutionTask<T> task) {
        task.setCountersRegistry(countersRegistry);

        countersRegistry.enqueue(task.getFeature());
        Future<T> future;
        try {
            future = executor.submit(task);
        } catch (RejectedExecutionException e) {
            countersRegistry.dequeue(task.getFeature());
            throw new QueueFullException(e.getMessage(), e);
        }
        try {
            return future.get();
        } catch (InterruptedException e) {
            logger.error("Task execution interrupted: " + e.getMessage(), e);
            throw e;
        } catch (ExecutionException e) {
            throw new RejectedExecutionException("Task execution failed: " + e.getMessage(), e);
        } finally {
            countersRegistry.completeExecution(task.getFeature());
        }
    }

    public long getQueuedCount(Feature feature) {
        return countersRegistry.getQueuedCount(feature);
    }

    public long getExecutingCount(Feature feature) {
        return countersRegistry.getExecutingCount(feature);
    }

    public void clearHistory() {
        for (Feature feature : Feature.values()) {
            history.put(feature, new CircularFifoQueue<>(MAX_ENTRIES));
        }
    }

    public Map<Feature, List<TimeframeStatisticsEntry>> getHistory(StatisticsParams statisticsParams) {
        Map<Feature, List<TimeframeStatisticsEntry>> result = new HashMap<>();
        history.forEach((key, value) -> result.put(key, statisticsParams.filterQueue(String.valueOf(workerId), value)));
        return result;
    }

}
