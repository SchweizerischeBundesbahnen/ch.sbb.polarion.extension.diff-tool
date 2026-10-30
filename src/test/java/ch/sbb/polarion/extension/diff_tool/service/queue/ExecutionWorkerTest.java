package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.StatisticsParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.ExecutionQueueModel;
import ch.sbb.polarion.extension.diff_tool.settings.ExecutionQueueSettings;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry.MAX_HISTORY_ENTRIES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class ExecutionWorkerTest {

    private static final int WORKER_ID = 1;
    private static final int OTHER_WORKER_ID = 2;
    private static final int WORKER_THREADS = 2;

    @Test
    void testConfiguredThreadsRunInParallelRightAfterConstruction() {
        ExecutionQueueModel model = new ExecutionQueueModel();
        model.getWorkers().put(Feature.DIFF_HTML, WORKER_ID);
        model.getThreads().put(WORKER_ID, WORKER_THREADS);

        try (MockedStatic<ExecutionQueueSettings> mockStaticSettings = mockStatic(ExecutionQueueSettings.class)) {
            mockStaticSettings.when(ExecutionQueueSettings::readAsSystemUser).thenReturn(model);

            ExecutionWorker worker = new ExecutionWorker(WORKER_ID);
            assertEquals(WORKER_THREADS, worker.getWorkerThreadCount());

            // Each task waits until both tasks have started, which only happens if they run on two threads at once
            CountDownLatch bothStarted = new CountDownLatch(WORKER_THREADS);
            CompletableFuture<Boolean> first = CompletableFuture.supplyAsync(() -> worker.executeAndWait(waitForAll(bothStarted)));
            CompletableFuture<Boolean> second = CompletableFuture.supplyAsync(() -> worker.executeAndWait(waitForAll(bothStarted)));

            assertTrue(first.join());
            assertTrue(second.join());
        }
    }

    @Test
    void testRefreshConfigurationWhileSnapshotting() throws InterruptedException {
        ExecutionQueueModel model = new ExecutionQueueModel();
        Feature.workerFeatures().forEach(feature -> model.getWorkers().put(feature, WORKER_ID));
        model.getThreads().put(WORKER_ID, 1);

        try (MockedStatic<ExecutionQueueSettings> mockStaticSettings = mockStatic(ExecutionQueueSettings.class)) {
            mockStaticSettings.when(ExecutionQueueSettings::readAsSystemUser).thenReturn(model);
            ExecutionWorker worker = new ExecutionWorker(WORKER_ID);

            // The settings are mocked for this thread only, so the refresh runs here and the snapshots in the background
            AtomicInteger refreshes = new AtomicInteger();
            Throwable failure = ConcurrentAccess.run(worker::snapshotHistory, () -> {
                model.getWorkers().put(Feature.DIFF_HTML, refreshes.incrementAndGet() % 2 == 0 ? WORKER_ID : OTHER_WORKER_ID);
                worker.refreshConfiguration();
            });

            assertNull(failure);
        }
    }

    @Test
    void testSnapshotOnlyFeaturesOfThisWorker() {
        ExecutionQueueModel model = new ExecutionQueueModel();
        model.getWorkers().put(Feature.DIFF_HTML, WORKER_ID);
        model.getWorkers().put(Feature.DIFF_TEXT, WORKER_ID);
        model.getThreads().put(WORKER_ID, 1);

        try (MockedStatic<ExecutionQueueSettings> mockStaticSettings = mockStatic(ExecutionQueueSettings.class)) {
            mockStaticSettings.when(ExecutionQueueSettings::readAsSystemUser).thenReturn(model);
            ExecutionWorker worker = new ExecutionWorker(WORKER_ID);

            model.getWorkers().put(Feature.DIFF_TEXT, OTHER_WORKER_ID);
            worker.refreshConfiguration();
            worker.snapshotHistory();

            Map<Feature, List<TimeframeStatisticsEntry>> history = worker.getHistory(new StatisticsParams());
            assertEquals(1, history.get(Feature.DIFF_HTML).size());
            assertTrue(history.get(Feature.DIFF_TEXT).isEmpty());
        }
    }

    @Test
    void testGetHistoryWhileSnapshotting() throws InterruptedException {
        ExecutionQueueModel model = new ExecutionQueueModel();
        model.getWorkers().put(Feature.DIFF_HTML, WORKER_ID);
        model.getThreads().put(WORKER_ID, 1);

        try (MockedStatic<ExecutionQueueSettings> mockStaticSettings = mockStatic(ExecutionQueueSettings.class)) {
            mockStaticSettings.when(ExecutionQueueSettings::readAsSystemUser).thenReturn(model);
            ExecutionWorker worker = new ExecutionWorker(WORKER_ID);
            for (int i = 0; i < MAX_HISTORY_ENTRIES; i++) {
                worker.snapshotHistory();
            }

            // With a start date the filter reads every entry, so an element seen half-written fails it
            String from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1).format(TimeframeStatisticsEntry.DATE_FORMATTER);
            StatisticsParams params = new StatisticsParams(Map.of(String.valueOf(WORKER_ID), from));
            Throwable failure = ConcurrentAccess.run(worker::snapshotHistory, () -> worker.getHistory(params));

            assertNull(failure);
        }
    }

    private static FeatureExecutionTask<Boolean> waitForAll(CountDownLatch latch) {
        return new FeatureExecutionTask<>(Feature.DIFF_HTML, () -> {
            latch.countDown();
            return latch.await(5, TimeUnit.SECONDS);
        });
    }
}
