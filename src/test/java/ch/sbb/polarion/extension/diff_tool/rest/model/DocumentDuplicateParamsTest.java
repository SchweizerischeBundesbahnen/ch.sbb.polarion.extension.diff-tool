package ch.sbb.polarion.extension.diff_tool.rest.model;

import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentDuplicateParamsTest {

    @Test
    void testBuilderCreatesInstance() {
        BaseDocumentIdentifier targetDoc = new BaseDocumentIdentifier("project", "space", "doc");

        DocumentDuplicateParams params = DocumentDuplicateParams.builder()
                .targetDocumentIdentifier(targetDoc)
                .targetDocumentTitle("Test Title")
                .linkRoleId("linkRole1")
                .configName("customConfig")
                .handleReferences(HandleReferencesType.KEEP)
                .build();

        assertEquals(targetDoc, params.getTargetDocumentIdentifier());
        assertEquals("Test Title", params.getTargetDocumentTitle());
        assertEquals("linkRole1", params.getLinkRoleId());
        assertEquals("customConfig", params.getConfigName());
        assertEquals(HandleReferencesType.KEEP, params.getHandleReferences());
    }

    @Test
    void testNoArgsConstructor() {
        DocumentDuplicateParams params = new DocumentDuplicateParams();

        assertNull(params.getTargetDocumentIdentifier());
        assertNull(params.getTargetDocumentTitle());
        assertNull(params.getLinkRoleId());
        assertNull(params.getHandleReferences());
    }

    @Test
    void testAllArgsConstructor() {
        BaseDocumentIdentifier targetDoc = new BaseDocumentIdentifier("project", "space", "doc");

        DocumentDuplicateParams params = new DocumentDuplicateParams(
                targetDoc, "Title", "role", "config", HandleReferencesType.CREATE_MISSING);

        assertEquals(targetDoc, params.getTargetDocumentIdentifier());
        assertEquals("Title", params.getTargetDocumentTitle());
        assertEquals("role", params.getLinkRoleId());
        assertEquals("config", params.getConfigName());
        assertEquals(HandleReferencesType.CREATE_MISSING, params.getHandleReferences());
    }

    @Test
    void testGetConfigNameReturnsDefaultWhenNull() {
        DocumentDuplicateParams params = new DocumentDuplicateParams();
        params.setConfigName(null);

        assertEquals(NamedSettings.DEFAULT_NAME, params.getConfigName());
    }

    @Test
    void testGetConfigNameReturnsSetValueWhenNotNull() {
        DocumentDuplicateParams params = new DocumentDuplicateParams();
        params.setConfigName("customConfig");

        assertEquals("customConfig", params.getConfigName());
    }

    @Test
    void testSettersAndGetters() {
        DocumentDuplicateParams params = new DocumentDuplicateParams();
        BaseDocumentIdentifier targetDoc = new BaseDocumentIdentifier("proj", "sp", "name");

        params.setTargetDocumentIdentifier(targetDoc);
        params.setTargetDocumentTitle("New Title");
        params.setLinkRoleId("newRole");
        params.setConfigName("newConfig");
        params.setHandleReferences(HandleReferencesType.ALWAYS_OVERWRITE);

        assertEquals(targetDoc, params.getTargetDocumentIdentifier());
        assertEquals("New Title", params.getTargetDocumentTitle());
        assertEquals("newRole", params.getLinkRoleId());
        assertEquals("newConfig", params.getConfigName());
        assertEquals(HandleReferencesType.ALWAYS_OVERWRITE, params.getHandleReferences());
    }

    @Test
    void testEqualsAndHashCode() {
        BaseDocumentIdentifier targetDoc = new BaseDocumentIdentifier("project", "space", "doc");

        DocumentDuplicateParams params1 = DocumentDuplicateParams.builder()
                .targetDocumentIdentifier(targetDoc)
                .targetDocumentTitle("Title")
                .linkRoleId("role")
                .configName("config")
                .handleReferences(HandleReferencesType.DEFAULT)
                .build();

        DocumentDuplicateParams params2 = DocumentDuplicateParams.builder()
                .targetDocumentIdentifier(targetDoc)
                .targetDocumentTitle("Title")
                .linkRoleId("role")
                .configName("config")
                .handleReferences(HandleReferencesType.DEFAULT)
                .build();

        assertEquals(params1, params2);
        assertEquals(params1.hashCode(), params2.hashCode());
    }

    @Test
    void testToString() {
        DocumentDuplicateParams params = DocumentDuplicateParams.builder()
                .targetDocumentTitle("Title")
                .build();

        String toString = params.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("targetDocumentTitle=Title"));
        assertTrue(toString.contains("DocumentDuplicateParams"));
    }
}
