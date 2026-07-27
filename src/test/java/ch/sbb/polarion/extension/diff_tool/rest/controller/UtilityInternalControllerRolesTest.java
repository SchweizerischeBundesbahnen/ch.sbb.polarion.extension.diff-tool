package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.settings.RolesModel;
import ch.sbb.polarion.extension.diff_tool.util.RolesUtils;
import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Covers the /roles endpoint, which replaces the RolesUtils scriptlets that the Merge Authorization JSP
 * rendered its checkbox lists from.
 * <p>
 * RolesUtils itself is stubbed rather than driven through mocked Polarion services: it is a
 * {@code @UtilityClass} that resolves ISecurityService/IProjectService once into static fields, so
 * whichever test class initializes it first owns those references for the rest of the JVM - driving it
 * for real here would make this class pass alone and fail in a full run. Its own resolution logic is
 * covered by {@code RolesUtilsTest}; what matters here is how the controller shapes the response. The
 * PlatformContext mock is still needed so the class can be initialized for instrumentation.
 */
@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
@CurrentContextConfig("diff-tool")
class UtilityInternalControllerRolesTest {

    private UtilityInternalController controller;

    @BeforeEach
    void setUp() {
        IPlatform platform = mock(IPlatform.class);
        when(PlatformContext.getPlatform()).thenReturn(platform);
        lenient().when(platform.lookupService(ISecurityService.class)).thenReturn(mock(ISecurityService.class));
        lenient().when(platform.lookupService(IProjectService.class)).thenReturn(mock(IProjectService.class));
        lenient().when(platform.lookupService(ITrackerService.class)).thenReturn(mock(ITrackerService.class));
        lenient().when(platform.lookupService(IPlatformService.class)).thenReturn(mock(IPlatformService.class));
        lenient().when(platform.lookupService(IRepositoryService.class)).thenReturn(mock(IRepositoryService.class));

        controller = new UtilityInternalController();
    }

    @Test
    void returnsBothGlobalAndProjectRolesForAProjectScope() {
        try (MockedStatic<RolesUtils> rolesUtils = mockStatic(RolesUtils.class)) {
            rolesUtils.when(RolesUtils::getGlobalRoles).thenReturn(List.of("admin", "developer"));
            rolesUtils.when(() -> RolesUtils.getProjectRoles("project/elibrary/")).thenReturn(Set.of("project_admin"));

            RolesModel roles = controller.getRoles("project/elibrary/");

            assertEquals(List.of("admin", "developer"), roles.getGlobalRoles());
            assertEquals(List.of("project_admin"), roles.getProjectRoles());
        }
    }

    @Test
    void returnsAnEmptyProjectRoleListForAScopeWithoutAProject() {
        try (MockedStatic<RolesUtils> rolesUtils = mockStatic(RolesUtils.class)) {
            rolesUtils.when(RolesUtils::getGlobalRoles).thenReturn(List.of("admin"));
            rolesUtils.when(() -> RolesUtils.getProjectRoles("")).thenReturn(Set.of());

            RolesModel roles = controller.getRoles("");

            assertEquals(List.of("admin"), roles.getGlobalRoles());
            assertTrue(roles.getProjectRoles().isEmpty());
        }
    }

    @Test
    void toleratesAMissingScopeParameter() {
        // The scope query parameter is absent when the page is opened in the repository scope.
        try (MockedStatic<RolesUtils> rolesUtils = mockStatic(RolesUtils.class)) {
            rolesUtils.when(RolesUtils::getGlobalRoles).thenReturn(List.of("admin"));
            rolesUtils.when(() -> RolesUtils.getProjectRoles(null)).thenReturn(Set.of());

            RolesModel roles = controller.getRoles(null);

            assertEquals(List.of("admin"), roles.getGlobalRoles());
            assertTrue(roles.getProjectRoles().isEmpty());
        }
    }
}
