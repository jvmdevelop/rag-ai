package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.CacheStats;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ProcessedQuery;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ScoredDocument;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ContextEntry;
import com.jvmd.digitalurpaq_ai_agent.config.ContextStoreConfig;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.storage.FileContextStore;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.storage.FileContextStore.Namespace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Two-tier cache service:
 *   L1: Spring Caffeine (@CacheEvict via Spring Cache)
 *   L2: FileContextStore (file-based, persistent across restarts)
 *
 * Senior rule: use Spring Cache annotations for L1 eviction,
 * delegate L2 persistence to FileContextStore.
 */
@Slf4j
@Service
public class CacheService {

    private final FileContextStore store;
    private final RagMetrics metrics;
    private final Duration searchTtl;
    private final Duration queryTtl;

    public CacheService(FileContextStore store, RagMetrics metrics, ContextStoreConfig config) {
        this.store = store;
        this.metrics = metrics;
        this.searchTtl = Duration.ofMinutes(config.getSearchTtlMinutes());
        this.queryTtl  = Duration.ofMinutes(config.getQueryTtlMinutes());
    }

    public Mono<List<ScoredDocument>> getOrComputeSearch(String query, Mono<List<ScoredDocument>> supplier) {
        String key = "search:" + query.toLowerCase().trim();
        TypeReference<ContextEntry<List<ScoredDocument>>> typeRef = new TypeReference<>() {};

        return store.get(Namespace.SEARCH, key, typeRef)
                .map(cached -> {
                    metrics.recordCacheHit("search");
                    log.debug("[Cache] L2 HIT search: {}", query);
                    return Mono.just(cached);
                })
                .orElseGet(() -> {
                    metrics.recordCacheMiss("search");
                    return supplier.doOnNext(result ->
                            store.put(Namespace.SEARCH, key, result, searchTtl));
                });
    }

    public Mono<ProcessedQuery> getOrComputeQuery(String query, Mono<ProcessedQuery> supplier) {
        String key = "query:" + query.toLowerCase().trim();
        TypeReference<ContextEntry<ProcessedQuery>> typeRef = new TypeReference<>() {};

        return store.get(Namespace.QUERY, key, typeRef)
                .map(cached -> {
                    metrics.recordCacheHit("query");
                    return Mono.just(cached);
                })
                .orElseGet(() -> {
                    metrics.recordCacheMiss("query");
                    return supplier.doOnNext(result ->
                            store.put(Namespace.QUERY, key, result, queryTtl));
                });
    }

    /**
     * Evicts L1 Caffeine cache AND L2 file store.
     * @CacheEvict triggers Spring's Caffeine eviction automatically.
     */
    @CacheEvict(value = "searchCache", allEntries = true)
    public void invalidateSearchCache() {
        store.clearNamespace(Namespace.SEARCH);
        log.info("[Cache] Search cache invalidated (L1+L2)");
    }

    @CacheEvict(value = {"searchCache", "queryCache", "responseCache"}, allEntries = true)
    public void invalidateAll() {
        store.clearAll();
        log.info("[Cache] All caches invalidated (L1+L2)");
    }

    public CacheStats getStats() {
        Map<String, Object> raw = store.getStats();
        return new CacheStats(
                (int) toLong(getOrEmpty(raw, "search"), "total"),
                (int) toLong(getOrEmpty(raw, "query"),  "total"),
                (int) toLong(getOrEmpty(raw, "response"), "total"),
                toLong(getOrEmpty(raw, "search"), "valid"),
                toLong(getOrEmpty(raw, "query"),  "valid"),
                toLong(getOrEmpty(raw, "response"), "valid")
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrEmpty(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private long toLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Number n ? n.longValue() : 0L;
    }
}
