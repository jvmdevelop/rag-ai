package com.jvmd.digitalurpaq_ai_agent.service.rag.model;

import com.jvmd.digitalurpaq_ai_agent.service.rag.util.ResponseValidator;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public record MetricsSnapshot(
        int totalRequests,
        int successfulRequests,
        int failedRequests,
        int totalRetries,
        double successRate,
        double avgResponseTimeMs,
        ConcurrentHashMap<ValidationIssue, AtomicInteger> validationIssues,
        ConcurrentHashMap<String, AtomicInteger> errorTypes,
        List<RequestMetric> recentRequests
) {
    @Override
    public String toString() {
        return String.format(
                """
                        === RAG Metrics ===
                        Total Requests: %d
                        Successful: %d (%.1f%%)
                        Failed: %d
                        Retries: %d
                        Avg Response Time: %.0fms
                        
                        Validation Issues:
                        %s
                        
                        Error Types:
                        %s
                        ==================
                        """,
                totalRequests,
                successfulRequests,
                successRate,
                failedRequests,
                totalRetries,
                avgResponseTimeMs,
                formatValidationIssues(),
                formatErrorTypes()
        );
    }

    private String formatValidationIssues() {
        if (validationIssues.isEmpty()) {
            return "  None";
        }

        return validationIssues.entrySet().stream()
                .map(e -> String.format("  %s: %d", e.getKey(), e.getValue().get()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("  None");
    }

    private String formatErrorTypes() {
        if (errorTypes.isEmpty()) {
            return "  None";
        }

        return errorTypes.entrySet().stream()
                .map(e -> String.format("  %s: %d", e.getKey(), e.getValue().get()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("  None");
    }
}