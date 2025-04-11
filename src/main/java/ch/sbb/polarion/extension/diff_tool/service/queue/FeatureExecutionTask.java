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
    public T call() throws Exception {
        countersRegistry.dequeue(feature);
        countersRegistry.registerExecution(feature);

        Thread.sleep(2000);
        RequestContextHolder.setRequestAttributes(requestAttributes);
        try {
            return task.call();
        } catch (Exception e) {
            logger.error("Execution failed", e);
            throw new RejectedExecutionException(e.getMessage(), e);
        } finally {
            countersRegistry.completeExecution(feature);
        }
    }

}
