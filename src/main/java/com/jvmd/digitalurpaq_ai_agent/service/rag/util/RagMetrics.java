package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import com.jvmd.digitalurpaq_ai_agent.service.rag.model.MetricsSnapshot;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.RequestMetric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class RagMetrics {

    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final AtomicInteger retries = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    
    private final ConcurrentHashMap<ResponseValidator.ValidationIssue, AtomicInteger> validationIssues = 
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> errorTypes = new ConcurrentHashMap<>();
    private final List<RequestMetric> recentRequests = new ArrayList<>();
    
    private static final int MAX_RECENT_REQUESTS = 100;

    public void recordSuccess(long responseTimeMs) {
        totalRequests.incrementAndGet();
        successfulRequests.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);
        
        addRecentRequest(new RequestMetric(
                Instant.now(),
                responseTimeMs,
                true,
                null
        ));
        
        log.debug("Recorded successful request, response time: {}ms", responseTimeMs);
    }

    public void recordFailure(Throwable error) {
        totalRequests.incrementAndGet();
        failedRequests.incrementAndGet();
        
        String errorType = error.getClass().getSimpleName();
        errorTypes.computeIfAbsent(errorType, k -> new AtomicInteger(0)).incrementAndGet();
        
        addRecentRequest(new RequestMetric(
                Instant.now(),
                0,
                false,
                errorType
        ));
        
        log.debug("Recorded failed request, error: {}", errorType);
    }

    public void recordRetry() {
        retries.incrementAndGet();
        log.debug("Recorded retry, total retries: {}", retries.get());
    }

    public void recordValidationFailure(ResponseValidator.ValidationIssue issue) {
        validationIssues.computeIfAbsent(issue, k -> new AtomicInteger(0)).incrementAndGet();
        log.debug("Recorded validation issue: {}", issue);
    }

    public MetricsSnapshot getSnapshot() {
        int total = totalRequests.get();
        int successful = successfulRequests.get();
        int failed = failedRequests.get();
        long totalTime = totalResponseTime.get();
        
        double successRate = total > 0 ? (double) successful / total * 100 : 0;
        double avgResponseTime = successful > 0 ? (double) totalTime / successful : 0;
        
        return new MetricsSnapshot(
                total,
                successful,
                failed,
                retries.get(),
                successRate,
                avgResponseTime,
                new ConcurrentHashMap<>(validationIssues),
                new ConcurrentHashMap<>(errorTypes),
                new ArrayList<>(recentRequests)
        );
    }

    public void reset() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        retries.set(0);
        totalResponseTime.set(0);
        validationIssues.clear();
        errorTypes.clear();
        recentRequests.clear();
        
        log.info("Metrics reset");
    }

    private synchronized void addRecentRequest(RequestMetric metric) {
        recentRequests.add(metric);

        if (recentRequests.size() > MAX_RECENT_REQUESTS) {
            recentRequests.remove(0);
        }
    }


}
