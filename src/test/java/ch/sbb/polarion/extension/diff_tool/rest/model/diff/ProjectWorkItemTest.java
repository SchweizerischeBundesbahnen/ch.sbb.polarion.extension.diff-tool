package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectWorkItemTest {

    @Test
    void testNoArgsConstructor() {
        ProjectWorkItem workItem = new ProjectWorkItem();
        assertNull(workItem.getId());
        assertNull(workItem.getProjectId());
        assertNull(workItem.getRevision());
    }

    @Test
    void testAllArgsConstructor() {
        ProjectWorkItem workItem = new ProjectWorkItem("wiId", "projectId", "revision");

        assertEquals("wiId", workItem.getId());
        assertEquals("projectId", workItem.getProjectId());
        assertEquals("revision", workItem.getRevision());
    }

    @Test
    void testBuilder() {
        ProjectWorkItem workItem = ProjectWorkItem.builder()
                .id("wiId")
                .projectId("projectId")
                .revision("revision")
                .build();

        assertEquals("wiId", workItem.getId());
        assertEquals("projectId", workItem.getProjectId());
        assertEquals("revision", workItem.getRevision());
    }

    @Test
    void testSetters() {
        ProjectWorkItem workItem = new ProjectWorkItem();

        workItem.setId("newId");
        workItem.setProjectId("newProjectId");
        workItem.setRevision("newRevision");

        assertEquals("newId", workItem.getId());
        assertEquals("newProjectId", workItem.getProjectId());
        assertEquals("newRevision", workItem.getRevision());
    }

    @Test
    void testEqualsAndHashCode() {
        ProjectWorkItem workItem1 = new ProjectWorkItem("id", "project", "rev");
        ProjectWorkItem workItem2 = new ProjectWorkItem("id", "project", "rev");
        ProjectWorkItem workItem3 = new ProjectWorkItem("differentId", "project", "rev");

        assertEquals(workItem1, workItem2);
        assertEquals(workItem1.hashCode(), workItem2.hashCode());
        assertNotEquals(workItem1, workItem3);
    }

    @Test
    void testToString() {
        ProjectWorkItem workItem = new ProjectWorkItem("id", "project", "rev");

        String toString = workItem.toString();

        assertTrue(toString.contains("id"));
        assertTrue(toString.contains("project"));
        assertTrue(toString.contains("rev"));
    }
}
