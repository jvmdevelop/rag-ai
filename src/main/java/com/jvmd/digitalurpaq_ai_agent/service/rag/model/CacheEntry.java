package com.jvmd.digitalurpaq_ai_agent.service.rag.model;

import java.time.Duration;
import java.time.Instant;

public record CacheEntry<T>(
        T value,
        Instant expiresAt
) implements Comparable<CacheEntry<T>> {

    public CacheEntry(T value, Duration ttl) {
        this(value, Instant.now().plus(ttl));
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    @Override
    public int compareTo(CacheEntry<T> other) {
        return this.expiresAt.compareTo(other.expiresAt);
    }
}