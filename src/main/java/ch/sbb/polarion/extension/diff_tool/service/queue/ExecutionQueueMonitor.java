package ch.sbb.polarion.extension.diff_tool.service.queue;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.CpuLoadEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.StatisticsParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.TimeframeStatisticsEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.ExecutionQueueModel;
import ch.sbb.polarion.extension.diff_tool.settings.ExecutionQueueSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.guice.internal.GuicePlatform;
import com.polarion.platform.security.ISecurityService;
import lombok.SneakyThrows;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ExecutionQueueMonitor {
    private static final Logger logger = Logger.getLogger(ExecutionQueueMonitor.class);
    private static final int COLLECTING_INTERVAL_MS = 1000; // 1 second
    public static final int MAX_ENTRIES = 30 * 60_000 / COLLECTING_INTERVAL_MS; // amount of entries to keep in the history for the last 30 minutes
    private final ScheduledExecutorService scheduler;
    private final ExecutionQueueService executionService;
    private final CircularFifoQueue<CpuLoadEntry> cpuHistory = new CircularFifoQueue<>(MAX_ENTRIES);
    private volatile boolean isRunning = false;

    public ExecutionQueueMonitor(ExecutionQueueService executionService) {
        this.executionService = executionService;
        GuicePlatform.tryInjectMembers(this);
        clearHistory();

        ThreadFactory threadFactory = r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setDaemon(true);
            thread.setName("ExecutionQueueMonitor-Scheduler");
            return thread;
        };

        scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
        start();
    }

    public void start() {
        if (isRunning) {
            return;
        }

        scheduler.scheduleAtFixedRate(this::collectStatistics, 0, 1, TimeUnit.SECONDS);
        isRunning = true;
    }

    private void collectStatistics() {
        try {
            cpuHistory.add(new CpuLoadEntry());
            executionService.snapshotHistory();
        } catch (Exception e) {
            logger.error("Error collecting statistics", e);
        }
    }

    public Map<String, Map<Feature, List<TimeframeStatisticsEntry>>> getHistory(StatisticsParams statisticsParams) {
        Map<String, Map<Feature, List<TimeframeStatisticsEntry>>> result = executionService.getHistory(statisticsParams);
        result.put("COMMON", Map.of(Feature.CPU_LOAD, statisticsParams.filterQueue(Feature.CPU_LOAD.name(), cpuHistory)));
        return result;
    }

    @SneakyThrows
    public void shutdown() {
        isRunning = false;
        scheduler.shutdown();
        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
            scheduler.shutdownNow();
        }
        logger.info("Statistics monitoring stopped");
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void clearHistory() {
        cpuHistory.clear();
        executionService.clearHistory();
    }

    public void refreshConfiguration() {
        executionService.refreshConfiguration();
    }

    public static ExecutionQueueModel getSettings() {
        return PlatformContext.getPlatform().lookupService(ISecurityService.class).doAsSystemUser(
                (PrivilegedAction<ExecutionQueueModel>) () -> (ExecutionQueueModel) NamedSettingsRegistry.INSTANCE.getByFeatureName(ExecutionQueueSettings.FEATURE_NAME).read(
                        "", SettingId.fromName(NamedSettings.DEFAULT_NAME), null));
    }
}
