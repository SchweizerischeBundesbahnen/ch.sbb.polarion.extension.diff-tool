package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.CpuLoadEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionQueueMonitorTest {

//    @Test
//    void testCpuLoad() {
//        ExecutionQueueMonitor monitor = new ExecutionQueueMonitor(new ExecutionQueueService());
//        await().pollDelay(2000, TimeUnit.MILLISECONDS).until(() -> true);
//        monitor.shutdown();
//        List<TimeframeStatisticsEntry> cpuLoadHistory = monitor.getHistory().get(Feature.CPU_LOAD);
//        assertFalse(cpuLoadHistory.isEmpty());
//        assertTrue(cpuLoadHistory.stream().allMatch(e -> ((CpuLoadEntry) e).getValue() > 0));
//    }

}
