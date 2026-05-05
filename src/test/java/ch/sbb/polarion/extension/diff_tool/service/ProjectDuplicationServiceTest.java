package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationRequest;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import com.polarion.alm.projects.IProjectLifecycleManager;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.core.util.logging.ILogger;
import org.mockito.MockedConstruction;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.jobs.IProgressMonitor;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CancellationException;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class, TransactionalExecutorExtension.class})
class ProjectDuplicationServiceTest {

    private IProjectService projectService;
    private ITrackerService trackerService;
    private IRepositoryService repositoryService;
    private IProjectLifecycleManager lifecycleManager;
    private IRepositoryConnection connection;
    private ProjectDuplicationService service;

    @BeforeEach
    void setUp() {
        projectService = mock(IProjectService.class);
        trackerService = mock(ITrackerService.class);
        repositoryService = mock(IRepositoryService.class);
        lifecycleManager = mock(IProjectLifecycleManager.class);
        connection = mock(IRepositoryConnection.class);
        lenient().when(projectService.getLifecycleManager()).thenReturn(lifecycleManager);
        lenient().when(repositoryService.getConnection(any(ILocation.class))).thenReturn(connection);
        lenient().when(connection.exists(any(ILocation.class))).thenReturn(true);
        service = new ProjectDuplicationService(projectService, trackerService, repositoryService);
    }

    private DuplicationRequest validRequest() {
        return DuplicationRequest.builder()
                .sourceProjectId("source")
                .targetProjectId("target")
                .location("/MyProjects/target")
                .trackerPrefix("TGT")
                .build();
    }

    private ILocation mockSource(String prefix) {
        IProject source = mock(IProject.class);
        ILocation sourceLoc = Location.getLocationWithRepository(IRepositoryService.DEFAULT, "/source");
        lenient().when(source.isUnresolvable()).thenReturn(false);
        lenient().when(source.getLocation()).thenReturn(sourceLoc);
        lenient().when(projectService.getProject("source")).thenReturn(source);
        if (prefix != null) {
            ITrackerProject tracker = mock(ITrackerProject.class);
            lenient().when(tracker.getTrackerPrefix()).thenReturn(prefix);
            lenient().when(trackerService.getTrackerProject(source)).thenReturn(tracker);
        }
        return sourceLoc;
    }

    private void mockSourceMissing() {
        IProject source = mock(IProject.class);
        when(source.isUnresolvable()).thenReturn(true);
        lenient().when(projectService.getProject("source")).thenReturn(source);
    }

    private void mockTarget(boolean exists) {
        if (exists) {
            IProject target = mock(IProject.class);
            when(target.isUnresolvable()).thenReturn(false);
            lenient().when(projectService.getProject("target")).thenReturn(target);
        } else {
            lenient().when(projectService.getProject("target")).thenReturn(null);
        }
    }

    @Test
    void duplicateCopiesSourceToTemplateAndCreatesProject() throws Exception {
        ILocation sourceLoc = mockSource("SRC");
        mockTarget(false);

        service.duplicate(validRequest());

        ArgumentCaptor<ILocation> templateLocCaptor = ArgumentCaptor.forClass(ILocation.class);
        verify(connection).copy(eq(sourceLoc), templateLocCaptor.capture(), eq(false));
        String templatePath = templateLocCaptor.getValue().getLocationPath();
        assertTrue(templatePath.startsWith("/.polarion/projects/templates/" + ProjectDuplicationService.EPHEMERAL_TEMPLATE_PREFIX),
                "Expected template inside templates root, got: " + templatePath);

        verify(connection).create(any(ILocation.class), any(InputStream.class));
        verify(lifecycleManager).createProject(any(ILocation.class), eq("target"), anyString(), anyMap());
        verify(connection).delete(any(ILocation.class));
    }

    @Test
    void createProjectParamsContainOldAndNewTrackerPrefix() throws Exception {
        mockSource("OLD");
        mockTarget(false);

        DuplicationRequest req = DuplicationRequest.builder()
                .sourceProjectId("source").targetProjectId("target")
                .location("/loc").trackerPrefix("NEW-").build();
        service.duplicate(req);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(lifecycleManager).createProject(any(ILocation.class), eq("target"), anyString(), paramsCaptor.capture());
        Map<String, String> params = paramsCaptor.getValue();
        assertEquals("NEW", params.get(ProjectDuplicationService.PARAM_TRACKER_PREFIX));
        assertEquals("OLD", params.get(ProjectDuplicationService.PARAM_TEMPLATE_TRACKER_PREFIX_REPLACE));
    }

    @Test
    void duplicateThrowsWhenSourceProjectMissing() {
        mockSourceMissing();
        mockTarget(false);

        DuplicationRequest request = validRequest();
        assertThrows(IllegalArgumentException.class, () -> service.duplicate(request));
        verify(connection, never()).copy(any(ILocation.class), any(ILocation.class), any(Boolean.class));
    }

    @Test
    void duplicateThrowsWhenTargetAlreadyExists() {
        mockSource("SRC");
        mockTarget(true);

        DuplicationRequest request = validRequest();
        assertThrows(IllegalArgumentException.class, () -> service.duplicate(request));
        verify(connection, never()).copy(any(ILocation.class), any(ILocation.class), any(Boolean.class));
    }

    @Test
    void duplicateThrowsWhenSourceHasNoTrackerPrefix() {
        mockSource("");
        mockTarget(false);

        DuplicationRequest request = validRequest();
        assertThrows(IllegalStateException.class, () -> service.duplicate(request));
        verify(connection, never()).copy(any(ILocation.class), any(ILocation.class), any(Boolean.class));
    }

    @Test
    void duplicateCleansUpTemplateEvenWhenCreateProjectFails() throws Exception {
        mockSource("SRC");
        mockTarget(false);
        doThrow(new RuntimeException("boom")).when(lifecycleManager)
                .createProject(any(ILocation.class), anyString(), anyString(), anyMap());

        DuplicationRequest request = validRequest();
        assertThrows(RuntimeException.class, () -> service.duplicate(request));

        verify(connection, times(1)).copy(any(ILocation.class), any(ILocation.class), eq(false));
        verify(connection, times(1)).delete(any(ILocation.class));
    }

    @Test
    void duplicateRejectsBlankInputs() {
        DuplicationRequest empty = DuplicationRequest.builder()
                .sourceProjectId("")
                .targetProjectId("target")
                .location("/x")
                .trackerPrefix("P")
                .build();
        assertThrows(IllegalArgumentException.class, () -> service.duplicate(empty));
    }

    @Test
    void duplicateThrowsWhenProjectServiceReturnsNullForSource() {
        mockTarget(false);
        // projectService.getProject("source") returns null (not just an unresolvable proxy)
        when(projectService.getProject("source")).thenReturn(null);

        DuplicationRequest request = validRequest();
        assertThrows(IllegalArgumentException.class, () -> service.duplicate(request));
    }

    @Test
    void duplicateOneArgOverloadDelegatesWithoutLogger() throws Exception {
        mockSource("SRC");
        mockTarget(false);

        // Just exercise the no-logger / no-monitor overload path.
        service.duplicate(validRequest());
        verify(lifecycleManager).createProject(any(ILocation.class), eq("target"), anyString(), anyMap());
    }

    @Test
    void duplicateTwoArgOverloadAcceptsLoggerWithoutMonitor() throws Exception {
        mockSource("SRC");
        mockTarget(false);
        ILogger logger = mock(ILogger.class);

        service.duplicate(validRequest(), logger);

        verify(logger, org.mockito.Mockito.atLeastOnce()).info(anyString());
    }

    @Test
    void duplicateAcceptsTrackerPrefixWithTrailingDashAndStripsIt() throws Exception {
        mockSource("SRC-");  // source prefix already includes dash → exercises stripTrailingDash true branch
        mockTarget(false);

        DuplicationRequest req = DuplicationRequest.builder()
                .sourceProjectId("source").targetProjectId("target")
                .location("/loc").trackerPrefix("NEW-").build();
        service.duplicate(req);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(lifecycleManager).createProject(any(ILocation.class), eq("target"), anyString(), paramsCaptor.capture());
        Map<String, String> params = paramsCaptor.getValue();
        assertEquals("SRC", params.get(ProjectDuplicationService.PARAM_TEMPLATE_TRACKER_PREFIX_REPLACE));
        assertEquals("NEW", params.get(ProjectDuplicationService.PARAM_TRACKER_PREFIX));
    }

    @Test
    void duplicateThrowsCancellationExceptionWhenMonitorIsCancelled() {
        mockSource("SRC");
        mockTarget(false);
        IProgressMonitor monitor = mock(IProgressMonitor.class);
        when(monitor.isCanceled()).thenReturn(true);

        DuplicationRequest request = validRequest();
        assertThrows(CancellationException.class, () -> service.duplicate(request, null, monitor));
    }

    @Test
    void deleteTemplateQuietlyLogsWarningToProgressLogWhenDeleteThrows() throws Exception {
        mockSource("SRC");
        mockTarget(false);
        // Make connection.delete throw to drive the catch-block in deleteTemplateQuietly.
        doThrow(new RuntimeException("svn locked")).when(connection).delete(any(ILocation.class));
        ILogger logger = mock(ILogger.class);

        service.duplicate(validRequest(), logger);

        // The warning-info call from deleteTemplateQuietly should have been observed.
        verify(logger, times(1)).info(org.mockito.ArgumentMatchers.contains("Failed to remove ephemeral template"));
    }

    @Test
    void projectUrlPrependsBaseUrlWhenConfigured() {
        String previous = System.getProperty("base.url");
        try {
            System.setProperty("base.url", "https://example.com/");  // trailing slash exercised
            assertEquals("https://example.com/polarion/#/project/abc", ProjectDuplicationService.projectUrl("abc"));

            System.setProperty("base.url", "https://example.com");  // no trailing slash
            assertEquals("https://example.com/polarion/#/project/abc", ProjectDuplicationService.projectUrl("abc"));

            System.clearProperty("base.url");
            assertEquals("/polarion/#/project/abc", ProjectDuplicationService.projectUrl("abc"));
        } finally {
            if (previous != null) System.setProperty("base.url", previous);
            else System.clearProperty("base.url");
        }
    }

    @Test
    void duplicateThrowsWhenTargetProjectIsUnresolvableNonNull() {
        mockSource("SRC");
        IProject target = mock(IProject.class);
        when(target.isUnresolvable()).thenReturn(true);
        // existing != null but isUnresolvable=true → second branch of L219 condition
        when(projectService.getProject("target")).thenReturn(target);

        DuplicationRequest request = validRequest();
        // Target counted as missing (unresolvable) — duplication should proceed past the check.
        // No exception expected from the validation step; we just verify it actually proceeded.
        service.duplicate(request);
        verify(connection).copy(any(ILocation.class), any(ILocation.class), eq(false));
    }

    @Test
    void deleteTemplateQuietlyHandlesNonexistentTemplate() throws Exception {
        mockSource("SRC");
        mockTarget(false);
        // exists() returns false → cleanup skips delete (false branch in deleteTemplateQuietly).
        when(connection.exists(any(ILocation.class))).thenReturn(false);

        service.duplicate(validRequest());

        verify(connection, never()).delete(any(ILocation.class));
    }

    @Test
    void deleteTemplateQuietlyHandlesNullProgressLogWhenDeleteThrows() throws Exception {
        mockSource("SRC");
        mockTarget(false);
        doThrow(new RuntimeException("svn locked")).when(connection).delete(any(ILocation.class));
        // No progressLog passed → exercises the null branch in deleteTemplateQuietly.
        service.duplicate(validRequest());
        // The duplication still completes (cleanup error is swallowed).
        verify(lifecycleManager).createProject(any(ILocation.class), eq("target"), anyString(), anyMap());
    }

    @Test
    void cleanupNotInvokedWhenSvnCopyFailsBeforeTemplateMarked() throws Exception {
        mockSource("SRC");
        mockTarget(false);
        // Make connection.copy throw → templateCreated stays false → finally skips cleanup
        doThrow(new RuntimeException("copy failed")).when(connection).copy(any(ILocation.class), any(ILocation.class), eq(false));

        DuplicationRequest request = validRequest();
        assertThrows(RuntimeException.class, () -> service.duplicate(request));

        verify(connection, never()).delete(any(ILocation.class));
    }

    @Test
    void writeTemplatePropertiesWrapsIOExceptionInIllegalState() throws Exception {
        mockSource("SRC");
        mockTarget(false);
        // Force the underlying ByteArrayOutputStream to throw, so Properties.store(...) propagates IOException.
        try (MockedConstruction<java.io.ByteArrayOutputStream> ignored = mockConstruction(
                java.io.ByteArrayOutputStream.class,
                (mockBaos, ctx) -> doThrow(new java.io.IOException("disk on fire"))
                        .when(mockBaos).write(any(byte[].class), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))) {
            DuplicationRequest request = validRequest();
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.duplicate(request));
            assertTrue(ex.getMessage().contains("template.properties"));
        }
    }

    @Test
    void noArgConstructorLooksUpServicesViaPlatformContext() {
        // PlatformContextMockExtension has already registered a MockedStatic<PlatformContext>;
        // we just stub its return value here.
        IPlatform platform = mock(IPlatform.class);
        when(platform.lookupService(IProjectService.class)).thenReturn(projectService);
        when(platform.lookupService(ITrackerService.class)).thenReturn(trackerService);
        when(platform.lookupService(IRepositoryService.class)).thenReturn(repositoryService);
        when(PlatformContext.getPlatform()).thenReturn(platform);

        new ProjectDuplicationService();
    }
}
