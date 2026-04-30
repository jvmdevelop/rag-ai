package com.jvmd.digitalurpaq_ai_agent.llm;

import com.jvmd.digitalurpaq_ai_agent.llm.model.ChatRequest;
import com.jvmd.digitalurpaq_ai_agent.llm.model.ChatResponse;
import com.jvmd.digitalurpaq_ai_agent.llm.model.Message;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * LLM7.io API client (OpenAI-compatible).
 *
 * Senior patterns applied:
 *  - reactor.util.retry.Retry: reactive-aware retry with exponential backoff.
 *    @Retryable (Spring Retry) is NOT used here because it works by wrapping the
 *    method call synchronously — it only sees the Mono being *constructed* (which
 *    never throws), so it would never retry on subscription-time failures.
 *    Reactor's retryWhen() hooks into the actual subscription and retries correctly.
 *  - Micrometer Timer: manual timer sample for accurate reactive timing.
 *  - onErrorResume: graceful fallback after all retries exhausted.
 */
@Slf4j
@Service
public class Llm7Client {

    private final WebClient webClient;
    private final String model;
    private final MeterRegistry meterRegistry;

    public Llm7Client(
            @Value("${llm7.api.base-url:https://api.llm7.io/v1}") String baseUrl,
            @Value("${llm7.api.key:unused}") String apiKey,
            @Value("${llm7.api.model:bidara}") String model,
            MeterRegistry meterRegistry) {
        this.model = model;
        this.meterRegistry = meterRegistry;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB
                .build();
        log.info("Llm7Client initialized: baseUrl={}, model={}", baseUrl, model);
    }

    /**
     * Send a chat completion request to LLM7.io.
     *
     * Retry strategy: up to 3 attempts with exponential backoff (1s → 2s → 4s),
     * only on transient network errors (WebClientRequestException).
     * HTTP 4xx/5xx errors (WebClientResponseException) are NOT retried — they are
     * deterministic and retrying would waste quota.
     */
    public Mono<String> chat(String userMessage) {
        ChatRequest request = new ChatRequest(
                model,
                List.of(new Message("user", userMessage)),
                0.7,
                1000
        );

        log.debug("LLM7 request: {} chars", userMessage.length());
        Timer.Sample sample = Timer.start(meterRegistry);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .timeout(Duration.ofSeconds(60))
                .map(response -> {
                    if (response.choices() != null && !response.choices().isEmpty()) {
                        String content = response.choices().get(0).message().content();
                        log.debug("LLM7 response: {} chars", content.length());
                        return content;
                    }
                    return "Ответ не получен";
                })
                // Reactive retry: only retries WebClientRequestException (transient network)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(8))
                        .filter(t -> t instanceof WebClientRequestException)
                        .doBeforeRetry(signal ->
                                log.warn("LLM7 retry #{} — {}", signal.totalRetries() + 1, signal.failure().getMessage())))
                // After all retries exhausted, or on non-retryable errors: graceful fallback
                .onErrorResume(WebClientRequestException.class, ex -> {
                    log.error("LLM7 unavailable after retries. Error: {}", ex.getMessage());
                    return Mono.just("Сервис AI временно недоступен. Пожалуйста, попробуйте через несколько минут.");
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("LLM7 responded with error {}: {}", ex.getStatusCode(), ex.getMessage());
                    return Mono.just("Ошибка при обращении к AI модели. Попробуйте ещё раз.");
                })
                .doFinally(signal -> sample.stop(Timer.builder("llm.chat")
                        .description("LLM7 chat completion duration")
                        .tag("status", signal.name())
                        .register(meterRegistry)));
    }
}
