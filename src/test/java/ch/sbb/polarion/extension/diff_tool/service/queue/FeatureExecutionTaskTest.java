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
    }

}
