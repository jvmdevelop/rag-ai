package com.jvmd.digitalurpaq_ai_agent.service.rag.util.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvmd.digitalurpaq_ai_agent.config.ContextStoreConfig;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ContextEntry;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ContextIndexEntry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FileContextStore {

    public enum Namespace {
        SEARCH("search"),
        QUERY("query"),
        RESPONSE("response");

        public final String dir;
        Namespace(String dir) { this.dir = dir; }
    }

    private final ObjectMapper objectMapper;
    private final ContextStoreConfig config;
    private final Path baseContextDir;

    private final Map<Namespace, Map<String, ContextIndexEntry>> indexes = new EnumMap<>(Namespace.class);
    private final Map<Namespace, ReadWriteLock> locks = new EnumMap<>(Namespace.class);

    public FileContextStore(ObjectMapper objectMapper, ContextStoreConfig config) {
        this.objectMapper = objectMapper;
        this.config = config;
        this.baseContextDir = Path.of(config.getBaseDir(), "context");

        for (Namespace ns : Namespace.values()) {
            indexes.put(ns, new ConcurrentHashMap<>());
            locks.put(ns, new ReentrantReadWriteLock());
        }
    }

    @PostConstruct
    public void init() {
        try {
            for (Namespace ns : Namespace.values()) {
                Path nsDir = namespaceDir(ns);
                Files.createDirectories(nsDir);
                loadIndex(ns);
            }
            log.info("FileContextStore initialized at: {}", baseContextDir.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to initialize FileContextStore", e);
        }
    }

    public <T> Optional<T> get(Namespace ns, String key, TypeReference<ContextEntry<T>> typeRef) {
        ReadWriteLock lock = locks.get(ns);
        lock.readLock().lock();
        try {
            ContextIndexEntry idxEntry = indexes.get(ns).get(key);
            if (idxEntry == null || idxEntry.isExpired()) {
                if (idxEntry != null) evict(ns, key);
                return Optional.empty();
            }

            Path entryFile = namespaceDir(ns).resolve(idxEntry.fileName());
            if (!Files.exists(entryFile)) {
                indexes.get(ns).remove(key);
                return Optional.empty();
            }

            ContextEntry<T> entry = objectMapper.readValue(entryFile.toFile(), typeRef);
            if (entry.isExpired()) {
                evict(ns, key);
                return Optional.empty();
            }

            updateAccessAsync(ns, key, entry, idxEntry);

            log.debug("[FileContextStore] HIT ns={} key={}", ns.dir, key);
            return Optional.of(entry.value());
        } catch (IOException e) {
            log.warn("[FileContextStore] Read error ns={} key={}: {}", ns.dir, key, e.getMessage());
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }


    public <T> void put(Namespace ns, String key, T value, Duration ttl) {
        ReadWriteLock lock = locks.get(ns);
        lock.writeLock().lock();
        try {
            evictIfOverCapacity(ns);

            String fileName = md5(key) + ".json";
            Path entryFile = namespaceDir(ns).resolve(fileName);
            Instant now = Instant.now();
            Instant expiresAt = now.plus(ttl);

            ContextEntry<T> entry = new ContextEntry<>(key, value, now, expiresAt, 0, now);
            objectMapper.writeValue(entryFile.toFile(), entry);

            long sizeBytes = Files.size(entryFile);
            ContextIndexEntry idxEntry = new ContextIndexEntry(key, fileName, expiresAt, sizeBytes);
            indexes.get(ns).put(key, idxEntry);
            persistIndex(ns);

            log.debug("[FileContextStore] PUT ns={} key={} ttl={}min", ns.dir, key, ttl.toMinutes());
        } catch (IOException e) {
            log.error("[FileContextStore] Write error ns={} key={}: {}", ns.dir, key, e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void evict(Namespace ns, String key) {
        ReadWriteLock lock = locks.get(ns);
        lock.writeLock().lock();
        try {
            ContextIndexEntry idxEntry = indexes.get(ns).remove(key);
            if (idxEntry != null) {
                Files.deleteIfExists(namespaceDir(ns).resolve(idxEntry.fileName()));
                persistIndex(ns);
            }
        } catch (IOException e) {
            log.warn("[FileContextStore] Evict error ns={} key={}: {}", ns.dir, key, e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clearNamespace(Namespace ns) {
        ReadWriteLock lock = locks.get(ns);
        lock.writeLock().lock();
        try {
            Path nsDir = namespaceDir(ns);
            try (var files = Files.list(nsDir)) {
                files.filter(p -> p.getFileName().toString().endsWith(".json")
                        && !p.getFileName().toString().equals("index.json"))
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                        });
            }
            indexes.get(ns).clear();
            persistIndex(ns);
            log.info("[FileContextStore] Cleared namespace: {}", ns.dir);
        } catch (IOException e) {
            log.error("[FileContextStore] Clear error ns={}: {}", ns.dir, e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clearAll() {
        for (Namespace ns : Namespace.values()) {
            clearNamespace(ns);
        }
    }


    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        for (Namespace ns : Namespace.values()) {
            Map<String, ContextIndexEntry> idx = indexes.get(ns);
            long valid = idx.values().stream().filter(e -> !e.isExpired()).count();
            long totalSize = idx.values().stream().mapToLong(ContextIndexEntry::sizeBytes).sum();
            stats.put(ns.dir, Map.of(
                    "total", idx.size(),
                    "valid", valid,
                    "expired", idx.size() - valid,
                    "sizeBytes", totalSize
            ));
        }
        return stats;
    }


    @Scheduled(fixedDelayString = "${app.storage.eviction-interval-ms:900000}")
    public void evictExpired() {
        int total = 0;
        for (Namespace ns : Namespace.values()) {
            ReadWriteLock lock = locks.get(ns);
            lock.writeLock().lock();
            try {
                List<String> expired = indexes.get(ns).entrySet().stream()
                        .filter(e -> e.getValue().isExpired())
                        .map(Map.Entry::getKey)
                        .toList();

                for (String key : expired) {
                    ContextIndexEntry idxEntry = indexes.get(ns).remove(key);
                    if (idxEntry != null) {
                        try {
                            Files.deleteIfExists(namespaceDir(ns).resolve(idxEntry.fileName()));
                        } catch (IOException ignored) {}
                    }
                }
                if (!expired.isEmpty()) {
                    persistIndex(ns);
                    total += expired.size();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        if (total > 0) {
            log.info("[FileContextStore] Eviction sweep removed {} expired entries", total);
        }
    }


    private void loadIndex(Namespace ns) {
        Path indexFile = namespaceDir(ns).resolve("index.json");
        if (!Files.exists(indexFile)) return;

        try {
            List<ContextIndexEntry> entries = objectMapper.readValue(indexFile.toFile(),
                    new TypeReference<>() {});
            Map<String, ContextIndexEntry> idx = indexes.get(ns);
            int loaded = 0;
            for (ContextIndexEntry e : entries) {
                if (!e.isExpired()) {
                    idx.put(e.key(), e);
                    loaded++;
                }
            }
            log.info("[FileContextStore] Loaded {} valid entries for namespace '{}'", loaded, ns.dir);
        } catch (IOException e) {
            log.warn("[FileContextStore] Could not load index for '{}': {}", ns.dir, e.getMessage());
        }
    }

    private void persistIndex(Namespace ns) {
        Path indexFile = namespaceDir(ns).resolve("index.json");
        List<ContextIndexEntry> entries = new ArrayList<>(indexes.get(ns).values());
        try {
            objectMapper.writeValue(indexFile.toFile(), entries);
        } catch (IOException e) {
            log.error("[FileContextStore] Failed to persist index for '{}': {}", ns.dir, e.getMessage());
        }
    }

    private void evictIfOverCapacity(Namespace ns) {
        Map<String, ContextIndexEntry> idx = indexes.get(ns);
        if (idx.size() < config.getMaxEntriesPerNamespace()) return;

        idx.entrySet().removeIf(e -> {
            if (e.getValue().isExpired()) {
                try {
                    Files.deleteIfExists(namespaceDir(ns).resolve(e.getValue().fileName()));
                } catch (IOException ignored) {}
                return true;
            }
            return false;
        });

        if (idx.size() >= config.getMaxEntriesPerNamespace()) {
            int toRemove = idx.size() - config.getMaxEntriesPerNamespace() / 2;
            idx.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getValue().expiresAt()))
                    .limit(toRemove)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList())
                    .forEach(key -> {
                        ContextIndexEntry e = idx.remove(key);
                        if (e != null) {
                            try { Files.deleteIfExists(namespaceDir(ns).resolve(e.fileName())); }
                            catch (IOException ignored) {}
                        }
                    });
            log.info("[FileContextStore] Capacity eviction: removed {} entries from '{}'", toRemove, ns.dir);
        }
    }

    private void updateAccessAsync(Namespace ns, String key, ContextEntry<?> entry, ContextIndexEntry idxEntry) {
        Thread.ofVirtual().start(() -> {
            ReadWriteLock lock = locks.get(ns);
            lock.writeLock().lock();
            try {
                ContextEntry<?> updated = entry.withAccess();
                Path entryFile = namespaceDir(ns).resolve(idxEntry.fileName());
                objectMapper.writeValue(entryFile.toFile(), updated);
            } catch (IOException ignored) {
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    private Path namespaceDir(Namespace ns) {
        return baseContextDir.resolve(ns.dir);
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(Math.abs(input.hashCode()));
        }
    }
}
