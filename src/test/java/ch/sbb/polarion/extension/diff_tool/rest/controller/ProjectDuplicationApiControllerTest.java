package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationJobInfo;
import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationRequest;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.job.ProjectDuplicationJobScheduler;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.jobs.IJobService;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
class ProjectDuplicationApiControllerTest {

    private PolarionService polarionService;
    private ProjectDuplicationJobScheduler scheduler;
    private ProjectDuplicationApiController controller;

    @BeforeEach
    void setUp() throws Exception {
        // Stub PlatformContext.getPlatform() so PolarionService no-arg ctor doesn't blow up.
        IPlatform platform = mock(IPlatform.class);
        lenient().when(platform.lookupService(IProjectService.class)).thenReturn(mock(IProjectService.class));
        lenient().when(platform.lookupService(ITrackerService.class)).thenReturn(mock(ITrackerService.class));
        lenient().when(platform.lookupService(IRepositoryService.class)).thenReturn(mock(IRepositoryService.class));
        lenient().when(platform.lookupService(ISecurityService.class)).thenReturn(mock(ISecurityService.class));
        lenient().when(platform.lookupService(IPlatformService.class)).thenReturn(mock(IPlatformService.class));
        lenient().when(platform.lookupService(IJobService.class)).thenReturn(mock(IJobService.class));
        when(PlatformContext.getPlatform()).thenReturn(platform);

        controller = new ProjectDuplicationApiController();

        // Replace inherited fields with our mocks for assertion-friendly behaviour.
        polarionService = mock(PolarionService.class);
        scheduler = mock(ProjectDuplicationJobScheduler.class);
        setField("polarionService", polarionService);
        setField("scheduler", scheduler);

        // callPrivileged executes the callable directly in tests
        when(polarionService.callPrivileged(any(Callable.class)))
                .thenAnswer(inv -> ((Callable<?>) inv.getArgument(0)).call());
    }

    private void setField(String name, Object value) throws Exception {
        Field f = ProjectDuplicationInternalController.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, value);
    }

    @Test
    void listProjectsDelegatesViaCallPrivileged() throws Exception {
        when(polarionService.getProjects()).thenReturn(List.of());

        controller.listProjects();

        verify(polarionService).callPrivileged(any(Callable.class));
        verify(polarionService).getProjects();
    }

    @Test
    void duplicateProjectDelegatesViaCallPrivileged() throws Exception {
        when(polarionService.hasSufficientPermissions()).thenReturn(true);
        DuplicationJobInfo info = DuplicationJobInfo.builder().jobId("J-API").build();
        when(scheduler.schedule(any())).thenReturn(info);
        DuplicationRequest req = DuplicationRequest.builder()
                .sourceProjectId("s").targetProjectId("t").location("/l").trackerPrefix("P").build();

        DuplicationJobInfo result = controller.duplicateProject(req);

        assertSame(info, result);
        verify(polarionService).callPrivileged(any(Callable.class));
    }

    @Test
    void listDuplicationJobsDelegatesViaCallPrivileged() throws Exception {
        when(polarionService.hasSufficientPermissions()).thenReturn(true);
        when(scheduler.listJobs()).thenReturn(List.of());

        controller.listDuplicationJobs();

        verify(polarionService).callPrivileged(any(Callable.class));
        verify(scheduler).listJobs();
    }
}
