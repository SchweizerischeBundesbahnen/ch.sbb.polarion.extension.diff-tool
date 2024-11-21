package ch.sbb.polarion.extension.diff_tool.util;

import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.subterra.base.location.ILocation;
import lombok.Builder;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import javax.security.auth.Subject;
import java.security.Principal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DocumentWorkItemsCache {
    private static final int CACHE_TIME_MINUTES = 10;
    private static final int CACHE_SIZE = 100;
    private static DocumentWorkItemsCache instance;
    private final Cache<CacheKey, Map<String, IWorkItem>> cache;

    public static synchronized DocumentWorkItemsCache getInstance() {
        if (instance == null) {
            instance = new DocumentWorkItemsCache();
        }
        return instance;
    }

    private DocumentWorkItemsCache() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        cacheManager.init();
        Class<Map<String, IWorkItem>> mapClass = cast(Map.class);
        cache = cacheManager.createCache("documentWorkItemsCache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(CacheKey.class, mapClass, ResourcePoolsBuilder.heap(CACHE_SIZE))
                        .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.of(CACHE_TIME_MINUTES, ChronoUnit.MINUTES)))
                        .build());
    }

    public void cacheWorkItemsFromDocument(IModule document, Subject userSubject) {
        CacheKey key = getCacheKey(document, userSubject);
        cache.put(key, getWorkItemsFromDocument(document));
    }

    public IWorkItem getWorkItem(IModule document, Subject userSubject, String id) {
        CacheKey key = getCacheKey(document, userSubject);
        synchronized (cache) {
            if (!cache.containsKey(key) || (cache.get(key) == null)) {
                cache.put(key, getWorkItemsFromDocument(document));
            }
            return cache.get(key).get(id);
        }
    }

    public void evictDocumentFromCache(IModule document, Subject userSubject) {
        CacheKey key = getCacheKey(document, userSubject);
        synchronized (cache) {
            if (cache.containsKey(key)) {
                cache.remove(key);
            }
        }
    }

    public void evictDocumentFromCache(String projectId, ILocation location, String revision, Subject userSubject) {
        CacheKey key = CacheKey.from(projectId, location, revision, userSubject);
        synchronized (cache) {
            if (cache.containsKey(key)) {
                cache.remove(key);
            }
        }
    }

    public void clearCache() {
        cache.clear();
    }

    private Map<String, IWorkItem> getWorkItemsFromDocument(IModule document) {
        return document.getAllWorkItems().stream()
                .collect(Collectors.toMap(IWorkItem::getId, Function.identity()));
    }

    @SuppressWarnings("unchecked")
    private <T, V> Class<V> cast(Class<T> t) {
        return (Class<V>) t;
    }

    private CacheKey getCacheKey(IModule document, Subject userSubject) {
        return CacheKey.from(
                document.getProjectId(),
                document.getModuleLocation(),
                document.getRevision(),
                userSubject);
    }

    @Builder
    private record CacheKey(String projectId, ILocation location, String revision, String userSubject) {
        public static CacheKey from(String projectId, ILocation location, String revision, Subject userSubject) {
            return new CacheKey(projectId,
                    location,
                    revision,
                    (userSubject != null) ? getSubjectKey(userSubject) : null);
        }

        private static String getSubjectKey(Subject userSubject) {
            return userSubject.getPrincipals().stream()
                    .map(Principal::toString)
                    .collect(Collectors.joining(",", "[", "]"));
        }
    }
}
