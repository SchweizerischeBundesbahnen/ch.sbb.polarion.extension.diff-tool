package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class FeatureExecutionTaskTest {

    @Test
    void testTaskExecutionCompletesSuccessfully() throws Exception {
        Feature feature = Feature.DIFF_DOCUMENTS;
        String expectedResult = "Success";
        Callable<String> mockTask = mock(Callable.class);
        when(mockTask.call()).thenReturn(expectedResult);

        CountersRegistry mockRegistry = mock(CountersRegistry.class);

        FeatureExecutionTask<String> executionTask = new FeatureExecutionTask<>(feature, mockTask);
        executionTask.setCountersRegistry(mockRegistry);

        String result = executionTask.call();

        assertEquals(expectedResult, result);
        verify(mockRegistry).dequeue(feature);
        verify(mockRegistry).registerExecution(feature);
        verify(mockRegistry).completeExecution(feature);
        verify(mockTask).call();
    }

    @Test
    void testTaskExecutionThrowsException() throws Exception {
        Feature feature = Feature.DIFF_COLLECTIONS;
        Callable<String> mockTask = mock(Callable.class);
        Exception originalException = new RuntimeException("Task failed");
        when(mockTask.call()).thenThrow(originalException);

        CountersRegistry mockRegistry = mock(CountersRegistry.class);

        FeatureExecutionTask<String> executionTask = new FeatureExecutionTask<>(feature, mockTask);
        executionTask.setCountersRegistry(mockRegistry);

        RejectedExecutionException exception = assertThrows(RejectedExecutionException.class, executionTask::call);
        assertEquals("Task failed", exception.getMessage());
        assertEquals(originalException, exception.getCause());

        verify(mockRegistry).dequeue(feature);
        verify(mockRegistry).registerExecution(feature);
        verify(mockRegistry).completeExecution(feature);
    }

    /**
     * The execution counter must be incremented and decremented by the same thread - the one running the
     * task - so it can never report more concurrent executions than the worker has threads.
     * <p>
     * It used to be decremented by the submitting thread in
     * {@link ExecutionWorker#executeAndWait(FeatureExecutionTask)}'s finally block, after
     * {@code future.get()} returned. With the increment happening on the worker thread, nothing ordered
     * the two: a single-threaded worker could start the next task (incrementing to 2) before the
     * previous submitter decremented, and ExecutionQueueMonitor could sample exactly there. That made
     * ExecutionQueueMonitorTest fail intermittently, and would have charted impossible concurrency on
     * the Execution Queue admin page.
     */
    @Test
    void countersReturnToZeroWhenTheTaskCompletes() {
        Feature feature = Feature.DIFF_HTML;
        CountersRegistry registry = new CountersRegistry();
        FeatureExecutionTask<String> executionTask = new FeatureExecutionTask<>(feature, () -> "ok");
        executionTask.setCountersRegistry(registry);
        registry.enqueue(feature);

        executionTask.call();

        assertEquals(0, registry.getExecutingCount(feature));
        assertEquals(0, registry.getQueuedCount(feature));
    }

    @Test
    void countersReturnToZeroWhenTheTaskFails() {
        Feature feature = Feature.DIFF_TEXT;
        CountersRegistry registry = new CountersRegistry();
        FeatureExecutionTask<String> executionTask = new FeatureExecutionTask<>(feature, () -> {
            throw new IllegalStateException("boom");
        });
        executionTask.setCountersRegistry(registry);
        registry.enqueue(feature);

        assertThrows(RejectedExecutionException.class, executionTask::call);

        assertEquals(0, registry.getExecutingCount(feature));
        assertEquals(0, registry.getQueuedCount(feature));
    }

}
