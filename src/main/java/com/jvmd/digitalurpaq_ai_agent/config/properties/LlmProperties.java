package com.jvmd.digitalurpaq_ai_agent.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "llm7.api")
public record LlmProperties(
        @NotBlank String baseUrl,
        @NotBlank String key,
        @NotBlank String model,
        @Positive double temperature,
        @Positive int maxTokens,
        @Positive int timeoutSeconds
) {
    public LlmProperties {
        if (temperature == 0) temperature = 0.7;
        if (maxTokens == 0) maxTokens = 500;
        if (timeoutSeconds == 0) timeoutSeconds = 60;
    }
}
