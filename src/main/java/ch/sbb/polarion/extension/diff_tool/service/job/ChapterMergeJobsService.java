package ch.sbb.polarion.extension.diff_tool.service.job;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.service.DocumentsChapterMergeService;
import ch.sbb.polarion.extension.diff_tool.service.MergeService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
 */
public class ChapterMergeJobsService {

    private static final Logger logger = Logger.getLogger(ChapterMergeJobsService.class);

    // Static, so that the jobs survive the controller instance which started them
    private static final Map<String, JobDetails> jobs = new ConcurrentHashMap<>();
    private static final Map<String, String> failedJobsReasons = new ConcurrentHashMap<>();
    private static final ExecutorService jobExecutor = Executors.newCachedThreadPool();

    private static final String UNKNOWN_JOB_MESSAGE = "Chapter merge job is unknown: %s";

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
     *
     * @param params           what to merge where
     * @param timeoutInMinutes how long the merge may run before it is given up on
     */
    public @NotNull String startJob(@NotNull ChapterMergeParams params, int timeoutInMinutes) {
        // Here, on the thread which serves the REST request: the cache is keyed by the user of that request, and
        // the request object is recycled by the servlet container as soon as the response is written.
        polarionService.evictDocumentsCache(params.getSourceDocument(), params.getTargetDocument());

        String jobId = UUID.randomUUID().toString();
        Subject userSubject = polarionService.getCurrentSubject();
        boolean logoutRequired = isJobLogoutRequired();
        AtomicReference<String> progressMessage = new AtomicReference<>();

        CompletableFuture<MergeResult> asyncJob = CompletableFuture.supplyAsync(() -> {
            try {
                return runAsSchedulingUser(userSubject, () -> documentsChapterMergeService.mergeChapter(params, message -> {
                    progressMessage.set(message);
                    logger.info("Chapter merge job '%s': %s".formatted(jobId, message));
                }));
            } catch (Exception e) {
                logger.error("Chapter merge job '%s' failed with error: %s".formatted(jobId, e.getMessage()), e);
                // only if no reason is set yet, so that a timeout keeps the reason it named
                failedJobsReasons.putIfAbsent(jobId, StringUtils.getEmptyIfNull(e.getMessage()));
                throw e;
            } finally {
                if (userSubject != null && logoutRequired) {
                    securityService.logout(userSubject);
                }
            }
        }, jobExecutor);

        asyncJob
                .orTimeout(timeoutInMinutes, TimeUnit.MINUTES)
                .exceptionally(e -> handleJobFailure(jobId, e, timeoutInMinutes, asyncJob));

        jobs.put(jobId, JobDetails.builder()
                .future(asyncJob)
                .user(polarionService.getCurrentUser())
                .startingTime(Instant.now())
                .progressMessage(progressMessage)
                .build());
        return jobId;
    }

    /**
     * Where a merge stands. A merge which is still running is a perfectly normal answer here - it is what the
     * caller of a merge polls to learn that it has finished.
     */
    public @NotNull JobState getJobState(@NotNull String jobId) {
        JobDetails jobDetails = getJobDetails(jobId);
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
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getJobState(entry.getKey())));
    }

    /**
     * Drops the jobs which have been finished for longer than the given timeout. A result is read right after its
     * merge is over, so nothing is kept for the sake of keeping it.
     */
    public static void cleanupExpiredJobs(int timeoutInMinutes) {
        Instant currentTime = Instant.now();
        jobs.entrySet().stream()
                .filter(entry -> entry.getValue().future().isDone()
                        && entry.getValue().startingTime().plus(timeoutInMinutes, ChronoUnit.MINUTES).isBefore(currentTime))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(ChapterMergeJobsService::removeJob);
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

    private @Nullable MergeResult handleJobFailure(@NotNull String jobId, @NotNull Throwable thrown, int timeoutInMinutes,
                                                   @NotNull CompletableFuture<MergeResult> asyncJob) {
        // A merge which ran out of time is given up on, not interrupted: it writes work items into the target
        // document as it goes, and a merge stopped halfway leaves a document nothing can put back together.
        String failedReason = rootReason(thrown) instanceof TimeoutException
                ? "Timeout after %d min".formatted(timeoutInMinutes)
                : describeFailure(thrown);
        failedJobsReasons.put(jobId, failedReason);
        logger.error("Chapter merge job '%s' failed with error: %s".formatted(jobId, failedReason), thrown);
        // orTimeout returns a stage of its own: the merge itself has to be marked as failed here too, since it is
        // the future the job state and the job result are read from
        asyncJob.completeExceptionally(thrown);
        // The returned value is discarded - the dependent stage is not kept
        return null;
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
        if (jobDetails == null || !Objects.equals(jobDetails.user(), polarionService.getCurrentUser())) {
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
     * A merge this service runs: the user it was scheduled for, the merge itself, and what it is doing right now.
     */
    @Builder
    public record JobDetails(
            @NotNull CompletableFuture<MergeResult> future,
            @Nullable String user,
            @NotNull Instant startingTime,
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
