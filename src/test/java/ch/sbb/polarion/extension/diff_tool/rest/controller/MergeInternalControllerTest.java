package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.properties.DiffToolExtensionConfiguration;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterInsertMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.jobs.ChapterMergeJobDetails;
import ch.sbb.polarion.extension.diff_tool.rest.model.jobs.ChapterMergeJobStatus;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.job.ChapterMergeJobsService;
import ch.sbb.polarion.extension.diff_tool.service.job.ChapterMergeJobsService.JobState;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MergeInternalControllerTest {

    private static final String JOB_ID = "J-1";
    private static final String JOBS_PATH = "/polarion/diff-tool/rest/internal/merge/chapter";

    private ChapterMergeJobsService chapterMergeJobsService;
    private UriInfo uriInfo;
    private MockedStatic<DiffToolExtensionConfiguration> configuration;
    private MergeInternalController controller;

    @BeforeEach
    void setUp() {
        chapterMergeJobsService = mock(ChapterMergeJobsService.class);
        uriInfo = mock(UriInfo.class);

        DiffToolExtensionConfiguration extensionConfiguration = mock(DiffToolExtensionConfiguration.class);
        lenient().when(extensionConfiguration.getChapterMergeTimeout()).thenReturn(60);
        configuration = mockStatic(DiffToolExtensionConfiguration.class);
        configuration.when(DiffToolExtensionConfiguration::getInstance).thenReturn(extensionConfiguration);

        controller = new MergeInternalController(mock(PolarionService.class), chapterMergeJobsService);
        controller.setUriInfo(uriInfo);
    }

    @AfterEach
    void tearDown() {
        configuration.close();
    }

    /**
     * A merge can take long, so the call which asks for it is answered as soon as the merge has started, and the
     * job which delivers its result is named in the Location header.
     */
    @Test
    void testStartingAMergeAnswersWithTheJobItIsPolledBy() {
        when(uriInfo.getRequestUri()).thenReturn(URI.create(JOBS_PATH));
        when(chapterMergeJobsService.startJob(any(), eq(60))).thenReturn(JOB_ID);

        Response response = controller.mergeChapter(params());

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        assertEquals(URI.create(JOBS_PATH + "/jobs/" + JOB_ID), response.getLocation());
    }

    @Test
    void testAMergeWithoutItsMandatoryParametersIsRefused() {
        assertThrows(BadRequestException.class, () -> controller.mergeChapter(null));

        ChapterMergeParams withoutTargetDocument = params();
        withoutTargetDocument.setTargetDocument(null);
        assertThrows(BadRequestException.class, () -> controller.mergeChapter(withoutTargetDocument));

        ChapterMergeParams withoutChapter = params();
        withoutChapter.setSourceChapterOutlineNumber(" ");
        assertThrows(BadRequestException.class, () -> controller.mergeChapter(withoutChapter));
    }

    @Test
    void testAMergeWhichIsStillRunningIsAnsweredWithWhatItIsDoing() {
        when(chapterMergeJobsService.getJobState(JOB_ID)).thenReturn(JobState.builder()
                .isDone(false)
                .progressMessage("Merged workitem 'EL-42'")
                .build());

        Response response = controller.getChapterMergeJob(JOB_ID);

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        ChapterMergeJobDetails jobDetails = (ChapterMergeJobDetails) response.getEntity();
        assertEquals(ChapterMergeJobStatus.IN_PROGRESS, jobDetails.getStatus());
        assertEquals("Merged workitem 'EL-42'", jobDetails.getProgressMessage());
    }

    @Test
    void testAFinishedMergeRedirectsToItsResult() {
        when(uriInfo.getRequestUri()).thenReturn(URI.create(JOBS_PATH + "/jobs/" + JOB_ID));
        when(chapterMergeJobsService.getJobState(JOB_ID)).thenReturn(JobState.builder().isDone(true).build());

        Response response = controller.getChapterMergeJob(JOB_ID);

        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        assertEquals(URI.create(JOBS_PATH + "/jobs/" + JOB_ID + "/result"), response.getLocation());
        ChapterMergeJobDetails jobDetails = (ChapterMergeJobDetails) response.getEntity();
        assertEquals(ChapterMergeJobStatus.SUCCESSFULLY_FINISHED, jobDetails.getStatus());
        // what it is doing is of no interest once it is done
        assertNull(jobDetails.getProgressMessage());
    }

    @Test
    void testAFailedMergeIsAnsweredWithWhatWentWrong() {
        when(chapterMergeJobsService.getJobState(JOB_ID)).thenReturn(JobState.builder()
                .isDone(true)
                .isFailed(true)
                .errorMessage("Node has been added before.")
                .build());

        Response response = controller.getChapterMergeJob(JOB_ID);

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        ChapterMergeJobDetails jobDetails = (ChapterMergeJobDetails) response.getEntity();
        assertEquals(ChapterMergeJobStatus.FAILED, jobDetails.getStatus());
        assertEquals("Node has been added before.", jobDetails.getErrorMessage());
    }

    @Test
    void testAnUnknownMergeIsNotFound() {
        when(chapterMergeJobsService.getJobState(JOB_ID)).thenThrow(new NoSuchElementException("Chapter merge job is unknown: J-1"));

        assertThrows(NoSuchElementException.class, () -> controller.getChapterMergeJob(JOB_ID));
    }

    @Test
    void testTheResultOfAFinishedMergeIsDelivered() {
        MergeResult mergeResult = MergeResult.builder().success(true).build();
        when(chapterMergeJobsService.getJobResult(JOB_ID)).thenReturn(Optional.of(mergeResult));

        Response response = controller.getChapterMergeJobResult(JOB_ID);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(mergeResult, response.getEntity());
    }

    @Test
    void testAMergeWhichIsStillRunningHasNoResultToDeliver() {
        when(chapterMergeJobsService.getJobResult(JOB_ID)).thenReturn(Optional.empty());

        Response response = controller.getChapterMergeJobResult(JOB_ID);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testTheMergesOfTheCurrentUserAreListedByTheirJobId() {
        when(chapterMergeJobsService.getAllJobsStates()).thenReturn(Map.of(
                JOB_ID, JobState.builder().isDone(true).build(),
                "J-2", JobState.builder().isDone(false).progressMessage("Merging").build()));

        Response response = controller.getAllChapterMergeJobs();

        Map<String, ChapterMergeJobDetails> jobsDetails = (Map<String, ChapterMergeJobDetails>) response.getEntity();
        assertEquals(2, jobsDetails.size());
        assertEquals(ChapterMergeJobStatus.SUCCESSFULLY_FINISHED, jobsDetails.get(JOB_ID).getStatus());
        assertEquals(ChapterMergeJobStatus.IN_PROGRESS, jobsDetails.get("J-2").getStatus());
        assertEquals("Merging", jobsDetails.get("J-2").getProgressMessage());
    }

    @Test
    void testAMergeIsStartedForAsLongAsTheConfigurationAllows() {
        when(uriInfo.getRequestUri()).thenReturn(URI.create(JOBS_PATH));
        when(chapterMergeJobsService.startJob(any(), eq(60))).thenReturn(JOB_ID);
        ChapterMergeParams params = params();

        controller.mergeChapter(params);

        verify(chapterMergeJobsService).startJob(params, 60);
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
}
