package com.jvmd.digitalurpaq_ai_agent.llm;

import com.jvmd.digitalurpaq_ai_agent.config.properties.LlmProperties;
import com.jvmd.digitalurpaq_ai_agent.llm.model.ChatRequest;
import com.jvmd.digitalurpaq_ai_agent.llm.model.ChatResponse;
import com.jvmd.digitalurpaq_ai_agent.llm.model.Message;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class Llm7Client {

    private final WebClient webClient;
    private final LlmProperties properties;

    public Llm7Client(LlmProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.key())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();

        log.info("LLM7 client initialized — base-url={}, model={}", properties.baseUrl(), properties.model());
    }

    @CircuitBreaker(name = "llm", fallbackMethod = "chatFallback")
    public Mono<String> chat(String userMessage) {
        ChatRequest request = new ChatRequest(
                properties.model(),
                List.of(new Message("user", userMessage)),
                properties.temperature(),
                properties.maxTokens()
        );

        log.debug("LLM request: {} chars", userMessage.length());

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .map(response -> {
                    if (response.choices() != null && !response.choices().isEmpty()) {
                        String content = response.choices().get(0).message().content();
                        log.debug("LLM response: {} chars, tokens: {}", content.length(),
                                response.usage() != null ? response.usage().totalTokens() : "N/A");
                        return content;
                    }
                    return "Ответ не получен";
                });
    }

    @SuppressWarnings("unused")
    private Mono<String> chatFallback(String userMessage, Throwable ex) {
        log.error("Circuit breaker open — LLM unavailable: {}", ex.getMessage());
        return Mono.just("Извините, AI модель временно недоступна. Попробуйте позже.");
    }
}
