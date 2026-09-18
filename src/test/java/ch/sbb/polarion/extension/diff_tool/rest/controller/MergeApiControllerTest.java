package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.properties.DiffToolExtensionConfiguration;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterInsertMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.job.ChapterMergeJobsService;
import ch.sbb.polarion.extension.diff_tool.service.job.ChapterMergeJobsService.JobState;
import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MergeApiControllerTest {

    private static final String JOB_ID = "J-1";
    private static final String JOBS_PATH = "/polarion/diff-tool/rest/api/merge/chapter";

    private PolarionService polarionService;
    private ChapterMergeJobsService chapterMergeJobsService;
    private HttpServletRequest request;
    private MockedStatic<DiffToolExtensionConfiguration> configuration;
    private MergeApiController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        polarionService = mock(PolarionService.class);
        chapterMergeJobsService = mock(ChapterMergeJobsService.class);

        DiffToolExtensionConfiguration extensionConfiguration = mock(DiffToolExtensionConfiguration.class);
        lenient().when(extensionConfiguration.getChapterMergeTimeout()).thenReturn(60);
        configuration = mockStatic(DiffToolExtensionConfiguration.class);
        configuration.when(DiffToolExtensionConfiguration::getInstance).thenReturn(extensionConfiguration);

        lenient().when(polarionService.userAuthorizedForMerge(any())).thenReturn(true);
        lenient().when(polarionService.callPrivileged(any(Callable.class)))
                .thenAnswer(invocation -> ((Callable<?>) invocation.getArgument(0)).call());

        UriInfo uriInfo = mock(UriInfo.class);
        lenient().when(uriInfo.getRequestUri()).thenReturn(URI.create(JOBS_PATH));

        controller = new MergeApiController(polarionService, chapterMergeJobsService);
        controller.setUriInfo(uriInfo);

        request = mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        configuration.close();
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * This call authenticated itself, so it got a session of its own which the logout filter would end with this
     * response - long before the merge it starts has finished. The merge ends that session itself when it is over.
     */
    @Test
    void testStartingAMergeKeepsTheSessionOfTheCallForIt() {
        when(chapterMergeJobsService.startJob(any(), any(Integer.class))).thenReturn(JOB_ID);

        Response response = controller.mergeChapter(params());

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        verify(request).setAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, Boolean.TRUE);
        verify(request, never()).removeAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT);
    }

    /**
     * The flag has to be set before the merge is started, since that is where it is read. A call which never gets as
     * far as a running merge therefore gives the session back: nothing else would end it.
     */
    @Test
    void testAMergeWhichNeverStartedGivesTheSessionBack() {
        ChapterMergeParams withoutChapters = params();
        withoutChapters.setSourceChapterOutlineNumber(null);

        assertThrows(BadRequestException.class, () -> controller.mergeChapter(withoutChapters));

        verify(request).setAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, Boolean.TRUE);
        verify(request).removeAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT);
    }

    @Test
    void testAMergeWhichCouldNotBeStartedGivesTheSessionBack() {
        // eg. a document of the merge which cannot be read, which the documents cache eviction runs into
        when(chapterMergeJobsService.startJob(any(), any(Integer.class))).thenThrow(new IllegalStateException("Project 'ELIBRARY' not found"));
        ChapterMergeParams params = params();

        assertThrows(IllegalStateException.class, () -> controller.mergeChapter(params));

        verify(request).removeAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testEveryChapterMergeCallRunsPrivileged() {
        when(chapterMergeJobsService.startJob(any(), any(Integer.class))).thenReturn(JOB_ID);
        when(chapterMergeJobsService.getJobState(JOB_ID)).thenReturn(JobState.builder().isDone(false).build());
        when(chapterMergeJobsService.getJobResult(JOB_ID)).thenReturn(Optional.of(MergeResult.builder().success(true).build()));
        when(chapterMergeJobsService.getAllJobsStates()).thenReturn(Map.of());

        controller.mergeChapter(params());
        controller.getChapterMergeJob(JOB_ID);
        controller.getChapterMergeJobResult(JOB_ID);
        controller.getAllChapterMergeJobs();

        verify(polarionService, times(4)).callPrivileged(any(Callable.class));
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
