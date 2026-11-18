package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import com.polarion.platform.security.ISecurityService;
import org.junit.jupiter.api.Test;

import javax.security.auth.Subject;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        ISecurityService securityService = mock(ISecurityService.class);

        FeatureExecutionTask<String> executionTask = new FeatureExecutionTask<>(feature, mockTask, securityService);
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
        ISecurityService securityService = mock(ISecurityService.class);

        FeatureExecutionTask<String> executionTask = new FeatureExecutionTask<>(feature, mockTask, securityService);
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
        ISecurityService securityService = mock(ISecurityService.class);
        FeatureExecutionTask<String> executionTask = new FeatureExecutionTask<>(feature, () -> "ok", securityService);
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
        ISecurityService securityService = mock(ISecurityService.class);
        FeatureExecutionTask<String> executionTask = new FeatureExecutionTask<>(feature, () -> {
            throw new IllegalStateException("boom");
        }, securityService);
        executionTask.setCountersRegistry(registry);
        registry.enqueue(feature);

        assertThrows(RejectedExecutionException.class, executionTask::call);

        assertEquals(0, registry.getExecutingCount(feature));
        assertEquals(0, registry.getQueuedCount(feature));
    }

    /**
     * A worker thread carries no subject of its own. The task must run as the user who scheduled it, or
     * Polarion answers its calls with unresolvable objects ("Project '...' not found").
     */
    @Test
    @SuppressWarnings("unchecked")
    void runsAsTheUserWhoScheduledIt() throws Exception {
        Feature feature = Feature.DIFF_DOCUMENTS;
        Subject scheduler = new Subject();
        ISecurityService securityService = mock(ISecurityService.class);
        CountersRegistry countersRegistry = mock(CountersRegistry.class);
        when(securityService.getCurrentSubject()).thenReturn(scheduler);
        when(securityService.doAsUser(eq(scheduler), any(PrivilegedExceptionAction.class)))
                .thenAnswer(invocation -> ((PrivilegedExceptionAction<String>) invocation.getArgument(1)).run());

        FeatureExecutionTask<String> executionTask = new FeatureExecutionTask<>(feature, () -> "diff", securityService);
        executionTask.setCountersRegistry(countersRegistry);
        when(securityService.getCurrentSubject()).thenReturn(null);

        assertEquals("diff", executionTask.call());
        verify(securityService).doAsUser(eq(scheduler), any(PrivilegedExceptionAction.class));
    }

    @Test
    void runsAsItIsWhenScheduledWithoutSubject() throws Exception {
        ISecurityService securityService = mock(ISecurityService.class);
        CountersRegistry countersRegistry = mock(CountersRegistry.class);
        FeatureExecutionTask<String> executionTask = new FeatureExecutionTask<>(Feature.DIFF_HTML, () -> "ok", securityService);
        executionTask.setCountersRegistry(countersRegistry);

        assertEquals("ok", executionTask.call());
        verify(securityService, never()).doAsUser(any(), any(PrivilegedExceptionAction.class));
    }

    @Test
    void unwrapsFailureOfTaskRunAsUser() throws Exception {
        Subject scheduler = new Subject();
        Exception originalException = new IllegalStateException("not found");
        ISecurityService securityService = mock(ISecurityService.class);
        CountersRegistry countersRegistry = mock(CountersRegistry.class);
        when(securityService.getCurrentSubject()).thenReturn(scheduler);
        when(securityService.doAsUser(eq(scheduler), any(PrivilegedExceptionAction.class)))
                .thenThrow(new PrivilegedActionException(originalException));
        FeatureExecutionTask<String> executionTask = new FeatureExecutionTask<>(Feature.DIFF_DOCUMENTS, () -> "never", securityService);
        executionTask.setCountersRegistry(countersRegistry);

        RejectedExecutionException exception = assertThrows(RejectedExecutionException.class, executionTask::call);
        assertEquals(originalException, exception.getCause());
    }

}
