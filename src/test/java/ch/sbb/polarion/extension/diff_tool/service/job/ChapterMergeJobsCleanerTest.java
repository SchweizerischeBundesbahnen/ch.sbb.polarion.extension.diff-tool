package ch.sbb.polarion.extension.diff_tool.service.job;

import ch.sbb.polarion.extension.diff_tool.properties.DiffToolExtensionConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChapterMergeJobsCleanerTest {

    @AfterEach
    void tearDown() {
        ChapterMergeJobsCleaner.stopCleaningJob();
    }

    @Test
    void testTheCleanerRunsAsOftenAsResultsAreKept() {
        ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);
        // stubbed outside the static mock, which a nested stubbing would leave unfinished
        DiffToolExtensionConfiguration extensionConfiguration = configurationWithResultTimeout(30);

        try (MockedStatic<Executors> executors = mockStatic(Executors.class);
             MockedStatic<DiffToolExtensionConfiguration> configuration = mockStatic(DiffToolExtensionConfiguration.class)) {
            executors.when(Executors::newSingleThreadScheduledExecutor).thenReturn(executorService);
            configuration.when(DiffToolExtensionConfiguration::getInstance).thenReturn(extensionConfiguration);

            ChapterMergeJobsCleaner.startCleaningJob();

            verify(executorService).scheduleWithFixedDelay(any(Runnable.class), eq(30L), eq(30L), eq(TimeUnit.MINUTES));
            assertTrue(ChapterMergeJobsCleaner.isRunning());
        }
    }

    @Test
    void testTheCleanerIsStartedOnlyOnce() {
        ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);
        // stubbed outside the static mock, which a nested stubbing would leave unfinished
        DiffToolExtensionConfiguration extensionConfiguration = configurationWithResultTimeout(30);

        try (MockedStatic<Executors> executors = mockStatic(Executors.class);
             MockedStatic<DiffToolExtensionConfiguration> configuration = mockStatic(DiffToolExtensionConfiguration.class)) {
            executors.when(Executors::newSingleThreadScheduledExecutor).thenReturn(executorService);
            configuration.when(DiffToolExtensionConfiguration::getInstance).thenReturn(extensionConfiguration);

            ChapterMergeJobsCleaner.startCleaningJob();
            ChapterMergeJobsCleaner.startCleaningJob();

            executors.verify(Executors::newSingleThreadScheduledExecutor, times(1));
            verify(executorService, times(1)).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        }
    }

    @Test
    void testAStoppedCleanerLetsGoOfItsThread() {
        ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);
        // stubbed outside the static mock, which a nested stubbing would leave unfinished
        DiffToolExtensionConfiguration extensionConfiguration = configurationWithResultTimeout(30);

        try (MockedStatic<Executors> executors = mockStatic(Executors.class);
             MockedStatic<DiffToolExtensionConfiguration> configuration = mockStatic(DiffToolExtensionConfiguration.class)) {
            executors.when(Executors::newSingleThreadScheduledExecutor).thenReturn(executorService);
            configuration.when(DiffToolExtensionConfiguration::getInstance).thenReturn(extensionConfiguration);

            ChapterMergeJobsCleaner.startCleaningJob();
            ChapterMergeJobsCleaner.stopCleaningJob();

            verify(executorService).shutdown();
            assertFalse(ChapterMergeJobsCleaner.isRunning());
        }
    }

    @Test
    void testStoppingACleanerWhichNeverRanDoesNothing() {
        assertDoesNotThrow(ChapterMergeJobsCleaner::stopCleaningJob);
    }

    private DiffToolExtensionConfiguration configurationWithResultTimeout(int minutes) {
        DiffToolExtensionConfiguration configuration = mock(DiffToolExtensionConfiguration.class);
        when(configuration.getChapterMergeResultTimeout()).thenReturn(minutes);
        return configuration;
    }
}
