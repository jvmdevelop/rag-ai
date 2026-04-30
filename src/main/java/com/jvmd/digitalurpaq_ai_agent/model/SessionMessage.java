package com.jvmd.digitalurpaq_ai_agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMessage(
        String role,        // "user" or "assistant" or "system"
        String content,
        Instant timestamp
) {}
