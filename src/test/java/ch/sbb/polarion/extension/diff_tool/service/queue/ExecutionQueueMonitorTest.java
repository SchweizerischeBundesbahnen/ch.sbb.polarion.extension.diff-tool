package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.CpuLoadEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.EndpointCallEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.StatisticsParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.ExecutionQueueModel;
import ch.sbb.polarion.extension.diff_tool.settings.ExecutionQueueSettings;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class ExecutionQueueMonitorTest {

    @Test
    void testExecutionStatistics() {

        ExecutionQueueModel model = new ExecutionQueueModel();
        Feature.workerFeatures().forEach(feature -> model.getWorkers().put(feature, 1));
        for (int i = 1; i <= ExecutionQueueService.WORKERS_COUNT; i++) {
            model.getThreads().put(i, 1);
        }

        try (MockedStatic<ExecutionQueueSettings> mockStaticSettings = mockStatic(ExecutionQueueSettings.class)) {
            mockStaticSettings.when(ExecutionQueueSettings::readAsSystemUser).thenReturn(model);

            ExecutionQueueService executionService = new ExecutionQueueService();
            ExecutionQueueMonitor monitor = new ExecutionQueueMonitor(executionService);

            // emulate 2 endpoint calls - they must run sequentially because worker 1 has only 1 thread
            Executors.defaultThreadFactory().newThread(() -> runTestEndpointCall(executionService, model)).start();
            Executors.defaultThreadFactory().newThread(() -> runTestEndpointCall(executionService, model)).start();

            await().pollDelay(2500, TimeUnit.MILLISECONDS).until(() -> true);

            Map<String, Map<Feature, List<TimeframeStatisticsEntry>>> recentHistory = monitor.getHistory(new StatisticsParams());

            List<EndpointCallEntry> diffEntries = recentHistory.get("1").get(Feature.DIFF_HTML).stream().map(s -> (EndpointCallEntry) s).toList();
            assertTrue(diffEntries.stream().anyMatch(e -> e.getQueued() == 1));
            assertTrue(diffEntries.stream().anyMatch(e -> e.getExecuting() == 1));
            assertTrue(diffEntries.stream().noneMatch(e -> e.getExecuting() == 2));

            // also, there must be at least 2 CPU Load records logged
            List<TimeframeStatisticsEntry> cpuLoadHistory = recentHistory.get("COMMON").get(Feature.CPU_LOAD);
            assertTrue(cpuLoadHistory.size() > 1);
            // experimental, probably there are some cases when all of them will be 0
            assertTrue(cpuLoadHistory.stream().anyMatch(e -> ((CpuLoadEntry) e).getValue() > 0));

            model.getWorkers().put(Feature.DIFF_HTML, 2);
            model.getThreads().put(2, 2);
            monitor.refreshConfiguration();
            monitor.clearHistory();

            // emulate again but this time with 2 threads worker - they must be run simultaneously
            Executors.defaultThreadFactory().newThread(() -> runTestEndpointCall(executionService, model)).start();
            Executors.defaultThreadFactory().newThread(() -> runTestEndpointCall(executionService, model)).start();

            await().pollDelay(1500, TimeUnit.MILLISECONDS).until(() -> true);

            recentHistory = monitor.getHistory(new StatisticsParams());
            diffEntries = recentHistory.get("2").get(Feature.DIFF_HTML).stream().map(s -> (EndpointCallEntry) s).toList();
            assertTrue(diffEntries.stream().anyMatch(e -> e.getExecuting() == 2));
            assertTrue(diffEntries.stream().noneMatch(e -> e.getQueued() > 0));
        }
    }

    private void runTestEndpointCall(ExecutionQueueService executionService, ExecutionQueueModel model) {
        try (MockedStatic<ExecutionQueueSettings> mockStaticSettings = mockStatic(ExecutionQueueSettings.class)) {
            mockStaticSettings.when(ExecutionQueueSettings::readAsSystemUser).thenReturn(model);
            executionService.executeAndWait(new FeatureExecutionTask<>(Feature.DIFF_HTML, () -> {
                await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);
                return true;
            }));
        }
    }

    @Test
    void testCloseNormalShutdown() {
        ExecutionQueueModel model = new ExecutionQueueModel();
        Feature.workerFeatures().forEach(feature -> model.getWorkers().put(feature, 1));
        for (int i = 1; i <= ExecutionQueueService.WORKERS_COUNT; i++) {
            model.getThreads().put(i, 1);
        }

        try (MockedStatic<ExecutionQueueSettings> mockStaticSettings = mockStatic(ExecutionQueueSettings.class)) {
            mockStaticSettings.when(ExecutionQueueSettings::readAsSystemUser).thenReturn(model);

            ExecutionQueueService executionService = new ExecutionQueueService();
            ExecutionQueueMonitor monitor = new ExecutionQueueMonitor(executionService);

            // Let it run for a bit
            await().pollDelay(100, TimeUnit.MILLISECONDS).until(() -> true);

            // Close should complete without throwing
            assertDoesNotThrow(monitor::close);
        }
    }

    @Test
    void testCloseWithInterruption() throws InterruptedException {
        ExecutionQueueModel model = new ExecutionQueueModel();
        Feature.workerFeatures().forEach(feature -> model.getWorkers().put(feature, 1));
        for (int i = 1; i <= ExecutionQueueService.WORKERS_COUNT; i++) {
            model.getThreads().put(i, 1);
        }

        try (MockedStatic<ExecutionQueueSettings> mockStaticSettings = mockStatic(ExecutionQueueSettings.class)) {
            mockStaticSettings.when(ExecutionQueueSettings::readAsSystemUser).thenReturn(model);

            ExecutionQueueService executionService = new ExecutionQueueService();
            ExecutionQueueMonitor monitor = new ExecutionQueueMonitor(executionService);

            CountDownLatch closeLatch = new CountDownLatch(1);
            AtomicBoolean wasInterrupted = new AtomicBoolean(false);

            Thread closeThread = new Thread(() -> {
                closeLatch.countDown();
                monitor.close();
                wasInterrupted.set(Thread.currentThread().isInterrupted());
            });

            closeThread.start();
            closeLatch.await();

            // Interrupt the thread while it's in awaitTermination
            closeThread.interrupt();
            closeThread.join(1000);

            // Thread should have handled the interruption and restored the interrupt flag
            assertTrue(wasInterrupted.get(), "Thread interrupt flag should be restored after InterruptedException");
        }
    }

}
