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

    @Test
    public void testFilterQueueWhenDateStringIsEmpty() {
        StatisticsParams params = new StatisticsParams();
        String workerId = "worker1";

        CircularFifoQueue<TimeframeStatisticsEntry> queue = new CircularFifoQueue<>(3);
        TimeframeStatisticsEntry entry1 = new TestTimeframeEntry(OffsetDateTime.parse("2023-01-01T10:00:00Z"));
        TimeframeStatisticsEntry entry2 = new TestTimeframeEntry(OffsetDateTime.parse("2023-01-02T10:00:00Z"));
        TimeframeStatisticsEntry entry3 = new TestTimeframeEntry(OffsetDateTime.parse("2023-01-03T10:00:00Z"));

        queue.add(entry1);
        queue.add(entry2);
        queue.add(entry3);

        // Test case 1: With null date string (from map is empty)
        Map<String, String> emptyMap = new HashMap<>();
        params.setFrom(emptyMap); // This makes dateString null because the map doesn't contain the workerId

        List<TimeframeStatisticsEntry> result1 = params.filterQueue(workerId, queue);
        assertEquals(3, result1.size());
        assertTrue(result1.contains(entry1) && result1.contains(entry2) && result1.contains(entry3));

        // Test case 2: With empty date string
        Map<String, String> mapWithEmptyString = new HashMap<>();
        mapWithEmptyString.put(workerId, "");
        params.setFrom(mapWithEmptyString);

        List<TimeframeStatisticsEntry> result2 = params.filterQueue(workerId, queue);
        assertEquals(3, result2.size());
        assertTrue(result2.contains(entry1) && result2.contains(entry2) && result2.contains(entry3));
    }

}
