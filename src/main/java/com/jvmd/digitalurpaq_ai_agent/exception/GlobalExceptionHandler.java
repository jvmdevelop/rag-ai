package com.jvmd.digitalurpaq_ai_agent.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public Mono<ResponseEntity<ApiError>> handleRateLimit(RateLimitExceededException ex, ServerWebExchange exchange) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "Rate Limit Exceeded", ex.getMessage(), exchange);
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public Mono<ResponseEntity<ApiError>> handleNotFound(DocumentNotFoundException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiError>> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        String message = ex.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Error", message, exchange);
    }

    @ExceptionHandler(TimeoutException.class)
    public Mono<ResponseEntity<ApiError>> handleTimeout(TimeoutException ex, ServerWebExchange exchange) {
        log.error("Request timeout: {}", ex.getMessage());
        return buildResponse(HttpStatus.GATEWAY_TIMEOUT, "Timeout",
                "Превышено время ожидания. Попробуйте упростить запрос.", exchange);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiError>> handleBadArgument(IllegalArgumentException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), exchange);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiError>> handleGeneric(Exception ex, ServerWebExchange exchange) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Произошла внутренняя ошибка сервера.", exchange);
    }

    private Mono<ResponseEntity<ApiError>> buildResponse(HttpStatus status, String error, String message,
                                                          ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");

        ApiError apiError = new ApiError(status.value(), error, message, path, requestId);
        return Mono.just(ResponseEntity.status(status).body(apiError));
    }
}
