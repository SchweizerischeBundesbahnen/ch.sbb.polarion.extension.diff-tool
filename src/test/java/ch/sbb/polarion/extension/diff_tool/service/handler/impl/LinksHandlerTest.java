package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsDiffParams;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import com.polarion.alm.tracker.model.IWorkItem;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinksHandlerTest {

    @Test
    @SneakyThrows
    void testAppendPairedWorkItemIdUsingRevision() {

        try (InputStream isUnprocessedHtml = this.getClass().getResourceAsStream("/pairedWorkItem.html");
             InputStream isExpectedResultHtml = this.getClass().getResourceAsStream("/pairedWorkItemResult.html")) {

            String unprocessedHtml = new String(Objects.requireNonNull(isUnprocessedHtml).readAllBytes(), StandardCharsets.UTF_8);
            String expectedHtml = new String(Objects.requireNonNull(isExpectedResultHtml).readAllBytes(), StandardCharsets.UTF_8);

            String projectId = "elibrary";

            PolarionService polarionService = mock(PolarionService.class);

            IWorkItem workItemLastRevision = mock(IWorkItem.class);
            when(polarionService.getWorkItem(eq(projectId), eq("EL-615"), isNull())).thenReturn(workItemLastRevision);

            IWorkItem linkedWorkItemLastRevision = mock(IWorkItem.class);
            when(linkedWorkItemLastRevision.getId()).thenReturn("EL-740");
            when(polarionService.getPairedWorkItems(eq(workItemLastRevision), eq(projectId), anyString())).thenReturn(Collections.singletonList(linkedWorkItemLastRevision));

            // let's assume that the same workitem but some specific revision returns another linked item
            IWorkItem workItemSpecificRevision = mock(IWorkItem.class);
            when(polarionService.getWorkItem(eq(projectId), eq("EL-615"), eq("766"))).thenReturn(workItemSpecificRevision); //NOSONAR

            IWorkItem linkedWorkItemSpecificRevision = mock(IWorkItem.class);
            when(linkedWorkItemSpecificRevision.getId()).thenReturn("EL-745");
            when(polarionService.getPairedWorkItems(eq(workItemSpecificRevision), eq(projectId), anyString())).thenReturn(Collections.singletonList(linkedWorkItemSpecificRevision));

            String result = new LinksHandler().appendPairedWorkItemId(unprocessedHtml,
                    projectId, projectId, new DiffContext(WorkItem.of(mock(IWorkItem.class), "wi_outline_number", false, false), WorkItem.of(mock(IWorkItem.class), "wi_outline_number", false, false),
                            "testFieldId", WorkItemsDiffParams.builder().pairedWorkItemsLinkRole("roleId").build(), polarionService));

            assertEquals(expectedHtml, result);
        }
    }

}
