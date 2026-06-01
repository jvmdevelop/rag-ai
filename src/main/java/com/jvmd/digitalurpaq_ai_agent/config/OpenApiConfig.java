package com.jvmd.digitalurpaq_ai_agent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Digital Urpaq AI Agent API")
                        .version("2.0.0")
                        .description("""
                                RAG-powered AI assistant for the Palace of Schoolchildren.

                                Features:
                                - Intelligent document search with hybrid ranking
                                - Context-aware response generation via LLM
                                - Real-time SSE streaming
                                - Admin panel with metrics and cache management
                                """)
                        .contact(new Contact()
                                .name("Digital Urpaq Team")
                                .email("dvorecsko@sqo.gov.kz")))
                .servers(List.of(
                        new Server().url("/").description("Current server")
                ));
    }
}
