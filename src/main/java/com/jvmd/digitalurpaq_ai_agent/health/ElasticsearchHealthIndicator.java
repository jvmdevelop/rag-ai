package com.jvmd.digitalurpaq_ai_agent.health;

import com.jvmd.digitalurpaq_ai_agent.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component("elasticsearch")
@RequiredArgsConstructor
public class ElasticsearchHealthIndicator implements ReactiveHealthIndicator {

    private final RetrievalService retrievalService;

    @Override
    public Mono<Health> health() {
        return retrievalService.count()
                .map(count -> Health.up()
                        .withDetail("documentsIndexed", count)
                        .withDetail("status", "connected")
                        .build())
                .onErrorResume(ex -> {
                    log.warn("[Health] Elasticsearch DOWN: {}", ex.getMessage());
                    return Mono.just(Health.down()
                            .withDetail("error", ex.getMessage())
                            .withDetail("status", "unreachable")
                            .build());
                })
                .timeout(java.time.Duration.ofSeconds(5),
                        Mono.just(Health.down().withDetail("error", "timeout").build()));
    }
}
