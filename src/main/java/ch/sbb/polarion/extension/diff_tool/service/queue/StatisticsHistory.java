package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry.MAX_HISTORY_ENTRIES;

/**
 * Recent statistics entries, keeping the newest {@link TimeframeStatisticsEntry#MAX_HISTORY_ENTRIES}.
 * The monitor thread adds entries while REST threads read and clear them, and {@link CircularFifoQueue} is not thread-safe.
 */
final class StatisticsHistory<T extends TimeframeStatisticsEntry> {

    private final CircularFifoQueue<T> entries = new CircularFifoQueue<>(MAX_HISTORY_ENTRIES);

    synchronized void add(@NotNull T entry) {
        entries.add(entry);
    }

    synchronized void clear() {
        entries.clear();
    }

    /**
     * Copies the entries, so the caller can iterate them while new entries are added.
     */
    synchronized @NotNull List<T> snapshot() {
        return new ArrayList<>(entries);
    }
}
