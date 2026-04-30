package com.jvmd.digitalurpaq_ai_agent.service;

import com.jvmd.digitalurpaq_ai_agent.model.ChatMessage;
import com.jvmd.digitalurpaq_ai_agent.model.ChatResponse;
import com.jvmd.digitalurpaq_ai_agent.model.EType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class ChatService {

    private final AiService aiService;

    public Mono<ChatMessage> processMessage(String userText, String sessionId) {
        log.info("Processing message sessionId={}: {}", sessionId, userText);

        return aiService.responseUrpaq(userText, sessionId)
                .map(aiText -> new ChatMessage(
                        EType.USER,
                        userText,
                        LocalDateTime.now(),
                        new ChatResponse(EType.AI_HELPER, aiText, LocalDateTime.now())
                ));
    }

    public Flux<String> streamMessage(String userText, String sessionId) {
        log.info("Streaming message sessionId={}: {}", sessionId, userText);
        return aiService.streamResponse(userText, sessionId);
    }

    // Backward compatible
    public Mono<ChatMessage> processMessage(String userText) {
        return processMessage(userText, "anonymous");
    }
}
