package ch.sbb.polarion.extension.diff_tool.navigation;

import com.polarion.subterra.base.data.identification.IContextId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentsNodeTest {
    @Test
    void testDocumentsNode() {
        IContextId contextId = mock(IContextId.class);

        DocumentsNode documentsNode = new DocumentsNode();
        assertEquals("documents-diff-to-pdf", documentsNode.getId());
        assertEquals("Documents diff to PDF", documentsNode.getLabel());
        assertEquals("/polarion/ria/images/topicIconsSmall/documentsAndWiki.svg", documentsNode.getIconUrl());
        when(contextId.getContextName()).thenReturn("context");
        assertEquals("/polarion/diff-tool-app/ui/app/topics.html?topic=documents-diff-to-pdf&sourceProjectId=context&buildId=null", documentsNode.getPageUrl(contextId));
        when(contextId.getContextName()).thenReturn(null);
        assertEquals("/polarion/diff-tool-app/ui/app/topics.html?topic=documents-diff-to-pdf&sourceProjectId=&buildId=null", documentsNode.getPageUrl(contextId));
        assertTrue(documentsNode.getChildren().isEmpty());
        assertFalse(documentsNode.requiresToken());
    }
}
