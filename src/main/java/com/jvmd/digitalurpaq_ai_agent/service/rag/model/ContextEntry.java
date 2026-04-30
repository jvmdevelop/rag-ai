package com.jvmd.digitalurpaq_ai_agent.service.rag.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ContextEntry<T>(
        String key,
        T value,
        Instant createdAt,
        Instant expiresAt,
        long accessCount,
        Instant lastAccessedAt
) {

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public ContextEntry<T> withAccess() {
        return new ContextEntry<>(key, value, createdAt, expiresAt, accessCount + 1, Instant.now());
    }
}
