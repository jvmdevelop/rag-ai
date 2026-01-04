package com.jvmd.digitalurpaq_ai_agent.service.rag.model;

import java.util.List;

public record RagResponse(
            String answer,
            ProcessedQuery processedQuery,
            List<ScoredDocument> sourceDocuments,
            boolean isValid,
            ValidationIssue validationIssue
    ) {
        public String getSourcesSummary() {
            if (sourceDocuments.isEmpty()) {
                return "Источники не найдены";
            }
            
            return sourceDocuments.stream()
                    .limit(3)
                    .map(doc -> String.format("• %s", doc.getName()))
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("Нет источников");
        }
        
        public int getSourceCount() {
            return sourceDocuments.size();
        }
    }