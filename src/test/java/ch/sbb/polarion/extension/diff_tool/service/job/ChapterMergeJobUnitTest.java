package ch.sbb.polarion.extension.diff_tool.service.job;

import ch.sbb.polarion.extension.diff_tool.report.MergeReport;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterInsertMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.service.DocumentsChapterMergeService;
import com.polarion.platform.jobs.IJob;
import com.polarion.platform.jobs.IJobStatus;
import com.polarion.platform.jobs.ILogger;
import com.polarion.platform.jobs.IProgressMonitor;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChapterMergeJobUnitTest {

    private DocumentsChapterMergeService documentsChapterMergeService;
    private IProgressMonitor monitor;
    private List<MergeResult> reportedResults;
    private RequestAttributes requestAttributes;

    @BeforeEach
    void setUp() {
        documentsChapterMergeService = mock(DocumentsChapterMergeService.class);
        monitor = mock(IProgressMonitor.class);
        reportedResults = new ArrayList<>();
        requestAttributes = new ServletRequestAttributes(mock(HttpServletRequest.class));
        RequestContextHolder.setRequestAttributes(requestAttributes);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testNameNamesBothChaptersAndBothDocuments() {
        assertEquals("Merge chapter '2' of 'sourceDoc' into chapter '3.1' of 'targetDoc'", jobUnit().getName());
    }

    /**
     * A job outlives the request which scheduled it: the servlet container recycles the request object as soon as
     * the response is written, so reading anything off it later fails with "The request object has been recycled".
     * The job runs as the user Polarion spawned it for and asks the request for nothing.
     */
    @Test
    void testMergeDoesNotReachForTheRequestWhichScheduledTheJob() {
        ChapterMergeJobUnit jobUnit = jobUnit();
        AtomicReference<RequestAttributes> attributesWhileMerging = new AtomicReference<>();
        when(documentsChapterMergeService.mergeChapter(any(), any())).thenAnswer(invocation -> {
            attributesWhileMerging.set(RequestContextHolder.getRequestAttributes());
            return MergeResult.builder().success(true).build();
        });
        // the job runs on another thread, which knows nothing about the request
        RequestContextHolder.resetRequestAttributes();

        jobUnit.run(monitor);

        assertNull(attributesWhileMerging.get());
    }

    @Test
    void testSuccessfulMergeReportsItsResultAndFinishesWithOk() {
        ChapterMergeJobUnit jobUnit = jobUnit();
        MergeResult mergeResult = MergeResult.builder().success(true).mergeReport(new MergeReport()).build();
        when(documentsChapterMergeService.mergeChapter(any(), any())).thenReturn(mergeResult);

        IJobStatus status = jobUnit.run(monitor);

        assertEquals(IJobStatus.JobStatusType.STATUS_TYPE_OK, status.getType());
        assertTrue(status.getMessage().contains("targetDoc"));
        assertEquals(List.of(mergeResult), reportedResults);
        verify(monitor).done();
    }

    @Test
    void testUnsuccessfulMergeReportsItsResultAndFinishesWithFailed() {
        ChapterMergeJobUnit jobUnit = jobUnit();
        MergeResult mergeResult = MergeResult.builder().success(false).mergeNotAuthorized(true).build();
        when(documentsChapterMergeService.mergeChapter(any(), any())).thenReturn(mergeResult);

        IJobStatus status = jobUnit.run(monitor);

        assertEquals(IJobStatus.JobStatusType.STATUS_TYPE_FAILED, status.getType());
        assertTrue(status.getMessage().contains("not authorized"));
        assertEquals(List.of(mergeResult), reportedResults);
    }

    @Test
    void testFailingMergeReportsAnUnsuccessfulResultAndFinishesWithFailed() {
        ChapterMergeJobUnit jobUnit = jobUnit();
        doThrow(new IllegalStateException("boom")).when(documentsChapterMergeService).mergeChapter(any(), any());

        IJobStatus status = jobUnit.run(monitor);

        assertEquals(IJobStatus.JobStatusType.STATUS_TYPE_FAILED, status.getType());
        assertTrue(status.getMessage().contains("boom"));
        assertEquals(1, reportedResults.size());
        assertTrue(!reportedResults.getFirst().isSuccess());
        verify(monitor).done();
    }

    @Test
    void testFailureMessageNamesWhatTheUserCanDoAboutIt() {
        ChapterMergeJobUnit jobUnit = jobUnit();

        assertTrue(jobUnit.failureMessage(MergeResult.builder().targetModuleHasStructuralChanges(true).build()).contains("reload"));
        assertTrue(jobUnit.failureMessage(MergeResult.builder().build()).contains("merge report"));
    }

    private ChapterMergeJobUnit jobUnit() {
        ChapterMergeParams params = ChapterMergeParams.builder()
                .sourceDocument(DocumentIdentifier.builder().projectId("source").spaceId("space").name("sourceDoc").build())
                .targetDocument(DocumentIdentifier.builder().projectId("target").spaceId("space").name("targetDoc").build())
                .mode(ChapterMergeMode.COPY)
                .insertMode(ChapterInsertMode.UNDER)
                .sourceChapterOutlineNumber("2")
                .targetChapterOutlineNumber("3.1")
                .build();
        ChapterMergeJobUnit jobUnit = new ChapterMergeJobUnit(params, documentsChapterMergeService, reportedResults::add, new ChapterMergeJobUnitFactory());
        jobUnit.setJob(mock(IJob.class));
        jobUnit.setLogger(mock(ILogger.class));
        return jobUnit;
    }
}
