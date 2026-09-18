package ch.sbb.polarion.extension.diff_tool.service.job;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterInsertMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeMode;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.ChapterMergeParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.service.DocumentsChapterMergeService;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.job.ChapterMergeJobsService.JobState;
import ch.sbb.polarion.extension.diff_tool.service.queue.QueueFullException;
import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import com.polarion.platform.security.ISecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChapterMergeJobsServiceTest {

    private static final String USER = "jdoe";
    private static final int TIMEOUT_IN_MINUTES = 10;

    private DocumentsChapterMergeService documentsChapterMergeService;
    private PolarionService polarionService;
    private ISecurityService securityService;
    private Subject userSubject;
    private ServletRequestAttributes requestAttributes;
    private ChapterMergeJobsService jobsService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        documentsChapterMergeService = mock(DocumentsChapterMergeService.class);
        polarionService = mock(PolarionService.class);
        securityService = mock(ISecurityService.class);
        userSubject = new Subject();
        requestAttributes = mock(ServletRequestAttributes.class);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        lenient().when(polarionService.getSecurityService()).thenReturn(securityService);
        lenient().when(polarionService.getCurrentUser()).thenReturn(USER);
        lenient().when(polarionService.getCurrentSubject()).thenReturn(userSubject);
        lenient().when(securityService.doAsUser(any(), any(PrivilegedAction.class)))
                .thenAnswer(invocation -> ((PrivilegedAction<Object>) invocation.getArgument(1)).run());
        // no request asks for its session to be kept unless the test says so
        lenient().when(requestAttributes.getAttribute(anyString(), eq(RequestAttributes.SCOPE_REQUEST))).thenReturn(null);

        jobsService = new ChapterMergeJobsService(documentsChapterMergeService, polarionService);
    }

    @AfterEach
    void tearDown() {
        jobsService.cancelJobsAndCleanMap();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testAFinishedMergeDeliversItsResult() {
        MergeResult mergeResult = MergeResult.builder().success(true).build();
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenReturn(mergeResult);

        String jobId = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);

        assertFalse(jobId.isBlank());
        awaitDone(jobId);
        JobState jobState = jobsService.getJobState(jobId);
        assertTrue(jobState.isDone());
        assertFalse(jobState.isFailed());
        assertNull(jobState.errorMessage());
        assertEquals(Optional.of(mergeResult), jobsService.getJobResult(jobId));
        // ...and it stays readable, since polling asks more than once
        assertEquals(Optional.of(mergeResult), jobsService.getJobResult(jobId));
    }

    /**
     * A merge which did not do what was asked of it says so in its result, which is what the caller is shown. Only
     * a merge which produced no result at all is a failed job.
     */
    @Test
    void testAnUnsuccessfulMergeResultIsAResultLikeAnyOther() {
        MergeResult mergeResult = MergeResult.builder().success(false).mergeNotAuthorized(true).build();
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenReturn(mergeResult);

        String jobId = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);

        awaitDone(jobId);
        assertFalse(jobsService.getJobState(jobId).isFailed());
        assertEquals(Optional.of(mergeResult), jobsService.getJobResult(jobId));
    }

    @Test
    void testAMergeWhichIsStillRunningHasNoResultYet() {
        CountDownLatch mergeStarted = new CountDownLatch(1);
        CountDownLatch releaseMerge = new CountDownLatch(1);
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenAnswer(invocation -> {
            mergeStarted.countDown();
            releaseMerge.await();
            return MergeResult.builder().success(true).build();
        });

        String jobId = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);

        try {
            await().atMost(Duration.ofSeconds(10)).until(() -> mergeStarted.getCount() == 0);
            JobState jobState = jobsService.getJobState(jobId);
            assertFalse(jobState.isDone());
            assertEquals(Optional.empty(), jobsService.getJobResult(jobId));
        } finally {
            releaseMerge.countDown();
        }
    }

    @Test
    void testAFailedMergeReportsWhatWentWrong() {
        doThrow(new IllegalStateException("Node has been added before.")).when(documentsChapterMergeService).mergeChapter(any(), any(), any());

        String jobId = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);

        awaitDone(jobId);
        JobState jobState = jobsService.getJobState(jobId);
        assertTrue(jobState.isFailed());
        assertEquals("Node has been added before.", jobState.errorMessage());
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> jobsService.getJobResult(jobId));
        assertTrue(exception.getMessage().contains("Node has been added before."));
    }

    /**
     * A merge reports how far it got, and that is what the caller is shown while it waits - the merge itself is the
     * only thing which knows what it is doing.
     */
    @Test
    void testTheMergeReportsWhatItIsDoing() {
        CountDownLatch progressReported = new CountDownLatch(1);
        CountDownLatch releaseMerge = new CountDownLatch(1);
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenAnswer(invocation -> {
            DocumentsChapterMergeService.ProgressReporter reporter = invocation.getArgument(1);
            reporter.report("Merged workitem 'EL-42'");
            progressReported.countDown();
            releaseMerge.await();
            return MergeResult.builder().success(true).build();
        });

        String jobId = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);

        try {
            await().atMost(Duration.ofSeconds(10)).until(() -> progressReported.getCount() == 0);
            assertEquals("Merged workitem 'EL-42'", jobsService.getJobState(jobId).progressMessage());
        } finally {
            releaseMerge.countDown();
        }
    }

    /**
     * A merge thread carries no subject of its own, and a call of nobody is answered by Polarion with unresolvable
     * objects - a document of an existing project then reads as "Project '...' not found".
     */
    @Test
    @SuppressWarnings("unchecked")
    void testTheMergeRunsAsTheUserWhoStartedIt() {
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenReturn(MergeResult.builder().success(true).build());

        String jobId = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);

        awaitDone(jobId);
        verify(securityService).doAsUser(eq(userSubject), any(PrivilegedAction.class));
    }

    /**
     * A merge started by a call which had no subject runs as it is, the way it would have run had it not been
     * handed to a background thread at all.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testAMergeStartedWithoutASubjectRunsAsItIs() {
        when(polarionService.getCurrentSubject()).thenReturn(null);
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenReturn(MergeResult.builder().success(true).build());

        String jobId = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);

        awaitDone(jobId);
        verify(securityService, never()).doAsUser(any(), any(PrivilegedAction.class));
        verify(securityService, never()).logout(any());
    }

    /**
     * The documents cache is keyed by the user of the request which starts the merge, and the servlet container
     * recycles the request object as soon as the response is written - so it is evicted here, not on the merge
     * thread.
     */
    @Test
    void testTheDocumentsCacheIsEvictedWhileTheRequestIsStillAlive() {
        CountDownLatch releaseMerge = new CountDownLatch(1);
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenAnswer(invocation -> {
            releaseMerge.await();
            return MergeResult.builder().success(true).build();
        });
        ChapterMergeParams params = params();

        try {
            jobsService.startJob(params, TIMEOUT_IN_MINUTES);

            verify(polarionService).evictDocumentsCache(params.getSourceDocument(), params.getTargetDocument());
        } finally {
            releaseMerge.countDown();
        }
    }

    /**
     * A call which authenticated itself got a session of its own, which the logout filter would end with the
     * response of the request which started the merge - long before the merge has finished. That request asked for
     * the session to be kept, so it is this merge which ends it.
     */
    @Test
    void testAMergeEndsTheSessionItWasHandedToKeep() {
        when(requestAttributes.getAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(Boolean.TRUE);
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenReturn(MergeResult.builder().success(true).build());

        String jobId = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);

        awaitDone(jobId);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> verify(securityService).logout(userSubject));
    }

    @Test
    void testAFailedMergeEndsThatSessionToo() {
        when(requestAttributes.getAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(Boolean.TRUE);
        doThrow(new IllegalStateException("boom")).when(documentsChapterMergeService).mergeChapter(any(), any(), any());

        String jobId = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);

        awaitDone(jobId);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> verify(securityService).logout(userSubject));
    }

    /**
     * A call authenticated by an XSRF token shares the session of the Polarion UI it was made from, which is not
     * this merge's to end.
     */
    @Test
    void testAMergeNeverEndsTheSessionOfTheUiItWasStartedFrom() {
        when(requestAttributes.getAttribute(LogoutFilter.XSRF_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(Boolean.TRUE);
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenReturn(MergeResult.builder().success(true).build());

        String jobId = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);

        awaitDone(jobId);
        verify(securityService, never()).logout(any());
    }

    @Test
    void testAMergeStartedOutsideOfARequestEndsNoSession() {
        RequestContextHolder.resetRequestAttributes();
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenReturn(MergeResult.builder().success(true).build());

        String jobId = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);

        awaitDone(jobId);
        verify(securityService, never()).logout(any());
    }

    /**
     * A merge holds a thread for as long as it takes, and a caller can ask for them faster than they finish. What
     * the server takes at once is bounded, and a merge beyond that bound is refused rather than started.
     */
    @Test
    void testAMergeBeyondWhatTheServerTakesAtOnceIsRefused() {
        CountDownLatch releaseMerges = new CountDownLatch(1);
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenAnswer(invocation -> {
            releaseMerges.await();
            return MergeResult.builder().success(true).build();
        });

        try {
            QueueFullException refused = assertThrows(QueueFullException.class, () -> {
                // one more than the merges which run and the merges which wait for their turn
                for (int merge = 0; merge <= ChapterMergeJobsService.CONCURRENT_MERGES + ChapterMergeJobsService.QUEUED_MERGES; merge++) {
                    jobsService.startJob(params(), TIMEOUT_IN_MINUTES);
                }
            });

            assertTrue(refused.getMessage().contains("Too many chapter merges"));
            // the merges which were taken are running or waiting, and the refused one left nothing behind
            assertEquals(ChapterMergeJobsService.CONCURRENT_MERGES + ChapterMergeJobsService.QUEUED_MERGES, jobsService.getAllJobsStates().size());
        } finally {
            releaseMerges.countDown();
        }
    }

    /**
     * A merge which runs longer than it may is asked to stop, and stops between two work items - before it has put
     * anything into the target document, so the document is left as it was and the caller is told so.
     */
    @Test
    void testAMergeWhichRunsTooLongIsAskedToStop() {
        CountDownLatch releaseMerge = new CountDownLatch(1);
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenAnswer(invocation -> {
            BooleanSupplier abortRequested = invocation.getArgument(2);
            // what the merge does between two work items
            while (!abortRequested.getAsBoolean()) {
                Thread.sleep(10);
            }
            releaseMerge.countDown();
            throw new CancellationException("Chapter merge was asked to stop before it merged the whole chapter");
        });

        String jobId = jobsService.startJob(params(), 0);

        await().atMost(Duration.ofSeconds(10)).until(() -> releaseMerge.getCount() == 0);
        awaitDone(jobId);
        JobState jobState = jobsService.getJobState(jobId);
        assertTrue(jobState.isFailed());
        assertEquals("Timeout after 0 min", jobState.errorMessage());
    }

    /**
     * A merge which has run out of time but is still writing is still running, and is not reported as failed: its
     * caller would otherwise be told that nothing was merged while the merge is about to change their document.
     */
    @Test
    void testAMergeWhichHasNotStoppedYetIsStillRunning() {
        CountDownLatch askedToStop = new CountDownLatch(1);
        CountDownLatch releaseMerge = new CountDownLatch(1);
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenAnswer(invocation -> {
            BooleanSupplier abortRequested = invocation.getArgument(2);
            while (!abortRequested.getAsBoolean()) {
                Thread.sleep(10);
            }
            // a merge which is past its last work item: what is left is the write, which it goes through with
            askedToStop.countDown();
            releaseMerge.await();
            return MergeResult.builder().success(true).build();
        });

        String jobId = jobsService.startJob(params(), 0);

        try {
            await().atMost(Duration.ofSeconds(10)).until(() -> askedToStop.getCount() == 0);
            JobState jobState = jobsService.getJobState(jobId);
            assertFalse(jobState.isDone());
            assertFalse(jobState.isFailed());
            assertEquals(Optional.empty(), jobsService.getJobResult(jobId));
        } finally {
            releaseMerge.countDown();
        }

        // ...and the result it went through with is the one its caller is given
        awaitDone(jobId);
        assertTrue(jobsService.getJobResult(jobId).orElseThrow().isSuccess());
    }

    /**
     * A merge names the documents it works on and reports which work items it created, so it belongs to the user
     * who started it - to everybody else it is a job which doesn't exist.
     */
    @Test
    void testTheMergesOfAnotherUserAreNeitherListedNorReadable() {
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenReturn(MergeResult.builder().success(true).build());
        String jobId = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);
        awaitDone(jobId);

        when(polarionService.getCurrentUser()).thenReturn("someone.else");

        assertThrows(NoSuchElementException.class, () -> jobsService.getJobState(jobId));
        assertThrows(NoSuchElementException.class, () -> jobsService.getJobResult(jobId));
        assertTrue(jobsService.getAllJobsStates().isEmpty());
    }

    @Test
    void testAnUnknownMergeIsNotFound() {
        assertThrows(NoSuchElementException.class, () -> jobsService.getJobState("no-such-job"));
    }

    @Test
    void testAllMergesOfTheCurrentUserAreListed() {
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenReturn(MergeResult.builder().success(true).build());
        String first = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);
        String second = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);
        awaitDone(first);
        awaitDone(second);

        Map<String, JobState> states = jobsService.getAllJobsStates();

        assertEquals(2, states.size());
        assertTrue(states.get(first).isDone());
        assertTrue(states.get(second).isDone());
    }

    /**
     * The cleaner runs on a thread of its own, so it can drop a merge while the listing is being built. That merge
     * is gone from the listing or in it, but it never takes the whole listing with it - which is what looking each
     * job up by its ID would do, since a job this service doesn't know is a 404.
     */
    @Test
    void testTheListingSurvivesAMergeDroppedWhileItIsBuilt() {
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenReturn(MergeResult.builder().success(true).build());
        String jobId = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);
        awaitDone(jobId);

        // the cleaner, running while the listing goes through the merges it holds
        AtomicBoolean cleanerDue = new AtomicBoolean(true);
        when(polarionService.getCurrentUser()).thenAnswer(invocation -> {
            if (cleanerDue.compareAndSet(true, false)) {
                ChapterMergeJobsService.cleanupExpiredJobs(0);
            }
            return USER;
        });

        Map<String, JobState> states = assertDoesNotThrow(() -> jobsService.getAllJobsStates());

        // whether the dropped merge is still in the listing depends on where the cleaner caught it; what the listing
        // must not do is report it as a merge of nobody
        assertTrue(states.isEmpty() || states.get(jobId).isDone());
    }

    /**
     * A result is read right after its merge is over, so nothing is kept for the sake of keeping it. A merge which
     * is still running is kept however long it takes.
     */
    @Test
    void testOnlyTheResultsOfFinishedMergesExpire() {
        CountDownLatch releaseMerge = new CountDownLatch(1);
        when(documentsChapterMergeService.mergeChapter(any(), any(), any()))
                .thenReturn(MergeResult.builder().success(true).build())
                .thenAnswer(invocation -> {
                    releaseMerge.await();
                    return MergeResult.builder().success(true).build();
                });
        String finished = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);
        String running = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);
        awaitDone(finished);

        try {
            ChapterMergeJobsService.cleanupExpiredJobs(0);

            assertThrows(NoSuchElementException.class, () -> jobsService.getJobState(finished));
            assertNotNull(jobsService.getJobState(running));
        } finally {
            releaseMerge.countDown();
        }
    }

    /**
     * A merge which took longer than its result is kept would be expired the moment it finished if its age were
     * counted from the moment it was asked for - and that is the merge whose result took the longest to produce.
     */
    @Test
    void testAResultAgesFromTheMomentItsMergeWasOver() {
        Instant now = Instant.now();
        ChapterMergeJobsService.JobDetails longRunningMerge = finishedJob(now.minus(Duration.ofMinutes(50)), now);

        assertFalse(ChapterMergeJobsService.expired(longRunningMerge, 30, now));
        assertFalse(ChapterMergeJobsService.expired(longRunningMerge, 30, now.plus(Duration.ofMinutes(29))));
        assertTrue(ChapterMergeJobsService.expired(longRunningMerge, 30, now.plus(Duration.ofMinutes(31))));
    }

    @Test
    void testAMergeWhichIsStillRunningNeverExpires() {
        Instant now = Instant.now();
        ChapterMergeJobsService.JobDetails runningMerge = ChapterMergeJobsService.JobDetails.builder()
                .future(new CompletableFuture<>())
                .user(USER)
                .startingTime(now.minus(Duration.ofHours(5)))
                .finishTime(new AtomicReference<>())
                .progressMessage(new AtomicReference<>())
                .build();

        assertFalse(ChapterMergeJobsService.expired(runningMerge, 30, now));
    }

    /**
     * A merge whose future is done but whose completion has not been recorded yet has just this moment finished.
     */
    @Test
    void testAMergeWhichJustFinishedIsNotExpiredBeforeItsFinishIsRecorded() {
        Instant now = Instant.now();
        ChapterMergeJobsService.JobDetails justFinished = finishedJob(now.minus(Duration.ofHours(5)), null);

        assertFalse(ChapterMergeJobsService.expired(justFinished, 0, now));
    }

    @Test
    void testAResultIsKeptAsLongAsItsTimeoutSays() {
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenReturn(MergeResult.builder().success(true).build());
        String jobId = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);
        awaitDone(jobId);

        ChapterMergeJobsService.cleanupExpiredJobs(30);

        assertNotNull(jobsService.getJobState(jobId));
    }

    /**
     * A future wraps what was thrown, and the wrapper says only which class it was, which tells the reader nothing
     * about what went wrong.
     */
    @Test
    void testAFailureIsDescribedByWhatWasActuallyThrown() {
        Throwable thrown = new ExecutionException(new IllegalStateException("Project 'ELIBRARY' not found"));

        assertEquals("Project 'ELIBRARY' not found", ChapterMergeJobsService.describeFailure(thrown));
        assertTrue(ChapterMergeJobsService.rootReason(thrown) instanceof IllegalStateException);
    }

    @Test
    void testAFailureWithoutAMessageIsDescribedByItsClass() {
        assertEquals(TimeoutException.class.getName(), ChapterMergeJobsService.describeFailure(new TimeoutException()));
    }

    /**
     * Every merge started here is tracked separately, so the result of one merge cannot be read as the result of
     * another one.
     */
    @Test
    void testEachMergeIsTrackedUnderAnIdOfItsOwn() {
        ConcurrentLinkedQueue<MergeResult> results = new ConcurrentLinkedQueue<>(List.of(
                MergeResult.builder().success(true).build(),
                MergeResult.builder().success(false).build()));
        AtomicReference<MergeResult> first = new AtomicReference<>();
        when(documentsChapterMergeService.mergeChapter(any(), any(), any())).thenAnswer(invocation -> {
            MergeResult result = results.poll();
            first.compareAndSet(null, result);
            return result;
        });

        String firstJob = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);
        awaitDone(firstJob);
        String secondJob = jobsService.startJob(params(), TIMEOUT_IN_MINUTES);
        awaitDone(secondJob);

        assertEquals(first.get(), jobsService.getJobResult(firstJob).orElseThrow());
        assertNotEquals(jobsService.getJobResult(secondJob).orElseThrow(), jobsService.getJobResult(firstJob).orElseThrow());
    }

    private ChapterMergeJobsService.JobDetails finishedJob(Instant startingTime, Instant finishTime) {
        return ChapterMergeJobsService.JobDetails.builder()
                .future(CompletableFuture.completedFuture(MergeResult.builder().success(true).build()))
                .user(USER)
                .startingTime(startingTime)
                .finishTime(new AtomicReference<>(finishTime))
                .progressMessage(new AtomicReference<>())
                .build();
    }

    private void awaitDone(String jobId) {
        await().atMost(Duration.ofSeconds(10)).until(() -> jobsService.getJobState(jobId).isDone());
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
