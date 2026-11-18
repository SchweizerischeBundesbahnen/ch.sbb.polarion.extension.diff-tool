package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.security.ISecurityService;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.security.auth.Subject;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;

@Getter
public class FeatureExecutionTask<T> implements Callable<T> {

    private static final Logger logger = Logger.getLogger(FeatureExecutionTask.class);
    private final Feature feature;
    private final Callable<T> task;
    private final RequestAttributes requestAttributes;
    private final ISecurityService securityService;
    /**
     * Subject of the user who scheduled the task. A worker thread carries no subject of its own, and Polarion
     * answers a call of nobody with unresolvable objects: an existing project then reads as "Project '...' not found".
     */
    private final @Nullable Subject subject;
    @Setter
    private CountersRegistry countersRegistry;

    public FeatureExecutionTask(Feature feature, Callable<T> task, @NotNull ISecurityService securityService) {
        this.feature = feature;
        this.task = task;
        this.requestAttributes = RequestContextHolder.getRequestAttributes();
        this.securityService = securityService;
        this.subject = securityService.getCurrentSubject();
    }

    @Override
    public T call() {
        countersRegistry.dequeue(feature);
        countersRegistry.registerExecution(feature);

        RequestContextHolder.setRequestAttributes(requestAttributes);
        try {
            return callAsScheduler();
        } catch (Exception e) {
            logger.error("Execution failed", e);
            throw new RejectedExecutionException(e.getMessage(), e);
        } finally {
            // Decremented here, on the thread that incremented it, so the counter is symmetric and can
            // never exceed the worker's thread count. It used to be decremented by the submitting thread
            // in ExecutionWorker.executeAndWait's finally block, after future.get() returned: with the
            // increment happening on the worker thread there was no ordering between the two, so the
            // pool could already have started the next task - and incremented - before the previous
            // submitter got around to decrementing. The monitor could then sample two concurrent
            // executions on a single-threaded worker, which is what the admin page charts.
            countersRegistry.completeExecution(feature);
        }
    }

    /**
     * Runs the task as the user who scheduled it. A task scheduled without a subject runs as it is, the way it
     * would run on the request thread.
     */
    private T callAsScheduler() throws Exception {
        if (subject == null) {
            return task.call();
        }
        try {
            return securityService.doAsUser(subject, (PrivilegedExceptionAction<T>) task::call);
        } catch (PrivilegedActionException e) {
            throw e.getException();
        }
    }

}
