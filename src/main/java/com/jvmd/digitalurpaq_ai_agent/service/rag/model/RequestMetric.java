package com.jvmd.digitalurpaq_ai_agent.service.rag.model;

import java.time.Instant;

public record RequestMetric(
        Instant timestamp,
        long responseTimeMs,
        boolean success,
        String errorType
) {
}
