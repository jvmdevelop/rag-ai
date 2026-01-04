package com.jvmd.digitalurpaq_ai_agent.service.rag.model;

public record CacheStats(
        int searchCacheSize,
        int queryCacheSize,
        int responseCacheSize,
        long searchCacheValid,
        long queryCacheValid,
        long responseCacheValid
) {
    @Override
    public String toString() {
        return String.format(
                "Cache Stats: Search[%d/%d] Query[%d/%d] Response[%d/%d]",
                searchCacheValid, searchCacheSize,
                queryCacheValid, queryCacheSize,
                responseCacheValid, responseCacheSize
        );
    }
}