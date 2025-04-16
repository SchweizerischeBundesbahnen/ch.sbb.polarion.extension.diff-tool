package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.EndpointCallEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.StatisticsParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry;
import lombok.SneakyThrows;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.sbb.polarion.extension.diff_tool.service.queue.ExecutionQueueMonitor.MAX_ENTRIES;

@SuppressWarnings("unused")
public class ExecutionWorker {

    private static final int MAX_QUEUE_CAPACITY = 1000;

    private final BlockingQueue<Runnable> taskQueue;
    private final ThreadPoolExecutor executor;
    private final CountersRegistry countersRegistry = new CountersRegistry();

    private final Map<Feature, CircularFifoQueue<TimeframeStatisticsEntry>> history = new ConcurrentHashMap<>();
    private final Set<Feature> features = new HashSet<>();
    private final int workerId;

    private volatile boolean running = true;

    public ExecutionWorker(int workerId) {
        this.workerId = workerId;
        this.taskQueue = new LinkedBlockingQueue<>(MAX_QUEUE_CAPACITY);
        clearHistory();
        refreshConfiguration();

        // Create a thread factory that names our worker threads
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r, "QueueWorker-" + counter.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };

        Integer workerThreads = ExecutionQueueMonitor.getSettings().getThreads().get(workerId);
        this.executor = new ThreadPoolExecutor(
                0, workerThreads,
                0L, TimeUnit.MILLISECONDS,
                taskQueue,
                threadFactory
        );
    }

    public void refreshConfiguration() {
        for (Map.Entry<Feature, Integer> entry : ExecutionQueueMonitor.getSettings().getWorkers().entrySet()) {
            if (entry.getValue().equals(workerId)) {
                features.add(entry.getKey());
            } else {
                features.remove(entry.getKey());
            }
        }
        if (executor != null) {
            Integer workerThreads = ExecutionQueueMonitor.getSettings().getThreads().get(workerId);
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

    /**
     * Gets the current queue capacity.
     *
     * @return the maximum queue capacity
     */
    public int getQueueCapacity() {
        return taskQueue.remainingCapacity() + taskQueue.size();
    }

    @SneakyThrows
    public <T> T executeAndWait(FeatureExecutionTask<T> task) {
        task.setCountersRegistry(countersRegistry);

        if (!running) {
            throw new RejectedExecutionException("Service is shutting down");
        }
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
        } catch (InterruptedException | ExecutionException e) {
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

    /**
     * Shuts down this service, waiting for all queued tasks to complete.
     */
    public void shutdown() throws InterruptedException {
        running = false;
        executor.shutdown();

        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
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
