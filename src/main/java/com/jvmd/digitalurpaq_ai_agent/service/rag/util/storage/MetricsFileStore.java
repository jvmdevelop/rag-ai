package com.jvmd.digitalurpaq_ai_agent.service.rag.util.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.MetricsSnapshot;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.RagMetrics;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class MetricsFileStore {

    private final ObjectMapper objectMapper;
    private final RagMetrics ragMetrics;
    private final Path metricsDir;
    private final Path historyDir;

    public MetricsFileStore(
            ObjectMapper objectMapper,
            RagMetrics ragMetrics,
            @Value("${app.storage.base-dir:./data}") String baseDir) {
        this.objectMapper = objectMapper;
        this.ragMetrics = ragMetrics;
        this.metricsDir = Path.of(baseDir, "metrics");
        this.historyDir = metricsDir.resolve("history");
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(historyDir);
            log.info("MetricsFileStore initialized at: {}", metricsDir.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to initialize MetricsFileStore", e);
        }
    }

    @Scheduled(fixedDelay = 300000)
    public void persistCurrentMetrics() {
        try {
            MetricsSnapshot snapshot = ragMetrics.getSnapshot();
            Map<String, Object> data = buildMetricsMap(snapshot);
            data.put("savedAt", java.time.Instant.now().toString());
            objectMapper.writeValue(metricsDir.resolve("current.json").toFile(), data);
        } catch (IOException e) {
            log.warn("[MetricsFileStore] Failed to persist current metrics: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void persistDailySnapshot() {
        try {
            String date = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            MetricsSnapshot snapshot = ragMetrics.getSnapshot();
            Map<String, Object> data = buildMetricsMap(snapshot);
            data.put("date", date);
            data.put("savedAt", java.time.Instant.now().toString());

            Path dailyFile = historyDir.resolve(date + ".json");
            objectMapper.writeValue(dailyFile.toFile(), data);
            log.info("[MetricsFileStore] Daily snapshot saved: {}", date);
        } catch (IOException e) {
            log.error("[MetricsFileStore] Failed to save daily snapshot: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> getHistory(int days) {
        List<Map<String, Object>> history = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= days; i++) {
            String date = today.minusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            Path file = historyDir.resolve(date + ".json");
            if (Files.exists(file)) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = objectMapper.readValue(file.toFile(), Map.class);
                    history.add(data);
                } catch (IOException ignored) {}
            }
        }
        return history;
    }

    private Map<String, Object> buildMetricsMap(MetricsSnapshot snapshot) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalRequests", snapshot.totalRequests());
        data.put("successfulRequests", snapshot.successfulRequests());
        data.put("failedRequests", snapshot.failedRequests());
        data.put("successRate", snapshot.successRate());
        data.put("avgResponseTimeMs", snapshot.avgResponseTimeMs());
        data.put("totalRetries", snapshot.totalRetries());
        return data;
    }
}
