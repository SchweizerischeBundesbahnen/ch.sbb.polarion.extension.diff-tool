package ch.sbb.polarion.extension.diff_tool.service.job;

import ch.sbb.polarion.extension.diff_tool.properties.DiffToolExtensionConfiguration;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.service.DocumentsChapterMergeService;
import ch.sbb.polarion.extension.diff_tool.service.MergeService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.queue.QueueFullException;
import ch.sbb.polarion.extension.generic.jobs.AsyncJobsService;
import ch.sbb.polarion.extension.generic.jobs.JobsRegistry;
import ch.sbb.polarion.extension.generic.jobs.JobsRegistryShutDownException;
import ch.sbb.polarion.extension.generic.jobs.TimeoutPolicy;
import com.polarion.core.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.concurrent.RejectedExecutionException;

/**
 * Runs chapter merges in the background, because merging a big chapter doesn't fit into one HTTP request.
 * <p>
 * The job mechanics - running as the scheduling user, ending the session it was asked to keep, polling, expiring
 * finished results - are the generic {@link AsyncJobsService}. What is particular to a merge is here:
 * <ul>
 *     <li>the documents cache is evicted in {@link #startJob(ChapterMergeParams, int)} itself, on the thread which
 *     serves the REST request: the cache is keyed by the user of that request, and the servlet container recycles the
 *     request object as soon as the response is written;</li>
 *     <li>a merge is never stopped from the outside ({@link TimeoutPolicy#COOPERATIVE}): it is asked to stop and stops
 *     itself between two work items, so a merge stuck inside a single call to Polarion stays a running job - which is
 *     what it is. This service never tells its caller that nothing was merged while the merge is still writing;</li>
 *     <li>how many merges the server takes at once is bounded, see {@link #CONCURRENT_MERGES}.</li>
 * </ul>
 */
public class ChapterMergeJobsService extends AsyncJobsService<ChapterMergeParams, MergeResult> {

    private static final Logger logger = Logger.getLogger(ChapterMergeJobsService.class);

    /**
     * How many merges run at once, and how many wait for their turn.
     * <p>
     * A merge is a long write operation on two documents, so a server which runs as many of them as it is asked to
     * has nothing left for anything else: a caller can start them faster than they finish, and each one holds a
     * thread for as long as its merge takes. Merges beyond these bounds are refused, not started.
     */
    @VisibleForTesting
    static final int CONCURRENT_MERGES = 2;
    @VisibleForTesting
    static final int QUEUED_MERGES = 10;

    private static final String TOO_MANY_MERGES_MESSAGE = "Too many chapter merges are running or waiting for their turn, please try again later";

    // Static, so that the jobs survive the controller instance which started them. Not final: a registry which is
    // shut down refuses every merge, and a bundle which is stopped and started again keeps its classes
    private static JobsRegistry<ChapterMergeParams, MergeResult> registry = createRegistry();
    private static boolean registryShutDown;

    private final DocumentsChapterMergeService documentsChapterMergeService;
    private final PolarionService polarionService;

    public ChapterMergeJobsService(@NotNull PolarionService polarionService) {
        this(new DocumentsChapterMergeService(polarionService, new MergeService(polarionService)), polarionService);
    }

    public ChapterMergeJobsService(@NotNull DocumentsChapterMergeService documentsChapterMergeService, @NotNull PolarionService polarionService) {
        this(documentsChapterMergeService, polarionService, registry);
    }

    @VisibleForTesting
    ChapterMergeJobsService(@NotNull DocumentsChapterMergeService documentsChapterMergeService, @NotNull PolarionService polarionService,
                            @NotNull JobsRegistry<ChapterMergeParams, MergeResult> registry) {
        super(registry, polarionService.getSecurityService());
        this.documentsChapterMergeService = documentsChapterMergeService;
        this.polarionService = polarionService;
    }

    /**
     * Starts dropping the results of finished chapter merges from memory, so that a server which merges all day does
     * not keep every merge report it ever produced. A result is read right after its merge is over, so it is kept only
     * as long as {@code chapter.merge.result.timeout} says.
     * <p>
     * Called when the bundle starts. If the bundle was stopped before, its registry is shut down and replaced first.
     */
    public static synchronized void startCleaner() {
        if (registryShutDown) {
            registry = createRegistry();
            registryShutDown = false;
        }
        registry.startCleaner(DiffToolExtensionConfiguration.getInstance().getChapterMergeResultTimeout());
    }

    /**
     * Stops the cleaner and the merge threads. Called when the bundle stops.
     */
    public static synchronized void shutdown() {
        registry.shutdown();
        registryShutDown = true;
    }

    /**
     * Starts a chapter merge and returns the ID it is polled by.
     * <p>
     * A caller who asks while {@link #CONCURRENT_MERGES} merges are running and {@link #QUEUED_MERGES} more are
     * waiting is turned away: a merge beyond those bounds never gets a thread of this service. Whether the caller
     * may merge at all is decided before that, by the endpoint which takes the request.
     *
     * @param params           what to merge where
     * @param timeoutInMinutes how long the merge may take, waiting for its turn included. A merge which runs longer
     *                         is asked to stop, and stops between two work items - before it has written anything
     * @throws QueueFullException if there is no room for another merge
     * @throws JobsRegistryShutDownException if the extension is stopping; trying again soon does not help, so it is
     *                                       not a {@link QueueFullException}
     */
    public @NotNull String startJob(@NotNull ChapterMergeParams params, int timeoutInMinutes) throws JobsRegistryShutDownException {
        // Here, on the thread which serves the REST request: the cache is keyed by the user of that request, and
        // the request object is recycled by the servlet container as soon as the response is written.
        polarionService.evictDocumentsCache(params.getSourceDocument(), params.getTargetDocument());

        try {
            return startJob(params, timeoutInMinutes, control -> documentsChapterMergeService.mergeChapter(params, message -> {
                control.reportProgress(message);
                logger.info("Chapter merge job '%s': %s".formatted(control.jobId(), message));
            }, control::isAbortRequested));
        } catch (JobsRegistryShutDownException e) {
            throw e;
        } catch (RejectedExecutionException e) {
            throw new QueueFullException(TOO_MANY_MERGES_MESSAGE, e);
        }
    }

    private static @NotNull JobsRegistry<ChapterMergeParams, MergeResult> createRegistry() {
        return registryBuilder().build();
    }

    /**
     * A merge writes, so it is asked to stop rather than interrupted, and at most {@link #CONCURRENT_MERGES} run while
     * {@link #QUEUED_MERGES} wait.
     */
    @VisibleForTesting
    static @NotNull JobsRegistry.Builder<ChapterMergeParams, MergeResult> registryBuilder() {
        return JobsRegistry.<ChapterMergeParams, MergeResult>builder("Chapter merge")
                .timeoutPolicy(TimeoutPolicy.COOPERATIVE)
                .maxConcurrentJobs(CONCURRENT_MERGES, QUEUED_MERGES);
    }
}
