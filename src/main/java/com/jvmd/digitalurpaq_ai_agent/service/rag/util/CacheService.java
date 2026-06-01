package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import com.jvmd.digitalurpaq_ai_agent.config.properties.RagProperties;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.CacheEntry;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.CacheStats;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ProcessedQuery;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ScoredDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CacheService {

    private final Map<String, CacheEntry<List<ScoredDocument>>> searchCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<ProcessedQuery>> queryCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<String>> responseCache = new ConcurrentHashMap<>();

    private final Duration searchCacheTtl;
    private final Duration queryCacheTtl;
    private final int maxCacheSize;

    public CacheService(RagProperties ragProperties) {
        this.searchCacheTtl = ragProperties.searchCacheTtl();
        this.queryCacheTtl = ragProperties.queryCacheTtl();
        this.maxCacheSize = ragProperties.maxCacheSize();
        log.info("CacheService initialized — searchTTL={}, queryTTL={}, maxSize={}",
                searchCacheTtl, queryCacheTtl, maxCacheSize);
    }

    public Mono<List<ScoredDocument>> getOrComputeSearch(
            String query,
            Mono<List<ScoredDocument>> supplier) {

        String cacheKey = generateSearchKey(query);

        CacheEntry<List<ScoredDocument>> cached = searchCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Search cache HIT for query: {}", query);
            return Mono.just(cached.value());
        }

        log.debug("Search cache MISS for query: {}", query);

        return supplier
                .doOnNext(result -> {
                    cleanupIfNeeded(searchCache, maxCacheSize);
                    searchCache.put(cacheKey, new CacheEntry<>(result, searchCacheTtl));
                });
    }

    public Mono<ProcessedQuery> getOrComputeQuery(
            String query,
            Mono<ProcessedQuery> supplier) {

        String cacheKey = generateQueryKey(query);

        CacheEntry<ProcessedQuery> cached = queryCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Query cache HIT for: {}", query);
            return Mono.just(cached.value());
        }

        log.debug("Query cache MISS for: {}", query);

        return supplier
                .doOnNext(result -> {
                    cleanupIfNeeded(queryCache, maxCacheSize);
                    queryCache.put(cacheKey, new CacheEntry<>(result, queryCacheTtl));
                });
    }

    public void invalidateSearchCache() {
        searchCache.clear();
        log.info("Search cache invalidated");
    }

    public void invalidateAll() {
        searchCache.clear();
        queryCache.clear();
        responseCache.clear();
        log.info("All caches invalidated");
    }

    @Scheduled(fixedRateString = "${app.rag.cache-cleanup-interval:300000}")
    public void scheduledCleanup() {
        int searchRemoved = removeExpired(searchCache);
        int queryRemoved = removeExpired(queryCache);
        int responseRemoved = removeExpired(responseCache);

        if (searchRemoved + queryRemoved + responseRemoved > 0) {
            log.info("Scheduled cache cleanup — removed: search={}, query={}, response={}",
                    searchRemoved, queryRemoved, responseRemoved);
        }
    }

    public CacheStats getStats() {
        return new CacheStats(
                searchCache.size(),
                queryCache.size(),
                responseCache.size(),
                countValid(searchCache),
                countValid(queryCache),
                countValid(responseCache)
        );
    }

    private String generateSearchKey(String query) {
        return "search:" + query.toLowerCase().trim();
    }

    private String generateQueryKey(String query) {
        return "query:" + query.toLowerCase().trim();
    }

    private <T> void cleanupIfNeeded(Map<String, CacheEntry<T>> cache, int maxSize) {
        if (cache.size() >= maxSize) {
            removeExpired(cache);

            if (cache.size() >= maxSize) {
                cache.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue())
                        .limit(maxSize / 4)
                        .map(Map.Entry::getKey)
                        .toList()
                        .forEach(cache::remove);

                log.info("Cache eviction performed, removed LRU entries");
            }
        }
    }

    private <T> int removeExpired(Map<String, CacheEntry<T>> cache) {
        int before = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return before - cache.size();
    }

    private <T> long countValid(Map<String, CacheEntry<T>> cache) {
        return cache.values().stream()
                .filter(entry -> !entry.isExpired())
                .count();
    }
}
