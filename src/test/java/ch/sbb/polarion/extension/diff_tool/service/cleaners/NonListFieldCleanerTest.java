package ch.sbb.polarion.extension.diff_tool.service.cleaners;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.subterra.base.data.model.IEnumType;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.IType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NonListFieldCleanerTest {

    private final NonListFieldCleaner cleaner = new NonListFieldCleaner();

    @Test
    void testClean_requiredCustomField_setsDefaultValue() {
        IWorkItem workItem = mock(IWorkItem.class);
        WorkItemField field = WorkItemField.builder()
                .key("customKey")
                .required(true)
                .customField(true)
                .defaultValue("defaultVal")
                .build();

        cleaner.clean(workItem, field);

        verify(workItem).setValue("customKey", "defaultVal");
    }

    @Test
    void testClean_requiredStandardEnumField_setsDefaultEnumOption() {
        IWorkItem workItem = mock(IWorkItem.class, RETURNS_DEEP_STUBS);
        WorkItemField field = WorkItemField.builder()
                .key("status")
                .required(true)
                .customField(false)
                .build();

        IEnumType enumType = mock(IEnumType.class);
        when(workItem.getPrototype().getKeyType("status")).thenReturn(enumType);

        IEnumeration<?> enumeration = mock(IEnumeration.class);
        when(workItem.getDataSvc().getEnumerationForKey(any(), eq("status"), any())).thenReturn(enumeration);

        IEnumOption defaultOption = mock(IEnumOption.class);
        when(enumeration.getDefaultOption(null)).thenReturn(defaultOption);

        cleaner.clean(workItem, field);

        verify(workItem).setValue("status", defaultOption);
    }

    @Test
    void testClean_requiredStandardEnumField_noDefaultOption() {
        IWorkItem workItem = mock(IWorkItem.class, RETURNS_DEEP_STUBS);
        WorkItemField field = WorkItemField.builder()
                .key("status")
                .required(true)
                .customField(false)
                .build();

        IEnumType enumType = mock(IEnumType.class);
        when(workItem.getPrototype().getKeyType("status")).thenReturn(enumType);

        IEnumeration<?> enumeration = mock(IEnumeration.class);
        when(workItem.getDataSvc().getEnumerationForKey(any(), eq("status"), any())).thenReturn(enumeration);
        when(enumeration.getDefaultOption(null)).thenReturn(null);

        cleaner.clean(workItem, field);

        verify(workItem, never()).setValue(eq("status"), any());
    }

    @Test
    void testClean_requiredStandardNonEnumField_doesNotSetValue() {
        IWorkItem workItem = mock(IWorkItem.class, RETURNS_DEEP_STUBS);
        WorkItemField field = WorkItemField.builder()
                .key("title")
                .required(true)
                .customField(false)
                .build();

        IType nonEnumType = mock(IType.class);
        when(workItem.getPrototype().getKeyType("title")).thenReturn(nonEnumType);

        cleaner.clean(workItem, field);

        verify(workItem, never()).setValue(eq("title"), any());
    }

    @Test
    void testClean_nonRequiredNonListField_setsNull() {
        IWorkItem workItem = mock(IWorkItem.class);
        WorkItemField field = WorkItemField.builder()
                .key("description")
                .required(false)
                .build();

        when(workItem.getFieldType("description")).thenReturn(mock(IType.class));

        cleaner.clean(workItem, field);

        verify(workItem).setValue("description", null);
    }

    @Test
    void testClean_nonRequiredListField_doesNotSetValue() {
        IWorkItem workItem = mock(IWorkItem.class);
        WorkItemField field = WorkItemField.builder()
                .key("approvals")
                .required(false)
                .build();

        when(workItem.getFieldType("approvals")).thenReturn(mock(IListType.class));

        cleaner.clean(workItem, field);

        verify(workItem, never()).setValue(eq("approvals"), any());
    }
}
