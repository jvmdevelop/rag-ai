package com.jvmd.digitalurpaq_ai_agent.llm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Choice(
        int index,
        Message message,
        @JsonProperty("finish_reason") String finishReason
) {
}

