package com.jvmd.digitalurpaq_ai_agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatSession(
        String sessionId,
        List<SessionMessage> messages,
        Instant createdAt,
        Instant lastAccessedAt,
        int messageCount
) {

    public boolean isExpired(long ttlHours) {
        return Instant.now().isAfter(lastAccessedAt.plusSeconds(ttlHours * 3600));
    }
}
