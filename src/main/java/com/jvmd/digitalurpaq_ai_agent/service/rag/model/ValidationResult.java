package com.jvmd.digitalurpaq_ai_agent.service.rag.model;

public record ValidationResult(
        boolean isValid,
        String processedResponse,
        ValidationIssue issue
) {
}