package ch.sbb.polarion.extension.diff_tool;

import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
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
import com.polarion.platform.persistence.model.IPObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private List<IProject> projects = List.of();
    private List<ILinkRoleOpt> linkRoles = List.of();
    private Set<SettingName> settingNames = Set.of(SettingName.builder().id("settingId").name("settingName").build());

    @BeforeEach
    void setUp() {
        // The whole fixture is stubbed leniently: it describes one document in one project, and the tests
        // below each look at a different slice of the props built from it (or, for the render tests, at
        // none of it).
        polarionServiceMockedConstruction = mockConstruction(PolarionService.class,
                (mock, context) -> {
                    lenient().when(mock.getProjects()).thenReturn(projects);
                    lenient().when(mock.getLinkRoles(anyString())).thenReturn(linkRoles);
                }
        );

        lenient().when(settings.getFeatureName()).thenReturn(DiffSettings.FEATURE_NAME);
        lenient().when(settings.readNames(any())).thenAnswer(invocation -> settingNames);
        NamedSettingsRegistry.INSTANCE.register(List.of(settings));

        mockScopeUtils = mockStatic(ScopeUtils.class);
        mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(any())).thenReturn("testScope");

        lenient().when(module.getProjectId()).thenReturn("projectId");
        lenient().when(module.getModuleFolder()).thenReturn("spaceId");
        lenient().when(module.getModuleName()).thenReturn("documentName");
        lenient().when(module.getTitleOrName()).thenReturn("Document Title");
    }

    @AfterEach
    void tearDown() {
        polarionServiceMockedConstruction.close();
        mockScopeUtils.close();

        NamedSettingsRegistry.INSTANCE.getAll().clear();
    }

    @Test
    void testSourceDocumentIdentity() {
        when(module.getRevision()).thenReturn("4711");

        BaseFormExtension.PanelProps props = new TestFormExtension(false).buildProps(module);

        assertEquals("projectId", props.sourceProjectId());
        assertEquals("spaceId", props.sourceSpaceId());
        assertEquals("documentName", props.sourceDocument());
        assertEquals("Document Title", props.sourceDocumentTitle());
        assertEquals("4711", props.sourceRevision());
    }

    @Test
    void testHeadDocumentReportsEmptyRevision() {
        when(module.getRevision()).thenReturn(null);

        // The panel treats "" as "latest" and omits the parameter from the comparison URL entirely.
        assertEquals("", new TestFormExtension(false).buildProps(module).sourceRevision());
    }

    @Test
    void testLinkRoles() {
        linkRoles = List.of(
                mockLinkRoleOpt("id1", "name1", "oppositeName1"),
                mockLinkRoleOpt("id2", "name2", "oppositeName2")
        );

        assertEquals(
                List.of(new BaseFormExtension.IdName("id1", "name1 / oppositeName1"),
                        new BaseFormExtension.IdName("id2", "name2 / oppositeName2")),
                new TestFormExtension(false).buildProps(module).linkRoles());
    }

    @Test
    void testLinkRolesWithEmptyOneAllowed() {
        linkRoles = List.of(mockLinkRoleOpt("id1", "name1", "oppositeName1"));

        assertEquals(
                List.of(new BaseFormExtension.IdName("", "none"),
                        new BaseFormExtension.IdName("id1", "name1 / oppositeName1")),
                new TestFormExtension(true).buildProps(module).linkRoles());
    }

    @Test
    void testProjectsFallBackToTheirIdWhenUnnamed() {
        projects = List.of(mockProject("named", "The Named One"), mockProject("unnamed", null));

        assertEquals(
                List.of(new BaseFormExtension.IdName("named", "The Named One"),
                        new BaseFormExtension.IdName("unnamed", "unnamed")),
                new TestFormExtension(false).buildProps(module).projects());
    }

    @Test
    void testConfigurations() {
        assertEquals(List.of("settingName"), new TestFormExtension(false).buildProps(module).configurations());
    }

    @Test
    void testConfigurationsNeverEmpty() {
        settingNames = Set.of();

        // The panel preselects the first entry, so an empty list would leave `config` unset in the URL.
        assertEquals(List.of(NamedSettings.DEFAULT_NAME),
                new TestFormExtension(false).buildProps(module).configurations());
    }

    @Test
    void testConfigurationsAreEmptyWhenReadInsideARunningTransaction() {
        when(settings.readNames(any())).thenThrow(new IllegalStateException("There is already a transaction."));

        assertEquals(List.of(NamedSettings.DEFAULT_NAME),
                new TestFormExtension(false).buildProps(module).configurations());
    }

    @Test
    void testHandleReferences() {
        List<BaseFormExtension.HandleReferencesOption> options =
                new TestFormExtension(false).buildProps(module).handleReferencesTypes();

        assertEquals(List.of("DEFAULT", "CREATE_MISSING", "KEEP", "ALWAYS_OVERWRITE"),
                options.stream().map(BaseFormExtension.HandleReferencesOption::id).toList());
        assertEquals("Remove when no counterpart found", options.get(0).title());
        assertTrue(options.get(0).description().startsWith("Attempts to replace reference"));
    }

    @Test
    void testRenderSubstitutesEscapedJsonForThePlaceholder() {
        mockScopeUtils.when(() -> ScopeUtils.getFileContent(any()))
                .thenReturn("<div data-props='{PANEL_PROPS}'></div>");
        HtmlFragmentBuilder builder = mock(HtmlFragmentBuilder.class);
        when(sharedContext.createHtmlFragmentBuilderFor().gwt()).thenReturn(builder);
        when(formExtensionContext.object().getOldApi()).thenReturn(module);
        when(transaction.context()).thenReturn(sharedContext);

        new TestFormExtension(false).render(formExtensionContext);

        String rendered = capturedHtml(builder);
        // JSON quotes arrive as entities, which the browser decodes back to exact JSON in dataset.props.
        assertTrue(rendered.contains("&quot;sourceProjectId&quot;:&quot;projectId&quot;"), rendered);
        assertFalse(rendered.contains("{PANEL_PROPS}"), rendered);
    }

    @Test
    void testRenderEscapesAQuoteInTheDocumentTitle() {
        // Regression guard: the legacy fillParams() interpolated the title into a `<link onload='...'>`
        // attribute with no escaping, so a quote in the title broke out and injected script.
        when(module.getTitleOrName()).thenReturn("Spec \"v2\" <b>' onerror=alert(1)");
        // The bare placeholder, so what the builder receives is exactly the attribute value.
        mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenReturn("{PANEL_PROPS}");
        HtmlFragmentBuilder builder = mock(HtmlFragmentBuilder.class);
        when(sharedContext.createHtmlFragmentBuilderFor().gwt()).thenReturn(builder);
        when(formExtensionContext.object().getOldApi()).thenReturn(module);
        when(transaction.context()).thenReturn(sharedContext);

        new TestFormExtension(false).render(formExtensionContext);

        String rendered = capturedHtml(builder);
        // Neither quote style nor a tag delimiter survives raw, so the attribute cannot be escaped from.
        assertFalse(rendered.contains("\""), rendered);
        assertFalse(rendered.contains("'"), rendered);
        assertFalse(rendered.contains("<b>"), rendered);
    }

    @Test
    void testRenderIgnoresObjectsThatAreNotDocuments() {
        HtmlFragmentBuilder builder = mock(HtmlFragmentBuilder.class);
        IPObject notADocument = mock(IPObject.class);
        when(sharedContext.createHtmlFragmentBuilderFor().gwt()).thenReturn(builder);
        when(formExtensionContext.object().getOldApi()).thenReturn(notADocument);
        when(transaction.context()).thenReturn(sharedContext);

        new TestFormExtension(false).render(formExtensionContext);

        verify(builder, never()).html(anyString());
        verify(builder).finished();
    }

    /**
     * The Java side substitutes one placeholder and the React side reads one attribute; this pins both
     * fragments to that contract, which no unit test of either half alone can see.
     */
    @Test
    void testBothFragmentsCarryThePlaceholderAndMountTheirPanel() throws Exception {
        assertTrue(fragment("diff-tool").contains("data-props='{PANEL_PROPS}'"));
        assertTrue(fragment("diff-tool").contains("mountDiffToolPanel(\"#diff-tool-panel\")"));
        assertTrue(fragment("diff-tool").contains("id=\"diff-tool-panel\""));

        assertTrue(fragment("copy-tool").contains("data-props='{PANEL_PROPS}'"));
        assertTrue(fragment("copy-tool").contains("mountCopyToolPanel(\"#copy-tool-panel\")"));
        assertTrue(fragment("copy-tool").contains("id=\"copy-tool-panel\""));
    }

    private String fragment(String name) throws Exception {
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("webapp/diff-tool/html/%s.html".formatted(name))) {
            return new String(Objects.requireNonNull(stream, name).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void testLabelFallsBackToTheDefaultWhenTheExtenderSetsNone() {
        TestFormExtension extension = new TestFormExtension(false);

        assertEquals("Test Form Extension", extension.getLabel(module, null));
        assertEquals("Test Form Extension", extension.getLabel(module, Map.of()));
        assertEquals("Custom", extension.getLabel(module, Map.of("label", "Custom")));
    }

    private String capturedHtml(HtmlFragmentBuilder builder) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(builder).html(captor.capture());
        return captor.getValue();
    }

    private IProject mockProject(String id, String name) {
        IProject project = mock(IProject.class);
        when(project.getId()).thenReturn(id);
        when(project.getName()).thenReturn(name);
        return project;
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
