package com.jvmd.digitalurpaq_ai_agent.model.event;

public record CacheInvalidatedEvent(
        String namespace,
        String reason
) {}
