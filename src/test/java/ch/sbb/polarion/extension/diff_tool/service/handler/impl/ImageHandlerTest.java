package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import com.polarion.alm.tracker.model.IModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
