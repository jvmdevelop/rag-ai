package com.jvmd.digitalurpaq_ai_agent.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequest {

    @NotBlank(message = "Сообщение не может быть пустым")
    @Size(min = 1, max = 2000, message = "Сообщение должно быть от 1 до 2000 символов")
    private String message;

    @Size(max = 100, message = "sessionId не должен превышать 100 символов")
    @Pattern(
        regexp = "^[a-zA-Z0-9_\\-]*$",
        message = "sessionId может содержать только буквы, цифры, дефис и подчёркивание"
    )
    private String sessionId;
}
