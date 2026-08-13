package ch.sbb.polarion.extension.diff_tool.navigation;

import com.polarion.alm.ui.server.navigation.NavigationExtenderNode;
import com.polarion.subterra.base.data.identification.IContextId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiffToolNavigationExtenderTest {
    @Test
    void testDiffToolNavigationExtender() {
        IContextId contextId = mock(IContextId.class);

        DiffToolNavigationExtender navigationExtender = new DiffToolNavigationExtender();
        assertEquals("diff-tool", navigationExtender.getId());
        assertEquals("Diff Tool", navigationExtender.getLabel());
        assertEquals("/polarion/diff-tool-app/ui/images/menu/30x30/_parent.svg", navigationExtender.getIconUrl());
        assertEquals("/polarion/diff-tool-app/ui/app/topics.html?topic=diff-tool&sourceProjectId=&buildId=null",
                navigationExtender.getPageUrl(contextId));
        List<NavigationExtenderNode> rootNodes = navigationExtender.getRootNodes(contextId);
        assertEquals(2, rootNodes.size());
        assertTrue(allowedRootNode(rootNodes.get(0)));
        assertTrue(allowedRootNode(rootNodes.get(1)));
        assertFalse(navigationExtender.requiresToken());
    }

    @Test
    void testTopicPageUrlLeavesProjectEmptyOutsideProjectScope() {
        // Polarion passes null at repository scope and a dash-prefixed name for a project group
        assertTrue(DiffToolNavigationExtender.topicPageUrl("compare-work-items", contextId(null)).contains("&sourceProjectId=&"));
        assertTrue(DiffToolNavigationExtender.topicPageUrl("compare-work-items", contextId("-someGroup")).contains("&sourceProjectId=&"));
        assertTrue(DiffToolNavigationExtender.topicPageUrl("compare-work-items", null).contains("&sourceProjectId=&"));
        assertTrue(DiffToolNavigationExtender.topicPageUrl("compare-work-items", contextId("elibrary")).contains("&sourceProjectId=elibrary&"));
    }

    private IContextId contextId(String contextName) {
        IContextId contextId = mock(IContextId.class);
        when(contextId.getContextName()).thenReturn(contextName);
        return contextId;
    }

    private boolean allowedRootNode(NavigationExtenderNode node) {
        return node instanceof MultipleWorkItemsNode || node instanceof CollectionsNode;
    }
}
