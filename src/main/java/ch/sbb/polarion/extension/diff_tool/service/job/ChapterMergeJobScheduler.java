package ch.sbb.polarion.extension.diff_tool.service.job;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeJobInfo;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.service.MergeService;
import ch.sbb.polarion.extension.diff_tool.service.DocumentsChapterMergeService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.jobs.GenericJobException;
import com.polarion.platform.jobs.IJob;
import com.polarion.platform.jobs.IJobService;
import com.polarion.platform.jobs.IJobStatus;
import com.polarion.platform.jobs.IJobUnit;
import com.polarion.platform.jobs.IJobUnitFactory;
import com.polarion.platform.jobs.JobState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spawns and tracks chapter merge jobs. A Polarion job reports only a status, so the merge result of a job is
 * kept here, which is what lets the caller of the merge see the merge report of its own operation.
 */
public class ChapterMergeJobScheduler {

    /**
     * How many merge results are kept. A result is read right after its job has finished, so only a handful is
     * ever needed at a time; the oldest one is dropped when the limit is reached.
     */
    @SuppressWarnings("java:S1104")
    static final int MAX_KEPT_RESULTS = 50;

    private final IJobService jobService;
    private final DocumentsChapterMergeService documentsChapterMergeService;
    private final PolarionService polarionService;
    private final IJobUnitFactory jobUnitFactory;

    /**
     * A job unit doesn't know the ID of its job while it is being constructed, so it reports its result into a
     * holder which is registered under the job ID as soon as the job is spawned.
     */
    static final class MergeResultHolder {
        private volatile MergeResult mergeResult;

        void set(@NotNull MergeResult mergeResult) {
            this.mergeResult = mergeResult;
        }

        @Nullable
        MergeResult get() {
            return mergeResult;
        }
    }

    /**
     * A merge this scheduler spawned: the user it was scheduled for, and the result it reports.
     */
    private record TrackedMerge(@Nullable String scheduledBy, @NotNull MergeResultHolder resultHolder) {
    }

    private final Map<String, TrackedMerge> merges = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, TrackedMerge> eldest) {
            return size() > MAX_KEPT_RESULTS;
        }
    };

    public ChapterMergeJobScheduler() {
        this(PlatformContext.getPlatform().lookupService(IJobService.class), new PolarionService(), new ChapterMergeJobUnitFactory());
    }

    public ChapterMergeJobScheduler(@NotNull IJobService jobService, @NotNull PolarionService polarionService, @NotNull IJobUnitFactory jobUnitFactory) {
        this(jobService, new DocumentsChapterMergeService(polarionService, new MergeService(polarionService)), polarionService, jobUnitFactory);
    }

    public ChapterMergeJobScheduler(@NotNull IJobService jobService, @NotNull DocumentsChapterMergeService documentsChapterMergeService,
                                    @NotNull PolarionService polarionService, @NotNull IJobUnitFactory jobUnitFactory) {
        this.jobService = jobService;
        this.documentsChapterMergeService = documentsChapterMergeService;
        this.polarionService = polarionService;
        this.jobUnitFactory = jobUnitFactory;
    }

    public @NotNull ChapterMergeJobInfo schedule(@NotNull ChapterMergeParams params) {
        // Here, on the thread which serves the REST request: the cache is keyed by the user of that request, and
        // the request object is recycled by the servlet container as soon as the response is written.
        polarionService.evictDocumentsCache(params.getSourceDocument(), params.getTargetDocument());

        MergeResultHolder resultHolder = new MergeResultHolder();
        ChapterMergeJobUnit jobUnit = new ChapterMergeJobUnit(params, documentsChapterMergeService, resultHolder::set, jobUnitFactory);
        IJob job;
        try {
            job = jobService.getJobManager().spawnJob(jobUnit, null);
        } catch (GenericJobException e) {
            throw new IllegalStateException("Failed to spawn chapter merge job: " + e.getMessage(), e);
        }
        trackMerge(job.getId(), new TrackedMerge(polarionService.getCurrentUser(), resultHolder));
        job.schedule();
        return toInfo(job, false);
    }

    /**
     * The chapter merge jobs of the current user, the most recent one first. A merge names the documents it works
     * on and reports which work items it created, so a job belongs to the user who scheduled it.
     */
    public @NotNull List<ChapterMergeJobInfo> listJobs() {
        return jobService.getJobManager().getJobs().stream()
                .filter(job -> isOurJob(job) && scheduledByCurrentUser(job.getId()))
                .sorted(Comparator.comparingLong(IJob::getCreationTime).reversed())
                .map(job -> toInfo(job, false))
                .toList();
    }

    /**
     * Returns a chapter merge job with its merge result, as soon as the job has produced one. A job which is still
     * running is a perfectly normal answer here - it is what the caller of a merge polls to learn that it finished.
     *
     * @return {@code null} if the current user has no chapter merge job with that ID
     */
    public @Nullable ChapterMergeJobInfo getJob(@NotNull String jobId) {
        if (!scheduledByCurrentUser(jobId)) {
            return null;
        }
        return jobService.getJobManager().getJobs().stream()
                .filter(job -> this.isOurJob(job) && jobId.equals(job.getId()))
                .findFirst()
                .map(job -> toInfo(job, true))
                .orElse(null);
    }

    @VisibleForTesting
    synchronized @Nullable MergeResult readResult(@NotNull String jobId) {
        MergeResultHolder resultHolder = resultHolderOf(jobId);
        return resultHolder == null ? null : resultHolder.get();
    }

    @VisibleForTesting
    synchronized @Nullable MergeResultHolder resultHolderOf(@NotNull String jobId) {
        TrackedMerge trackedMerge = merges.get(jobId);
        return trackedMerge == null ? null : trackedMerge.resultHolder();
    }

    /**
     * Whether a job was scheduled for the user of the current call. A job this scheduler doesn't know belongs to
     * nobody: it either never ran here, or its result was dropped long ago.
     */
    @VisibleForTesting
    synchronized boolean scheduledByCurrentUser(@Nullable String jobId) {
        TrackedMerge trackedMerge = jobId == null ? null : merges.get(jobId);
        String currentUser = polarionService.getCurrentUser();
        return trackedMerge != null && currentUser != null && currentUser.equals(trackedMerge.scheduledBy());
    }

    private synchronized void trackMerge(@NotNull String jobId, @NotNull TrackedMerge trackedMerge) {
        merges.put(jobId, trackedMerge);
    }



    private boolean isOurJob(@NotNull IJob job) {
        IJobUnit unit = job.getJobUnit();
        if (unit == null) {
            return false;
        }
        IJobUnitFactory creator = unit.getCreator();
        return creator != null && ChapterMergeJobUnitFactory.NAME.equals(creator.getName());
    }

    private @NotNull ChapterMergeJobInfo toInfo(@NotNull IJob job, boolean withResult) {
        MergeResult mergeResult = job.getId() == null ? null : readResult(job.getId());
        IJobStatus status = job.getStatus();
        JobState state = job.getState();
        return ChapterMergeJobInfo.builder()
                .jobId(job.getId())
                .jobName(job.getName())
                .state(state == null ? null : state.getName())
                .statusType(status == null || status.getType() == null ? null : status.getType().getName())
                .statusMessage(status == null ? null : status.getMessage())
                .creationTime(job.getCreationTime())
                .startTime(job.getStartTime())
                .finishTime(job.getFinishTime())
                .completeness(job.getCompletness())
                .currentTaskName(job.getCurrentTaskName())
                .logUrl("/polarion/job-report?jobId=" + job.getId())
                .resultAvailable(mergeResult != null)
                .mergeResult(withResult ? mergeResult : null)
                .build();
    }
}
