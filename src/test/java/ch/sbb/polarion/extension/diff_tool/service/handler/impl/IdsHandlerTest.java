package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentWorkItemsPairDiffParams;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdsHandlerTest {
    private static final String ID = "id";

    private IdsHandler handler;

    @Mock
    private WorkItem workItemA;

    @Mock
    private WorkItem workItemB;

    @BeforeEach
    public void init() {
        handler = new IdsHandler();
    }

    @Test
    void testIgnoreDifferenceByDefault() {
        when(workItemA.getField(ID)).thenReturn(WorkItem.Field.builder().id(ID).html("EL-1").build());
        when(workItemB.getField(ID)).thenReturn(WorkItem.Field.builder().id(ID).html("EL-2").build());

        DiffContext context = new DiffContext(workItemA, workItemB, ID, "", false, mock(PolarionService.class));

        Pair<String, String> preProcessed = handler.preProcess(Pair.of(workItemA.getField(ID).getHtml(), workItemB.getField(ID).getHtml()), context);//NOSONAR
        assertEquals("EL-1", preProcessed.getLeft());
        assertEquals("EL-1", preProcessed.getRight());

        // Test revert modification by post process
        assertEquals("EL-2", handler.postProcess("EL-1", context));
        assertEquals("EL-2", handler.postProcess("EL-2", context));
    }

    @Test
    void testTakeDifferenceIntoAccountWhenRequested() {
        when(workItemA.getField(ID)).thenReturn(WorkItem.Field.builder().id(ID).html("EL-1").build());
        when(workItemB.getField(ID)).thenReturn(WorkItem.Field.builder().id(ID).html("EL-2").build());

        DiffContext context = new DiffContext(workItemA, workItemB, ID, "", true, mock(PolarionService.class));

        Pair<String, String> preProcessed = handler.preProcess(Pair.of(workItemA.getField(ID).getHtml(), workItemB.getField(ID).getHtml()), context);//NOSONAR
        assertEquals("EL-1", preProcessed.getLeft());
        assertEquals("EL-2", preProcessed.getRight());

        // Test post process don't corrupt data
        assertEquals("EL-1", handler.postProcess("EL-1", context));
        assertEquals("EL-2", handler.postProcess("EL-2", context));
    }

    @Test
    void testDifferWhenOnlyLeft() {
        when(workItemA.getField(ID)).thenReturn(WorkItem.Field.builder().id(ID).html("EL-1").build());

        DiffContext context = new DiffContext(workItemA, null, ID, "", false, mock(PolarionService.class));

        Pair<String, String> preProcessed = handler.preProcess(Pair.of(workItemA.getField(ID).getHtml(), ""), context);//NOSONAR
        assertEquals("EL-1", preProcessed.getLeft());
        assertEquals("", preProcessed.getRight());

        // Test post process don't corrupt data
        assertEquals("EL-1", handler.postProcess("EL-1", context));
        assertEquals("", handler.postProcess("", context));
    }

    @Test
    void testDifferWhenOnlyRight() {
        when(workItemB.getField(ID)).thenReturn(WorkItem.Field.builder().id(ID).html("EL-2").build());

        DiffContext context = new DiffContext(null, workItemB, ID, "", false, mock(PolarionService.class));

        Pair<String, String> preProcessed = handler.preProcess(Pair.of("", workItemB.getField(ID).getHtml()), context);//NOSONAR
        assertEquals("", preProcessed.getLeft());
        assertEquals("EL-2", preProcessed.getRight());

        // Test post process don't corrupt data
        assertEquals("", handler.postProcess("", context));
        assertEquals("EL-2", handler.postProcess("EL-2", context));
    }
}
