package com.jvmd.digitalurpaq_ai_agent.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String error,
        String message,
        String path,
        String requestId,
        Instant timestamp
) {
    public ApiError(int status, String error, String message, String path, String requestId) {
        this(status, error, message, path, requestId, Instant.now());
    }
}
