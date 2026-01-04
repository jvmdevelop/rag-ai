package com.jvmd.digitalurpaq_ai_agent.service.rag.model;

import com.jvmd.digitalurpaq_ai_agent.model.RetrievalDocument;

public record ScoredDocument(
        RetrievalDocument document,
        double score
) {
    public String getText() {
        return document.getText();
    }

    public String getName() {
        return document.getName();
    }
}