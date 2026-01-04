package com.jvmd.digitalurpaq_ai_agent.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class ChatResponse {
    private EType entity ;
    private String message;
    private LocalDateTime timestamp;
}
