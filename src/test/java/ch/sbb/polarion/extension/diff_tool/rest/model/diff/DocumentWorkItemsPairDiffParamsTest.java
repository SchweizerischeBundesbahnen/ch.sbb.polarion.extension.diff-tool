package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentWorkItemsPairDiffParamsTest {

    @Test
    void testNoArgsConstructor() {
        DocumentWorkItemsPairDiffParams params = new DocumentWorkItemsPairDiffParams();
        assertNull(params.getLeftWorkItem());
        assertNull(params.getRightWorkItem());
    }

    @Test
    void testInheritanceFromWorkItemsPairDiffParams() {
        DocumentWorkItemsPairDiffParams params = new DocumentWorkItemsPairDiffParams();

        assertTrue(params instanceof WorkItemsPairDiffParams);
    }

    @Test
    void testSetAndGetLeftWorkItem() {
        DocumentWorkItemsPairDiffParams params = new DocumentWorkItemsPairDiffParams();

        DocumentWorkItem leftWorkItem = DocumentWorkItem.builder()
                .id("leftId")
                .projectId("leftProject")
                .documentProjectId("docProject")
                .documentLocationPath("docPath")
                .build();

        params.setLeftWorkItem(leftWorkItem);

        assertEquals(leftWorkItem, params.getLeftWorkItem());
        assertEquals("leftId", params.getLeftWorkItem().getId());
        assertEquals("leftProject", params.getLeftWorkItem().getProjectId());
        assertEquals("docProject", params.getLeftWorkItem().getDocumentProjectId());
        assertEquals("docPath", params.getLeftWorkItem().getDocumentLocationPath());
    }

    @Test
    void testSetAndGetRightWorkItem() {
        DocumentWorkItemsPairDiffParams params = new DocumentWorkItemsPairDiffParams();

        DocumentWorkItem rightWorkItem = DocumentWorkItem.builder()
                .id("rightId")
                .projectId("rightProject")
                .documentProjectId("docProject")
                .documentLocationPath("docPath")
                .build();

        params.setRightWorkItem(rightWorkItem);

        assertEquals(rightWorkItem, params.getRightWorkItem());
        assertEquals("rightId", params.getRightWorkItem().getId());
    }

    @Test
    void testInheritedMethods() {
        DocumentWorkItemsPairDiffParams params = new DocumentWorkItemsPairDiffParams();

        params.setLeftProjectId("testProjectId");
        params.setPairedWorkItemsLinkRole("testLinkRole");
        params.setPairedWorkItemsDiffer(true);
        params.setCompareEnumsById(false);
        params.setConfigName("testConfig");

        assertEquals("testProjectId", params.getLeftProjectId());
        assertEquals("testLinkRole", params.getPairedWorkItemsLinkRole());
        assertTrue(params.isPairedWorkItemsDiffer());
        assertFalse(params.isCompareEnumsById());
        assertEquals("testConfig", params.getConfigName());
    }

    @Test
    void testWithBothWorkItems() {
        DocumentWorkItemsPairDiffParams params = new DocumentWorkItemsPairDiffParams();

        DocumentWorkItem leftWorkItem = new DocumentWorkItem(
                "leftWiId", "leftWiProject", "leftWiRev",
                "leftDocProject", "leftDocPath", "leftDocRev"
        );
        DocumentWorkItem rightWorkItem = new DocumentWorkItem(
                "rightWiId", "rightWiProject", "rightWiRev",
                "rightDocProject", "rightDocPath", "rightDocRev"
        );

        params.setLeftWorkItem(leftWorkItem);
        params.setRightWorkItem(rightWorkItem);

        assertEquals("leftWiId", params.getLeftWorkItem().getId());
        assertEquals("leftDocPath", params.getLeftWorkItem().getDocumentLocationPath());
        assertEquals("rightWiId", params.getRightWorkItem().getId());
        assertEquals("rightDocPath", params.getRightWorkItem().getDocumentLocationPath());
    }
}
