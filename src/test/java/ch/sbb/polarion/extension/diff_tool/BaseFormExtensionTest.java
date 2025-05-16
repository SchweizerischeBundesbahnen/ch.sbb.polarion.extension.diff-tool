package ch.sbb.polarion.extension.diff_tool;

import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.test_extensions.CustomExtensionMock;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.UiContext;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.ui.server.forms.extensions.IFormExtensionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class, TransactionalExecutorExtension.class})
@SuppressWarnings("unused")
class BaseFormExtensionTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IFormExtensionContext formExtensionContext;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private UiContext sharedContext;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IModule module;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DiffSettings settings;

    @CustomExtensionMock
    private InternalReadOnlyTransaction transaction;

    private MockedConstruction<PolarionService> polarionServiceMockedConstruction;
    private MockedStatic<ScopeUtils> mockScopeUtils;
    private List<IProject> projects;
    private List<ILinkRoleOpt> linkRoles;

    @BeforeEach
    void setUp() {
        polarionServiceMockedConstruction = mockConstruction(PolarionService.class,
                (mock, context) -> {
                    when(mock.getProjects()).thenReturn(projects);
                    when(mock.getLinkRoles(anyString())).thenReturn(linkRoles);
                }
        );

        when(settings.getFeatureName()).thenReturn(DiffSettings.FEATURE_NAME);
        when(settings.readNames(any())).thenReturn(Set.of(SettingName.builder().id("settingId").name("settingName").build()));
        NamedSettingsRegistry.INSTANCE.register(List.of(settings));

        when(formExtensionContext.object().getOldApi()).thenReturn(module);
        when(transaction.context()).thenReturn(sharedContext);

        mockScopeUtils = mockStatic(ScopeUtils.class);
    }

    @AfterEach
    void tearDown() {
        polarionServiceMockedConstruction.close();
        mockScopeUtils.close();
    }

    @Test
    void testLinkRolesRender() {

        when(module.getProjectId()).thenReturn("projectId");

        projects = List.of();

        linkRoles = List.of(
                mockLinkRoleOpt("id1", "name1", "oppositeName1"),
                mockLinkRoleOpt("id2", "name2", "oppositeName2")
        );

        mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(any())).thenReturn("testScope");
        mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenReturn("{LINK_ROLE_OPTIONS}");

        HtmlFragmentBuilder builder1 = mock(HtmlFragmentBuilder.class);
        when(sharedContext.createHtmlFragmentBuilderFor().gwt()).thenReturn(builder1);
        new TestFormExtension(true).render(formExtensionContext);
        verify(builder1, times(1)).html("<option value='' >none</option><option value='id1' >name1 / oppositeName1</option><option value='id2' >name2 / oppositeName2</option>");

        HtmlFragmentBuilder builder2 = mock(HtmlFragmentBuilder.class);
        when(sharedContext.createHtmlFragmentBuilderFor().gwt()).thenReturn(builder2);
        new TestFormExtension(false).render(formExtensionContext);
        verify(builder2, times(1)).html("<option value='id1' >name1 / oppositeName1</option><option value='id2' >name2 / oppositeName2</option>");

    }

    private ILinkRoleOpt mockLinkRoleOpt(String id, String name, String oppositeName) {
        ILinkRoleOpt linkRoleOpt = mock(ILinkRoleOpt.class);
        when(linkRoleOpt.getId()).thenReturn(id);
        when(linkRoleOpt.getName()).thenReturn(name);
        when(linkRoleOpt.getOppositeName()).thenReturn(oppositeName);
        return linkRoleOpt;
    }


    static class TestFormExtension extends BaseFormExtension {
        public TestFormExtension(boolean allowEmptyLinkRole) {
            super("test", "Test Form Extension", allowEmptyLinkRole);
        }
    }

}
