package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.CpuLoadEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.EndpointCallEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.StatisticsParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.ExecutionQueueModel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
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

        try (MockedStatic<ExecutionQueueModel> mockStaticModel = mockStatic(ExecutionQueueModel.class)) {
            mockStaticModel.when(ExecutionQueueModel::readAsSystemUser).thenReturn(model);

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
        try (MockedStatic<ExecutionQueueModel> mockStaticModel = mockStatic(ExecutionQueueModel.class)) {
            mockStaticModel.when(ExecutionQueueModel::readAsSystemUser).thenReturn(model);
            executionService.executeAndWait(new FeatureExecutionTask<>(Feature.DIFF_HTML, () -> {
                await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);
                System.out.println("noop");
                return true;
            }));
        }
    }

}
