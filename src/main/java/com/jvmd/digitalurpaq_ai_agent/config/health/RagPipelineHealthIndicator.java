package com.jvmd.digitalurpaq_ai_agent.config.health;

import com.jvmd.digitalurpaq_ai_agent.service.RetrievalService;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.MetricsSnapshot;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.CacheService;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.RagMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RagPipelineHealthIndicator implements ReactiveHealthIndicator {

    private final RagMetrics ragMetrics;
    private final CacheService cacheService;
    private final RetrievalService retrievalService;

    @Override
    public Mono<Health> health() {
        MetricsSnapshot snapshot = ragMetrics.getSnapshot();

        return retrievalService.count()
                .map(docCount -> {
                    Health.Builder builder = snapshot.successRate() >= 50 ? Health.up() : Health.down();
                    return builder
                            .withDetail("documentsIndexed", docCount)
                            .withDetail("totalRequests", snapshot.totalRequests())
                            .withDetail("successRate", String.format("%.1f%%", snapshot.successRate()))
                            .withDetail("avgResponseTimeMs", String.format("%.0f", snapshot.avgResponseTimeMs()))
                            .withDetail("cacheStats", cacheService.getStats().toString())
                            .build();
                })
                .onErrorResume(ex -> Mono.just(Health.down()
                        .withDetail("error", ex.getMessage())
                        .build()));
    }
}
