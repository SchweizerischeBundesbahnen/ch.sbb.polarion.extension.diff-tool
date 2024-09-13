package ch.sbb.polarion.extension.diff_tool.util;

import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
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
import java.util.Optional;

@UtilityClass
@SuppressWarnings("resource")
public class DiffModelCachedResource {

    private static final int CACHE_TIME_MINUTES = 5;
    private static final int CACHE_SIZE = 100;
    private static Cache<String, DiffModel> cache;

    /**
     * Loads and caches settings value. Stored value will be removed/updated if the time since it was last accessed > CACHE_TIME_MINUTES
     */
    public static synchronized DiffModel get(String projectId, String settingName, String cacheBucketId) {
        if (StringUtils.isEmpty(cacheBucketId)) { // do not cache value if the bucket isn't used
            return loadModel(projectId, settingName);
        }
        if (cache == null) {
            cache = initCache();
        }

        String key = "%s|%s|%s".formatted(projectId, settingName, cacheBucketId);
        return Optional.ofNullable(cache.get(key)).orElseGet(() -> {
            DiffModel model = loadModel(projectId, settingName);
            if (model != null) { // just in case, ehcache restricts caching null values
                cache.put(key, model);
            }
            return model;
        });
    }

    private static DiffModel loadModel(String projectId, String settingName) {
        DiffSettings diffSettings = (DiffSettings) NamedSettingsRegistry.INSTANCE.getByFeatureName(DiffSettings.FEATURE_NAME);
        return settingName == null ? diffSettings.defaultValues() :
                diffSettings.read(ScopeUtils.getScopeFromProject(projectId), SettingId.fromName(settingName), null);
    }

    private static Cache<String, DiffModel> initCache() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        cacheManager.init();
        return cacheManager.createCache("diffModelCache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, DiffModel.class, ResourcePoolsBuilder.heap(CACHE_SIZE))
                        .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.of(CACHE_TIME_MINUTES, ChronoUnit.MINUTES)))
                        .build());
    }
}
