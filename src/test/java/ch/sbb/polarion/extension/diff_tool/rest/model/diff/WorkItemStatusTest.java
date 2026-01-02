package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkItemStatusTest {
    @Test
    void testBuilder() {
        WorkItemStatus workItemStatus = WorkItemStatus.builder().id("statusId").name("statusName").wiTypeId("typeId").wiTypeName("typeName").build();

        assertEquals("statusId", workItemStatus.getId());
        assertEquals("statusName", workItemStatus.getName());
        assertEquals("typeId", workItemStatus.getWiTypeId());
        assertEquals("typeName", workItemStatus.getWiTypeName());
    }

}
