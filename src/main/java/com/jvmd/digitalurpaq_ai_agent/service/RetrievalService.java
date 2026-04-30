package com.jvmd.digitalurpaq_ai_agent.service;

import com.jvmd.digitalurpaq_ai_agent.model.event.CacheInvalidatedEvent;
import com.jvmd.digitalurpaq_ai_agent.model.event.DocumentIndexedEvent;
import com.jvmd.digitalurpaq_ai_agent.model.RetrievalDocument;
import com.jvmd.digitalurpaq_ai_agent.repo.RetrievalDocumentRepository;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.DocumentChunk;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.CacheService;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.DocumentChunker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Retrieval service with Spring Events integration.
 *
 * Publishes domain events on document lifecycle changes:
 *  - DocumentIndexedEvent  — after successful save
 *  - CacheInvalidatedEvent — after cache clear
 *
 * Senior rule: use ApplicationEventPublisher for decoupled side effects.
 * RetrievalService doesn't know (or care) who handles these events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final RetrievalDocumentRepository repo;
    private final DocumentChunker documentChunker;
    private final CacheService cacheService;
    private final ApplicationEventPublisher eventPublisher;  // Spring built-in

    public Mono<RetrievalDocument> save(RetrievalDocument document) {
        log.info("Saving document: {}", document.getName());
        return repo.save(document)
                .doOnSuccess(saved -> {
                    log.info("Document saved: {}", saved.getId());
                    cacheService.invalidateSearchCache();
                    // Publish event — decoupled from who handles it
                    eventPublisher.publishEvent(new DocumentIndexedEvent(
                            saved.getId(), saved.getName(), 1, Instant.now()));
                })
                .doOnError(e -> log.error("Save error: {}", e.getMessage()));
    }

    public Flux<RetrievalDocument> saveWithChunking(RetrievalDocument document) {
        log.info("Saving with chunking: {}", document.getName());
        List<DocumentChunk> chunks = documentChunker.chunkDocument(document);

        if (chunks.isEmpty()) {
            return save(document).flux();
        }

        int chunkCount = chunks.size();
        log.info("Created {} chunks for: {}", chunkCount, document.getName());

        return Flux.fromIterable(chunks)
                .map(DocumentChunk::toRetrievalDocument)
                .flatMap(this::save)
                .doOnComplete(() -> {
                    eventPublisher.publishEvent(new DocumentIndexedEvent(
                            document.getId(), document.getName(), chunkCount, Instant.now()));
                    eventPublisher.publishEvent(new CacheInvalidatedEvent("search", "document-indexed"));
                });
    }

    public Flux<RetrievalDocument> saveAll(List<RetrievalDocument> documents) {
        return Flux.fromIterable(documents).flatMap(this::save);
    }

    public Flux<RetrievalDocument> saveAllWithChunking(List<RetrievalDocument> documents) {
        return Flux.fromIterable(documents).flatMap(this::saveWithChunking);
    }

    public Mono<RetrievalDocument> findById(String id) {
        return repo.findById(id);
    }

    public Flux<RetrievalDocument> findAll() {
        return repo.findAll();
    }

    public Mono<Void> delete(String id) {
        return repo.deleteById(id)
                .doOnSuccess(v -> {
                    cacheService.invalidateSearchCache();
                    eventPublisher.publishEvent(new CacheInvalidatedEvent("search", "document-deleted"));
                });
    }

    public Mono<Void> deleteAll() {
        return repo.deleteAll()
                .doOnSuccess(v -> {
                    cacheService.invalidateAll();
                    eventPublisher.publishEvent(new CacheInvalidatedEvent("all", "all-documents-deleted"));
                });
    }

    public Mono<Long> count() {
        return repo.count();
    }

    public Mono<Boolean> exists(String id) {
        return repo.existsById(id);
    }
}
