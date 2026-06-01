package com.jvmd.digitalurpaq_ai_agent.config.filter;

import com.jvmd.digitalurpaq_ai_agent.config.properties.AppProperties;
import com.jvmd.digitalurpaq_ai_agent.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter implements WebFilter {

    private final int maxRequests;
    private final long windowSeconds;
    private final Map<String, Queue<Instant>> requestLog = new ConcurrentHashMap<>();

    public RateLimitingFilter(AppProperties appProperties) {
        this.maxRequests = appProperties.rateLimit().requestsPerMinute();
        this.windowSeconds = appProperties.rateLimit().windowSeconds();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith("/api/chat")) {
            return chain.filter(exchange);
        }

        String clientIp = getClientIp(exchange.getRequest());
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(windowSeconds);

        Queue<Instant> timestamps = requestLog.computeIfAbsent(clientIp, k -> new ConcurrentLinkedQueue<>());
        timestamps.removeIf(ts -> ts.isBefore(windowStart));

        int remaining = maxRequests - timestamps.size();
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(Math.max(0, remaining - 1)));
        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(maxRequests));

        if (timestamps.size() >= maxRequests) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            return Mono.error(new RateLimitExceededException(
                    "Превышен лимит запросов: максимум " + maxRequests + " в " + windowSeconds + " секунд"));
        }

        timestamps.add(now);
        return chain.filter(exchange);
    }

    private String getClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }
}
