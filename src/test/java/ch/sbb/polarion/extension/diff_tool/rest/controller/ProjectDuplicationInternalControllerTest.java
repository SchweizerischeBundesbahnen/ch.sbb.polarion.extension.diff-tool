package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationJobInfo;
import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationRequest;
import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.ProjectInfo;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.job.ProjectDuplicationJobScheduler;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.jobs.IJobService;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectDuplicationInternalControllerTest {

    private PolarionService polarionService;
    private ProjectDuplicationJobScheduler scheduler;
    private ProjectDuplicationInternalController controller;

    @BeforeEach
    void setUp() {
        polarionService = mock(PolarionService.class);
        scheduler = mock(ProjectDuplicationJobScheduler.class);
        controller = new ProjectDuplicationInternalController(polarionService, scheduler);
    }

    private DuplicationRequest sampleRequest() {
        return DuplicationRequest.builder()
                .sourceProjectId("src").targetProjectId("dst")
                .location("/loc").trackerPrefix("DST").build();
    }

    @Test
    void noArgConstructorLooksUpServicesViaPlatformContext() {
        IPlatform platform = mock(IPlatform.class);
        when(platform.lookupService(IProjectService.class)).thenReturn(mock(IProjectService.class));
        when(platform.lookupService(ITrackerService.class)).thenReturn(mock(ITrackerService.class));
        when(platform.lookupService(IRepositoryService.class)).thenReturn(mock(IRepositoryService.class));
        when(platform.lookupService(ISecurityService.class)).thenReturn(mock(ISecurityService.class));
        when(platform.lookupService(IPlatformService.class)).thenReturn(mock(IPlatformService.class));
        when(platform.lookupService(IJobService.class)).thenReturn(mock(IJobService.class));
        try (MockedStatic<PlatformContext> mocked = mockStatic(PlatformContext.class)) {
            mocked.when(PlatformContext::getPlatform).thenReturn(platform);
            assertNotNull(new ProjectDuplicationInternalController());
        }
    }

    @Test
    void listProjectsReturnsSortedByName() {
        IProject a = mock(IProject.class);
        when(a.getId()).thenReturn("a"); when(a.getName()).thenReturn("Bravo");
        IProject b = mock(IProject.class);
        when(b.getId()).thenReturn("b"); when(b.getName()).thenReturn("alpha");
        when(polarionService.getProjects()).thenReturn(List.of(a, b));

        List<ProjectInfo> projects = controller.listProjects();

        assertEquals(2, projects.size());
        assertEquals("alpha", projects.get(0).getName());
        assertEquals("Bravo", projects.get(1).getName());
    }

    @Test
    void listProjectsFallsBackToIdWhenNameIsNull() {
        IProject withName = mock(IProject.class);
        when(withName.getId()).thenReturn("x"); when(withName.getName()).thenReturn("Zeta");
        IProject noName = mock(IProject.class);
        when(noName.getId()).thenReturn("apple"); when(noName.getName()).thenReturn(null);
        when(polarionService.getProjects()).thenReturn(List.of(withName, noName));

        List<ProjectInfo> projects = controller.listProjects();

        assertEquals(2, projects.size());
        // 'apple' (from id, since name is null) sorts before 'Zeta'
        assertEquals("apple", projects.get(0).getId());
        assertEquals("x", projects.get(1).getId());
    }

    @Test
    void listDuplicationJobsForbiddenForNonAdmin() {
        when(polarionService.hasSufficientPermissions()).thenReturn(false);

        assertThrows(ForbiddenException.class, controller::listDuplicationJobs);
        verify(scheduler, never()).listJobs();
    }

    @Test
    void listDuplicationJobsReturnsListWhenAdmin() {
        when(polarionService.hasSufficientPermissions()).thenReturn(true);
        DuplicationJobInfo info = DuplicationJobInfo.builder().jobId("J-1").build();
        when(scheduler.listJobs()).thenReturn(List.of(info));

        List<DuplicationJobInfo> jobs = controller.listDuplicationJobs();

        assertEquals(1, jobs.size());
        assertEquals("J-1", jobs.get(0).getJobId());
    }

    @Test
    void duplicateProjectRejectsNullBody() {
        assertThrows(BadRequestException.class, () -> controller.duplicateProject(null));
        verify(scheduler, never()).schedule(any());
    }

    @Test
    void duplicateProjectForbiddenForNonAdmin() {
        when(polarionService.hasSufficientPermissions()).thenReturn(false);
        DuplicationRequest request = sampleRequest();

        assertThrows(ForbiddenException.class, () -> controller.duplicateProject(request));
        verify(scheduler, never()).schedule(any());
    }

    @Test
    void duplicateProjectSchedulesWhenAdmin() {
        when(polarionService.hasSufficientPermissions()).thenReturn(true);
        DuplicationJobInfo info = DuplicationJobInfo.builder().jobId("J-1").jobName("test").logUrl("/polarion/job-report?jobId=J-1").build();
        when(scheduler.schedule(any())).thenReturn(info);

        DuplicationJobInfo result = controller.duplicateProject(sampleRequest());

        assertEquals("J-1", result.getJobId());
        verify(scheduler).schedule(any());
    }

    @Test
    void duplicateProjectMapsValidationErrorsTo400() {
        when(polarionService.hasSufficientPermissions()).thenReturn(true);
        when(scheduler.schedule(any())).thenThrow(new IllegalArgumentException("invalid"));
        DuplicationRequest request = sampleRequest();

        assertThrows(BadRequestException.class, () -> controller.duplicateProject(request));
    }
}
