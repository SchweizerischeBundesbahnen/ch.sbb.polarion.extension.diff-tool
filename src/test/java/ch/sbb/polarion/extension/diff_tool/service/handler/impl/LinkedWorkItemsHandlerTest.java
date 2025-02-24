package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;

import com.polarion.alm.tracker.model.IWorkItem;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkedWorkItemsHandlerTest {

    private LinkedWorkItemsHandler handler;

    @Mock
    private DiffContext context;

    @Mock
    private IWorkItem workItemA;

    @Mock
    private IWorkItem workItemB;

    @BeforeEach
    void setUp() {
        handler = new LinkedWorkItemsHandler();
    }

    @Test
    void testPreProcessInappropriateCase() {
        when(context.pairedWorkItemsDiffer).thenReturn(true);
        Pair<String, String> initialPair = Pair.of("originalA", "originalB");
        Pair<String, String> result = handler.preProcess(initialPair, context);
        assertEquals(initialPair, result, "PreProcess should return the initial pair if inappropriate case");
    }

    @Test
    void testPreProcessAppropriateCase() {
        when(context.pairedWorkItemsDiffer).thenReturn(false);
        Pair<String, String> initialPair = Pair.of("originalA", "originalB");
        Pair<String, String> result = handler.preProcess(initialPair, context);
        assertEquals(Pair.of("", ""), result, "PreProcess should return empty pair if appropriate case");
    }

    @Test
    void testPostProcessInappropriateCase() {
        when(context.pairedWorkItemsDiffer).thenReturn(true);
        String diff = "originalDiff";
        String result = handler.postProcess(diff, context);
        assertEquals(diff, result, "PostProcess should return original diff if inappropriate case");
    }

//    @Test
//    void testPostProcessAppropriateCase() {
//        when(context.isPairedWorkItemsDiffer()).thenReturn(false);
//        when(context.getWorkItemA()).thenReturn(mock(WorkItem.class));
//        when(context.getWorkItemB()).thenReturn(mock(WorkItem.class));
//        when(context.getFieldA())
//
//        String result = handler.postProcess("originalDiff", context);
//        assertNotNull(result, "PostProcess should return a processed diff");
//    }
}
