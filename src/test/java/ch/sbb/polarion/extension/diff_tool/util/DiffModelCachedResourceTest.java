package ch.sbb.polarion.extension.diff_tool.util;

import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiffModelCachedResourceTest {

    private static final String PROJECT_ID = "TEST_PROJECT";
    private static final String SETTING_NAME = "diff";
    private static final String CACHE_BUCKET_ID = "TEST_BUCKET";
    private static final String SCOPE = "TEST_SCOPE";

    @Mock
    private DiffSettings mockDiffSettings;
    @Mock
    private DiffModel mockDiffModel;
    private MockedStatic<ScopeUtils> mockedScopeUtils;

    @BeforeEach
    void setUp() {
        // Setup ScopeUtils mock
        mockedScopeUtils = mockStatic(ScopeUtils.class);
        mockedScopeUtils.when(() -> ScopeUtils.getScopeFromProject(PROJECT_ID)).thenReturn(SCOPE);
        ILocation mockLocation = mock(ILocation.class);
        mockedScopeUtils.when(() -> ScopeUtils.getContextLocation(SCOPE)).thenReturn(mockLocation);

        // Setup mock behavior
        lenient().when(mockDiffSettings.getFeatureName()).thenReturn(SETTING_NAME);
        lenient().when(mockDiffSettings.read(eq(SCOPE), any(SettingId.class), isNull())).thenReturn(mockDiffModel);
        lenient().when(mockDiffSettings.defaultValues()).thenReturn(mockDiffModel);

        // Register our mock settings with the actual registry
        NamedSettingsRegistry.INSTANCE.register(List.of(mockDiffSettings));

        // Clear the cache before each test
        clearCache();
    }

    @AfterEach
    void tearDown() {
        // Close static mocks
        if (mockedScopeUtils != null) {
            mockedScopeUtils.close();
        }

        // Unregister our mock settings
        NamedSettingsRegistry.INSTANCE.getAll().clear();

        // Clear the cache between tests
        clearCache();
    }

    private void clearCache() {
        try {
            // Access the inner CacheHolder class using reflection
            Class<?> cacheHolderClass = Class.forName("ch.sbb.polarion.extension.diff_tool.util.DiffModelCachedResource$CacheHolder");
            Field cacheField = cacheHolderClass.getDeclaredField("CACHE");
            cacheField.setAccessible(true);
            Object cache = cacheField.get(null);
            if (cache != null) {
                cache.getClass().getMethod("clear").invoke(cache);
            }

            // Clear the locks map
            Field locksField = DiffModelCachedResource.class.getDeclaredField("KEY_LOCKS");
            locksField.setAccessible(true);
            ((Map<?, ?>) locksField.get(null)).clear();
        } catch (Exception e) {
            // Log or handle the exception
            System.err.println("Error clearing cache: " + e.getMessage());
        }
    }

    @Test
    void testGetWithNullCacheBucket() {
        // When cache bucket is null, should load directly without caching
        DiffModel result = DiffModelCachedResource.get(PROJECT_ID, SETTING_NAME, null);

        // Verify
        assertNotNull(result);
        assertEquals(mockDiffModel, result);

        // Verify called once
        verify(mockDiffSettings).read(eq(SCOPE), any(SettingId.class), isNull());
    }

    @Test
    void testGetWithValidParameters() {
        // First call should load from settings
        DiffModel result1 = DiffModelCachedResource.get(PROJECT_ID, SETTING_NAME, CACHE_BUCKET_ID);

        // Second call should return cached value
        DiffModel result2 = DiffModelCachedResource.get(PROJECT_ID, SETTING_NAME, CACHE_BUCKET_ID);

        // Verify
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(mockDiffModel, result1);
        assertEquals(mockDiffModel, result2);

        // Verify loaded only once
        verify(mockDiffSettings, times(1)).read(eq(SCOPE), any(SettingId.class), isNull());
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        // Clear the cache to ensure a clean state
        clearCache();

        // Setup
        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger loadCounter = new AtomicInteger(0);

        // Create a special mock for this test
        DiffSettings testMock = mock(DiffSettings.class, withSettings());
        when(testMock.getFeatureName()).thenReturn(DiffSettings.FEATURE_NAME);

        // Set the counter behavior with any argument matchers
        doAnswer(invocation -> {
            // Simulate some loading delay
            await().pollDelay(50, TimeUnit.MILLISECONDS).until(() -> true);
            loadCounter.incrementAndGet();
            return mockDiffModel;
        }).when(testMock).read(anyString(), any(SettingId.class), any());

        lenient().when(testMock.defaultValues()).thenReturn(mockDiffModel);

        try {
            // Replace the mock in the registry
            NamedSettingsRegistry.INSTANCE.getAll().clear();
            NamedSettingsRegistry.INSTANCE.register(List.of(testMock));

            // Execute concurrent get requests
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await(); // Wait for signal to start
                        DiffModelCachedResource.get(PROJECT_ID, SETTING_NAME, CACHE_BUCKET_ID);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            // Start all threads at once
            startLatch.countDown();

            // Wait for all threads to complete
            boolean completed = finishLatch.await(5, TimeUnit.SECONDS);
            assertTrue(completed, "Not all threads completed in time");

            executorService.shutdown();
            boolean terminated = executorService.awaitTermination(1, TimeUnit.SECONDS);
            assertTrue(terminated, "Executor service did not terminate in time");

            // Verify model was loaded only once despite concurrent access
            assertEquals(1, loadCounter.get(), "Model should be loaded only once");
        } finally {
            executorService.shutdownNow(); // Ensure executor is shut down
        }
    }

    @Test
    void testDifferentKeysLoadDifferentModels() {
        // Setup different models for different settings
        DiffModel model1 = mock(DiffModel.class);
        DiffModel model2 = mock(DiffModel.class);

        when(mockDiffSettings.read(eq(SCOPE), eq(SettingId.fromName(SETTING_NAME + "1")), isNull())).thenReturn(model1);
        when(mockDiffSettings.read(eq(SCOPE), eq(SettingId.fromName(SETTING_NAME + "2")), isNull())).thenReturn(model2);

        // Get models with different keys
        DiffModel result1 = DiffModelCachedResource.get(PROJECT_ID, SETTING_NAME + "1", CACHE_BUCKET_ID);
        DiffModel result2 = DiffModelCachedResource.get(PROJECT_ID, SETTING_NAME + "2", CACHE_BUCKET_ID);

        // Verify different models are returned
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(model1, result1);
        assertEquals(model2, result2);
        assertNotSame(result1, result2);
    }

    @Test
    void testDifferentBucketsLoadDifferentCache() {
        // Get model with one bucket
        DiffModel result1 = DiffModelCachedResource.get(PROJECT_ID, SETTING_NAME, "BUCKET1");

        // Get model with another bucket - should load again
        DiffModel result2 = DiffModelCachedResource.get(PROJECT_ID, SETTING_NAME, "BUCKET2");

        // Verify loaded twice for different buckets
        verify(mockDiffSettings, times(2)).read(eq(SCOPE), any(SettingId.class), isNull());
        assertEquals(mockDiffModel, result1);
        assertEquals(mockDiffModel, result2);
    }
}
