package com.jvmd.digitalurpaq_ai_agent.service.rag.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ContextIndexEntry(
        String key,
        String fileName,
        Instant expiresAt,
        long sizeBytes
) {

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
