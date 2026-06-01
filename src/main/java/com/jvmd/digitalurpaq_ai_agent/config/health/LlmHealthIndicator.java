package com.jvmd.digitalurpaq_ai_agent.config.health;

import com.jvmd.digitalurpaq_ai_agent.config.properties.LlmProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class LlmHealthIndicator implements ReactiveHealthIndicator {

    private final LlmProperties llmProperties;

    @Override
    public Mono<Health> health() {
        return WebClient.create(llmProperties.baseUrl())
                .get()
                .uri("/models")
                .retrieve()
                .toBodilessEntity()
                .map(response -> Health.up()
                        .withDetail("model", llmProperties.model())
                        .withDetail("baseUrl", llmProperties.baseUrl())
                        .build())
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(ex -> Mono.just(Health.down()
                        .withDetail("model", llmProperties.model())
                        .withDetail("error", ex.getMessage())
                        .build()));
    }
}
