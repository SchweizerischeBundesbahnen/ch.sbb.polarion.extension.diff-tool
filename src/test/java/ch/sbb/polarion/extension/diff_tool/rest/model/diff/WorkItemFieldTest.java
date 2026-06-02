package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkItemFieldTest {
    @Test
    void testComparison() {
        WorkItemField workItemField1 = WorkItemField.builder().name("wi1").build();
        WorkItemField workItemField2 = WorkItemField.builder().name("wi2").build();

        assertEquals("wi1".compareTo("wi2"), workItemField1.compareTo(workItemField2));
    }

    @Test
    void shouldDistinguishFieldsWithSameNameAndTypeButDifferentKey() {
        WorkItemField field1 = WorkItemField.builder().key("key1").name("Priority").wiTypeName("Requirement").build();
        WorkItemField field2 = WorkItemField.builder().key("key2").name("Priority").wiTypeName("Requirement").build();

        assertTrue(field1.compareTo(field2) != 0);
        assertEquals("key1".compareTo("key2"), field1.compareTo(field2));
    }

    @Test
    void shouldKeepGlobalAndTypeSpecificCopiesDistinct() {
        // Same custom field surfaced once globally (no work item type) and once for a specific type: same key
        WorkItemField globalCopy = WorkItemField.builder().key("custom").name("Custom Field").build();
        WorkItemField typeCopy = WorkItemField.builder().key("custom").name("Custom Field").wiTypeName("Requirement").build();

        // wiTypeName still differentiates the two copies for ordering, but they remain distinct entries
        assertTrue(globalCopy.compareTo(typeCopy) != 0);
    }

    @Test
    void treeSetShouldKeepDistinctFieldsSharingTheSameName() {
        WorkItemField field1 = WorkItemField.builder().key("key1").name("Priority").wiTypeName("Requirement").build();
        WorkItemField field2 = WorkItemField.builder().key("key2").name("Priority").wiTypeName("Requirement").build();
        WorkItemField duplicateOfField1 = WorkItemField.builder().key("key1").name("Priority").wiTypeName("Requirement").build();

        Set<WorkItemField> fields = new TreeSet<>();
        fields.add(field1);
        fields.add(field2);
        fields.add(duplicateOfField1);

        // The two fields with distinct keys are both retained; the true duplicate (same key/name/type) is discarded
        assertEquals(2, fields.size());
    }

    @Test
    void compareNullableShouldReturnZeroWhenBothNull() {
        assertEquals(0, WorkItemField.compareNullable(null, null));
    }

    @Test
    void compareNullableShouldReturnPositiveWhenSecondIsNull() {
        assertEquals(1, WorkItemField.compareNullable("a", null));
    }

    @Test
    void compareNullableShouldReturnNegativeWhenFirstIsNull() {
        assertEquals(-1, WorkItemField.compareNullable(null, "a"));
    }

    @Test
    void compareNullableShouldReturnZeroWhenBothEqual() {
        assertEquals(0, WorkItemField.compareNullable("a", "a"));
    }

    @Test
    void compareNullableShouldDelegateToStringCompareWhenBothNonNull() {
        assertEquals("a".compareTo("b"), WorkItemField.compareNullable("a", "b"));
        assertEquals("b".compareTo("a"), WorkItemField.compareNullable("b", "a"));
        assertTrue(WorkItemField.compareNullable("a", "b") < 0);
        assertTrue(WorkItemField.compareNullable("b", "a") > 0);
    }
}
