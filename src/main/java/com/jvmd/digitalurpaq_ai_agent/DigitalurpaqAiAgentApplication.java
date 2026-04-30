package com.jvmd.digitalurpaq_ai_agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableWebFlux
@EnableScheduling
@EnableCaching          // Activates @Cacheable, @CacheEvict, @CachePut
@EnableRetry            // Activates @Retryable, @Recover
@EnableConfigurationProperties
public class DigitalurpaqAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalurpaqAiAgentApplication.class, args);
    }
}
