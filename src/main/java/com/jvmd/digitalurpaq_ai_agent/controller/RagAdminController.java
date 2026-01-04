package com.jvmd.digitalurpaq_ai_agent.controller;

import com.jvmd.digitalurpaq_ai_agent.service.rag.model.MetricsSnapshot;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.CacheService;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.RagMetrics;
import com.jvmd.digitalurpaq_ai_agent.service.RetrievalService;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.CacheStats;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/rag")
@AllArgsConstructor
public class RagAdminController {

    private final RagMetrics ragMetrics;
    private final CacheService cacheService;
    private final RetrievalService retrievalService;

    @GetMapping(value = "/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getMetrics() {
        log.info("Fetching RAG metrics");
        
        return Mono.fromCallable(() -> {
            MetricsSnapshot snapshot = ragMetrics.getSnapshot();
            CacheStats cacheStats = cacheService.getStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("metrics", Map.of(
                    "totalRequests", snapshot.totalRequests(),
                    "successfulRequests", snapshot.successfulRequests(),
                    "failedRequests", snapshot.failedRequests(),
                    "successRate", snapshot.successRate(),
                    "avgResponseTimeMs", snapshot.avgResponseTimeMs(),
                    "totalRetries", snapshot.totalRetries()
            ));
            
            response.put("cache", Map.of(
                    "searchCacheSize", cacheStats.searchCacheSize(),
                    "queryCacheSize", cacheStats.queryCacheSize(),
                    "responseCacheSize", cacheStats.responseCacheSize(),
                    "searchCacheValid", cacheStats.searchCacheValid(),
                    "queryCacheValid", cacheStats.queryCacheValid(),
                    "responseCacheValid", cacheStats.responseCacheValid()
            ));
            
            return response;
        });
    }

    @GetMapping(value = "/documents/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getDocumentStats() {
        log.info("Fetching document statistics");
        
        return retrievalService.count()
                .map(count -> {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("totalDocuments", count);
                    stats.put("status", "healthy");
                    return stats;
                })
                .onErrorResume(e -> {
                    log.error("Error fetching document stats", e);
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("totalDocuments", 0);
                    stats.put("status", "error");
                    stats.put("error", e.getMessage());
                    return Mono.just(stats);
                });
    }

    @PostMapping(value = "/cache/clear", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, String>> clearCache(@RequestParam(defaultValue = "all") String type) {
        log.info("Clearing cache: {}", type);
        
        return Mono.fromCallable(() -> {
            switch (type.toLowerCase()) {
                case "search":
                    cacheService.invalidateSearchCache();
                    break;
                case "all":
                default:
                    cacheService.invalidateAll();
                    break;
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cache cleared: " + type);
            return response;
        });
    }

    @PostMapping(value = "/metrics/reset", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, String>> resetMetrics() {
        log.info("Resetting metrics");
        
        return Mono.fromCallable(() -> {
            ragMetrics.reset();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Metrics reset successfully");
            return response;
        });
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> healthCheck() {
        return retrievalService.count()
                .map(count -> {
                    Map<String, Object> health = new HashMap<>();
                    health.put("status", "UP");
                    health.put("documentsIndexed", count);
                    health.put("cacheStats", cacheService.getStats().toString());
                    
                    MetricsSnapshot metrics = ragMetrics.getSnapshot();
                    health.put("successRate", String.format("%.1f%%", metrics.successRate()));
                    
                    return health;
                })
                .onErrorResume(e -> {
                    log.error("Health check failed", e);
                    Map<String, Object> health = new HashMap<>();
                    health.put("status", "DOWN");
                    health.put("error", e.getMessage());
                    return Mono.just(health);
                });
    }
}
