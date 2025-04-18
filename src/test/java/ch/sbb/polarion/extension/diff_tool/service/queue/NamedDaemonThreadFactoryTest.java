package ch.sbb.polarion.extension.diff_tool.service.queue;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NamedDaemonThreadFactoryTest {

    @Test
    void testThreadNamingPattern() {
        NamedDaemonThreadFactory factory = new NamedDaemonThreadFactory("test");

        Thread thread1 = factory.newThread(() -> {
        });
        Thread thread2 = factory.newThread(() -> {
        });
        Thread thread3 = factory.newThread(() -> {
        });

        assertEquals("test-1", thread1.getName());
        assertEquals("test-2", thread2.getName());
        assertEquals("test-3", thread3.getName());

        assertTrue(thread1.isDaemon());
        assertTrue(thread2.isDaemon());
        assertTrue(thread3.isDaemon());
    }

    @Test
    void testCounterWraparound() {
        NamedDaemonThreadFactory factory = new NamedDaemonThreadFactory("counter-test");

        // Use reflection to set the counter to MAX_VALUE
        try {
            Field counterField = NamedDaemonThreadFactory.class.getDeclaredField("counter");
            counterField.setAccessible(true);
            AtomicInteger counter = (AtomicInteger) counterField.get(factory);
            counter.set(Integer.MAX_VALUE);

            // Create two threads to test wraparound
            Thread threadMax = factory.newThread(() -> {
            });
            Thread threadWrapped = factory.newThread(() -> {
            });

            assertEquals("counter-test-" + Integer.MAX_VALUE, threadMax.getName());
            assertEquals("counter-test-" + Integer.MIN_VALUE, threadWrapped.getName());

            assertTrue(threadMax.isDaemon());
            assertTrue(threadWrapped.isDaemon());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to access counter field: " + e.getMessage());
        }
    }

}
