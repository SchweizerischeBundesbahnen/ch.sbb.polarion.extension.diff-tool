package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.EndpointCallEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.StatisticsParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.ExecutionQueueModel;
import ch.sbb.polarion.extension.diff_tool.settings.ExecutionQueueSettings;
import ch.sbb.polarion.extension.generic.util.NamedDaemonThreadFactory;
import com.polarion.core.util.logging.Logger;
import lombok.SneakyThrows;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.EnumMap;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ExecutionWorker {

    private static final Logger logger = Logger.getLogger(ExecutionWorker.class);
    /**
     * Public so the Execution Queue admin page can state the real limit instead of repeating it as
     * literal help text, which is how it silently drifted before.
     */
    public static final int DEFAULT_MAX_QUEUE_CAPACITY = 1000;

    private final ThreadPoolExecutor executor;
    private final CountersRegistry countersRegistry = new CountersRegistry();

    private final Map<Feature, StatisticsHistory<TimeframeStatisticsEntry>> history = new ConcurrentHashMap<>();
    // Holds an immutable set, replaced as a whole: the monitor thread iterates it while a settings save refreshes it
    private final AtomicReference<Set<Feature>> features = new AtomicReference<>(Set.of());
    private final int workerId;

    public ExecutionWorker(int workerId) {
        this(workerId, DEFAULT_MAX_QUEUE_CAPACITY);
    }

    @VisibleForTesting
    public ExecutionWorker(int workerId, int queueCapacity) {
        this.workerId = workerId;
        clearHistory();
        refreshConfiguration();

        Integer workerThreads = ExecutionQueueSettings.readAsSystemUser().getThreads().get(workerId);
        // Core size must equal the thread count: with fewer core threads, tasks queue up instead of starting new threads
        this.executor = new ThreadPoolExecutor(
                workerThreads, workerThreads,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new NamedDaemonThreadFactory("ExecutionThread")
        );
        this.executor.allowCoreThreadTimeOut(true);
    }

    public void refreshConfiguration() {
        ExecutionQueueModel settings = ExecutionQueueSettings.readAsSystemUser();
        features.set(settings.getWorkers().entrySet().stream()
                .filter(entry -> entry.getValue().equals(workerId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet()));
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
        for (Feature feature : features.get()) {
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
        }
        // No completeExecution here: the task decrements its own execution counter in
        // FeatureExecutionTask.call()'s finally block. Doing it from this thread was both racy (see the
        // comment there) and wrong when the task never ran at all, in which case it decremented a
        // counter that had never been incremented.
    }

    public long getQueuedCount(Feature feature) {
        return countersRegistry.getQueuedCount(feature);
    }

    public long getExecutingCount(Feature feature) {
        return countersRegistry.getExecutingCount(feature);
    }

    public void clearHistory() {
        for (Feature feature : Feature.values()) {
            history.put(feature, new StatisticsHistory<>());
        }
    }

    public Map<Feature, List<TimeframeStatisticsEntry>> getHistory(StatisticsParams statisticsParams) {
        Map<Feature, List<TimeframeStatisticsEntry>> result = new EnumMap<>(Feature.class);
        history.forEach((key, value) -> result.put(key, statisticsParams.filterQueue(String.valueOf(workerId), value.snapshot())));
        return result;
    }

}
