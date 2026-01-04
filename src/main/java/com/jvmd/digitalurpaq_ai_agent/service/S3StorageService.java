package com.jvmd.digitalurpaq_ai_agent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Instant;
import java.util.UUID;

@Service
public class S3StorageService {
    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public S3StorageService(S3Client s3, S3Presigner presigner, @Value("${app.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
    }

    public Mono<String> upload(String name, FilePart file) {
        String key = "documents/" + name + "/" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + "-" + file.filename();
        return DataBufferUtils.join(file.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .map(bytes -> {
                    PutObjectRequest req = PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.headers().getContentType() != null ? file.headers().getContentType().toString() : "application/octet-stream")
                            .build();
                    s3.putObject(req, RequestBody.fromBytes(bytes));
                    return key;
                });
    }

    public Flux<S3Object> listAllObjects() {
        S3Client s3 = S3Client.builder().build();

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .build();

        return Flux.fromIterable(s3.listObjectsV2Paginator(request))
                .flatMap(response -> Flux.fromIterable(response.contents()));
    }

    public Mono<byte[]> downloadObject(String key) {
        return Mono.fromCallable(() -> {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            return s3.getObject(getObjectRequest).readAllBytes();
        }).subscribeOn(Schedulers.boundedElastic());
    }
}