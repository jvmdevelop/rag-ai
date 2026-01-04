package com.jvmd.digitalurpaq_ai_agent.service.rag.model;

public record ProcessedQuery(
        String originalQuery,
        String metadata,
        QueryCategory category,
        String keywords
) {
    public String getSearchQuery() {
        return keywords.isBlank() ? originalQuery : keywords;
    }
}