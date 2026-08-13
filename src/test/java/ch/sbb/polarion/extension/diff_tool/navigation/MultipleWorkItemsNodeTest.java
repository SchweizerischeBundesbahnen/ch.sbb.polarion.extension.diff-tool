package ch.sbb.polarion.extension.diff_tool.navigation;

import com.polarion.subterra.base.data.identification.IContextId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultipleWorkItemsNodeTest {
    @Test
    void testMultipleWorkItemsNode() {
        IContextId contextId = mock(IContextId.class);

        MultipleWorkItemsNode multipleWorkItemsNode = new MultipleWorkItemsNode();
        assertEquals("compare-work-items", multipleWorkItemsNode.getId());
        assertEquals("Multiple Work Items", multipleWorkItemsNode.getLabel());
        assertEquals("/polarion/ria/images/topicIconsSmall/workItems.svg", multipleWorkItemsNode.getIconUrl());
        when(contextId.getContextName()).thenReturn("context");
        assertEquals("/polarion/diff-tool-app/ui/app/topics.html?topic=compare-work-items&sourceProjectId=context&buildId=null", multipleWorkItemsNode.getPageUrl(contextId));
        when(contextId.getContextName()).thenReturn(null);
        assertEquals("/polarion/diff-tool-app/ui/app/topics.html?topic=compare-work-items&sourceProjectId=&buildId=null", multipleWorkItemsNode.getPageUrl(contextId));
        assertTrue(multipleWorkItemsNode.getChildren().isEmpty());
        assertFalse(multipleWorkItemsNode.requiresToken());
    }
}
