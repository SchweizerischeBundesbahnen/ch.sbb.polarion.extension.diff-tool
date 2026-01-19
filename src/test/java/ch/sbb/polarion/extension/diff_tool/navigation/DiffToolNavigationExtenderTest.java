package ch.sbb.polarion.extension.diff_tool.navigation;

import com.polarion.alm.ui.server.navigation.NavigationExtenderNode;
import com.polarion.subterra.base.data.identification.IContextId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DiffToolNavigationExtenderTest {
    @Test
    void testDiffToolNavigationExtender() {
        IContextId contextId = mock(IContextId.class);

        DiffToolNavigationExtender navigationExtender = new DiffToolNavigationExtender();
        assertEquals("diff-tool", navigationExtender.getId());
        assertEquals("Diff Tool", navigationExtender.getLabel());
        assertEquals("/polarion/icons/default/topicIcons/documentsAndWiki.svg", navigationExtender.getIconUrl());
        assertEquals("/polarion/diff-tool/pages/diff-tool.jsp", navigationExtender.getPageUrl(contextId));
        List<NavigationExtenderNode> rootNodes = navigationExtender.getRootNodes(contextId);
        assertEquals(2, rootNodes.size());
        assertTrue(allowedRootNode(rootNodes.get(0)));
        assertTrue(allowedRootNode(rootNodes.get(1)));
        assertFalse(navigationExtender.requiresToken());
    }

    private boolean allowedRootNode(NavigationExtenderNode node) {
        return node instanceof MultipleWorkItemsNode || node instanceof CollectionsNode;
    }
}
