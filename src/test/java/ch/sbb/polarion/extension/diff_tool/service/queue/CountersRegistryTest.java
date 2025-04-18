package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CountersRegistryTest {

    @Test
    void testRegisterExecutionIncreasesCount() {
        CountersRegistry registry = new CountersRegistry();

        Feature feature = Feature.DIFF_HTML;
        assertEquals(0, registry.getExecutingCount(feature));

        registry.registerExecution(feature);
        assertEquals(1, registry.getExecutingCount(feature));

        registry.registerExecution(feature);
        assertEquals(2, registry.getExecutingCount(feature));
    }

    @Test
    void testCompleteExecutionOfNonRegisteredFeature() {
        CountersRegistry registry = new CountersRegistry();

        Feature feature = Feature.DIFF_TEXT;
        assertEquals(0, registry.getExecutingCount(feature));

        registry.completeExecution(feature);
        assertEquals(0, registry.getExecutingCount(feature));
    }

    @Test
    void testCompleteExecutionDecreasesCount() {
        CountersRegistry registry = new CountersRegistry();

        Feature feature = Feature.DIFF_HTML;
        registry.registerExecution(feature);
        registry.registerExecution(feature);
        assertEquals(2, registry.getExecutingCount(feature));

        registry.completeExecution(feature);
        assertEquals(1, registry.getExecutingCount(feature));

        registry.completeExecution(feature);
        assertEquals(0, registry.getExecutingCount(feature));
    }

    @Test
    void testEnqueueIncreasesQueuedCount() {
        CountersRegistry registry = new CountersRegistry();

        Feature feature = Feature.DIFF_HTML;
        assertEquals(0, registry.getQueuedCount(feature));

        registry.enqueue(feature);
        assertEquals(1, registry.getQueuedCount(feature));

        registry.enqueue(feature);
        assertEquals(2, registry.getQueuedCount(feature));
    }

    @Test
    void testDequeueDecreasesQueuedCount() {
        CountersRegistry registry = new CountersRegistry();

        Feature feature = Feature.DIFF_HTML;
        registry.enqueue(feature);
        registry.enqueue(feature);
        assertEquals(2, registry.getQueuedCount(feature));

        registry.dequeue(feature);
        assertEquals(1, registry.getQueuedCount(feature));

        registry.dequeue(feature);
        assertEquals(0, registry.getQueuedCount(feature));
    }

    @Test
    void testDequeueOfNonQueuedFeature() {
        CountersRegistry registry = new CountersRegistry();

        Feature feature = Feature.DIFF_TEXT;
        assertEquals(0, registry.getQueuedCount(feature));

        registry.dequeue(feature);
        assertEquals(0, registry.getQueuedCount(feature));
    }

    @Test
    void testMultipleFeatures() {
        CountersRegistry registry = new CountersRegistry();

        Feature feature1 = Feature.DIFF_HTML;
        Feature feature2 = Feature.DIFF_TEXT;

        // Test execution counts
        registry.registerExecution(feature1);
        registry.registerExecution(feature2);
        registry.registerExecution(feature2);

        assertEquals(1, registry.getExecutingCount(feature1));
        assertEquals(2, registry.getExecutingCount(feature2));

        // Test queued counts
        registry.enqueue(feature1);
        registry.enqueue(feature1);
        registry.enqueue(feature2);

        assertEquals(2, registry.getQueuedCount(feature1));
        assertEquals(1, registry.getQueuedCount(feature2));

        // Verify operations are isolated between features
        registry.completeExecution(feature1);
        registry.dequeue(feature2);

        assertEquals(0, registry.getExecutingCount(feature1));
        assertEquals(2, registry.getExecutingCount(feature2));
        assertEquals(2, registry.getQueuedCount(feature1));
        assertEquals(0, registry.getQueuedCount(feature2));
    }

}
