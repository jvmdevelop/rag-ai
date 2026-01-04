package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import com.jvmd.digitalurpaq_ai_agent.service.rag.model.CacheEntry;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.CacheStats;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ProcessedQuery;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ScoredDocument;
import lombok.extern.slf4j.Slf4j;
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

    private static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration QUERY_CACHE_TTL = Duration.ofMinutes(60);
    private static final int MAX_CACHE_SIZE = 1000;

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
                    cleanupIfNeeded(searchCache, MAX_CACHE_SIZE);
                    searchCache.put(cacheKey, new CacheEntry<>(result, SEARCH_CACHE_TTL));
                    log.debug("Cached search results for query: {}", query);
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
                    cleanupIfNeeded(queryCache, MAX_CACHE_SIZE);
                    queryCache.put(cacheKey, new CacheEntry<>(result, QUERY_CACHE_TTL));
                    log.debug("Cached processed query: {}", query);
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
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired());

            if (cache.size() >= maxSize) {
                cache.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue())
                        .limit(maxSize / 4)
                        .map(Map.Entry::getKey)
                        .toList()
                        .forEach(cache::remove);

                log.info("Cache cleanup performed, removed old entries");
            }
        }
    }

    private <T> long countValid(Map<String, CacheEntry<T>> cache) {
        return cache.values().stream()
                .filter(entry -> !entry.isExpired())
                .count();
    }


}
