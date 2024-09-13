package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkItemFieldTest {
    @Test
    void testComparison() {
        WorkItemField workItemField1 = WorkItemField.builder().name("wi1").build();
        WorkItemField workItemField2 = WorkItemField.builder().name("wi2").build();

        assertEquals("wi1".compareTo("wi2"), workItemField1.compareTo(workItemField2));
    }
}
