package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.EndpointCallEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry.MAX_HISTORY_ENTRIES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsHistoryTest {

    @Test
    void testKeepsNewestEntries() {
        StatisticsHistory<EndpointCallEntry> history = new StatisticsHistory<>();
        EndpointCallEntry first = new EndpointCallEntry(0, 0);
        history.add(first);
        for (int i = 0; i < MAX_HISTORY_ENTRIES; i++) {
            history.add(new EndpointCallEntry(1, 1));
        }

        List<EndpointCallEntry> snapshot = history.snapshot();
        assertEquals(MAX_HISTORY_ENTRIES, snapshot.size());
        assertTrue(snapshot.stream().noneMatch(entry -> entry == first));
    }

    @Test
    void testSnapshotIsIndependentCopy() {
        StatisticsHistory<EndpointCallEntry> history = new StatisticsHistory<>();
        EndpointCallEntry entry = new EndpointCallEntry(1, 1);
        history.add(entry);

        List<EndpointCallEntry> snapshot = history.snapshot();
        history.add(new EndpointCallEntry(2, 2));
        history.clear();

        assertEquals(1, snapshot.size());
        assertSame(entry, snapshot.getFirst());
        assertTrue(history.snapshot().isEmpty());
    }

    @Test
    void testSnapshotWhileAdding() throws InterruptedException {
        StatisticsHistory<EndpointCallEntry> history = new StatisticsHistory<>();
        for (int i = 0; i < MAX_HISTORY_ENTRIES; i++) {
            history.add(new EndpointCallEntry(1, 1));
        }

        // A full queue drops its oldest entry on every add, which a reader without the lock can see as a null element
        Throwable failure = ConcurrentAccess.run(
                () -> history.add(new EndpointCallEntry(1, 1)),
                () -> {
                    List<EndpointCallEntry> snapshot = history.snapshot();
                    if (snapshot.size() != MAX_HISTORY_ENTRIES || snapshot.stream().anyMatch(Objects::isNull)) {
                        throw new IllegalStateException("Inconsistent snapshot of " + snapshot.size() + " entries");
                    }
                });

        assertNull(failure);
    }
}
