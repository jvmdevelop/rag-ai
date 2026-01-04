package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ValidationIssue;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class ResponseValidator {

    private static final int MIN_RESPONSE_LENGTH = 10;
    private static final int MAX_RESPONSE_LENGTH = 5000;
    private static final Pattern HALLUCINATION_PATTERNS = Pattern.compile(
            "(я не знаю|не могу сказать|информация отсутствует|данных нет)",
            Pattern.CASE_INSENSITIVE
    );

    public ValidationResult validate(String response, String originalQuery) {
        if (response == null || response.isBlank()) {
            log.warn("Empty response received");
            return new ValidationResult(
                    false,
                    "Извините, не удалось сформировать ответ. Попробуйте переформулировать вопрос.",
                    ValidationIssue.EMPTY_RESPONSE
            );
        }

        if (response.length() < MIN_RESPONSE_LENGTH) {
            log.warn("Response too short: {} chars", response.length());
            return new ValidationResult(
                    false,
                    "Ответ слишком короткий. Пожалуйста, уточните ваш вопрос.",
                    ValidationIssue.TOO_SHORT
            );
        }

        if (response.length() > MAX_RESPONSE_LENGTH) {
            log.warn("Response too long: {} chars, truncating", response.length());
            String truncated = truncateResponse(response);
            return new ValidationResult(
                    true,
                    truncated,
                    ValidationIssue.TRUNCATED
            );
        }

        if (containsHallucination(response)) {
            log.warn("Potential hallucination detected in response");
            return new ValidationResult(
                    false,
                    "К сожалению, в базе знаний недостаточно информации для ответа на ваш вопрос. " +
                            "Попробуйте задать более конкретный вопрос или обратитесь к администратору.",
                    ValidationIssue.HALLUCINATION
            );
        }

        String processed = postProcess(response);

        log.info("Response validated successfully, length: {}", processed.length());
        return new ValidationResult(true, processed, ValidationIssue.NONE);
    }

    private boolean containsHallucination(String response) {
        return HALLUCINATION_PATTERNS.matcher(response).find();
    }

    private String truncateResponse(String response) {
        String truncated = response.substring(0, MAX_RESPONSE_LENGTH);

        int lastPeriod = truncated.lastIndexOf('.');
        if (lastPeriod > MAX_RESPONSE_LENGTH - 200) {
            truncated = truncated.substring(0, lastPeriod + 1);
        }

        return truncated + "\n\n[Ответ сокращен для удобства чтения]";
    }

    private String postProcess(String response) {
        String processed = response.trim();

        processed = processed.replaceAll("\\s+", " ");
        processed = processed.replaceAll("\\n{3,}", "\n\n");

        processed = processed.replaceAll("\\[INST\\]|\\[/INST\\]|<\\|.*?\\|>", "");

        processed = formatLists(processed);

        return processed.trim();
    }

    private String formatLists(String text) {
        text = text.replaceAll("(?m)^\\s*[-*]\\s+", "• ");
        text = text.replaceAll("(?m)^\\s*(\\d+)[.):]\\s+", "$1. ");

        return text;
    }


}
