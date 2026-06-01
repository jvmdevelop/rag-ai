package com.jvmd.digitalurpaq_ai_agent.controller;

import com.jvmd.digitalurpaq_ai_agent.model.ChatMessage;
import com.jvmd.digitalurpaq_ai_agent.model.ChatRequest;
import com.jvmd.digitalurpaq_ai_agent.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI chat endpoints")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Send a message", description = "Send a message and get an AI response (JSON)")
    @PostMapping(value = "/message", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ChatMessage> sendMessage(@Valid @RequestBody ChatRequest request) {
        log.info("Received message: {}", request.getMessage());
        return chatService.processMessage(request.getMessage());
    }

    @Operation(summary = "Stream a message", description = "Send a message and receive the AI response as Server-Sent Events")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamMessage(@Valid @RequestBody ChatRequest request) {
        log.info("Streaming message: {}", request.getMessage());

        return chatService.processMessage(request.getMessage())
                .flatMapMany(chatMessage -> {
                    String fullResponse = chatMessage.getResponse().getMessage();
                    String[] words = fullResponse.split("(?<=\\s)");

                    return Flux.fromArray(words)
                            .index()
                            .delayElements(Duration.ofMillis(30))
                            .map(tuple -> ServerSentEvent.<String>builder()
                                    .id(String.valueOf(tuple.getT1()))
                                    .event("message")
                                    .data(tuple.getT2())
                                    .build())
                            .concatWith(Flux.just(
                                    ServerSentEvent.<String>builder()
                                            .event("done")
                                            .data("[DONE]")
                                            .build()
                            ));
                })
                .onErrorResume(ex -> {
                    log.error("Stream error: {}", ex.getMessage());
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("Произошла ошибка при генерации ответа")
                            .build());
                });
    }
}
