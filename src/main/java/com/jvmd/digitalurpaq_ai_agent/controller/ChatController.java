package com.jvmd.digitalurpaq_ai_agent.controller;

import com.jvmd.digitalurpaq_ai_agent.model.ChatMessage;
import com.jvmd.digitalurpaq_ai_agent.model.ChatRequest;
import com.jvmd.digitalurpaq_ai_agent.service.ChatService;
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
public class ChatController {

    private final ChatService chatService;

    @PostMapping(value = "/message", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ChatMessage> sendMessage(@Valid @RequestBody ChatRequest request) {
        String sessionId = resolveSession(request.getSessionId());
        log.info("POST /api/chat/message sessionId={}", sessionId);
        return chatService.processMessage(request.getMessage(), sessionId);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamMessage(
            @RequestParam @jakarta.validation.constraints.NotBlank String message,
            @RequestParam(required = false) String sessionId) {

        String sid = resolveSession(sessionId);
        log.info("GET /api/chat/stream sessionId={}", sid);

        return chatService.streamMessage(message, sid)
                .map(token -> ServerSentEvent.<String>builder()
                        .event("token")
                        .data(token)
                        .build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build()))
                .onErrorResume(e -> {
                    log.error("SSE stream error: {}", e.getMessage());
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(e.getMessage())
                            .build());
                })
                .timeout(Duration.ofSeconds(120));
    }

    private String resolveSession(String sessionId) {
        return (sessionId == null || sessionId.isBlank()) ? "anonymous" : sessionId;
    }
}
