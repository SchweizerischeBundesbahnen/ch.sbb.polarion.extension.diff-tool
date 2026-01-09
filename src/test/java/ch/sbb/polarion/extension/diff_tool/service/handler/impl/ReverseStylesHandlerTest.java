package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPairDiffParams;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReverseStylesHandlerTest {
    private static final String STATUS = "status";

    private ReverseStylesHandler handler;

    @Mock
    private WorkItem workItemA;

    @Mock
    private WorkItem workItemB;

    @BeforeEach
    void init() {
        TestUtils.mockDiffSettings();
        handler = new ReverseStylesHandler();
    }

    @AfterEach
    void teardown() {
        TestUtils.clearSettings();
    }

    @Test
    void testDirectStyles() {
        when(workItemA.getField(STATUS)).thenReturn(WorkItem.Field.builder().id(STATUS).build());
        when(workItemB.getField(STATUS)).thenReturn(WorkItem.Field.builder().id(STATUS).build());

        DiffContext context = new DiffContext(workItemA, workItemB, STATUS, WorkItemsPairDiffParams.builder().build(), mock(PolarionService.class));

        assertEquals("<span class=\"diff-html-removed\">val1</span><span class=\"diff-html-added\">val2</span>",
                handler.postProcess("<span class=\"diff-html-removed\">val1</span><span class=\"diff-html-added\">val2</span>", context));
    }

    @Test
    void testReversedStyles() {
        when(workItemA.getField(STATUS)).thenReturn(WorkItem.Field.builder().id(STATUS).build());
        when(workItemB.getField(STATUS)).thenReturn(WorkItem.Field.builder().id(STATUS).build());

        DiffContext context = new DiffContext(workItemA, workItemB, STATUS, WorkItemsPairDiffParams.builder().build(), mock(PolarionService.class)).reverseStyles(true);

        assertEquals("<span class=\"diff-html-added\">val1</span><span class=\"diff-html-removed\">val2</span>",
                handler.postProcess("<span class=\"diff-html-removed\">val1</span><span class=\"diff-html-added\">val2</span>", context));
    }
}
