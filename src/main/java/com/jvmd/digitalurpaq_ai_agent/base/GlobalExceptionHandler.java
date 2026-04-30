package com.jvmd.digitalurpaq_ai_agent.base;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_BASE_URI = "https://digitalurpaq.kz/errors";

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ProblemDetail> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        pd.setTitle("Validation Error");
        pd.setType(URI.create(ERROR_BASE_URI + "/validation"));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("path", exchange.getRequest().getPath().value());
        pd.setProperty("violations", ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList());
        log.warn("[Error] Validation failed at {}: {}", exchange.getRequest().getPath(), ex.getMessage());
        return Mono.just(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex, ServerWebExchange exchange) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Constraint violation");
        pd.setTitle("Validation Error");
        pd.setType(URI.create(ERROR_BASE_URI + "/validation"));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("violations", ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .toList());
        return Mono.just(pd);
    }

    @ExceptionHandler(RagProcessingException.class)
    public Mono<ProblemDetail> handleRagError(RagProcessingException ex, ServerWebExchange exchange) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        pd.setTitle("RAG Processing Error");
        pd.setType(URI.create(ERROR_BASE_URI + "/rag-error"));
        pd.setProperty("errorCode", ex.getErrorCode());
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("path", exchange.getRequest().getPath().value());
        log.error("[Error] RAG error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return Mono.just(pd);
    }

    @ExceptionHandler(TimeoutException.class)
    public Mono<ProblemDetail> handleTimeout(TimeoutException ex, ServerWebExchange exchange) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT,
                "Превышено время ожидания от LLM сервиса. Попробуйте упростить запрос.");
        pd.setTitle("LLM Timeout");
        pd.setType(URI.create(ERROR_BASE_URI + "/timeout"));
        pd.setProperty("timestamp", Instant.now());
        log.warn("[Error] Timeout at {}", exchange.getRequest().getPath());
        return Mono.just(pd);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ProblemDetail> handleGeneral(Exception ex, ServerWebExchange exchange) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Внутренняя ошибка сервера. Попробуйте позже.");
        pd.setTitle("Internal Server Error");
        pd.setType(URI.create(ERROR_BASE_URI + "/internal-error"));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("path", exchange.getRequest().getPath().value());
        log.error("[Error] Unhandled exception at {}: {}", exchange.getRequest().getPath(), ex.getMessage(), ex);
        return Mono.just(pd);
    }
}
