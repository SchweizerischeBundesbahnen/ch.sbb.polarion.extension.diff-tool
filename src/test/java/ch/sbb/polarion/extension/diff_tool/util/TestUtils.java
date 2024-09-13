package ch.sbb.polarion.extension.diff_tool.util;

import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@UtilityClass
public class TestUtils {

    public PolarionService mockPolarionService(ITrackerService trackerService, IProjectService projectService, ISecurityService securityService) {
        PolarionService polarionService = mock(PolarionService.class, withSettings().useConstructor(
                trackerService == null ? mock(ITrackerService.class) : trackerService,
                projectService == null ? mock(IProjectService.class) : projectService,
                securityService == null ? mock(ISecurityService.class) : securityService,
                mock(IPlatformService.class),
                mock(IRepositoryService.class)
        ));
        lenient().when(polarionService.getGeneralFields(any())).thenCallRealMethod();
        lenient().when(polarionService.getWorkItem(any(), any())).thenCallRealMethod();
        lenient().when(polarionService.getAttachment(any())).thenCallRealMethod();
        lenient().when(polarionService.getProjectService()).thenCallRealMethod();
        lenient().when(polarionService.getTrackerService()).thenCallRealMethod();
        mockSecureCalls(polarionService);
        return polarionService;
    }

    public MockedConstruction<PolarionService> mockPolarionServiceConstruction() {
        return mockConstruction(PolarionService.class,
                (mock, context) -> TestUtils.mockSecureCalls(mock));
    }

    public void mockSecureCalls(PolarionService polarionService) {
        ArgumentCaptor<Runnable> argument = ArgumentCaptor.forClass(Runnable.class);
        lenient().doAnswer(invocation -> {
            argument.getValue().run();
            return null;
        }).when(polarionService).callPrivileged(argument.capture());
    }

    public @NotNull String removeLineEndings(@NotNull String input) {
        return input.replaceAll("\\r\\n|\\r|\\n", "");
    }
}
