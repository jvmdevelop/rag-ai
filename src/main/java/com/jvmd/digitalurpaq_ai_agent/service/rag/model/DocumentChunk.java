package com.jvmd.digitalurpaq_ai_agent.service.rag.model;

import com.jvmd.digitalurpaq_ai_agent.model.RetrievalDocument;

public record DocumentChunk(
        String id,
        String documentId,
        String documentName,
        String text,
        int chunkIndex
) {
    public RetrievalDocument toRetrievalDocument() {
        return new RetrievalDocument(id, documentName + " (часть " + (chunkIndex + 1) + ")", text);
    }
}