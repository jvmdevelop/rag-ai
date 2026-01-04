package com.jvmd.digitalurpaq_ai_agent.service;

import com.jvmd.digitalurpaq_ai_agent.llm.Llm7Client;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.RagOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class AiService {

    private final Llm7Client llm7Client;
    private final RagOrchestrator ragOrchestrator;

    public AiService(Llm7Client llm7Client, RagOrchestrator ragOrchestrator) {
        this.llm7Client = llm7Client;
        this.ragOrchestrator = ragOrchestrator;
    }

    public Mono<String> responseUrpaq(String message) {
        log.info("Processing RAG request: {}", message);
        
        return ragOrchestrator.processQuery(message)
                .map(RagOrchestrator.RagResponse::answer)
                .doOnSuccess(response -> log.info("RAG response generated successfully"))
                .doOnError(error -> log.error("Error in RAG processing: {}", error.getMessage()));
    }

    public Mono<String> responseUrpaq(Mono<String> message) {
        return message.flatMap(this::responseUrpaq);
    }

    public Mono<String> simpleResponse(String prompt) {
        log.debug("Generating simple response");
        
        return llm7Client.chat(prompt)
                .doOnError(error -> log.error("Error generating simple response: {}", error.getMessage()));
    }

    public Mono<String> response(Mono<String> message) {
        return message.flatMap(o -> {
            String prompt = "Ты инструмент который сокращает сообщения по форме : " +
                    "[Название]: <|РАСПИСАНИЕ|КАБИНЕТЫ|УЧИТЕЛЯ|НАПРАВЛЕНИЯ>\n" +
                    "[Ключевые слова]: <ключевые слова>\n" +
                    "[Краткое описание]: <6 предложения>\n" +
                    "Вот текст который надо сократить:" + o;

            return llm7Client.chat(prompt);
        });
    }
}
