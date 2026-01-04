package com.jvmd.digitalurpaq_ai_agent.service;

import com.jvmd.digitalurpaq_ai_agent.model.RetrievalDocument;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.DocumentChunk;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.CacheService;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.DocumentChunker;
import com.jvmd.digitalurpaq_ai_agent.repo.RetrievalDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
public class RetrievalService {

    private final RetrievalDocumentRepository repo;
    private final DocumentChunker documentChunker;
    private final CacheService cacheService;

    public RetrievalService(RetrievalDocumentRepository repo,
                           DocumentChunker documentChunker,
                           CacheService cacheService) {
        this.repo = repo;
        this.documentChunker = documentChunker;
        this.cacheService = cacheService;
    }

    public Mono<RetrievalDocument> save(RetrievalDocument document) {
        log.info("Saving document: {}", document.getName());
        return repo.save(document)
                .doOnSuccess(saved -> {
                    log.info("Document saved successfully: {}", saved.getId());
                    cacheService.invalidateSearchCache();
                })
                .doOnError(error -> log.error("Error saving document: {}", error.getMessage()));
    }

    public Flux<RetrievalDocument> saveWithChunking(RetrievalDocument document) {
        log.info("Saving document with chunking: {}", document.getName());
        
        List<DocumentChunk> chunks = documentChunker.chunkDocument(document);
        
        if (chunks.isEmpty()) {
            log.warn("No chunks created for document: {}", document.getName());
            return save(document).flux();
        }
        
        log.info("Created {} chunks for document: {}", chunks.size(), document.getName());
        
        return Flux.fromIterable(chunks)
                .map(DocumentChunk::toRetrievalDocument)
                .flatMap(this::save)
                .doOnComplete(() -> cacheService.invalidateSearchCache());
    }

    public Flux<RetrievalDocument> saveAll(List<RetrievalDocument> documents) {
        log.info("Saving {} documents", documents.size());
        return Flux.fromIterable(documents)
                .flatMap(this::save)
                .doOnComplete(() -> {
                    log.info("All documents saved successfully");
                    cacheService.invalidateSearchCache();
                });
    }

    public Flux<RetrievalDocument> saveAllWithChunking(List<RetrievalDocument> documents) {
        log.info("Saving {} documents with chunking", documents.size());
        return Flux.fromIterable(documents)
                .flatMap(this::saveWithChunking)
                .doOnComplete(() -> {
                    log.info("All documents with chunks saved successfully");
                    cacheService.invalidateSearchCache();
                });
    }

    public Mono<RetrievalDocument> findById(String id) {
        return repo.findById(id)
                .doOnSuccess(doc -> log.debug("Found document: {}", id))
                .doOnError(error -> log.error("Error finding document {}: {}", id, error.getMessage()));
    }

    public Flux<RetrievalDocument> findAll() {
        return repo.findAll()
                .doOnComplete(() -> log.debug("Retrieved all documents"));
    }

    public Mono<Void> delete(String id) {
        log.info("Deleting document: {}", id);
        return repo.deleteById(id)
                .doOnSuccess(v -> {
                    log.info("Document deleted: {}", id);
                    cacheService.invalidateSearchCache();
                })
                .doOnError(error -> log.error("Error deleting document {}: {}", id, error.getMessage()));
    }

    public Mono<Void> deleteAll() {
        log.warn("Deleting all documents");
        return repo.deleteAll()
                .doOnSuccess(v -> {
                    log.info("All documents deleted");
                    cacheService.invalidateAll();
                })
                .doOnError(error -> log.error("Error deleting all documents: {}", error.getMessage()));
    }

    public Mono<Long> count() {
        return repo.count()
                .doOnSuccess(count -> log.debug("Total documents: {}", count));
    }

    public Mono<Boolean> exists(String id) {
        return repo.existsById(id);
    }
}
