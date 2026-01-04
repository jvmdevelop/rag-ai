package com.jvmd.digitalurpaq_ai_agent.service;

import com.jvmd.digitalurpaq_ai_agent.model.RetrievalDocument;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

@AllArgsConstructor
@Service
public class LoaderService {

    private final S3StorageService s3StorageService;
    private final RetrievalService retrievalService;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize(ApplicationReadyEvent event) {
        initializeData().subscribe(
                doc -> System.out.println("Loaded: " + doc.getId()),
                err -> System.err.println("Error loading data: " + err.getMessage())
        );
    }

    private Flux<RetrievalDocument> initializeData() {
        return s3StorageService.listAllObjects()
                .flatMap(s3Object ->
                        s3StorageService.downloadObject(s3Object.key())
                                .flatMap(bytes -> retrievalService.save(new RetrievalDocument(UUID.randomUUID().toString(), UUID.randomUUID().toString(), new String(bytes))))
                );
    }
}