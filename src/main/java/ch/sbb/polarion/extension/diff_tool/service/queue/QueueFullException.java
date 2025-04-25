package ch.sbb.polarion.extension.diff_tool.service.queue;

import java.util.concurrent.RejectedExecutionException;

/**
 * Thrown when the queue is full and a new request cannot be processed.
 */
public class QueueFullException extends RejectedExecutionException {
    public QueueFullException(String message, Throwable cause) {
        super(message, cause);
    }
}
