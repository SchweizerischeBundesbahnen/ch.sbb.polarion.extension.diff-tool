package ch.sbb.polarion.extension.diff_tool.service.job;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.service.DocumentsChapterMergeService;
import ch.sbb.polarion.extension.diff_tool.service.MergeService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.queue.NamedDaemonThreadFactory;
import ch.sbb.polarion.extension.diff_tool.service.queue.QueueFullException;
import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.security.ISecurityService;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Runs chapter merges in the background, because merging a big chapter doesn't fit into one HTTP request.
 * <p>
 * A merge is started on the thread which serves the REST request, runs on a thread of this service, and is polled
 * by the caller until it has a result. Everything which needs the scheduling request is done in
 * {@link #startJob(ChapterMergeParams, int)} itself: the documents cache is keyed by the user of that request, and
 * the servlet container recycles the request object as soon as the response is written.
 * <p>
 * A merge thread carries no subject of its own, and a call of nobody is answered by Polarion with unresolvable
 * objects - a document of an existing project then reads as "Project '...' not found". The merge therefore takes
 * the subject of the scheduling user along and runs as that user.
 * <p>
 * A merge is never stopped from the outside: its thread is not interrupted, and its future is not completed while it
 * still runs. It is asked to stop and stops itself where stopping is safe, so a merge stuck inside a single call to
 * Polarion keeps its thread and stays a running job - which is what it is. What such a merge cannot do is have this
 * service tell its caller that nothing was merged while it is still writing.
 */
public class ChapterMergeJobsService {

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

    // Static, so that the jobs survive the controller instance which started them
    private static final Map<String, JobDetails> jobs = new ConcurrentHashMap<>();
    private static final Map<String, String> failedJobsReasons = new ConcurrentHashMap<>();
    private static final ExecutorService jobExecutor = new ThreadPoolExecutor(
            CONCURRENT_MERGES, CONCURRENT_MERGES,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(QUEUED_MERGES),
            new NamedDaemonThreadFactory("ChapterMergeThread"));
    private static final ScheduledExecutorService deadlineScheduler =
            Executors.newSingleThreadScheduledExecutor(new NamedDaemonThreadFactory("ChapterMergeDeadline"));

    private static final String UNKNOWN_JOB_MESSAGE = "Chapter merge job is unknown: %s";
    private static final String TOO_MANY_MERGES_MESSAGE = "Too many chapter merges are running or waiting for their turn, please try again later";

    private final DocumentsChapterMergeService documentsChapterMergeService;
    private final PolarionService polarionService;
    private final ISecurityService securityService;

    public ChapterMergeJobsService(@NotNull PolarionService polarionService) {
        this(new DocumentsChapterMergeService(polarionService, new MergeService(polarionService)), polarionService);
    }

    public ChapterMergeJobsService(@NotNull DocumentsChapterMergeService documentsChapterMergeService, @NotNull PolarionService polarionService) {
        this.documentsChapterMergeService = documentsChapterMergeService;
        this.polarionService = polarionService;
        this.securityService = polarionService.getSecurityService();
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
     *                         is asked to stop, and stops between two work items - before it has written anything.
     *                         It is not declared over while its thread is still working: a caller told that nothing
     *                         was merged, of a merge which is about to change their document, would be told wrong
     * @throws QueueFullException if there is no room for another merge
     */
    public @NotNull String startJob(@NotNull ChapterMergeParams params, int timeoutInMinutes) {
        // Here, on the thread which serves the REST request: the cache is keyed by the user of that request, and
        // the request object is recycled by the servlet container as soon as the response is written.
        polarionService.evictDocumentsCache(params.getSourceDocument(), params.getTargetDocument());

        String jobId = UUID.randomUUID().toString();
        // The subject of the thread this runs on, not the one in the request: the request carries a subject
        // (`AuthenticationFilter.USER_SUBJECT`) only where `AuthenticationFilter` runs, and that filter is bound to
        // `@Secured` - to the /api controller, not to the /internal one the Document Properties panel calls. On
        // /api the two are the same object anyway: `callPrivileged` hands that very subject to
        // `ISecurityService.doAsUser`, which installs it with `Subject.doAs`, and `getCurrentSubject` reads it back
        // out of the access control context. So this is also the subject `LogoutFilter` was asked to keep alive.
        Subject userSubject = polarionService.getCurrentSubject();
        boolean logoutRequired = isJobLogoutRequired();
        AtomicReference<String> progressMessage = new AtomicReference<>();
        AtomicReference<Instant> finishTime = new AtomicReference<>();
        AtomicBoolean abortRequested = new AtomicBoolean();

        CompletableFuture<MergeResult> asyncJob;
        try {
            asyncJob = CompletableFuture.supplyAsync(() -> {
                try {
                    return runAsSchedulingUser(userSubject, () -> documentsChapterMergeService.mergeChapter(params, message -> {
                        progressMessage.set(message);
                        logger.info("Chapter merge job '%s': %s".formatted(jobId, message));
                    }, abortRequested::get));
                } catch (Exception e) {
                    String failedReason = abortRequested.get() ? timeoutMessage(timeoutInMinutes) : describeFailure(e);
                    logger.error("Chapter merge job '%s' failed with error: %s".formatted(jobId, failedReason), e);
                    failedJobsReasons.put(jobId, failedReason);
                    throw e;
                } finally {
                    if (userSubject != null && logoutRequired) {
                        securityService.logout(userSubject);
                    }
                }
            }, jobExecutor);
        } catch (RejectedExecutionException e) {
            logger.error(TOO_MANY_MERGES_MESSAGE, e);
            throw new QueueFullException(TOO_MANY_MERGES_MESSAGE, e);
        }

        // The deadline runs from here, so it covers the wait for a free merge thread as well as the merge itself:
        // a merge which sat in the queue for as long as it was given is of no use to the caller who asked for it.
        // It asks the merge to stop rather than declaring it over: a merge declared over while its thread keeps
        // writing would have this service report a failure to a caller whose document is about to change anyway.
        ScheduledFuture<?> deadline = deadlineScheduler.schedule(
                () -> requestAbort(jobId, abortRequested, timeoutInMinutes), timeoutInMinutes, TimeUnit.MINUTES);

        asyncJob.whenComplete((mergeResult, thrown) -> {
            // when the merge was over, whichever way it ended: its result is kept from that moment, not from the
            // moment it was asked for
            finishTime.set(Instant.now());
            deadline.cancel(false);
        });

        jobs.put(jobId, JobDetails.builder()
                .future(asyncJob)
                .user(polarionService.getCurrentUser())
                .startingTime(Instant.now())
                .finishTime(finishTime)
                .progressMessage(progressMessage)
                .build());
        return jobId;
    }

    /**
     * Where a merge stands. A merge which is still running is a perfectly normal answer here - it is what the
     * caller of a merge polls to learn that it has finished.
     */
    public @NotNull JobState getJobState(@NotNull String jobId) {
        return jobState(jobId, getJobDetails(jobId));
    }

    private static @NotNull JobState jobState(@NotNull String jobId, @NotNull JobDetails jobDetails) {
        return JobState.builder()
                .isDone(jobDetails.future().isDone())
                .isFailed(jobDetails.future().isCompletedExceptionally())
                .progressMessage(jobDetails.progressMessage().get())
                .errorMessage(failedJobsReasons.get(jobId))
                .build();
    }

    /**
     * The result of a merge, once it has one. A merge which did not do what was asked of it reports that in its
     * result too, so an unsuccessful {@link MergeResult} is a result like any other.
     *
     * @return empty while the merge is still running
     * @throws IllegalStateException if the merge failed and has no result of its own
     */
    public @NotNull Optional<MergeResult> getJobResult(@NotNull String jobId) {
        CompletableFuture<MergeResult> future = getJobDetails(jobId).future();
        if (!future.isDone()) {
            return Optional.empty();
        }
        if (future.isCancelled() || future.isCompletedExceptionally()) {
            throw new IllegalStateException("Chapter merge job failed: " + failedJobsReasons.get(jobId));
        }
        try {
            return Optional.of(future.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cannot extract result of chapter merge job " + jobId + ": " + e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Cannot extract result of chapter merge job " + jobId + ": " + e.getMessage(), e);
        }
    }

    /**
     * The chapter merge jobs of the current user. A merge names the documents it works on and reports which work
     * items it created, so a job belongs to the user who scheduled it.
     */
    public @NotNull Map<String, JobState> getAllJobsStates() {
        return jobs.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue().user(), polarionService.getCurrentUser()))
                // Built from the entry this listing already holds, not looked up by its key again: a merge whose
                // result the cleaner drops meanwhile is unknown to this service from that moment, and looking it
                // up would answer the whole listing with the 404 of that one job.
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> jobState(entry.getKey(), entry.getValue())));
    }

    /**
     * Drops the jobs which have been finished for longer than the given timeout. A result is read right after its
     * merge is over, so nothing is kept for the sake of keeping it.
     */
    public static void cleanupExpiredJobs(int timeoutInMinutes) {
        Instant currentTime = Instant.now();
        jobs.entrySet().stream()
                .filter(entry -> expired(entry.getValue(), timeoutInMinutes, currentTime))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(ChapterMergeJobsService::removeJob);
    }

    /**
     * Whether a merge has been over for longer than its result is kept.
     * <p>
     * Counted from the moment the merge was over, not from the moment it was asked for: a merge which took longer
     * than its result is kept would otherwise be expired before its caller could read it, which is exactly the
     * merge whose result took the longest to produce.
     */
    @VisibleForTesting
    static boolean expired(@NotNull JobDetails jobDetails, int timeoutInMinutes, @NotNull Instant currentTime) {
        if (!jobDetails.future().isDone()) {
            return false;
        }
        // a merge which is done but whose completion has not been recorded yet has just this moment finished
        Instant finishTime = jobDetails.finishTime().get();
        return finishTime != null && finishTime.plus(timeoutInMinutes, ChronoUnit.MINUTES).isBefore(currentTime);
    }

    private static void removeJob(@NotNull String jobId) {
        jobs.remove(jobId);
        failedJobsReasons.remove(jobId);
    }

    @VisibleForTesting
    void cancelJobsAndCleanMap() {
        jobs.values().forEach(jobDetails -> jobDetails.future().cancel(true));
        jobs.clear();
        failedJobsReasons.clear();
    }

    /**
     * Runs the merge as the user who scheduled it. A merge scheduled by a call which had no subject runs as it is,
     * the way it would have run had it not been handed to a background thread at all.
     */
    private <T> T runAsSchedulingUser(@Nullable Subject userSubject, @NotNull Supplier<T> action) {
        if (userSubject == null) {
            return action.get();
        }
        return securityService.doAsUser(userSubject, (PrivilegedAction<T>) action::get);
    }

    /**
     * Asks a merge which has run out of time to stop. It stops between two work items, which is before it has put
     * anything into the target document - and a merge which is already past its last work item is left to finish
     * the write it is about to make, since that is the result its caller asked for.
     */
    private static void requestAbort(@NotNull String jobId, @NotNull AtomicBoolean abortRequested, int timeoutInMinutes) {
        logger.warn("Chapter merge job '%s' has run for %d min and is asked to stop".formatted(jobId, timeoutInMinutes));
        abortRequested.set(true);
    }

    private static @NotNull String timeoutMessage(int timeoutInMinutes) {
        return "Timeout after %d min".formatted(timeoutInMinutes);
    }

    /**
     * Whether this merge has to end the session of its user when it is over.
     * <p>
     * A call authenticated by an XSRF token shares the session of the Polarion UI it was made from, which is not
     * this merge's to end. A call which authenticated itself gets a session of its own, which {@link LogoutFilter}
     * would end with the response of the scheduling request - long before the merge has finished - unless that
     * request asked for it to be kept. It is then this merge which ends it.
     */
    private boolean isJobLogoutRequired() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return false;
        }
        if (requestAttributes.getAttribute(LogoutFilter.XSRF_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST) == Boolean.TRUE) {
            return false;
        }
        return requestAttributes.getAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST) == Boolean.TRUE;
    }

    private @NotNull JobDetails getJobDetails(@NotNull String jobId) {
        JobDetails jobDetails = jobs.get(jobId);
        if (jobDetails == null || jobDetails.user() == null || !Objects.equals(jobDetails.user(), polarionService.getCurrentUser())) {
            throw new NoSuchElementException(UNKNOWN_JOB_MESSAGE.formatted(jobId));
        }
        return jobDetails;
    }

    /**
     * @return what the caller of the merge is shown: the message of the failure itself, or its class where it
     * carries no message, since an empty reason tells the reader nothing
     */
    @VisibleForTesting
    static @NotNull String describeFailure(@NotNull Throwable thrown) {
        Throwable reason = rootReason(thrown);
        String message = StringUtils.getEmptyIfNull(reason.getMessage());
        return message.isBlank() ? reason.getClass().getName() : message;
    }

    /**
     * @return the failure worth showing: a future wraps what was thrown, and the wrapper says only which class it
     * was
     */
    @VisibleForTesting
    static @NotNull Throwable rootReason(@NotNull Throwable thrown) {
        Throwable reason = thrown;
        while ((reason instanceof CompletionException || reason instanceof ExecutionException) && reason.getCause() != null) {
            reason = reason.getCause();
        }
        return reason;
    }

    /**
     * A merge this service runs: the user it was scheduled for, the merge itself, what it is doing right now, and
     * when it was over - which is when its result starts to age.
     */
    @Builder
    public record JobDetails(
            @NotNull CompletableFuture<MergeResult> future,
            @Nullable String user,
            @NotNull Instant startingTime,
            @NotNull AtomicReference<Instant> finishTime,
            @NotNull AtomicReference<String> progressMessage) {
    }

    /**
     * Where a merge stands, in the terms its caller polls for.
     */
    @Builder
    public record JobState(
            boolean isDone,
            boolean isFailed,
            @Nullable String progressMessage,
            @Nullable String errorMessage) {
    }
}
