package com.jvmd.digitalurpaq_ai_agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
public class ChatMessage {
    private EType entity;
    private String message;
    private LocalDateTime timestamp;

    private ChatResponse response;
}
