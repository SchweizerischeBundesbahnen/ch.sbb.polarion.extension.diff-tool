package ch.sbb.polarion.extension.diff_tool.navigation;

import com.polarion.subterra.base.data.identification.IContextId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CollectionsNodeTest {
    @Test
    void testCollectionsNode() {
        IContextId contextId = mock(IContextId.class);

        CollectionsNode collectionsNode = new CollectionsNode();
        assertEquals("compare-collections", collectionsNode.getId());
        assertEquals("Collections", collectionsNode.getLabel());
        assertEquals("/polarion/ria/images/topicIconsSmall/collectionsTopic.svg", collectionsNode.getIconUrl());
        when(contextId.getContextName()).thenReturn("context");
        assertEquals("/polarion/diff-tool-app/ui/app/topics.html?topic=compare-collections&sourceProjectId=context&buildId=null", collectionsNode.getPageUrl(contextId));
        when(contextId.getContextName()).thenReturn(null);
        assertEquals("/polarion/diff-tool-app/ui/app/topics.html?topic=compare-collections&sourceProjectId=&buildId=null", collectionsNode.getPageUrl(contextId));
        assertTrue(collectionsNode.getChildren().isEmpty());
        assertFalse(collectionsNode.requiresToken());
    }
}
