package com.jvmd.digitalurpaq_ai_agent.service.rag.model;

import com.jvmd.digitalurpaq_ai_agent.service.rag.util.SearchStrategy;

import java.util.List;

public record ResponseWithContext(
            String response,
            ProcessedQuery query,
            List<ScoredDocument> documents
    ) {}