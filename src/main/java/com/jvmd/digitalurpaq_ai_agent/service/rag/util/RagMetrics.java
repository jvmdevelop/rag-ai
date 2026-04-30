package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import com.jvmd.digitalurpaq_ai_agent.service.rag.model.MetricsSnapshot;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ValidationIssue;
import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RAG pipeline metrics using Micrometer MeterRegistry.
 *
 * Metrics exposed via /actuator/metrics and /actuator/prometheus:
 *   rag.requests{status=success|failure}  — total request counts
 *   rag.duration                          — response time distribution
 *   rag.retries.total                     — retry count
 *   rag.cache.hits / rag.cache.misses     — cache effectiveness
 *   rag.validation.failures{issue=...}    — validation issues by type
 *
 * Senior rule: never write custom AtomicInteger counters when Micrometer exists.
 */
@Slf4j
@Component
public class RagMetrics {

    private final MeterRegistry registry;

    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter retryCounter;
    private final Timer   responseTimer;

    // Kept for snapshot API backward compatibility
    private final ConcurrentHashMap<ValidationIssue, AtomicInteger> validationIssues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> errorTypes = new ConcurrentHashMap<>();
    private final List<com.jvmd.digitalurpaq_ai_agent.service.rag.model.RequestMetric> recentRequests = new ArrayList<>();
    private static final int MAX_RECENT = 100;

    public RagMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.successCounter = Counter.builder("rag.requests")
                .tag("status", "success")
                .description("Successful RAG pipeline executions")
                .register(registry);

        this.failureCounter = Counter.builder("rag.requests")
                .tag("status", "failure")
                .description("Failed RAG pipeline executions")
                .register(registry);

        this.retryCounter = Counter.builder("rag.retries")
                .description("Total RAG pipeline retries")
                .register(registry);

        this.responseTimer = Timer.builder("rag.duration")
                .description("RAG pipeline response time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // Gauge for active recent requests buffer
        Gauge.builder("rag.recent.requests.size", recentRequests, List::size)
                .description("Number of recent request records kept in memory")
                .register(registry);
    }

    public void recordSuccess(long responseTimeMs) {
        successCounter.increment();
        responseTimer.record(responseTimeMs, TimeUnit.MILLISECONDS);
        addRecent(responseTimeMs, true, null);
        log.debug("[Metrics] success recorded, {}ms", responseTimeMs);
    }

    public void recordFailure(Throwable error) {
        failureCounter.increment();
        String errorType = error.getClass().getSimpleName();
        errorTypes.computeIfAbsent(errorType, k -> new AtomicInteger()).incrementAndGet();

        // Tag-based counter per error type
        Counter.builder("rag.errors")
                .tag("type", errorType)
                .register(registry)
                .increment();

        addRecent(0, false, errorType);
        log.debug("[Metrics] failure recorded: {}", errorType);
    }

    public void recordRetry() {
        retryCounter.increment();
    }

    public void recordValidationFailure(ValidationIssue issue) {
        validationIssues.computeIfAbsent(issue, k -> new AtomicInteger()).incrementAndGet();
        Counter.builder("rag.validation.failures")
                .tag("issue", issue.name())
                .register(registry)
                .increment();
    }

    /** Cache hit tracking */
    public void recordCacheHit(String namespace) {
        Counter.builder("rag.cache.hits")
                .tag("namespace", namespace)
                .register(registry)
                .increment();
    }

    public void recordCacheMiss(String namespace) {
        Counter.builder("rag.cache.misses")
                .tag("namespace", namespace)
                .register(registry)
                .increment();
    }

    public MetricsSnapshot getSnapshot() {
        long total = (long) successCounter.count() + (long) failureCounter.count();
        long successful = (long) successCounter.count();
        long failed = (long) failureCounter.count();
        double successRate = total > 0 ? (double) successful / total * 100 : 0;
        double avgMs = responseTimer.count() > 0
                ? responseTimer.totalTime(TimeUnit.MILLISECONDS) / responseTimer.count()
                : 0;

        return new MetricsSnapshot(
                (int) total,
                (int) successful,
                (int) failed,
                (int) retryCounter.count(),
                successRate,
                avgMs,
                new ConcurrentHashMap<>(validationIssues),
                new ConcurrentHashMap<>(errorTypes),
                new ArrayList<>(recentRequests)
        );
    }

    public void reset() {
        // Micrometer counters are not resettable by design (monotonic).
        // We reset only the supplemental tracking structures.
        validationIssues.clear();
        errorTypes.clear();
        recentRequests.clear();
        log.info("[Metrics] supplemental metrics reset (Micrometer counters are monotonic)");
    }

    private synchronized void addRecent(long ms, boolean success, String error) {
        recentRequests.add(new com.jvmd.digitalurpaq_ai_agent.service.rag.model.RequestMetric(
                Instant.now(), ms, success, error));
        if (recentRequests.size() > MAX_RECENT) recentRequests.remove(0);
    }
}
