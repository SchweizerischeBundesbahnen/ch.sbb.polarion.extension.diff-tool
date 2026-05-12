package ch.sbb.polarion.extension.diff_tool.service.job;

import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationJobInfo;
import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationRequest;
import ch.sbb.polarion.extension.diff_tool.service.ProjectDuplicationService;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.jobs.GenericJobException;
import com.polarion.platform.jobs.IJob;
import com.polarion.platform.jobs.IJobService;
import com.polarion.platform.jobs.IJobStatus;
import com.polarion.platform.jobs.IJobUnit;
import com.polarion.platform.jobs.IJobUnitFactory;
import com.polarion.platform.jobs.JobState;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

public class ProjectDuplicationJobScheduler {

    private final IJobService jobService;
    private final ProjectDuplicationService duplicationService;
    private final IJobUnitFactory jobUnitFactory;

    public ProjectDuplicationJobScheduler() {
        this(PlatformContext.getPlatform().lookupService(IJobService.class),
                new ProjectDuplicationService(),
                new ProjectDuplicationJobUnitFactory());
    }

    public ProjectDuplicationJobScheduler(@NotNull IJobService jobService,
                                          @NotNull ProjectDuplicationService duplicationService,
                                          @NotNull IJobUnitFactory jobUnitFactory) {
        this.jobService = jobService;
        this.duplicationService = duplicationService;
        this.jobUnitFactory = jobUnitFactory;
    }

    public @NotNull DuplicationJobInfo schedule(@NotNull DuplicationRequest request) {
        ProjectDuplicationJobUnit jobUnit = new ProjectDuplicationJobUnit(request, duplicationService, jobUnitFactory);
        IJob job;
        try {
            job = jobService.getJobManager().spawnJob(jobUnit, null);
        } catch (GenericJobException e) {
            throw new IllegalStateException("Failed to spawn project duplication job: " + e.getMessage(), e);
        }
        job.schedule();
        return toInfo(job);
    }

    public @NotNull List<DuplicationJobInfo> listJobs() {
        return jobService.getJobManager().getJobs().stream()
                .filter(this::isOurJob)
                .sorted(Comparator.comparingLong(IJob::getCreationTime).reversed())
                .map(this::toInfo)
                .toList();
    }

    private boolean isOurJob(@NotNull IJob job) {
        IJobUnit unit = job.getJobUnit();
        if (unit == null) {
            return false;
        }
        IJobUnitFactory creator = unit.getCreator();
        return creator != null && ProjectDuplicationJobUnitFactory.NAME.equals(creator.getName());
    }

    private @NotNull DuplicationJobInfo toInfo(@NotNull IJob job) {
        IJobStatus status = job.getStatus();
        JobState state = job.getState();
        return DuplicationJobInfo.builder()
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
                .build();
    }
}
