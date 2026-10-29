package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.ExecutionQueueModel;
import ch.sbb.polarion.extension.diff_tool.settings.ExecutionQueueSettings;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class ExecutionWorkerTest {

    private static final int WORKER_ID = 1;
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

    private static FeatureExecutionTask<Boolean> waitForAll(CountDownLatch latch) {
        return new FeatureExecutionTask<>(Feature.DIFF_HTML, () -> {
            latch.countDown();
            return latch.await(5, TimeUnit.SECONDS);
        });
    }
}
