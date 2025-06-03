package ch.sbb.polarion.extension.diff_tool.util;

import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import com.polarion.core.util.logging.Logger;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@UtilityClass
@SuppressWarnings("resource")
public class DiffModelCachedResource {

    private static final Logger logger = Logger.getLogger(DiffModelCachedResource.class);

    private static final int CACHE_TIME_MINUTES = 5;
    private static final int CACHE_SIZE = 100;

    // Store lock objects for synchronization instead of using the strings directly
    private static final Map<String, Object> KEY_LOCKS = new ConcurrentHashMap<>();

    /**
     * Static holder class for lazy initialization of the cache.
     * The class is loaded only when first accessed, and the static initialization
     * is guaranteed to be thread-safe by the JVM.
     */
    private static class CacheHolder {
        static final Cache<String, DiffModel> CACHE = initCache(); // Initialized when the class is loaded

        private static Cache<String, DiffModel> initCache() {
            CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
            cacheManager.init();
            return cacheManager.createCache("diffModelCache",
                    CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, DiffModel.class, ResourcePoolsBuilder.heap(CACHE_SIZE))
                            .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.of(CACHE_TIME_MINUTES, ChronoUnit.MINUTES)))
                            .build());
        }
    }

    /**
     * Loads and caches settings value. Stored value will be removed/updated if the time since it was last accessed > CACHE_TIME_MINUTES
     */
    public static DiffModel get(String projectId, String settingName, String cacheBucketId) {
        // Do not cache value if the bucket isn't used
        if (StringUtils.isEmpty(cacheBucketId)) {
            logger.info("Bucket is not used for getting model. " + Arrays.stream(Thread.currentThread().getStackTrace())
                    .skip(1)
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining(System.lineSeparator())));
            return loadModel(projectId, settingName);
        }

        String key = "%s|%s|%s".formatted(projectId, settingName, cacheBucketId);

        // Cache is lazily initialized when first accessed
        DiffModel cachedModel = CacheHolder.CACHE.get(key);
        if (cachedModel != null) {
            return cachedModel;
        }

        // Get or create a lock object for this key
        Object lockObject = KEY_LOCKS.computeIfAbsent(key, k -> new Object());

        // Synchronize on the lock object, not the string itself
        synchronized (lockObject) {
            try {
                // Double-check if another thread has populated the cache while we were waiting
                cachedModel = CacheHolder.CACHE.get(key);
                if (cachedModel != null) {
                    return cachedModel;
                }

                // If still null, load the model and cache it
                DiffModel model = loadModel(projectId, settingName);
                CacheHolder.CACHE.put(key, model);
                return model;
            } finally {
                KEY_LOCKS.remove(key);
            }
        }
    }

    private static DiffModel loadModel(String projectId, String settingName) {
        DiffSettings diffSettings = (DiffSettings) NamedSettingsRegistry.INSTANCE.getByFeatureName(DiffSettings.FEATURE_NAME);
        return settingName == null ? diffSettings.defaultValues() :
                diffSettings.read(ScopeUtils.getScopeFromProject(projectId), SettingId.fromName(settingName), null);
    }
}
