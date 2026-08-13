package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import com.polarion.core.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;

@Getter
public class FeatureExecutionTask<T> implements Callable<T> {

    private static final Logger logger = Logger.getLogger(FeatureExecutionTask.class);
    private final Feature feature;
    private final Callable<T> task;
    private final RequestAttributes requestAttributes;
    @Setter
    private CountersRegistry countersRegistry;

    public FeatureExecutionTask(Feature feature, Callable<T> task) {
        this.feature = feature;
        this.task = task;
        this.requestAttributes = RequestContextHolder.getRequestAttributes();
    }

    @Override
    public T call() {
        countersRegistry.dequeue(feature);
        countersRegistry.registerExecution(feature);

        RequestContextHolder.setRequestAttributes(requestAttributes);
        try {
            return task.call();
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

}
