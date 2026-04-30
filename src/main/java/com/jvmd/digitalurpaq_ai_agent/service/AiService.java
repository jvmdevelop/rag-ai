package com.jvmd.digitalurpaq_ai_agent.service;

import com.jvmd.digitalurpaq_ai_agent.llm.langchain.DigitalUrpaqAssistant;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.RagOrchestrator;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.SessionStore;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class AiService {

    private final DigitalUrpaqAssistant assistant;
    private final RagOrchestrator ragOrchestrator;
    private final SessionStore sessionStore;

    public AiService(DigitalUrpaqAssistant assistant,
                     RagOrchestrator ragOrchestrator,
                     SessionStore sessionStore) {
        this.assistant = assistant;
        this.ragOrchestrator = ragOrchestrator;
        this.sessionStore = sessionStore;
    }

    public Mono<String> responseUrpaq(String message, String sessionId) {
        log.info("Processing RAG request: sessionId={}, message={}", sessionId, message);

        return ragOrchestrator.processQuery(message)
                .map(RagOrchestrator.RagResponse::answer)
                .flatMap(ragContext -> Mono.fromCallable(() -> {
                    // Combine RAG context with the user message for the assistant
                    String enrichedMessage = buildEnrichedMessage(message, ragContext);
                    String response = assistant.chat(sessionId, enrichedMessage);
                    sessionStore.addMessage(sessionId, "user", message);
                    sessionStore.addMessage(sessionId, "assistant", response);
                    return response;
                }).subscribeOn(Schedulers.boundedElastic()))
                .doOnSuccess(r -> log.info("Response generated for session={}", sessionId))
                .doOnError(e -> log.error("Error in AiService for session={}: {}", sessionId, e.getMessage()));
    }

    public Flux<String> streamResponse(String message, String sessionId) {
        log.info("Streaming request: sessionId={}", sessionId);

        return ragOrchestrator.processQuery(message)
                .map(RagOrchestrator.RagResponse::answer)
                .flatMapMany(ragContext -> {
                    String enrichedMessage = buildEnrichedMessage(message, ragContext);
                    return tokenStreamToFlux(sessionId, enrichedMessage);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Flux<String> tokenStreamToFlux(String sessionId, String message) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        StringBuilder fullResponse = new StringBuilder();

        Schedulers.boundedElastic().schedule(() -> {
            try {
                TokenStream tokenStream = assistant.chatStream(sessionId, message);
                tokenStream
                        .onNext(token -> {
                            fullResponse.append(token);
                            sink.tryEmitNext(token);
                        })
                        .onComplete(response -> {
                            String full = fullResponse.toString();
                            sessionStore.addMessage(sessionId, "user", message);
                            sessionStore.addMessage(sessionId, "assistant", full);
                            sink.tryEmitComplete();
                        })
                        .onError(error -> {
                            log.error("Streaming error for session={}: {}", sessionId, error.getMessage());
                            sink.tryEmitError(error);
                        })
                        .start();
            } catch (Exception e) {
                log.error("Failed to start token stream: {}", e.getMessage());
                sink.tryEmitError(e);
            }
        });

        return sink.asFlux();
    }

    private String buildEnrichedMessage(String userMessage, String ragContext) {
        if (ragContext == null || ragContext.isBlank() || ragContext.contains("не найден")) {
            return userMessage;
        }
        return String.format("""
                [КОНТЕКСТ ИЗ БАЗЫ ЗНАНИЙ]:
                %s

                [ВОПРОС ПОЛЬЗОВАТЕЛЯ]:
                %s
                """, ragContext, userMessage);
    }

    public Mono<String> responseUrpaq(String message) {
        return responseUrpaq(message, "anonymous");
    }

    public Mono<String> responseUrpaq(Mono<String> message) {
        return message.flatMap(this::responseUrpaq);
    }
}
