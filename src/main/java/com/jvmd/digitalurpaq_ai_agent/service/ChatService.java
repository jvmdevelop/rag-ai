package com.jvmd.digitalurpaq_ai_agent.service;

import com.jvmd.digitalurpaq_ai_agent.model.ChatMessage;
import com.jvmd.digitalurpaq_ai_agent.model.ChatResponse;
import com.jvmd.digitalurpaq_ai_agent.model.EType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class ChatService {

    private final AiService aiService;

    public Mono<ChatMessage> processMessage(String userText) {
        log.info("Processing message from user: {}", userText);
        
        return aiService.responseUrpaq(Mono.just(userText))
                .map(aiText -> {
                    ChatMessage chatMessage = new ChatMessage(
                            EType.USER, 
                            userText, 
                            LocalDateTime.now(),
                            new ChatResponse(EType.AI_HELPER, aiText, LocalDateTime.now())
                    );
                    
                    log.info("Response created: {}", chatMessage);
                    return chatMessage;
                });
    }
}
