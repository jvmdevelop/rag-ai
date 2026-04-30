package com.jvmd.digitalurpaq_ai_agent.health;

import com.jvmd.digitalurpaq_ai_agent.config.ContextStoreConfig;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.storage.FileContextStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

@Slf4j
@Component("fileStore")
@RequiredArgsConstructor
public class FileStoreHealthIndicator implements HealthIndicator {

    private final FileContextStore fileContextStore;
    private final ContextStoreConfig config;

    private static final long MIN_FREE_SPACE_BYTES = 100L * 1024 * 1024;

    @Override
    public Health health() {
        try {
            Path dataDir = Path.of(config.getBaseDir());

            if (!Files.exists(dataDir)) {
                return Health.down()
                        .withDetail("error", "Data directory does not exist: " + dataDir.toAbsolutePath())
                        .build();
            }

            if (!Files.isWritable(dataDir)) {
                return Health.down()
                        .withDetail("error", "Data directory is not writable")
                        .withDetail("path", dataDir.toAbsolutePath().toString())
                        .build();
            }

            FileStore fs = Files.getFileStore(dataDir);
            long freeBytes = fs.getUsableSpace();

            Map<String, Object> storeStats = fileContextStore.getStats();

            Health.Builder builder = freeBytes < MIN_FREE_SPACE_BYTES
                    ? Health.outOfService().withDetail("warning", "Low disk space")
                    : Health.up();

            return builder
                    .withDetail("path", dataDir.toAbsolutePath().toString())
                    .withDetail("freeDiskMB", freeBytes / (1024 * 1024))
                    .withDetail("cacheNamespaces", storeStats)
                    .build();

        } catch (IOException e) {
            log.warn("[Health] FileStore health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
