package com.jvmd.digitalurpaq_ai_agent.model.event;

import java.time.Instant;

public record DocumentIndexedEvent(
        String documentId,
        String documentName,
        int chunkCount,
        Instant indexedAt
) {}
