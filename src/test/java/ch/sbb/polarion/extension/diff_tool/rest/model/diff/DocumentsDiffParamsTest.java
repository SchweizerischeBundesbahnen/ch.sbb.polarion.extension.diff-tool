package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentsDiffParamsTest {

    @Test
    void testNoArgsConstructor() {
        DocumentsDiffParams params = new DocumentsDiffParams();
        assertNull(params.getLeftDocument());
        assertNull(params.getRightDocument());
        assertNull(params.getLinkRole());
        assertEquals(NamedSettings.DEFAULT_NAME, params.getConfigName());
        assertNull(params.getConfigCacheBucketId());
    }

    @Test
    void testAllArgsConstructor() {
        DocumentIdentifier leftDoc = DocumentIdentifier.builder()
                .projectId("leftProject")
                .spaceId("leftSpace")
                .name("leftDoc")
                .build();
        DocumentIdentifier rightDoc = DocumentIdentifier.builder()
                .projectId("rightProject")
                .spaceId("rightSpace")
                .name("rightDoc")
                .build();

        DocumentsDiffParams params = new DocumentsDiffParams(leftDoc, rightDoc, "linkRole", "customConfig", "bucketId");

        assertEquals(leftDoc, params.getLeftDocument());
        assertEquals(rightDoc, params.getRightDocument());
        assertEquals("linkRole", params.getLinkRole());
        assertEquals("customConfig", params.getConfigName());
        assertEquals("bucketId", params.getConfigCacheBucketId());
    }

    @Test
    void testGetConfigNameReturnsDefaultWhenNull() {
        DocumentsDiffParams params = new DocumentsDiffParams();
        params.setConfigName(null);

        assertEquals(NamedSettings.DEFAULT_NAME, params.getConfigName());
    }

    @Test
    void testGetConfigNameReturnsCustomValue() {
        DocumentsDiffParams params = new DocumentsDiffParams();
        params.setConfigName("customConfig");

        assertEquals("customConfig", params.getConfigName());
    }

    @Test
    void testSettersAndGetters() {
        DocumentsDiffParams params = new DocumentsDiffParams();

        DocumentIdentifier leftDoc = DocumentIdentifier.builder().projectId("project1").build();
        DocumentIdentifier rightDoc = DocumentIdentifier.builder().projectId("project2").build();

        params.setLeftDocument(leftDoc);
        params.setRightDocument(rightDoc);
        params.setLinkRole("testRole");
        params.setConfigName("testConfig");
        params.setConfigCacheBucketId("testBucket");

        assertEquals(leftDoc, params.getLeftDocument());
        assertEquals(rightDoc, params.getRightDocument());
        assertEquals("testRole", params.getLinkRole());
        assertEquals("testConfig", params.getConfigName());
        assertEquals("testBucket", params.getConfigCacheBucketId());
    }
}
