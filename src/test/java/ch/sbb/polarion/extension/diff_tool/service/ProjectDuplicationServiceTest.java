package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationRequest;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import com.polarion.alm.projects.IProjectLifecycleManager;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.platform.service.repository.IRepositoryConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

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

        assertThrows(IllegalArgumentException.class, () -> service.duplicate(validRequest()));
        verify(connection, never()).copy(any(ILocation.class), any(ILocation.class), any(Boolean.class));
    }

    @Test
    void duplicateThrowsWhenTargetAlreadyExists() {
        mockSource("SRC");
        mockTarget(true);

        assertThrows(IllegalArgumentException.class, () -> service.duplicate(validRequest()));
        verify(connection, never()).copy(any(ILocation.class), any(ILocation.class), any(Boolean.class));
    }

    @Test
    void duplicateThrowsWhenSourceHasNoTrackerPrefix() {
        mockSource("");
        mockTarget(false);

        assertThrows(IllegalStateException.class, () -> service.duplicate(validRequest()));
        verify(connection, never()).copy(any(ILocation.class), any(ILocation.class), any(Boolean.class));
    }

    @Test
    void duplicateCleansUpTemplateEvenWhenCreateProjectFails() throws Exception {
        mockSource("SRC");
        mockTarget(false);
        doThrow(new RuntimeException("boom")).when(lifecycleManager)
                .createProject(any(ILocation.class), anyString(), anyString(), anyMap());

        assertThrows(RuntimeException.class, () -> service.duplicate(validRequest()));

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
}
