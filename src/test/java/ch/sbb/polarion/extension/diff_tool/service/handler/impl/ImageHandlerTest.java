package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPairDiffParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.util.DiffModelCachedResource;
import com.polarion.alm.tracker.model.IModule;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageHandlerTest {
    @Test
    void testCalculateHash() {
        WorkItem workItem = mock(WorkItem.class);
        when(workItem.getId()).thenReturn("testId");
        when(workItem.getProjectId()).thenReturn("testProjectId");

        ImageHandler imageHandler = spy(new ImageHandler());
        doReturn("test".getBytes()).when(imageHandler).loadImage("/polarion/wi-attachment/testProjectId/testId/test.png");

        assertEquals("<test><img src=\"workitemimg:test.png\" md5=\"098f6bcd4621d373cade4e832627b4f6\"\"/></test>", imageHandler.calculateHash("<test><img src=\"workitemimg:test.png\"/></test>", workItem));
    }

    @Test
    void testCalculateHashReturnsNullForNullHtml() {
        // Branched documents can produce header/unpaired items with no rendered content - html comes in as null
        assertNull(new ImageHandler().calculateHash(null, mock(WorkItem.class)));
        assertNull(new ImageHandler().calculateHash(null, null));
    }

    @Test
    void testFixImagePathReturnsUrlUnchangedForNullWorkItem() {
        // Unpaired items (e.g. right paragraphs added in a branch) have no work item to resolve the attachment path against
        String url = "workitemimg:test.png";
        assertSame(url, new ImageHandler().fixImagePath(url, null));
    }

    @Test
    void testFixImagePath() {
        WorkItem workItem = mock(WorkItem.class);
        when(workItem.getId()).thenReturn("testId");
        when(workItem.getProjectId()).thenReturn("testProjectId");
        assertEquals("/polarion/wi-attachment/testProjectId/testId/test.png", new ImageHandler().fixImagePath("workitemimg:test.png", workItem));

        IModule iModule = mock(IModule.class);
        when(iModule.getModuleFolder()).thenReturn("testSpace");
        when(iModule.getId()).thenReturn("testModuleId");
        when(workItem.getModule()).thenReturn(iModule);
        assertEquals("/polarion/module-attachment/testProjectId/testSpace/testModuleId/test.png", new ImageHandler().fixImagePath("attachment:test.png", workItem));
    }

    @Test
    void testPreProcessForEmptyWorkItems() {
        Pair<String, String> htmlPair = Pair.of("left", "right");
        try (MockedStatic<DiffModelCachedResource> mockDiffModelCachedResource = mockStatic(DiffModelCachedResource.class)) {
            mockDiffModelCachedResource.when(() -> DiffModelCachedResource.get(any(), any(), any())).thenReturn(DiffModel.builder().build());
            DiffContext context = new DiffContext(null, null, "test", WorkItemsPairDiffParams.builder().pairedWorkItemsDiffer(true).build(), mock(PolarionService.class));
            assertEquals(htmlPair, new ImageHandler().preProcess(htmlPair, context));
        }
    }
}
