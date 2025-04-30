package ch.sbb.polarion.extension.diff_tool.settings;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.ExecutionQueueModel;
import ch.sbb.polarion.extension.diff_tool.util.OSUtils;
import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
@CurrentContextConfig("diff-tool")
class ExecutionQueueSettingsTest {

    @Test
    void testValidModelWithValidThreadCounts() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            ExecutionQueueSettings settings = new ExecutionQueueSettings(mockedSettingsService);
            ExecutionQueueModel model = new ExecutionQueueModel();

            int maxThreads = OSUtils.getMaxRecommendedParallelThreads();
            Map<Integer, Integer> validThreads = new HashMap<>();
            validThreads.put(1, 1);
            validThreads.put(2, maxThreads);
            validThreads.put(3, maxThreads / 2);
            model.setThreads(validThreads);

            // Act & Assert - no exception should be thrown
            assertDoesNotThrow(() -> settings.validateThreadsCount(model));
        }
    }

    @Test
    void testInvalidThreadCountThrowsException() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            ExecutionQueueSettings settings = new ExecutionQueueSettings(mockedSettingsService);
            ExecutionQueueModel model = new ExecutionQueueModel();

            Map<Integer, Integer> invalidThreads = new HashMap<>();
            invalidThreads.put(1, 0); // Invalid thread count
            model.setThreads(invalidThreads);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> settings.validateThreadsCount(model)
            );

            assertEquals("Threads count must be between 1 and " + OSUtils.getMaxRecommendedParallelThreads(),
                    exception.getMessage());
        }
    }

    @Test
    void testDefaultValues() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            ExecutionQueueSettings settings = new ExecutionQueueSettings(mockedSettingsService);

            ExecutionQueueModel model = settings.defaultValues();

            assertNotNull(model);
            Map<Feature, Integer> workers = model.getWorkers();
            assertNotNull(workers);
            assertEquals(Feature.workerFeatures().size(), workers.size());

            for (Feature feature : Feature.workerFeatures()) {
                assertTrue(workers.containsKey(feature));
                assertEquals(0, workers.get(feature));
            }

            Map<Integer, Integer> threads = model.getThreads();
            assertNotNull(threads);
            assertEquals(Feature.workerFeatures().size(), threads.size());

            for (int i = 1; i <= Feature.workerFeatures().size(); i++) {
                assertTrue(threads.containsKey(i));
                assertEquals(1, threads.get(i));
            }
        }
    }

}
