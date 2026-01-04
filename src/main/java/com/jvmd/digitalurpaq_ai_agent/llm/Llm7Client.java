package com.jvmd.digitalurpaq_ai_agent.llm;

import com.jvmd.digitalurpaq_ai_agent.llm.model.ChatRequest;
import com.jvmd.digitalurpaq_ai_agent.llm.model.ChatResponse;
import com.jvmd.digitalurpaq_ai_agent.llm.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
public class Llm7Client {

    private final WebClient webClient;
    private final String model;

    public Llm7Client(
            @Value("${llm7.api.base-url:https://api.llm7.io/v1}") String baseUrl,
            @Value("${llm7.api.key:unused}") String apiKey,
            @Value("${llm7.api.model:bidara}") String model
    ) {
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("Initialized LLM7 client with base URL: {} and model: {}", baseUrl, model);
    }

    public Mono<String> chat(String userMessage) {
        ChatRequest request = new ChatRequest(
                model,
                List.of(new Message("user", userMessage)),
                0.7,
                500
        );

        log.debug("Sending chat request to LLM7.io: {}", userMessage);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .map(response -> {
                    if (response.choices() != null && !response.choices().isEmpty()) {
                        String content = response.choices().get(0).message().content();
                        log.debug("Received response from LLM7.io: {} chars", content.length());
                        return content;
                    }
                    return "Ответ не получен";
                })
                .onErrorResume(e -> {
                    log.error("Error calling LLM7.io API: {}", e.getMessage(), e);
                    return Mono.just("Извините, произошла ошибка при обращении к AI модели.");
                });
    }


}
