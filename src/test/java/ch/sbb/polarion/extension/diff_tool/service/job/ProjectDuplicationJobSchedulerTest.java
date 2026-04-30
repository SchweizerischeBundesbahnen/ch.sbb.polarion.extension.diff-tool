package ch.sbb.polarion.extension.diff_tool.service.job;

import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationJobInfo;
import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationRequest;
import ch.sbb.polarion.extension.diff_tool.service.ProjectDuplicationService;
import com.polarion.platform.jobs.GenericJobException;
import com.polarion.platform.jobs.IJob;
import com.polarion.platform.jobs.IJobManager;
import com.polarion.platform.jobs.IJobService;
import com.polarion.platform.jobs.IJobStatus;
import com.polarion.platform.jobs.IJobUnit;
import com.polarion.platform.jobs.IJobUnitFactory;
import com.polarion.platform.jobs.JobState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectDuplicationJobSchedulerTest {

    private IJobService jobService;
    private IJobManager jobManager;
    private ProjectDuplicationService duplicationService;
    private ProjectDuplicationJobUnitFactory jobUnitFactory;
    private ProjectDuplicationJobScheduler scheduler;

    @BeforeEach
    void setUp() {
        jobService = mock(IJobService.class);
        jobManager = mock(IJobManager.class);
        duplicationService = mock(ProjectDuplicationService.class);
        jobUnitFactory = new ProjectDuplicationJobUnitFactory();
        when(jobService.getJobManager()).thenReturn(jobManager);
        scheduler = new ProjectDuplicationJobScheduler(jobService, duplicationService, jobUnitFactory);
    }

    private DuplicationRequest sampleRequest() {
        return DuplicationRequest.builder()
                .sourceProjectId("src").targetProjectId("dst")
                .location("/loc").trackerPrefix("DST").build();
    }

    private IJob mockJob(String id, String name, IJobUnitFactory creator,
                         JobState state, IJobStatus.JobStatusType statusType, long creationTime) {
        IJob job = mock(IJob.class);
        lenient().when(job.getId()).thenReturn(id);
        lenient().when(job.getName()).thenReturn(name);
        lenient().when(job.getCreationTime()).thenReturn(creationTime);
        lenient().when(job.getStartTime()).thenReturn(creationTime + 1);
        lenient().when(job.getFinishTime()).thenReturn(creationTime + 2);
        lenient().when(job.getCompletness()).thenReturn(0.5f);
        lenient().when(job.getCurrentTaskName()).thenReturn("task");
        lenient().when(job.getState()).thenReturn(state);
        IJobStatus status = mock(IJobStatus.class);
        lenient().when(status.getType()).thenReturn(statusType);
        lenient().when(status.getMessage()).thenReturn("ok-message");
        lenient().when(job.getStatus()).thenReturn(status);
        IJobUnit unit = mock(IJobUnit.class);
        lenient().when(unit.getCreator()).thenReturn(creator);
        lenient().when(job.getJobUnit()).thenReturn(unit);
        return job;
    }

    @Test
    void scheduleSpawnsAndSchedulesJobAndReturnsBuiltInfo() throws GenericJobException {
        IJob job = mockJob("J-1", "Duplicate project 'src' -> 'dst'",
                jobUnitFactory,
                JobState.STATE_RUNNING, IJobStatus.JobStatusType.STATUS_TYPE_OK, 1000L);
        when(jobManager.spawnJob(any(ProjectDuplicationJobUnit.class), eq(null))).thenReturn(job);

        DuplicationJobInfo info = scheduler.schedule(sampleRequest());

        verify(jobManager).spawnJob(any(ProjectDuplicationJobUnit.class), eq(null));
        verify(job).schedule();
        assertEquals("J-1", info.getJobId());
        assertEquals("RUNNING", info.getState());
        assertEquals("OK", info.getStatusType());
        assertEquals("/polarion/#/jobs?jobId=J-1", info.getMonitorUrl());
        assertEquals("/polarion/job-report?jobId=J-1", info.getLogUrl());
        assertEquals(0.5f, info.getCompleteness());
    }

    @Test
    void scheduleWrapsGenericJobExceptionInIllegalState() throws GenericJobException {
        when(jobManager.spawnJob(any(ProjectDuplicationJobUnit.class), eq(null)))
                .thenThrow(new GenericJobException("nope"));
        DuplicationRequest request = sampleRequest();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> scheduler.schedule(request));
        assertTrue(ex.getMessage().contains("nope"));
    }

    @Test
    void listJobsKeepsOnlyOurFactoryAndSortsByCreationTimeDescending() {
        IJobUnitFactory ours = mock(IJobUnitFactory.class);
        when(ours.getName()).thenReturn(ProjectDuplicationJobUnitFactory.NAME);
        IJobUnitFactory other = mock(IJobUnitFactory.class);
        when(other.getName()).thenReturn("some.other.factory");

        IJob old = mockJob("OLD", "Duplicate project 'a' -> 'b'", ours,
                JobState.STATE_FINISHED, IJobStatus.JobStatusType.STATUS_TYPE_OK, 1000L);
        IJob newer = mockJob("NEW", "Duplicate project 'c' -> 'd'", ours,
                JobState.STATE_RUNNING, null, 5000L);
        IJob foreign = mockJob("F", "Some other job", other,
                JobState.STATE_RUNNING, null, 9000L);
        when(jobManager.getJobs()).thenReturn(List.of(old, foreign, newer));

        List<DuplicationJobInfo> result = scheduler.listJobs();

        assertEquals(2, result.size());
        assertEquals("NEW", result.get(0).getJobId());
        assertEquals("OLD", result.get(1).getJobId());
    }

    @Test
    void listJobsSkipsJobsWithMissingUnitOrCreator() {
        IJob noUnit = mock(IJob.class);
        when(noUnit.getJobUnit()).thenReturn(null);
        when(noUnit.getCreationTime()).thenReturn(1L);
        IJob noCreator = mock(IJob.class);
        IJobUnit unit = mock(IJobUnit.class);
        when(unit.getCreator()).thenReturn(null);
        when(noCreator.getJobUnit()).thenReturn(unit);
        when(noCreator.getCreationTime()).thenReturn(2L);
        when(jobManager.getJobs()).thenReturn(List.of(noUnit, noCreator));

        assertTrue(scheduler.listJobs().isEmpty());
    }

    @Test
    void toInfoToleratesNullStateAndStatus() {
        IJob job = mock(IJob.class);
        when(job.getId()).thenReturn("X");
        when(job.getName()).thenReturn("name");
        when(job.getStatus()).thenReturn(null);
        when(job.getState()).thenReturn(null);
        IJobUnit unit = mock(IJobUnit.class);
        IJobUnitFactory ours = mock(IJobUnitFactory.class);
        when(ours.getName()).thenReturn(ProjectDuplicationJobUnitFactory.NAME);
        when(unit.getCreator()).thenReturn(ours);
        when(job.getJobUnit()).thenReturn(unit);
        when(jobManager.getJobs()).thenReturn(List.of(job));

        List<DuplicationJobInfo> result = scheduler.listJobs();

        assertEquals(1, result.size());
        assertEquals("X", result.get(0).getJobId());
        assertSame(null, result.get(0).getState());
        assertSame(null, result.get(0).getStatusType());
    }
}
