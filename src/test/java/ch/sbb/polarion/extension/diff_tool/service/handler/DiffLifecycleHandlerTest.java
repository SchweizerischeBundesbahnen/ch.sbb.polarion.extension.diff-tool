package ch.sbb.polarion.extension.diff_tool.service.handler;

import ch.sbb.polarion.extension.diff_tool.service.handler.impl.EnumReplaceHandler;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.HyperlinksHandler;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.IdsHandler;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.ImageHandler;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.LinkedWorkItemsHandler;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.LinksHandler;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.OuterWrapperHandler;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.ReverseStylesHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MockitoExtension.class)
class DiffLifecycleHandlerTest {

    @Mock
    private DiffContext diffContext;

    @Test
    void testGetHandlersReturnsExpectedHandlers() {
        List<DiffLifecycleHandler> handlers = DiffLifecycleHandler.getHandlers();

        assertNotNull(handlers);
        assertEquals(8, handlers.size());
    }

    @Test
    void testGetHandlersReturnsHandlersInCorrectOrder() {
        List<DiffLifecycleHandler> handlers = DiffLifecycleHandler.getHandlers();

        assertInstanceOf(IdsHandler.class, handlers.get(0));
        assertInstanceOf(LinkedWorkItemsHandler.class, handlers.get(1));
        assertInstanceOf(LinksHandler.class, handlers.get(2));
        assertInstanceOf(HyperlinksHandler.class, handlers.get(3));
        assertInstanceOf(ImageHandler.class, handlers.get(4));
        assertInstanceOf(OuterWrapperHandler.class, handlers.get(5));
        assertInstanceOf(EnumReplaceHandler.class, handlers.get(6));
        assertInstanceOf(ReverseStylesHandler.class, handlers.get(7));
    }

    @Test
    void testDefaultPreProcessReturnsInputUnchanged() {
        DiffLifecycleHandler handler = new DiffLifecycleHandler() {};
        Pair<String, String> input = Pair.of("left", "right");

        Pair<String, String> result = handler.preProcess(input, diffContext);

        assertSame(input, result);
        assertEquals("left", result.getLeft());
        assertEquals("right", result.getRight());
    }

    @Test
    void testDefaultPostProcessReturnsInputUnchanged() {
        DiffLifecycleHandler handler = new DiffLifecycleHandler() {};
        String input = "diff content";

        String result = handler.postProcess(input, diffContext);

        assertSame(input, result);
        assertEquals("diff content", result);
    }

    @Test
    void testGetHandlersReturnsNewListOnEachCall() {
        List<DiffLifecycleHandler> handlers1 = DiffLifecycleHandler.getHandlers();
        List<DiffLifecycleHandler> handlers2 = DiffLifecycleHandler.getHandlers();

        assertEquals(handlers1.size(), handlers2.size());
        for (int i = 0; i < handlers1.size(); i++) {
            assertEquals(handlers1.get(i).getClass(), handlers2.get(i).getClass());
        }
    }
}
