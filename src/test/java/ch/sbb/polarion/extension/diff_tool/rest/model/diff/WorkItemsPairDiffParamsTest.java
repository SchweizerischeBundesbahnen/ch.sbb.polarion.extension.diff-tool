package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkItemsPairDiffParamsTest {

    @Test
    void testNoArgsConstructor() {
        WorkItemsPairDiffParams<ProjectWorkItem> params = new WorkItemsPairDiffParams<>();
        assertNull(params.getLeftProjectId());
        assertNull(params.getLeftWorkItem());
        assertNull(params.getRightWorkItem());
        assertNull(params.getPairedWorkItemsLinkRole());
        assertNull(params.getPairedWorkItemsDiffer());
        assertNull(params.getCompareEnumsById());
        assertNull(params.getConfigName());
        assertNull(params.getConfigCacheBucketId());
    }

    @Test
    void testAllArgsConstructor() {
        ProjectWorkItem leftWorkItem = ProjectWorkItem.builder().id("leftId").projectId("leftProject").build();
        ProjectWorkItem rightWorkItem = ProjectWorkItem.builder().id("rightId").projectId("rightProject").build();

        WorkItemsPairDiffParams<ProjectWorkItem> params = new WorkItemsPairDiffParams<>(
                "leftProjectId", leftWorkItem, rightWorkItem,
                "linkRole", true, false, "configName", "bucketId"
        );

        assertEquals("leftProjectId", params.getLeftProjectId());
        assertEquals(leftWorkItem, params.getLeftWorkItem());
        assertEquals(rightWorkItem, params.getRightWorkItem());
        assertEquals("linkRole", params.getPairedWorkItemsLinkRole());
        assertTrue(params.getPairedWorkItemsDiffer());
        assertFalse(params.getCompareEnumsById());
        assertEquals("configName", params.getConfigName());
        assertEquals("bucketId", params.getConfigCacheBucketId());
    }

    @Test
    void testBuilder() {
        ProjectWorkItem leftWorkItem = ProjectWorkItem.builder().id("leftId").build();
        ProjectWorkItem rightWorkItem = ProjectWorkItem.builder().id("rightId").build();

        WorkItemsPairDiffParams<ProjectWorkItem> params = WorkItemsPairDiffParams.<ProjectWorkItem>builder()
                .leftProjectId("leftProjectId")
                .leftWorkItem(leftWorkItem)
                .rightWorkItem(rightWorkItem)
                .pairedWorkItemsLinkRole("linkRole")
                .pairedWorkItemsDiffer(true)
                .compareEnumsById(true)
                .configName("configName")
                .configCacheBucketId("bucketId")
                .build();

        assertEquals("leftProjectId", params.getLeftProjectId());
        assertEquals(leftWorkItem, params.getLeftWorkItem());
        assertEquals(rightWorkItem, params.getRightWorkItem());
        assertEquals("linkRole", params.getPairedWorkItemsLinkRole());
        assertTrue(params.getPairedWorkItemsDiffer());
        assertTrue(params.getCompareEnumsById());
        assertEquals("configName", params.getConfigName());
        assertEquals("bucketId", params.getConfigCacheBucketId());
    }

    @Test
    void testIsPairedWorkItemsDifferWhenTrue() {
        WorkItemsPairDiffParams<ProjectWorkItem> params = new WorkItemsPairDiffParams<>();
        params.setPairedWorkItemsDiffer(true);

        assertTrue(params.isPairedWorkItemsDiffer());
    }

    @Test
    void testIsPairedWorkItemsDifferWhenFalse() {
        WorkItemsPairDiffParams<ProjectWorkItem> params = new WorkItemsPairDiffParams<>();
        params.setPairedWorkItemsDiffer(false);

        assertFalse(params.isPairedWorkItemsDiffer());
    }

    @Test
    void testIsPairedWorkItemsDifferWhenNull() {
        WorkItemsPairDiffParams<ProjectWorkItem> params = new WorkItemsPairDiffParams<>();
        params.setPairedWorkItemsDiffer(null);

        assertFalse(params.isPairedWorkItemsDiffer());
    }

    @Test
    void testIsCompareEnumsByIdWhenTrue() {
        WorkItemsPairDiffParams<ProjectWorkItem> params = new WorkItemsPairDiffParams<>();
        params.setCompareEnumsById(true);

        assertTrue(params.isCompareEnumsById());
    }

    @Test
    void testIsCompareEnumsByIdWhenFalse() {
        WorkItemsPairDiffParams<ProjectWorkItem> params = new WorkItemsPairDiffParams<>();
        params.setCompareEnumsById(false);

        assertFalse(params.isCompareEnumsById());
    }

    @Test
    void testIsCompareEnumsByIdWhenNull() {
        WorkItemsPairDiffParams<ProjectWorkItem> params = new WorkItemsPairDiffParams<>();
        params.setCompareEnumsById(null);

        assertFalse(params.isCompareEnumsById());
    }

    @Test
    void testSetters() {
        WorkItemsPairDiffParams<ProjectWorkItem> params = new WorkItemsPairDiffParams<>();

        ProjectWorkItem leftWorkItem = ProjectWorkItem.builder().id("leftId").build();
        ProjectWorkItem rightWorkItem = ProjectWorkItem.builder().id("rightId").build();

        params.setLeftProjectId("newLeftProjectId");
        params.setLeftWorkItem(leftWorkItem);
        params.setRightWorkItem(rightWorkItem);
        params.setPairedWorkItemsLinkRole("newLinkRole");
        params.setPairedWorkItemsDiffer(true);
        params.setCompareEnumsById(true);
        params.setConfigName("newConfigName");
        params.setConfigCacheBucketId("newBucketId");

        assertEquals("newLeftProjectId", params.getLeftProjectId());
        assertEquals(leftWorkItem, params.getLeftWorkItem());
        assertEquals(rightWorkItem, params.getRightWorkItem());
        assertEquals("newLinkRole", params.getPairedWorkItemsLinkRole());
        assertTrue(params.getPairedWorkItemsDiffer());
        assertTrue(params.getCompareEnumsById());
        assertEquals("newConfigName", params.getConfigName());
        assertEquals("newBucketId", params.getConfigCacheBucketId());
    }
}
