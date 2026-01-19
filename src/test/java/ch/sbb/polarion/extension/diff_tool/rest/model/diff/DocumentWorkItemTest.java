package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentWorkItemTest {

    @Test
    void testNoArgsConstructor() {
        DocumentWorkItem workItem = new DocumentWorkItem();
        assertNull(workItem.getId());
        assertNull(workItem.getProjectId());
        assertNull(workItem.getRevision());
        assertNull(workItem.getDocumentProjectId());
        assertNull(workItem.getDocumentLocationPath());
        assertNull(workItem.getDocumentRevision());
    }

    @Test
    void testAllArgsConstructor() {
        DocumentWorkItem workItem = new DocumentWorkItem(
                "wiId", "wiProjectId", "wiRevision",
                "docProjectId", "docLocationPath", "docRevision"
        );

        assertEquals("wiId", workItem.getId());
        assertEquals("wiProjectId", workItem.getProjectId());
        assertEquals("wiRevision", workItem.getRevision());
        assertEquals("docProjectId", workItem.getDocumentProjectId());
        assertEquals("docLocationPath", workItem.getDocumentLocationPath());
        assertEquals("docRevision", workItem.getDocumentRevision());
    }

    @Test
    void testBuilder() {
        DocumentWorkItem workItem = DocumentWorkItem.builder()
                .id("wiId")
                .projectId("wiProjectId")
                .revision("wiRevision")
                .documentProjectId("docProjectId")
                .documentLocationPath("docLocationPath")
                .documentRevision("docRevision")
                .build();

        assertEquals("wiId", workItem.getId());
        assertEquals("wiProjectId", workItem.getProjectId());
        assertEquals("wiRevision", workItem.getRevision());
        assertEquals("docProjectId", workItem.getDocumentProjectId());
        assertEquals("docLocationPath", workItem.getDocumentLocationPath());
        assertEquals("docRevision", workItem.getDocumentRevision());
    }

    @Test
    void testSetters() {
        DocumentWorkItem workItem = new DocumentWorkItem();

        workItem.setId("wiId");
        workItem.setProjectId("wiProjectId");
        workItem.setRevision("wiRevision");
        workItem.setDocumentProjectId("docProjectId");
        workItem.setDocumentLocationPath("docLocationPath");
        workItem.setDocumentRevision("docRevision");

        assertEquals("wiId", workItem.getId());
        assertEquals("wiProjectId", workItem.getProjectId());
        assertEquals("wiRevision", workItem.getRevision());
        assertEquals("docProjectId", workItem.getDocumentProjectId());
        assertEquals("docLocationPath", workItem.getDocumentLocationPath());
        assertEquals("docRevision", workItem.getDocumentRevision());
    }

    @Test
    void testInheritanceFromProjectWorkItem() {
        DocumentWorkItem workItem = new DocumentWorkItem();

        assertTrue(workItem instanceof ProjectWorkItem);
    }

    @Test
    void testEqualsAndHashCode() {
        DocumentWorkItem workItem1 = new DocumentWorkItem(
                "wiId", "wiProjectId", "wiRevision",
                "docProjectId", "docLocationPath", "docRevision"
        );
        DocumentWorkItem workItem2 = new DocumentWorkItem(
                "wiId", "wiProjectId", "wiRevision",
                "docProjectId", "docLocationPath", "docRevision"
        );
        DocumentWorkItem workItem3 = new DocumentWorkItem(
                "differentId", "wiProjectId", "wiRevision",
                "docProjectId", "docLocationPath", "docRevision"
        );

        assertEquals(workItem1, workItem2);
        assertEquals(workItem1.hashCode(), workItem2.hashCode());
        assertNotEquals(workItem1, workItem3);
    }
}
