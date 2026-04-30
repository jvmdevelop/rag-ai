package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvmd.digitalurpaq_ai_agent.model.ChatSession;
import com.jvmd.digitalurpaq_ai_agent.model.SessionMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SessionStore {

    private static final String ANONYMOUS = "anonymous";

    private final ObjectMapper objectMapper;
    private final Path sessionsDir;
    private final int maxMessages;
    private final long ttlHours;

    private final Map<String, ChatSession> sessionCache = new ConcurrentHashMap<>();

    public SessionStore(
            ObjectMapper objectMapper,
            @Value("${app.storage.base-dir:./data}") String baseDir,
            @Value("${app.session.max-messages:20}") int maxMessages,
            @Value("${app.session.ttl-hours:24}") long ttlHours) {
        this.objectMapper = objectMapper;
        this.sessionsDir = Path.of(baseDir, "sessions");
        this.maxMessages = maxMessages;
        this.ttlHours = ttlHours;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(sessionsDir);
            loadAllSessions();
            log.info("SessionStore initialized at: {}", sessionsDir.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to initialize SessionStore", e);
        }
    }


    public void addMessage(String sessionId, String role, String content) {
        String sid = resolveSessionId(sessionId);
        ChatSession existing = sessionCache.get(sid);

        List<SessionMessage> messages = existing != null
                ? new ArrayList<>(existing.messages())
                : new ArrayList<>();

        messages.add(new SessionMessage(role, content, Instant.now()));

        if (messages.size() > maxMessages) {
            messages = messages.subList(messages.size() - maxMessages, messages.size());
        }

        Instant now = Instant.now();
        ChatSession updated = new ChatSession(
                sid, messages, existing != null ? existing.createdAt() : now, now, messages.size());

        sessionCache.put(sid, updated);
        persistSession(updated);
    }

    public Optional<ChatSession> getSession(String sessionId) {
        String sid = resolveSessionId(sessionId);
        ChatSession session = sessionCache.get(sid);
        if (session == null || session.isExpired(ttlHours)) {
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public List<ChatSession> getAllActiveSessions() {
        return sessionCache.values().stream()
                .filter(s -> !s.isExpired(ttlHours))
                .sorted(Comparator.comparing(ChatSession::lastAccessedAt).reversed())
                .collect(Collectors.toList());
    }

    public void deleteSession(String sessionId) {
        sessionCache.remove(sessionId);
        try {
            Files.deleteIfExists(sessionFile(sessionId));
            log.info("Session deleted: {}", sessionId);
        } catch (IOException e) {
            log.warn("Could not delete session file: {}", sessionId);
        }
    }

    public Map<String, Object> getStats() {
        List<ChatSession> active = getAllActiveSessions();
        long totalMessages = active.stream().mapToLong(ChatSession::messageCount).sum();
        return Map.of(
                "activeSessions", active.size(),
                "totalSessions", sessionCache.size(),
                "totalMessages", totalMessages
        );
    }

    public ChatMemoryStore toChatMemoryStore() {
        return new ChatMemoryStore() {
            @Override
            public List<ChatMessage> getMessages(Object memoryId) {
                String sid = memoryId.toString();
                return getSession(sid)
                        .map(session -> session.messages().stream()
                                .map(SessionStore.this::toLC4jMessage)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()))
                        .orElse(List.of());
            }

            @Override
            public void updateMessages(Object memoryId, List<ChatMessage> lc4jMessages) {
                String sid = memoryId.toString();
                List<SessionMessage> msgs = lc4jMessages.stream()
                        .map(SessionStore.this::fromLC4jMessage)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                Instant now = Instant.now();
                ChatSession existing = sessionCache.get(sid);
                ChatSession updated = new ChatSession(
                        sid, msgs, existing != null ? existing.createdAt() : now, now, msgs.size());
                sessionCache.put(sid, updated);
                persistSession(updated);
            }

            @Override
            public void deleteMessages(Object memoryId) {
                deleteSession(memoryId.toString());
            }
        };
    }


    @Scheduled(fixedDelay = 3600000)
    public void cleanupExpiredSessions() {
        List<String> expired = sessionCache.entrySet().stream()
                .filter(e -> e.getValue().isExpired(ttlHours))
                .map(Map.Entry::getKey)
                .toList();

        for (String sid : expired) {
            sessionCache.remove(sid);
            try { Files.deleteIfExists(sessionFile(sid)); } catch (IOException ignored) {}
        }

        if (!expired.isEmpty()) {
            log.info("Cleaned up {} expired sessions", expired.size());
        }
    }

    private void loadAllSessions() {
        try (var files = Files.list(sessionsDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            ChatSession session = objectMapper.readValue(p.toFile(), ChatSession.class);
                            if (!session.isExpired(ttlHours)) {
                                sessionCache.put(session.sessionId(), session);
                            }
                        } catch (IOException ignored) {}
                    });
            log.info("Loaded {} active sessions from disk", sessionCache.size());
        } catch (IOException e) {
            log.warn("Could not load sessions: {}", e.getMessage());
        }
    }

    private void persistSession(ChatSession session) {
        try {
            objectMapper.writeValue(sessionFile(session.sessionId()).toFile(), session);
        } catch (IOException e) {
            log.error("Failed to persist session {}: {}", session.sessionId(), e.getMessage());
        }
    }

    private Path sessionFile(String sessionId) {
        String safe = sessionId.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return sessionsDir.resolve(safe + ".json");
    }

    private String resolveSessionId(String sessionId) {
        return (sessionId == null || sessionId.isBlank()) ? ANONYMOUS : sessionId;
    }

    private ChatMessage toLC4jMessage(SessionMessage msg) {
        return switch (msg.role()) {
            case "user" -> UserMessage.from(msg.content());
            case "assistant" -> AiMessage.from(msg.content());
            case "system" -> SystemMessage.from(msg.content());
            default -> null;
        };
    }

    private SessionMessage fromLC4jMessage(ChatMessage msg) {
        String role = switch (msg.type()) {
            case USER -> "user";
            case AI -> "assistant";
            case SYSTEM -> "system";
            default -> null;
        };
        if (role == null) return null;
        String content = msg.type() == ChatMessageType.AI
                ? ((AiMessage) msg).text()
                : msg.type() == ChatMessageType.USER
                ? ((UserMessage) msg).singleText()
                : ((SystemMessage) msg).text();
        return new SessionMessage(role, content, Instant.now());
    }
}
