package com.jvmd.digitalurpaq_ai_agent.service;

import lombok.AllArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class UploadService {

    private final S3StorageService s3StorageService;

    public Mono<Void> uploadTxt(String name, FilePart multipartFile) {
        return Mono.fromRunnable(()->{
            s3StorageService.upload("txt", multipartFile);
        });
    }
}
