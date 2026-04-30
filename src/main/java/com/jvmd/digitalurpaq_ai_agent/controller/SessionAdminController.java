package com.jvmd.digitalurpaq_ai_agent.controller;

import com.jvmd.digitalurpaq_ai_agent.model.ChatSession;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.SessionStore;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/sessions")
@AllArgsConstructor
public class SessionAdminController {

    private final SessionStore sessionStore;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> listSessions() {
        List<ChatSession> sessions = sessionStore.getAllActiveSessions();
        return Mono.just(Map.of(
                "sessions", sessions.stream().map(s -> Map.of(
                        "sessionId", s.sessionId(),
                        "messageCount", s.messageCount(),
                        "createdAt", s.createdAt().toString(),
                        "lastAccessedAt", s.lastAccessedAt().toString()
                )).toList(),
                "stats", sessionStore.getStats()
        ));
    }

    @GetMapping(value = "/{sessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Object> getSession(@PathVariable String sessionId) {
        return Mono.justOrEmpty(sessionStore.getSession(sessionId))
                .<Object>map(s -> s)
                .switchIfEmpty(Mono.just(Map.of("error", "Session not found: " + sessionId)));
    }

    @DeleteMapping("/{sessionId}")
    public Mono<Map<String, String>> deleteSession(@PathVariable String sessionId) {
        sessionStore.deleteSession(sessionId);
        return Mono.just(Map.of("status", "deleted", "sessionId", sessionId));
    }
}
