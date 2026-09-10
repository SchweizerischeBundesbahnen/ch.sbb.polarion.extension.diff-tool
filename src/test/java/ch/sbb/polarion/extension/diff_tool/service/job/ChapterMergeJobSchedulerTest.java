package ch.sbb.polarion.extension.diff_tool.service.job;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterInsertMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeJobInfo;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.service.DocumentsChapterMergeService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.generic.rest.filter.AuthenticationFilter;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import com.polarion.platform.jobs.GenericJobException;
import com.polarion.platform.jobs.IJob;
import com.polarion.platform.jobs.IJobManager;
import com.polarion.platform.jobs.IJobService;
import com.polarion.platform.jobs.IJobStatus;
import com.polarion.platform.jobs.IJobUnit;
import com.polarion.platform.jobs.IJobUnitFactory;
import com.polarion.platform.jobs.JobState;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.security.auth.Subject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
class ChapterMergeJobSchedulerTest {

    private IJobService jobService;
    private IJobManager jobManager;
    private DocumentsChapterMergeService documentsChapterMergeService;
    private PolarionService polarionService;
    private ChapterMergeJobUnitFactory jobUnitFactory;
    private ChapterMergeJobScheduler scheduler;

    @BeforeEach
    void setUp() {
        jobService = mock(IJobService.class);
        jobManager = mock(IJobManager.class);
        documentsChapterMergeService = mock(DocumentsChapterMergeService.class);
        polarionService = mock(PolarionService.class);
        jobUnitFactory = new ChapterMergeJobUnitFactory();
        lenient().when(jobService.getJobManager()).thenReturn(jobManager);
        scheduler = new ChapterMergeJobScheduler(jobService, documentsChapterMergeService, polarionService, jobUnitFactory);

        // A job is scheduled on the thread which serves the REST request, and takes its context along.
        HttpServletRequest request = mock(HttpServletRequest.class);
        lenient().when(request.getAttribute(AuthenticationFilter.USER_SUBJECT)).thenReturn(new Subject());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testScheduleSpawnsAndSchedulesTheJob() throws GenericJobException {
        IJob job = mockJob("J-1", jobUnitFactory, JobState.STATE_RUNNING, IJobStatus.JobStatusType.STATUS_TYPE_OK, 1000L);
        when(jobManager.spawnJob(any(ChapterMergeJobUnit.class), eq(null))).thenReturn(job);

        ChapterMergeJobInfo info = scheduler.schedule(params());

        verify(job).schedule();
        assertEquals("J-1", info.getJobId());
        assertEquals("RUNNING", info.getState());
        assertEquals("/polarion/job-report?jobId=J-1", info.getLogUrl());
        assertFalse(info.isResultAvailable());
    }


    @Test
    void testTheDocumentsCacheIsEvictedWhileTheRequestIsStillAlive() throws GenericJobException {
        IJob job = mockJob("J-1", jobUnitFactory, JobState.STATE_RUNNING, null, 1000L);
        when(jobManager.spawnJob(any(ChapterMergeJobUnit.class), eq(null))).thenReturn(job);
        ChapterMergeParams params = params();

        scheduler.schedule(params);

        // The cache is keyed by the user of the request, and a job thread has no request: the servlet container
        // recycles the request object as soon as the response is written.
        InOrder order = inOrder(polarionService, job);
        order.verify(polarionService).evictDocumentsCache(params.getSourceDocument(), params.getTargetDocument());
        order.verify(job).schedule();
    }

    @Test
    void testScheduleWrapsGenericJobExceptionInIllegalState() throws GenericJobException {
        when(jobManager.spawnJob(any(ChapterMergeJobUnit.class), eq(null))).thenThrow(new GenericJobException("nope"));
        ChapterMergeParams params = params();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> scheduler.schedule(params));
        assertTrue(exception.getMessage().contains("nope"));
    }

    @Test
    void testJobCarriesItsMergeResultAsSoonAsItHasOne() throws GenericJobException {
        IJob job = mockJob("J-1", jobUnitFactory, JobState.STATE_RUNNING, null, 1000L);
        when(jobManager.spawnJob(any(ChapterMergeJobUnit.class), eq(null))).thenReturn(job);
        when(jobManager.getJobs()).thenReturn(List.of(job));
        scheduler.schedule(params());

        // A running job is a normal answer: no result yet, and nothing which looks like an error
        ChapterMergeJobInfo running = scheduler.getJob("J-1");
        assertNotNull(running);
        assertNull(running.getMergeResult());
        assertFalse(running.isResultAvailable());

        // What the job unit does when it has finished, without running a Polarion job in a unit test
        MergeResult mergeResult = MergeResult.builder().success(true).build();
        scheduler.resultHolderOf("J-1").set(mergeResult);

        ChapterMergeJobInfo finished = scheduler.getJob("J-1");
        assertNotNull(finished);
        assertEquals(mergeResult, finished.getMergeResult());
        assertTrue(finished.isResultAvailable());
        // ...and it stays readable, since polling asks more than once
        assertEquals(mergeResult, scheduler.getJob("J-1").getMergeResult());
    }

    @Test
    void testUnknownJobIsNotFound() {
        when(jobManager.getJobs()).thenReturn(List.of());

        assertNull(scheduler.getJob("J-1"));
    }

    @Test
    void testListedJobsDoNotCarryTheirMergeResults() throws GenericJobException {
        IJob job = mockJob("J-1", jobUnitFactory, JobState.STATE_FINISHED, IJobStatus.JobStatusType.STATUS_TYPE_OK, 1000L);
        when(jobManager.spawnJob(any(ChapterMergeJobUnit.class), eq(null))).thenReturn(job);
        when(jobManager.getJobs()).thenReturn(List.of(job));
        scheduler.schedule(params());
        scheduler.resultHolderOf("J-1").set(MergeResult.builder().success(true).build());

        ChapterMergeJobInfo listed = scheduler.listJobs().getFirst();

        assertTrue(listed.isResultAvailable());
        assertNull(listed.getMergeResult());
    }

    @Test
    void testListJobsKeepsOnlyChapterMergeJobsAndSortsThemNewestFirst() {
        IJobUnitFactory ours = mock(IJobUnitFactory.class);
        when(ours.getName()).thenReturn(ChapterMergeJobUnitFactory.NAME);
        IJobUnitFactory other = mock(IJobUnitFactory.class);
        when(other.getName()).thenReturn("some.other.factory");

        IJob older = mockJob("OLD", ours, JobState.STATE_FINISHED, IJobStatus.JobStatusType.STATUS_TYPE_OK, 1000L);
        IJob newer = mockJob("NEW", ours, JobState.STATE_RUNNING, null, 5000L);
        IJob foreign = mockJob("FOREIGN", other, JobState.STATE_RUNNING, null, 9000L);
        when(jobManager.getJobs()).thenReturn(List.of(older, foreign, newer));

        List<ChapterMergeJobInfo> result = scheduler.listJobs();

        assertEquals(2, result.size());
        assertEquals("NEW", result.get(0).getJobId());
        assertEquals("OLD", result.get(1).getJobId());
    }

    @Test
    void testListJobsSkipsJobsWithoutUnitOrCreator() {
        IJob withoutUnit = mock(IJob.class);
        lenient().when(withoutUnit.getJobUnit()).thenReturn(null);
        IJob withoutCreator = mock(IJob.class);
        IJobUnit unit = mock(IJobUnit.class);
        lenient().when(unit.getCreator()).thenReturn(null);
        lenient().when(withoutCreator.getJobUnit()).thenReturn(unit);
        when(jobManager.getJobs()).thenReturn(List.of(withoutUnit, withoutCreator));

        assertTrue(scheduler.listJobs().isEmpty());
    }

    private ChapterMergeParams params() {
        return ChapterMergeParams.builder()
                .sourceDocument(DocumentIdentifier.builder().projectId("source").spaceId("space").name("sourceDoc").build())
                .targetDocument(DocumentIdentifier.builder().projectId("target").spaceId("space").name("targetDoc").build())
                .mode(ChapterMergeMode.COPY)
                .insertMode(ChapterInsertMode.UNDER)
                .sourceChapterOutlineNumber("2")
                .targetChapterOutlineNumber("3.1")
                .build();
    }

    private IJob mockJob(String id, IJobUnitFactory creator, JobState state, IJobStatus.JobStatusType statusType, long creationTime) {
        IJob job = mock(IJob.class);
        lenient().when(job.getId()).thenReturn(id);
        lenient().when(job.getName()).thenReturn("Merge chapter");
        lenient().when(job.getCreationTime()).thenReturn(creationTime);
        lenient().when(job.getState()).thenReturn(state);
        IJobStatus status = mock(IJobStatus.class);
        lenient().when(status.getType()).thenReturn(statusType);
        lenient().when(job.getStatus()).thenReturn(status);
        IJobUnit unit = mock(IJobUnit.class);
        lenient().when(unit.getCreator()).thenReturn(creator);
        lenient().when(job.getJobUnit()).thenReturn(unit);
        return job;
    }
}
