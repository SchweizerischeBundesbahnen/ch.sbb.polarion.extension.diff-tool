package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.polarion.alm.tracker.model.IWorkItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MergeWorkItemsPairTest {

    @Test
    void testCreationWithIWorkItems() {
        IWorkItem leftMock = mock(IWorkItem.class);
        when(leftMock.getId()).thenReturn("left-1");
        IWorkItem rightMock = mock(IWorkItem.class);
        when(rightMock.getId()).thenReturn("right-1");

        List<MergeField> fields = List.of(
                MergeField.builder().id("title").selected(true).build(),
                MergeField.builder().id("description").selected(false).build()
        );

        MergeWorkItemsPair pair = new MergeWorkItemsPair(leftMock, rightMock, fields);

        assertEquals("left-1", pair.getLeftWorkItem().getId());
        assertEquals("right-1", pair.getRightWorkItem().getId());
        assertEquals(2, pair.getFieldsToMerge().size());
    }

    @Test
    void testCreationWithWorkItems() {
        WorkItem left = WorkItem.builder().id("left-1").build();
        WorkItem right = WorkItem.builder().id("right-1").build();

        List<MergeField> fields = List.of(MergeField.builder().id("title").selected(true).build());

        MergeWorkItemsPair pair = new MergeWorkItemsPair(left, right, fields);

        assertEquals("left-1", pair.getLeftWorkItem().getId());
        assertEquals("right-1", pair.getRightWorkItem().getId());
        assertEquals(1, pair.getFieldsToMerge().size());
    }

    @Test
    void testCreationWithNullRightWorkItem() {
        WorkItem left = WorkItem.builder().id("left-1").build();

        MergeWorkItemsPair pair = new MergeWorkItemsPair(left, null, List.of());

        assertEquals("left-1", pair.getLeftWorkItem().getId());
        assertNull(pair.getRightWorkItem());
    }

    @Test
    void testFieldSelectedForMerge_whenFieldsToMergeIsNull_returnsTrue() {
        MergeWorkItemsPair pair = new MergeWorkItemsPair();
        pair.setFieldsToMerge(null);

        DiffField diffField = DiffField.builder().key("title").build();
        assertTrue(pair.fieldSelectedForMerge(diffField));
    }

    @Test
    void testFieldSelectedForMerge_whenFieldsToMergeIsEmpty_returnsTrue() {
        MergeWorkItemsPair pair = new MergeWorkItemsPair();
        pair.setFieldsToMerge(Collections.emptyList());

        DiffField diffField = DiffField.builder().key("title").build();
        assertTrue(pair.fieldSelectedForMerge(diffField));
    }

    @Test
    void testFieldSelectedForMerge_whenFieldIsSelected_returnsTrue() {
        MergeWorkItemsPair pair = new MergeWorkItemsPair();
        pair.setFieldsToMerge(List.of(
                MergeField.builder().id("title").selected(true).build(),
                MergeField.builder().id("description").selected(false).build()
        ));

        assertTrue(pair.fieldSelectedForMerge(DiffField.builder().key("title").build()));
    }

    @Test
    void testFieldSelectedForMerge_whenFieldIsNotSelected_returnsFalse() {
        MergeWorkItemsPair pair = new MergeWorkItemsPair();
        pair.setFieldsToMerge(List.of(
                MergeField.builder().id("title").selected(true).build(),
                MergeField.builder().id("description").selected(false).build()
        ));

        assertFalse(pair.fieldSelectedForMerge(DiffField.builder().key("description").build()));
    }

    @Test
    void testFieldSelectedForMerge_whenFieldNotInList_returnsFalse() {
        MergeWorkItemsPair pair = new MergeWorkItemsPair();
        pair.setFieldsToMerge(List.of(
                MergeField.builder().id("title").selected(true).build()
        ));

        assertFalse(pair.fieldSelectedForMerge(DiffField.builder().key("status").build()));
    }

    @Test
    void testBuilder() {
        List<MergeField> fields = List.of(MergeField.builder().id("title").selected(true).build());

        MergeWorkItemsPair pair = MergeWorkItemsPair.builder()
                .leftWorkItem(WorkItem.builder().id("left-1").build())
                .rightWorkItem(WorkItem.builder().id("right-1").build())
                .fieldsToMerge(fields)
                .build();

        assertEquals("left-1", pair.getLeftWorkItem().getId());
        assertEquals("right-1", pair.getRightWorkItem().getId());
        assertEquals(fields, pair.getFieldsToMerge());
    }
}
