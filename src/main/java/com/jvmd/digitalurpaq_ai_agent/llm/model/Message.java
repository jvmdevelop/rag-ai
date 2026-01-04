package com.jvmd.digitalurpaq_ai_agent.llm.model;

public record Message(
        String role,
        String content
) {
}