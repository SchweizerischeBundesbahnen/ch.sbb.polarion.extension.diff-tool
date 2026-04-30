package ch.sbb.polarion.extension.diff_tool.service.job;

import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationRequest;
import ch.sbb.polarion.extension.diff_tool.service.ProjectDuplicationService;
import com.polarion.platform.jobs.IJob;
import com.polarion.platform.jobs.IJobStatus;
import com.polarion.platform.jobs.ILogger;
import com.polarion.platform.jobs.IProgressMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProjectDuplicationJobUnitTest {

    private ProjectDuplicationService duplicationService;
    private IProgressMonitor monitor;
    private ProjectDuplicationJobUnit jobUnit;

    @BeforeEach
    void setUp() {
        duplicationService = mock(ProjectDuplicationService.class);
        monitor = mock(IProgressMonitor.class);
        DuplicationRequest request = DuplicationRequest.builder()
                .sourceProjectId("src").targetProjectId("dst")
                .location("/loc").trackerPrefix("DST").build();
        jobUnit = new ProjectDuplicationJobUnit(request, duplicationService, new ProjectDuplicationJobUnitFactory());
        jobUnit.setJob(mock(IJob.class));
        jobUnit.setLogger(mock(ILogger.class));
    }

    @Test
    void nameContainsSourceAndTargetIds() {
        assertEquals("Duplicate project 'src' -> 'dst'", jobUnit.getName());
    }

    @Test
    void runInternalReturnsOkAndCallsServiceOnSuccess() {
        IJobStatus status = jobUnit.run(monitor);

        assertNotNull(status);
        assertEquals(IJobStatus.JobStatusType.STATUS_TYPE_OK, status.getType());
        assertTrue(status.getMessage().contains("dst"));
        assertTrue(status.getMessage().contains("/project/dst"));
        verify(duplicationService).duplicate(any(DuplicationRequest.class), any(ILogger.class));
        verify(monitor).beginTask(jobUnit.getName(), IProgressMonitor.UNKNOWN);
        verify(monitor).done();
    }

    @Test
    void runInternalReturnsFailedStatusOnException() {
        doThrow(new IllegalStateException("boom"))
                .when(duplicationService).duplicate(any(DuplicationRequest.class), any(ILogger.class));

        IJobStatus status = jobUnit.run(monitor);

        assertEquals(IJobStatus.JobStatusType.STATUS_TYPE_FAILED, status.getType());
        assertTrue(status.getMessage().contains("boom"));
        verify(monitor).done();
    }
}
