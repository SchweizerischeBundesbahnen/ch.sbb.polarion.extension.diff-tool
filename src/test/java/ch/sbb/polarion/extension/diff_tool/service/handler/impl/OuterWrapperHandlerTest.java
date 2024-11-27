package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPairDiffParams;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OuterWrapperHandlerTest {
    private static final String STATUS = "status";

    private OuterWrapperHandler handler;

    @Mock
    private WorkItem workItemA;

    @Mock
    private WorkItem workItemB;

    @BeforeEach
    public void init() {
        handler = new OuterWrapperHandler();
    }

    @Test
    void testHandler() {
        when(workItemA.getField(STATUS)).thenReturn(WorkItem.Field.builder().id(STATUS).html("val1").build());
        when(workItemB.getField(STATUS)).thenReturn(WorkItem.Field.builder().id(STATUS).html("val2").build());

        DiffContext context = new DiffContext(workItemA, workItemB, STATUS, WorkItemsPairDiffParams.builder().build(), mock(PolarionService.class));

        Pair<String, String> preProcessed = handler.preProcess(Pair.of(workItemA.getField(STATUS).getHtml(), workItemB.getField(STATUS).getHtml()), context);//NOSONAR
        assertEquals("<outer_surrogate_tag>val1</outer_surrogate_tag>", preProcessed.getLeft());
        assertEquals("<outer_surrogate_tag>val2</outer_surrogate_tag>", preProcessed.getRight());

        assertEquals("val1", handler.postProcess("<outer_surrogate_tag>val1</outer_surrogate_tag>", context));
        assertEquals("val2", handler.postProcess("<outer_surrogate_tag>val2</outer_surrogate_tag>", context));
    }

}
