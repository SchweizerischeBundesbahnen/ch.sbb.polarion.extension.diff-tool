package ch.sbb.polarion.extension.diff_tool.rest.model.queue;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsParamsTest {

    @Test
    public void testFilterQueueWithValidFromDate() {
        StatisticsParams statisticsParams = new StatisticsParams();
        String workerId = "worker1";

        // Create a timestamp in the past
        OffsetDateTime pastDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2);
        String pastDateString = pastDate.format(TimeframeStatisticsEntry.DATE_FORMATTER);

        // Set the 'from' date for the worker
        Map<String, String> fromMap = new HashMap<>();
        fromMap.put(workerId, pastDateString);
        statisticsParams.setFrom(fromMap);

        // Create test entries - one before the 'from' date, one after
        TestTimeframeEntry oldEntry = new TestTimeframeEntry(pastDate.minusDays(1));
        TestTimeframeEntry newEntry = new TestTimeframeEntry(pastDate.plusDays(1));

        // Create and populate the queue
        CircularFifoQueue<TestTimeframeEntry> queue = new CircularFifoQueue<>(10);
        queue.add(oldEntry);
        queue.add(newEntry);

        List<TimeframeStatisticsEntry> result = statisticsParams.filterQueue(workerId, queue);
        assertEquals(1, result.size());
        assertEquals(newEntry, result.get(0));
    }

    @Test
    public void testFilterQueueWithEmptyQueue() {
        StatisticsParams statisticsParams = new StatisticsParams();
        String workerId = "worker1";

        // Create a timestamp and set it as the 'from' date
        OffsetDateTime fromDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        String fromDateString = fromDate.format(TimeframeStatisticsEntry.DATE_FORMATTER);

        Map<String, String> fromMap = new HashMap<>();
        fromMap.put(workerId, fromDateString);
        statisticsParams.setFrom(fromMap);

        // Create an empty queue
        CircularFifoQueue<TestTimeframeEntry> emptyQueue = new CircularFifoQueue<>(10);

        List<TimeframeStatisticsEntry> result = statisticsParams.filterQueue(workerId, emptyQueue);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

}
