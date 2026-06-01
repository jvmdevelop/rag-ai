package com.jvmd.digitalurpaq_ai_agent.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        int topK,
        int maxRetries,
        Duration pipelineTimeout,
        int maxContextLength,
        Duration searchCacheTtl,
        Duration queryCacheTtl,
        int maxCacheSize,
        int chunkSize,
        int chunkOverlap
) {
    public RagProperties {
        if (topK == 0) topK = 5;
        if (maxRetries == 0) maxRetries = 2;
        if (pipelineTimeout == null) pipelineTimeout = Duration.ofSeconds(30);
        if (maxContextLength == 0) maxContextLength = 4000;
        if (searchCacheTtl == null) searchCacheTtl = Duration.ofMinutes(30);
        if (queryCacheTtl == null) queryCacheTtl = Duration.ofMinutes(60);
        if (maxCacheSize == 0) maxCacheSize = 1000;
        if (chunkSize == 0) chunkSize = 500;
        if (chunkOverlap == 0) chunkOverlap = 100;
    }
}
