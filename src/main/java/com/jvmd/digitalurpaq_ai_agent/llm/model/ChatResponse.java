package com.jvmd.digitalurpaq_ai_agent.llm.model;

import java.util.List;

public record ChatResponse(
        String id,
        String object,
        long created,
        String model,
        List<Choice> choices,
        Usage usage
) {
}