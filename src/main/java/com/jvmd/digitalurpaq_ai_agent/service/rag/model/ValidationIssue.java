package com.jvmd.digitalurpaq_ai_agent.service.rag.model;

public enum ValidationIssue {
        NONE,
        EMPTY_RESPONSE,
        TOO_SHORT,
        TOO_LONG,
        TRUNCATED,
        HALLUCINATION,
        IRRELEVANT;
}