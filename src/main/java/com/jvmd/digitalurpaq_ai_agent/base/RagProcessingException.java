package com.jvmd.digitalurpaq_ai_agent.base;

import lombok.Getter;

@Getter
public class RagProcessingException extends RuntimeException {

    private final String errorCode;

    public RagProcessingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public RagProcessingException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

}
