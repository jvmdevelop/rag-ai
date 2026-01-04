package com.jvmd.digitalurpaq_ai_agent.controller;

import com.jvmd.digitalurpaq_ai_agent.model.ChatMessage;
import com.jvmd.digitalurpaq_ai_agent.model.ChatRequest;
import com.jvmd.digitalurpaq_ai_agent.service.ChatService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@AllArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping(value = "/message", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ChatMessage> sendMessage(@RequestBody ChatRequest request) {
        log.info("Received HTTP message: {}", request.getMessage());
        return chatService.processMessage(request.getMessage());
    }
}
