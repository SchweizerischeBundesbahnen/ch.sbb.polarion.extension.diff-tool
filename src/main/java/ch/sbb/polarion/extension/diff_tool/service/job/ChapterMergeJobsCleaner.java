package ch.sbb.polarion.extension.diff_tool.service.job;

import ch.sbb.polarion.extension.diff_tool.properties.DiffToolExtensionConfiguration;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Drops the results of finished chapter merges from memory, so that a server which merges all day does not keep
 * every merge report it ever produced. A result is read right after its merge is over, so it is kept only as long
 * as {@code chapter.merge.result.timeout} says.
 */
public final class ChapterMergeJobsCleaner {

    private static ScheduledExecutorService executorService;

    private ChapterMergeJobsCleaner() {
    }

    public static synchronized void startCleaningJob() {
        if (executorService != null) {
            return;
        }
        int resultTimeout = DiffToolExtensionConfiguration.getInstance().getChapterMergeResultTimeout();

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(
                () -> ChapterMergeJobsService.cleanupExpiredJobs(resultTimeout),
                resultTimeout,
                resultTimeout,
                TimeUnit.MINUTES);
    }

    public static synchronized void stopCleaningJob() {
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }

    @VisibleForTesting
    static synchronized boolean isRunning() {
        return executorService != null;
    }
}
