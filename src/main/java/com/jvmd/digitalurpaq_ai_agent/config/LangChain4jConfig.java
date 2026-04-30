package com.jvmd.digitalurpaq_ai_agent.config;

import com.jvmd.digitalurpaq_ai_agent.llm.langchain.DigitalUrpaqAssistant;
import com.jvmd.digitalurpaq_ai_agent.llm.langchain.UrpaqTools;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.SessionStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class LangChain4jConfig {

    @Value("${llm7.api.base-url:https://api.llm7.io/v1}")
    private String baseUrl;

    @Value("${llm7.api.key:unused}")
    private String apiKey;

    @Value("${llm7.api.model:bidara}")
    private String model;

    @Value("${app.session.max-messages:20}")
    private int maxMessages;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("Configuring LangChain4j ChatLanguageModel: baseUrl={}, model={}", baseUrl, model);
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(0.7)
                .maxTokens(1000)
                .timeout(Duration.ofSeconds(60))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        log.info("Configuring LangChain4j StreamingChatLanguageModel: baseUrl={}, model={}", baseUrl, model);
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(0.7)
                .maxTokens(1000)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public DigitalUrpaqAssistant digitalUrpaqAssistant(
            ChatLanguageModel chatLanguageModel,
            StreamingChatLanguageModel streamingChatLanguageModel,
            UrpaqTools urpaqTools,
            SessionStore sessionStore) {

        return AiServices.builder(DigitalUrpaqAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .streamingChatLanguageModel(streamingChatLanguageModel)
                .tools(urpaqTools)
                .chatMemoryProvider(sessionId -> MessageWindowChatMemory.builder()
                        .id(sessionId)
                        .maxMessages(maxMessages)
                        .chatMemoryStore(sessionStore.toChatMemoryStore())
                        .build())
                .build();
    }
}
