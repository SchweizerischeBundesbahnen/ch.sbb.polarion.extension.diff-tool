package ch.sbb.polarion.extension.diff_tool.widgets;

import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.Scope;
import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.ModelObjectsBase;
import com.polarion.alm.shared.api.model.ModelObjectsSearch;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.Renderer;
import com.polarion.alm.shared.api.model.fields.Field;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.model.wi.WorkItem;
import com.polarion.alm.shared.api.model.wi.WorkItemFields;
import com.polarion.alm.shared.api.model.wi.WorkItemPermissions;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.SharedLocalization;
import com.polarion.alm.shared.api.utils.html.HtmlAttributesBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagSelector;
import com.polarion.alm.shared.api.utils.internal.InternalPolarionUtils;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.model.IPrototype;
import com.polarion.subterra.base.data.identification.ILocalId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
class WorkItemsDiffWidgetRendererTest {

    @Mock
    private RichPageWidgetCommonContext context;

    @Mock
    private DiffWidgetParams params;

    @Mock
    private PolarionService polarionService;

    @Test
    void testConstructor() {
        WorkItemsDiffWidgetRenderer renderer = mockRenderer(context, params);
        assertNotNull(renderer);
    }

    @Test
    void testRenderUnresolvableItem() {
        SharedLocalization localization = mock(SharedLocalization.class);
        when(context.localization()).thenReturn(localization);
        when(localization.getString("richpages.widget.table.unresolvableItem", "path")).thenReturn("Unresolvable item");

        WorkItemsDiffWidgetRenderer renderer = mockRenderer(context, params);

        HtmlContentBuilder builder = mock(HtmlContentBuilder.class);
        ModelObject item = mock(WorkItem.class);
        when(item.isUnresolvable()).thenReturn(true);
        WorkItemReference reference = mock(WorkItemReference.class);
        when(item.getReferenceToCurrent()).thenReturn(reference);
        when(reference.toPath()).thenReturn("path");

        HtmlTagSelector<HtmlTagBuilder> tagSelector = mock(HtmlTagSelector.class);
        when(builder.tag()).thenReturn(tagSelector);

        HtmlTagBuilder tdBuilder = mock(HtmlTagBuilder.class);
        when(tagSelector.td()).thenReturn(tdBuilder);

        HtmlAttributesBuilder attributesBuilder = mock(HtmlAttributesBuilder.class);
        when(tdBuilder.attributes()).thenReturn(attributesBuilder);
        when(attributesBuilder.colspan(anyString())).thenReturn(attributesBuilder);
        when(attributesBuilder.className(anyString())).thenReturn(attributesBuilder);

        HtmlContentBuilder tdContentBuilder = mock(HtmlContentBuilder.class);
        when(tdBuilder.append()).thenReturn(tdContentBuilder);
        when(tdContentBuilder.text(anyString())).thenReturn(tdContentBuilder);

        renderer.renderItem(builder, item, mock(DataSetWidgetParams.class));

        verify(attributesBuilder, times(1)).colspan("6");
        verify(attributesBuilder, times(1)).className("polarion-rpw-table-not-readable-cell");
        verify(tdContentBuilder, times(1)).text("Unresolvable item");
    }

    @Test
    void testRenderNotReadableItem() {
        SharedLocalization localization = mock(SharedLocalization.class);
        when(context.localization()).thenReturn(localization);
        when(localization.getString("security.cannotread")).thenReturn("Not readable item");

        WorkItemsDiffWidgetRenderer renderer = mockRenderer(context, params);

        HtmlContentBuilder builder = mock(HtmlContentBuilder.class);
        ModelObject item = mock(WorkItem.class);
        when(item.isUnresolvable()).thenReturn(false);

        WorkItemPermissions permissions = mock(WorkItemPermissions.class);
        when(item.can()).thenReturn(permissions);
        when(permissions.read()).thenReturn(false);

        HtmlTagSelector<HtmlTagBuilder> tagSelector = mock(HtmlTagSelector.class);
        when(builder.tag()).thenReturn(tagSelector);

        HtmlTagBuilder tdBuilder = mock(HtmlTagBuilder.class);
        when(tagSelector.td()).thenReturn(tdBuilder);

        HtmlAttributesBuilder attributesBuilder = mock(HtmlAttributesBuilder.class);
        when(tdBuilder.attributes()).thenReturn(attributesBuilder);
        when(attributesBuilder.colspan(anyString())).thenReturn(attributesBuilder);
        when(attributesBuilder.className(anyString())).thenReturn(attributesBuilder);

        HtmlContentBuilder tdContentBuilder = mock(HtmlContentBuilder.class);
        when(tdBuilder.append()).thenReturn(tdContentBuilder);
        when(tdContentBuilder.text(anyString())).thenReturn(tdContentBuilder);

        renderer.renderItem(builder, item, mock(DataSetWidgetParams.class));

        verify(attributesBuilder, times(1)).colspan("6");
        verify(attributesBuilder, times(1)).className("polarion-rpw-table-not-readable-cell");
        verify(tdContentBuilder, times(1)).text("Not readable item");
    }

    @Test
    void testRenderLinkRole() {
        DataSetWidgetParams sourceParams = mock(DataSetWidgetParams.class);
        when(sourceParams.projectId()).thenReturn("projectId");

        when(params.linkRole()).thenReturn("id2");

        List<ILinkRoleOpt> linkRoles = List.of(
                mockLinkRoleOpt("id1", "name1", "oppositeName1"),
                mockLinkRoleOpt("id2", "name2", "oppositeName2")
        );
        when(polarionService.getLinkRoles("projectId")).thenReturn(linkRoles);

        WorkItemsDiffWidgetRenderer renderer = mockRenderer(context, params, sourceParams);

        HtmlTagBuilder parentTagBuilder = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);

        HtmlTagBuilder linkRolePane = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(parentTagBuilder.append().tag().div()).thenReturn(linkRolePane);

        HtmlContentBuilder labelContentBuilder = mock(HtmlContentBuilder.class);
        when(linkRolePane.append().tag().byName("label").append()).thenReturn(labelContentBuilder);

        HtmlTagBuilder linkRoleSelect = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(linkRolePane.append().tag().byName("select")).thenReturn(linkRoleSelect);

        HtmlAttributesBuilder linkRoleSelectAttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(linkRoleSelect.attributes()).thenReturn(linkRoleSelectAttributesBuilder);

        HtmlTagBuilder option1 = mock(HtmlTagBuilder.class);
        HtmlAttributesBuilder option1AttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(option1.attributes()).thenReturn(option1AttributesBuilder);
        HtmlContentBuilder option1ContentBuilder = mock(HtmlContentBuilder.class);
        when(option1.append()).thenReturn(option1ContentBuilder);

        HtmlTagBuilder option2 = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        HtmlAttributesBuilder option2AttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(option2.attributes()).thenReturn(option2AttributesBuilder);
        HtmlContentBuilder option2ContentBuilder = mock(HtmlContentBuilder.class);
        when(option2.append()).thenReturn(option2ContentBuilder);

        when(linkRoleSelect.append().tag().byName("option")).thenReturn(option1).thenReturn(option2);

        renderer.renderLinkRole(parentTagBuilder);

        verify(labelContentBuilder, times(1)).text("Link role:");
        verify(linkRoleSelectAttributesBuilder, times(1)).id("link-role-selector");

        verify(option1ContentBuilder, times(1)).text("name1 / oppositeName1");
        verify(option1AttributesBuilder, times(1)).byName("value", "id1");

        verify(option2ContentBuilder, times(1)).text("name2 / oppositeName2");
        verify(option2AttributesBuilder, times(1)).byName("value", "id2");
        verify(option2AttributesBuilder, times(1)).booleanAttribute("selected");
    }

    @Test
    void testRenderConfiguration() {
        DataSetWidgetParams sourceParams = mock(DataSetWidgetParams.class);
        when(sourceParams.projectId()).thenReturn("projectId");

        when(params.configuration()).thenReturn("Setting 2");

        WorkItemsDiffWidgetRenderer renderer = mockRenderer(context, params, sourceParams);

        HtmlTagBuilder parentTagBuilder = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);

        HtmlTagBuilder configurationPane = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(parentTagBuilder.append().tag().div()).thenReturn(configurationPane);

        HtmlAttributesBuilder configurationPaneAttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(configurationPane.attributes()).thenReturn(configurationPaneAttributesBuilder);

        HtmlContentBuilder labelContentBuilder = mock(HtmlContentBuilder.class);
        when(configurationPane.append().tag().byName("label").append()).thenReturn(labelContentBuilder);

        HtmlTagBuilder configurationSelect = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(configurationPane.append().tag().byName("select")).thenReturn(configurationSelect);

        HtmlAttributesBuilder configurationSelectAttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(configurationSelect.attributes()).thenReturn(configurationSelectAttributesBuilder);

        try {
            DiffSettings settingsMock = mock(DiffSettings.class);
            when(settingsMock.getFeatureName()).thenReturn(DiffSettings.FEATURE_NAME);
            Collection<SettingName> settingNames = new ArrayList<>();
            settingNames.add(SettingName.builder().id("id1").name("Setting 1").build());
            settingNames.add(SettingName.builder().id("id2").name("Setting 2").build());
            when(settingsMock.readNames(ScopeUtils.getScopeFromProject("projectId"))).thenReturn(settingNames);
            NamedSettingsRegistry.INSTANCE.register(List.of(settingsMock));

            HtmlTagBuilder option1 = mock(HtmlTagBuilder.class);
            HtmlAttributesBuilder option1AttributesBuilder = mock(HtmlAttributesBuilder.class);
            when(option1.attributes()).thenReturn(option1AttributesBuilder);
            HtmlContentBuilder option1ContentBuilder = mock(HtmlContentBuilder.class);
            when(option1.append()).thenReturn(option1ContentBuilder);

            HtmlTagBuilder option2 = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
            HtmlAttributesBuilder option2AttributesBuilder = mock(HtmlAttributesBuilder.class);
            when(option2.attributes()).thenReturn(option2AttributesBuilder);
            HtmlContentBuilder option2ContentBuilder = mock(HtmlContentBuilder.class);
            when(option2.append()).thenReturn(option2ContentBuilder);

            when(configurationSelect.append().tag().byName("option")).thenReturn(option1).thenReturn(option2);

            renderer.renderConfiguration(parentTagBuilder);

            verify(configurationPaneAttributesBuilder, times(1)).className("configuration");

            verify(labelContentBuilder, times(1)).text("Configuration:");
            verify(configurationSelectAttributesBuilder, times(1)).id("config-selector");

            verify(option1ContentBuilder, times(1)).text("Setting 1");
            verify(option1AttributesBuilder, times(1)).byName("value", "Setting 1");

            verify(option2ContentBuilder, times(1)).text("Setting 2");
            verify(option2AttributesBuilder, times(1)).byName("value", "Setting 2");
            verify(option2AttributesBuilder, times(1)).booleanAttribute("selected");
        } finally {
            NamedSettingsRegistry.INSTANCE.getAll().clear();
        }
    }

    @Test
    void testRenderQueryPanel() {
        DataSetWidgetParams sourceParams = mock(DataSetWidgetParams.class);
        when(sourceParams.side()).thenReturn(DataSetWidgetParams.Side.SOURCE);
        when(sourceParams.query()).thenReturn("query");
        when(sourceParams.recordsPerPage()).thenReturn(10);

        WorkItemsDiffWidgetRenderer renderer = mockRenderer(context, params, sourceParams);

        HtmlTagBuilder parentTagBuilder = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);

        HtmlTagBuilder queryPane = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(parentTagBuilder.append().tag().div()).thenReturn(queryPane);

        HtmlAttributesBuilder queryPaneAttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(queryPane.attributes()).thenReturn(queryPaneAttributesBuilder);

        HtmlContentBuilder queryLabelContentBuilder = mock(HtmlContentBuilder.class);
        HtmlContentBuilder recordsPerPageLabelContentBuilder = mock(HtmlContentBuilder.class);
        when(queryPane.append().tag().byName("label").append()).thenReturn(queryLabelContentBuilder).thenReturn(recordsPerPageLabelContentBuilder);

        HtmlTagBuilder queryInput = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        HtmlTagBuilder recordsPerPageInput = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(queryPane.append().tag().byName("input")).thenReturn(queryInput).thenReturn(recordsPerPageInput);

        HtmlAttributesBuilder queryInputAttributesBuilder = mock(HtmlAttributesBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(queryInput.attributes()).thenReturn(queryInputAttributesBuilder);

        HtmlAttributesBuilder recordsPerPageInputAttributesBuilder = mock(HtmlAttributesBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(recordsPerPageInput.attributes()).thenReturn(recordsPerPageInputAttributesBuilder);

        HtmlTagBuilder applyButton = mock(HtmlTagBuilder.class);
        HtmlContentBuilder applyButtonContentBuilder = mock(HtmlContentBuilder.class);
        when(applyButton.append()).thenReturn(applyButtonContentBuilder);
        HtmlAttributesBuilder applyButtonAttributesBuilder = mock(HtmlAttributesBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(applyButton.attributes()).thenReturn(applyButtonAttributesBuilder);

        HtmlTagBuilder resetButton = mock(HtmlTagBuilder.class);
        HtmlContentBuilder resetButtonContentBuilder = mock(HtmlContentBuilder.class);
        when(resetButton.append()).thenReturn(resetButtonContentBuilder);
        HtmlAttributesBuilder resetButtonAttributesBuilder = mock(HtmlAttributesBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(resetButton.attributes()).thenReturn(resetButtonAttributesBuilder);

        when(queryPane.append().tag().byName("button")).thenReturn(applyButton).thenReturn(resetButton);

        renderer.renderQueryPanel(parentTagBuilder, sourceParams);

        verify(queryPaneAttributesBuilder, times(1)).className("query");

        verify(queryLabelContentBuilder, times(1)).text("Query (Lucene):");
        verify(queryInputAttributesBuilder, times(1)).id("source-query-input");
        verify(queryInputAttributesBuilder, times(1)).byName("value", "query");
        verify(queryInputAttributesBuilder, times(1)).className("query-input");

        verify(recordsPerPageLabelContentBuilder, times(1)).text("Records per page:");
        verify(recordsPerPageInputAttributesBuilder, times(1)).id("source-records-per-page-input");
        verify(recordsPerPageInputAttributesBuilder, times(1)).byName("value", "10");
        verify(recordsPerPageInputAttributesBuilder, times(1)).className("records-per-page");

        verify(applyButtonContentBuilder, times(1)).text("Apply");
        verify(applyButtonAttributesBuilder, times(1)).onClick("""
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'targetProjectId', document.getElementById('target-project-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'linkRole', document.getElementById('link-role-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'configuration', document.getElementById('config-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'sourceQuery', document.getElementById('source-query-input').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'sourceRecordsPerPage', document.getElementById('source-records-per-page-input').value);
                top.location.reload()""");

        verify(resetButtonContentBuilder, times(1)).text("Reset");
        verify(resetButtonAttributesBuilder, times(1)).onClick("""
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'targetProjectId', document.getElementById('target-project-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'linkRole', document.getElementById('link-role-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'configuration', document.getElementById('config-selector').value);
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'sourceQuery', '');
                top.location.href = DiffTool.replaceUrlParam(top.location.href, 'sourceRecordsPerPage', '20');
                top.location.reload()""");

    }

    @Test
    void testRenderTargetProjectWithPreselection() {
        DataSetWidgetParams sourceParams = mock(DataSetWidgetParams.class);
        when(sourceParams.projectId()).thenReturn("project2");

        List<IProject> projects = List.of(
                mockProject("project1", "Project 1"),
                mockProject("project2", "Project 2")
        );
        when(polarionService.getProjects()).thenReturn(projects);

        WorkItemsDiffWidgetRenderer renderer = mockRenderer(context, params, sourceParams);

        HtmlTagBuilder parentTagBuilder = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);

        HtmlTagBuilder targetProjectPane = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(parentTagBuilder.append().tag().div()).thenReturn(targetProjectPane);

        HtmlAttributesBuilder targetProjectPaneAttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(targetProjectPane.attributes()).thenReturn(targetProjectPaneAttributesBuilder);

        HtmlContentBuilder labelContentBuilder = mock(HtmlContentBuilder.class);
        when(targetProjectPane.append().tag().byName("label").append()).thenReturn(labelContentBuilder);

        HtmlTagBuilder targetProjectSelect = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(targetProjectPane.append().tag().byName("select")).thenReturn(targetProjectSelect);

        HtmlAttributesBuilder targetProjectSelectAttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(targetProjectSelect.attributes()).thenReturn(targetProjectSelectAttributesBuilder);

        HtmlTagBuilder project1 = mock(HtmlTagBuilder.class);
        HtmlAttributesBuilder project1AttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(project1.attributes()).thenReturn(project1AttributesBuilder);
        HtmlContentBuilder project1ContentBuilder = mock(HtmlContentBuilder.class);
        when(project1.append()).thenReturn(project1ContentBuilder);

        HtmlTagBuilder project2 = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        HtmlAttributesBuilder project2AttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(project2.attributes()).thenReturn(project2AttributesBuilder);
        HtmlContentBuilder project2ContentBuilder = mock(HtmlContentBuilder.class);
        when(project2.append()).thenReturn(project2ContentBuilder);

        when(targetProjectSelect.append().tag().byName("option")).thenReturn(project1).thenReturn(project2);

        renderer.renderTargetProject(parentTagBuilder, sourceParams);

        verify(targetProjectPaneAttributesBuilder, times(1)).className("target-project");

        verify(labelContentBuilder, times(1)).text("Target project:");
        verify(targetProjectSelectAttributesBuilder, times(1)).id("target-project-selector");

        verify(project1ContentBuilder, times(1)).text("Project 1");
        verify(project1AttributesBuilder, times(1)).byName("value", "project1");

        verify(project2ContentBuilder, times(1)).text("Project 2");
        verify(project2AttributesBuilder, times(1)).byName("value", "project2");
        verify(project2AttributesBuilder, times(1)).booleanAttribute("selected");
    }

    @Test
    void testRenderTargetProjectWithoutPreselection() {
        List<IProject> projects = List.of(
                mockProject("project1", "Project 1"),
                mockProject("project2", "Project 2")
        );
        when(polarionService.getProjects()).thenReturn(projects);

        WorkItemsDiffWidgetRenderer renderer = mockRenderer(context, params, mock(DataSetWidgetParams.class));

        HtmlTagBuilder parentTagBuilder = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);

        HtmlTagBuilder targetProjectPane = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(parentTagBuilder.append().tag().div()).thenReturn(targetProjectPane);

        HtmlAttributesBuilder targetProjectPaneAttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(targetProjectPane.attributes()).thenReturn(targetProjectPaneAttributesBuilder);

        HtmlContentBuilder labelContentBuilder = mock(HtmlContentBuilder.class);
        when(targetProjectPane.append().tag().byName("label").append()).thenReturn(labelContentBuilder);

        HtmlTagBuilder targetProjectSelect = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(targetProjectPane.append().tag().byName("select")).thenReturn(targetProjectSelect);

        HtmlAttributesBuilder targetProjectSelectAttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(targetProjectSelect.attributes()).thenReturn(targetProjectSelectAttributesBuilder);

        HtmlTagBuilder project1 = mock(HtmlTagBuilder.class);
        HtmlAttributesBuilder project1AttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(project1.attributes()).thenReturn(project1AttributesBuilder);
        HtmlContentBuilder project1ContentBuilder = mock(HtmlContentBuilder.class);
        when(project1.append()).thenReturn(project1ContentBuilder);

        HtmlTagBuilder project2 = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        HtmlAttributesBuilder project2AttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(project2.attributes()).thenReturn(project2AttributesBuilder);
        HtmlContentBuilder project2ContentBuilder = mock(HtmlContentBuilder.class);
        when(project2.append()).thenReturn(project2ContentBuilder);

        when(targetProjectSelect.append().tag().byName("option")).thenReturn(project1).thenReturn(project2);

        renderer.renderTargetProject(parentTagBuilder, null);

        verify(targetProjectPaneAttributesBuilder, times(1)).className("target-project");

        verify(labelContentBuilder, times(1)).text("Target project:");
        verify(targetProjectSelectAttributesBuilder, times(1)).id("target-project-selector");

        verify(project1ContentBuilder, times(1)).text("Project 1");
        verify(project1AttributesBuilder, times(1)).byName("value", "project1");
        verify(project1AttributesBuilder, never()).booleanAttribute("selected");

        verify(project2ContentBuilder, times(1)).text("Project 2");
        verify(project2AttributesBuilder, times(1)).byName("value", "project2");
        verify(project2AttributesBuilder, never()).booleanAttribute("selected");
    }

    @Test
    void testRenderHeaderRow() {
        WorkItemsDiffWidgetRenderer renderer = mockRenderer(context, params, mock(DataSetWidgetParams.class));

        HtmlContentBuilder parentContentBuilder = mock(HtmlContentBuilder.class, Answers.RETURNS_DEEP_STUBS);

        HtmlTagBuilder row = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(parentContentBuilder.tag().tr()).thenReturn(row);

        HtmlAttributesBuilder rowAttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(row.attributes()).thenReturn(rowAttributesBuilder);

        renderer.renderHeaderRow(parentContentBuilder);

        verify(rowAttributesBuilder, times(1)).className("polarion-rpw-table-header-row");
        verify(row.append().tag(), times(6)).th();
    }

    @Test
    void testRenderItem() {
        WorkItemsDiffWidgetRenderer renderer = mockRenderer(context, params);

        HtmlContentBuilder builder = mock(HtmlContentBuilder.class);
        ModelObject item = mock(WorkItem.class);
        when(item.isUnresolvable()).thenReturn(false);

        WorkItemPermissions permissions = mock(WorkItemPermissions.class);
        when(item.can()).thenReturn(permissions);
        when(permissions.read()).thenReturn(true);

        IWorkItem oldApi = mock(IWorkItem.class);
        when(item.getOldApi()).thenReturn(oldApi);
        IPrototype prototype = mock(IPrototype.class);
        when(oldApi.getPrototype()).thenReturn(prototype);
        when(prototype.getName()).thenReturn(PrototypeEnum.WorkItem.name());
        ILocalId localId = mock(ILocalId.class);
        lenient().when(oldApi.getLocalId()).thenReturn(localId);
        lenient().when(localId.getObjectName()).thenReturn("objectName");

        WorkItemFields fields = mock(WorkItemFields.class);
        when(item.fields()).thenReturn(fields);
        Field field = mock(Field.class);
        when(fields.get(anyString())).thenReturn(field);
        Renderer fieldRenderer = mock(Renderer.class);
        when(field.render()).thenReturn(fieldRenderer);
        when(fieldRenderer.withLinks(anyBoolean())).thenReturn(fieldRenderer);

        HtmlTagSelector<HtmlTagBuilder> tagSelector = mock(HtmlTagSelector.class);
        when(builder.tag()).thenReturn(tagSelector);

        HtmlTagBuilder tdBuilder = mock(HtmlTagBuilder.class);
        when(tagSelector.td()).thenReturn(tdBuilder);

        HtmlContentBuilder tdContentBuilder = mock(HtmlContentBuilder.class);
        when(tdBuilder.append()).thenReturn(tdContentBuilder);

        HtmlTagSelector<HtmlTagBuilder> tdContentTagSelector = mock(HtmlTagSelector.class);
        when(tdContentBuilder.tag()).thenReturn(tdContentTagSelector);

        HtmlTagBuilder checkboxBuilder = mock(HtmlTagBuilder.class);
        when(tdContentTagSelector.byName("input")).thenReturn(checkboxBuilder);

        HtmlAttributesBuilder checkboxAttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(checkboxBuilder.attributes()).thenReturn(checkboxAttributesBuilder);
        when(checkboxAttributesBuilder.byName(anyString(), anyString())).thenReturn(checkboxAttributesBuilder);
        when(checkboxAttributesBuilder.className(anyString())).thenReturn(checkboxAttributesBuilder);

        renderer.renderItem(builder, item, mock(DataSetWidgetParams.class));

        verify(checkboxAttributesBuilder, times(1)).byName("type", "checkbox");
        verify(checkboxAttributesBuilder, times(1)).byName("data-type", PrototypeEnum.WorkItem.name());
        verify(checkboxAttributesBuilder, times(1)).byName("data-space", "");
        verify(checkboxAttributesBuilder, times(1)).byName("data-id", "");
        verify(checkboxAttributesBuilder, times(1)).className("export-item");
    }

    private WorkItemsDiffWidgetRenderer mockRenderer(RichPageWidgetCommonContext context, DiffWidgetParams params) {
        return mockRenderer(context, params, null);
    }

    private WorkItemsDiffWidgetRenderer mockRenderer(RichPageWidgetCommonContext context, DiffWidgetParams params, DataSetWidgetParams sourceParams) {
        Scope scope = mock(Scope.class);
        when(context.getDisplayedScope()).thenReturn(scope);

        InternalReadOnlyTransaction internalReadOnlyTransaction = mock(InternalReadOnlyTransaction.class);
        when(internalReadOnlyTransaction.utils()).thenReturn(mock(InternalPolarionUtils.class));
        ModelObjectsBase modelObjectsBase = mock(ModelObjectsBase.class);
        ModelObjectsSearch modelObjectsSearch = mock(ModelObjectsSearch.class);
        when(modelObjectsSearch.query(null)).thenReturn(modelObjectsSearch);
        when(modelObjectsSearch.sort("id")).thenReturn(modelObjectsSearch);
        when(modelObjectsBase.search()).thenReturn(modelObjectsSearch);
        when(internalReadOnlyTransaction.byEnum(any())).thenReturn(modelObjectsBase);
        when(context.transaction()).thenReturn(internalReadOnlyTransaction);

        when(params.sourceParams()).thenReturn(sourceParams != null ? sourceParams : mock(DataSetWidgetParams.class));

        when(scope.isGlobal()).thenReturn(false);
        return new WorkItemsDiffWidgetRenderer(context, params, polarionService);
    }

    private ILinkRoleOpt mockLinkRoleOpt(String id, String name, String oppositeName) {
        ILinkRoleOpt linkRoleOpt = mock(ILinkRoleOpt.class);
        when(linkRoleOpt.getId()).thenReturn(id);
        when(linkRoleOpt.getName()).thenReturn(name);
        when(linkRoleOpt.getOppositeName()).thenReturn(oppositeName);
        return linkRoleOpt;
    }

    private IProject mockProject(String id, String name) {
        IProject project = mock(IProject.class);
        when(project.getId()).thenReturn(id);
        when(project.getName()).thenReturn(name);
        return project;
    }
}
