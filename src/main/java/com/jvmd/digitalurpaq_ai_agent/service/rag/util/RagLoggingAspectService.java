package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Service
@RequiredArgsConstructor
public class RagLoggingAspectService {

    private final MeterRegistry meterRegistry;

    @Pointcut("within(com.jvmd.digitalurpaq_ai_agent.service..*) " +
              "|| within(com.jvmd.digitalurpaq_ai_agent.storage..*) " +
              "|| within(com.jvmd.digitalurpaq_ai_agent.session..*)")
    public void applicationLayer() {}

    @Around("applicationLayer()")
    public Object logAndMeasure(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String className  = sig.getDeclaringType().getSimpleName();
        String methodName = sig.getName();
        String fullMethod = className + "." + methodName;

        log.debug("[AOP] → {}", fullMethod);
        long start = System.currentTimeMillis();

        Object result = pjp.proceed();

        if (result instanceof Mono<?> mono) {
            return mono
                    .doOnSuccess(v -> record(className, methodName, fullMethod, start, "ok"))
                    .doOnError(t -> {
                        record(className, methodName, fullMethod, start, "error");
                        log.error("[AOP] ✗ {} — {}: {}", fullMethod, t.getClass().getSimpleName(), t.getMessage());
                    });
        }

        if (result instanceof Flux<?> flux) {
            return flux
                    .doOnComplete(() -> record(className, methodName, fullMethod, start, "ok"))
                    .doOnError(t -> {
                        record(className, methodName, fullMethod, start, "error");
                        log.error("[AOP] ✗ {} — {}: {}", fullMethod, t.getClass().getSimpleName(), t.getMessage());
                    });
        }

        long elapsed = System.currentTimeMillis() - start;
        record(className, methodName, elapsed, "ok");
        log.debug("[AOP] ← {} ({}ms)", fullMethod, elapsed);
        return result;
    }

    private void record(String className, String methodName, String fullMethod, long start, String status) {
        long elapsed = System.currentTimeMillis() - start;
        record(className, methodName, elapsed, status);
        log.debug("[AOP] ← {} ({}ms) [{}]", fullMethod, elapsed, status);
    }

    private void record(String className, String methodName, long elapsedMs, String status) {
        Timer.builder("method.duration")
                .tag("class",  className)
                .tag("method", methodName)
                .tag("status", status)
                .register(meterRegistry)
                .record(elapsedMs, TimeUnit.MILLISECONDS);
    }
}
