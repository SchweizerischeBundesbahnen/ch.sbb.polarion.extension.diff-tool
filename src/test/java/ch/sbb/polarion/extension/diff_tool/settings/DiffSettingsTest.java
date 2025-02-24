package ch.sbb.polarion.extension.diff_tool.settings;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DiffField;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
@CurrentContextConfig("diff-tool")
public class DiffSettingsTest {

    @Test
    void testDefaultSettings() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            final DiffModel defaultValues = new DiffSettings(mockedSettingsService).defaultValues();
            assertNotNull(defaultValues);
            assertNotNull(defaultValues.getDiffFields());
            assertEquals(3, defaultValues.getDiffFields().size());
        }
    }

    @Test
    void testLoadCustomWhenSettingExists() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            GenericNamedSettings<DiffModel> exporterSettings = new DiffSettings(mockedSettingsService);

            String projectName = "test_project";

            ILocation mockDefaultLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(anyString())).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);

            ILocation mockProjectLocation = mock(ILocation.class);
            when(mockProjectLocation.append(anyString())).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(projectName)).thenCallRealMethod();
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/test_project/")).thenReturn(mockProjectLocation);

            List<DiffField> diffFields = List.of(
                    new DiffField("title", "value1"),
                    new DiffField("description", "value2")
            );

            List<String> hyperlinkRoles = List.of("role1", "role2");
            List<String> statusToIgnore = List.of("status1", "status2");

            DiffModel customProjectModel = DiffModel.builder()
                    .diffFields(diffFields)
                    .hyperlinkRoles(hyperlinkRoles)
                    .statusesToIgnore(statusToIgnore)
                    .build();
            customProjectModel.setBundleTimestamp("custom");
            when(mockedSettingsService.read(eq(mockProjectLocation), any())).thenReturn(customProjectModel.serialize());

            when(mockedSettingsService.getLastRevision(mockProjectLocation)).thenReturn("345");
            when(mockedSettingsService.getPersistedSettingFileNames(mockProjectLocation)).thenReturn(List.of("Any setting name"));

            DiffModel loadedModel = exporterSettings.load(projectName, SettingId.fromName("Any setting name"));
            assertEquals("custom", loadedModel.getBundleTimestamp());

            assertNotNull(loadedModel);

            assertNotNull(loadedModel.getDiffFields());
            assertFalse(loadedModel.getDiffFields().isEmpty());
            assertEquals(2, loadedModel.getDiffFields().size());

            assertNotNull(loadedModel.getHyperlinkRoles());
            assertTrue(loadedModel.getHyperlinkRoles().contains("role1"));
            assertTrue(loadedModel.getHyperlinkRoles().contains("role2"));

            assertNotNull(loadedModel.getStatusesToIgnore());
            assertEquals(List.of("status1", "status2"), loadedModel.getStatusesToIgnore());
        }
    }

    @Test
    void testNamedSettings() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            GenericNamedSettings<DiffModel> settings = new DiffSettings(mockedSettingsService);

            String projectName = "test_project";
            String settingOne = "setting_one";
            String settingTwo = "setting_two";

            mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(projectName)).thenReturn("project/test_project/");

            ILocation mockDefaultLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(anyString())).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);
            when(mockedSettingsService.getLastRevision(mockDefaultLocation)).thenReturn("some_revision");

            ILocation mockProjectLocation = mock(ILocation.class);
            when(mockedSettingsService.getPersistedSettingFileNames(mockProjectLocation)).thenReturn(List.of(settingOne, settingTwo));
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/test_project/")).thenReturn(mockProjectLocation);
            when(mockProjectLocation.append(contains("diff"))).thenReturn(mockProjectLocation);
            ILocation settingOneLocation = mock(ILocation.class);
            when(mockProjectLocation.append(contains(settingOne))).thenReturn(settingOneLocation);
            ILocation settingTwoLocation = mock(ILocation.class);
            when(mockProjectLocation.append(contains(settingTwo))).thenReturn(settingTwoLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            when(mockedSettingsService.getLastRevision(mockProjectLocation)).thenReturn("some_revision");

            List<DiffField> diffFieldsOne = List.of(
                    new DiffField("title", "value1"),
                    new DiffField("description", "value2")
            );

            List<String> hyperlinkRolesOne = List.of("role1", "role2");
            List<String> statusToIgnoreOne = List.of("status1", "status2");

            DiffModel settingOneModel = DiffModel.builder()
                    .diffFields(diffFieldsOne)
                    .hyperlinkRoles(hyperlinkRolesOne)
                    .statusesToIgnore(statusToIgnoreOne)
                    .build();
            settingOneModel.setBundleTimestamp("setting_one");
            when(mockedSettingsService.read(eq(settingOneLocation), any())).thenReturn(settingOneModel.serialize());

            List<DiffField> diffFieldsTwo = List.of(new DiffField("title", "value1"));
            List<String> hyperlinkRolesTwo = List.of("role1");
            List<String> statusToIgnoreTwo = List.of("status1");

            DiffModel settingTwoModel = DiffModel.builder()
                    .diffFields(diffFieldsTwo)
                    .hyperlinkRoles(hyperlinkRolesTwo)
                    .statusesToIgnore(statusToIgnoreTwo)
                    .build();
            settingTwoModel.setBundleTimestamp("setting_two");
            when(mockedSettingsService.read(eq(settingTwoLocation), any())).thenReturn(settingTwoModel.serialize());


            DiffModel loadedOneModel = settings.load(projectName, SettingId.fromName(settingOne));
            assertNotNull(loadedOneModel);
            assertEquals("setting_one", loadedOneModel.getBundleTimestamp());
            assertEquals(2, loadedOneModel.getDiffFields().size());
            assertTrue(loadedOneModel.getHyperlinkRoles().contains("role1"));
            assertTrue(loadedOneModel.getHyperlinkRoles().contains("role2"));
            assertEquals(List.of("status1", "status2"), loadedOneModel.getStatusesToIgnore());

            DiffModel loadedTwoModel = settings.load(projectName, SettingId.fromName(settingTwo));
            assertNotNull(loadedTwoModel);
            assertEquals("setting_two", loadedTwoModel.getBundleTimestamp());
            assertEquals(1, loadedTwoModel.getDiffFields().size());
            assertTrue(loadedTwoModel.getHyperlinkRoles().contains("role1"));
            assertEquals(List.of("status1"), loadedTwoModel.getStatusesToIgnore());
        }
    }
}
