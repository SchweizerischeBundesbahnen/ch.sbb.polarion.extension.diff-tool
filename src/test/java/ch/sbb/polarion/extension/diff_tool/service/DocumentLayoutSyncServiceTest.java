package ch.sbb.polarion.extension.diff_tool.service;

import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.model.IStructure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentLayoutSyncServiceTest {

    private DocumentLayoutSyncService service;
    private IModule sourceModule;
    private IModule targetModule;

    @BeforeEach
    void init() {
        service = new DocumentLayoutSyncService();
        sourceModule = mock(IModule.class);
        targetModule = mock(IModule.class);
        when(sourceModule.getRenderingLayouts()).thenReturn(new ArrayList<>());
        when(targetModule.getRenderingLayouts()).thenReturn(new ArrayList<>());
        when(targetModule.getAllowedWITypes()).thenReturn(new ArrayList<>());
    }

    @Test
    void testMissingLayoutIsCopiedWithAllItsProperties() {
        IModule.IRenderingLayoutStruct sourceLayout = layout(sourceModule, "requirement", "Requirement", "workItemLayouter");
        withProperties(sourceLayout, "fieldsAtStart", "id,title", "fieldsAtEnd", "status");
        IModule.IRenderingLayoutStruct targetLayout = mock(IModule.IRenderingLayoutStruct.class);
        when(targetModule.addRenderingLayout("Requirement", "requirement", "workItemLayouter")).thenReturn(targetLayout);

        DocumentLayoutSyncService.LayoutSyncResult result = service.copyMissingLayouts(sourceModule, targetModule, Set.of("requirement"));

        assertEquals(List.of("requirement"), result.copiedLayoutTypeIds());
        verify(targetLayout).setProperty("fieldsAtStart", "id,title");
        verify(targetLayout).setProperty("fieldsAtEnd", "status");
    }

    @Test
    void testExistingTargetLayoutIsNotTouched() {
        layout(sourceModule, "requirement", "Requirement", "workItemLayouter");
        layout(targetModule, "requirement", "Anforderung", "anotherLayouter");

        DocumentLayoutSyncService.LayoutSyncResult result = service.copyMissingLayouts(sourceModule, targetModule, Set.of("requirement"));

        assertTrue(result.copiedLayoutTypeIds().isEmpty());
        verify(targetModule, never()).addRenderingLayout(anyString(), anyString(), anyString());
    }

    @Test
    void testTypeWithoutSourceLayoutIsSkipped() {
        DocumentLayoutSyncService.LayoutSyncResult result = service.copyMissingLayouts(sourceModule, targetModule, Set.of("requirement"));

        assertTrue(result.copiedLayoutTypeIds().isEmpty());
        verify(targetModule, never()).addRenderingLayout(anyString(), anyString(), anyString());
    }

    @Test
    void testTypeIsAddedToTheListOfAllowedTypesTheDocumentHolds() {
        ITypeOpt allowedType = typeOpt("task");
        List<ITypeOpt> allowedTypes = new ArrayList<>(List.of(allowedType));
        when(targetModule.getAllowedWITypes()).thenReturn(allowedTypes);
        mockTypeEnum(targetModule, "requirement");

        List<String> allowedTypeIds = service.allowWorkItemTypes(targetModule, Set.of("requirement"));

        assertEquals(List.of("requirement"), allowedTypeIds);
        assertEquals(List.of("task", "requirement"), allowedTypes.stream().map(ITypeOpt::getId).toList());
        // 'allowedWITypes' is a list field, and a list field is never written as a whole
        verify(targetModule, never()).setValue(anyString(), any());
    }

    @Test
    void testAlreadyAllowedTypeIsNotAddedTwice() {
        ITypeOpt allowedType = typeOpt("requirement");
        List<ITypeOpt> allowedTypes = new ArrayList<>(List.of(allowedType));
        when(targetModule.getAllowedWITypes()).thenReturn(allowedTypes);

        assertTrue(service.allowWorkItemTypes(targetModule, Set.of("requirement")).isEmpty());
        assertEquals(1, allowedTypes.size());
    }

    @Test
    void testEmptyAllowedTypesMeanAllTypesAreAllowed() {
        assertTrue(service.allowWorkItemTypes(targetModule, Set.of("requirement")).isEmpty());
        verify(targetModule, never()).setValue(anyString(), any());
    }

    @Test
    void testLayoutWithoutALayouterIsNotCopied() {
        layout(sourceModule, "requirement", "Requirement", null);

        DocumentLayoutSyncService.LayoutSyncResult result = service.copyMissingLayouts(sourceModule, targetModule, Set.of("requirement"));

        assertTrue(result.copiedLayoutTypeIds().isEmpty());
        verify(targetModule, never()).addRenderingLayout(anyString(), anyString(), anyString());
    }

    private IModule.IRenderingLayoutStruct layout(IModule module, String typeId, String label, String layouter) {
        IModule.IRenderingLayoutStruct layout = mock(IModule.IRenderingLayoutStruct.class);
        when(layout.getType()).thenReturn(typeId);
        when(layout.getLabel()).thenReturn(label);
        when(layout.getLayouter()).thenReturn(layouter);
        module.getRenderingLayouts().add(layout);
        return layout;
    }

    private void withProperties(IModule.IRenderingLayoutStruct layout, String... keysAndValues) {
        List<IStructure> properties = new ArrayList<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            IStructure property = mock(IStructure.class);
            when(property.getValue(IModule.IRenderingLayoutStruct.KEY_PROPERTIES_KEY)).thenReturn(keysAndValues[i]);
            when(property.getValue(IModule.IRenderingLayoutStruct.KEY_PROPERTIES_VALUE)).thenReturn(keysAndValues[i + 1]);
            properties.add(property);
        }
        when(layout.getValue(IModule.IRenderingLayoutStruct.KEY_PROPERTIES)).thenReturn(properties);
    }

    private ITypeOpt typeOpt(String id) {
        ITypeOpt type = mock(ITypeOpt.class);
        when(type.getId()).thenReturn(id);
        return type;
    }

    @SuppressWarnings("unchecked")
    private void mockTypeEnum(IModule module, String typeId) {
        ITrackerProject project = mock(ITrackerProject.class);
        IEnumeration<ITypeOpt> typeEnum = mock(IEnumeration.class);
        when(module.getProject()).thenReturn(project);
        when(project.getWorkItemTypeEnum()).thenReturn(typeEnum);
        ITypeOpt wrapped = typeOpt(typeId);
        when(typeEnum.wrapOption(typeId)).thenReturn(wrapped);
    }
}
