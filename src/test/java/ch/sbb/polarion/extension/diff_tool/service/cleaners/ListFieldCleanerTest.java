package ch.sbb.polarion.extension.diff_tool.service.cleaners;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.IType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListFieldCleanerTest {

    private final ListFieldCleaner cleaner = new ListFieldCleaner();

    @Test
    void testClean_listFieldWithKnownCleaner_cleansField() {
        IWorkItem workItem = mock(IWorkItem.class);
        WorkItemField field = WorkItemField.builder()
                .key(IWorkItem.KEY_APPROVALS)
                .required(false)
                .build();

        IListType listType = mock(IListType.class);
        when(workItem.getFieldType(IWorkItem.KEY_APPROVALS)).thenReturn(listType);
        when(workItem.getApprovals()).thenReturn(java.util.List.of());

        cleaner.clean(workItem, field);

        verify(workItem).getApprovals();
    }

    @Test
    void testClean_listFieldWithNoKnownCleaner_doesNothing() {
        IWorkItem workItem = mock(IWorkItem.class);
        WorkItemField field = WorkItemField.builder()
                .key("unknownListField")
                .required(false)
                .build();

        IListType listType = mock(IListType.class);
        when(workItem.getFieldType("unknownListField")).thenReturn(listType);

        cleaner.clean(workItem, field);

        verify(workItem).getFieldType("unknownListField");
        verifyNoMoreInteractions(workItem);
    }

    @Test
    void testClean_nonListField_doesNothing() {
        IWorkItem workItem = mock(IWorkItem.class);
        WorkItemField field = WorkItemField.builder()
                .key("description")
                .required(false)
                .build();

        IType type = mock(IType.class);
        when(workItem.getFieldType("description")).thenReturn(type);

        cleaner.clean(workItem, field);

        verify(workItem).getFieldType("description");
        verifyNoMoreInteractions(workItem);
    }

    @Test
    void testClean_requiredListField_doesNotClean() {
        IWorkItem workItem = mock(IWorkItem.class);
        WorkItemField field = WorkItemField.builder()
                .key(IWorkItem.KEY_APPROVALS)
                .required(true)
                .build();

        IListType listType = mock(IListType.class);
        when(workItem.getFieldType(IWorkItem.KEY_APPROVALS)).thenReturn(listType);

        cleaner.clean(workItem, field);

        verify(workItem).getFieldType(IWorkItem.KEY_APPROVALS);
        verifyNoMoreInteractions(workItem);
    }
}
