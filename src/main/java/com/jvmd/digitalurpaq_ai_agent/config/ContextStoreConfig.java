package com.jvmd.digitalurpaq_ai_agent.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.storage")
public class ContextStoreConfig {

    @NotBlank(message = "app.storage.base-dir must not be blank")
    private String baseDir = "./data";

    @Min(value = 1, message = "search-ttl-minutes must be at least 1")
    @Max(value = 1440, message = "search-ttl-minutes must be at most 1440 (24h)")
    private int searchTtlMinutes = 30;

    @Min(value = 1, message = "query-ttl-minutes must be at least 1")
    @Max(value = 1440, message = "query-ttl-minutes must be at most 1440 (24h)")
    private int queryTtlMinutes = 60;

    @Min(value = 1, message = "response-ttl-minutes must be at least 1")
    @Max(value = 2880, message = "response-ttl-minutes must be at most 2880 (48h)")
    private int responseTtlMinutes = 120;

    @Min(value = 100, message = "max-entries-per-namespace must be at least 100")
    private int maxEntriesPerNamespace = 500;

    private long evictionIntervalMs = 900_000L; // 15 minutes
}
