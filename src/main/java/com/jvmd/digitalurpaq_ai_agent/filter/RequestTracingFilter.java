package com.jvmd.digitalurpaq_ai_agent.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTracingFilter implements WebFilter {

    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String TRACE_KEY    = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String traceId = request.getHeaders().containsKey(TRACE_HEADER)
                ? request.getHeaders().getFirst(TRACE_HEADER)
                : UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        long startMs = System.currentTimeMillis();

        exchange.getResponse().getHeaders().add(TRACE_HEADER, traceId);

        log.debug("[{}] → {} {}", traceId, request.getMethod(), request.getPath());

        return chain.filter(exchange)
                .doFinally(signal -> {
                    long elapsed = System.currentTimeMillis() - startMs;
                    int status = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : 0;
                    log.debug("[{}] ← {} {} {}ms status={}",
                            traceId, request.getMethod(), request.getPath(), elapsed, status);
                })
                .contextWrite(Context.of(TRACE_KEY, traceId));
    }
}
