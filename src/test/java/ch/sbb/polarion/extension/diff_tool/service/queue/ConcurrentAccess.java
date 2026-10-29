package ch.sbb.polarion.extension.diff_tool.service.queue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs one action in a loop on a background thread while the calling thread repeats another one, to expose races.
 */
final class ConcurrentAccess {

    static final int ITERATIONS = 20_000;

    private ConcurrentAccess() {
    }

    /**
     * Returns the first exception thrown by either action, or {@code null} if there was none.
     */
    static Throwable run(Runnable background, Runnable foreground) throws InterruptedException {
        AtomicBoolean stop = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                while (!stop.get()) {
                    background.run();
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        });
        thread.start();
        try {
            for (int i = 0; i < ITERATIONS && failure.get() == null; i++) {
                foreground.run();
            }
        } catch (Throwable t) {
            failure.compareAndSet(null, t);
        } finally {
            stop.set(true);
            thread.join();
        }
        return failure.get();
    }
}
