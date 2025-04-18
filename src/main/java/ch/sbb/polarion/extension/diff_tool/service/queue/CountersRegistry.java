package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CountersRegistry {
    private static final AtomicInteger ZERO = new AtomicInteger(0);
    private final ConcurrentHashMap<Feature, AtomicInteger> executingFeatureRegistry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Feature, AtomicInteger> queuedFeatureRegistry = new ConcurrentHashMap<>();

    public void registerExecution(Feature feature) {
        executingFeatureRegistry.computeIfAbsent(feature, k -> new AtomicInteger()).incrementAndGet();
    }

    public void completeExecution(Feature feature) {
        executingFeatureRegistry.computeIfPresent(feature, (k, v) -> {
            v.decrementAndGet();
            return v;
        });
    }

    public void enqueue(Feature feature) {
        queuedFeatureRegistry.computeIfAbsent(feature, k -> new AtomicInteger()).incrementAndGet();
    }

    public void dequeue(Feature feature) {
        queuedFeatureRegistry.computeIfPresent(feature, (k, v) -> {
            v.decrementAndGet();
            return v;
        });
    }

    public long getExecutingCount(Feature feature) {
        return executingFeatureRegistry.getOrDefault(feature, ZERO).longValue();
    }

    public long getQueuedCount(Feature feature) {
        return queuedFeatureRegistry.getOrDefault(feature, ZERO).longValue();
    }
}
