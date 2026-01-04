package com.jvmd.digitalurpaq_ai_agent.controller;

import com.jvmd.digitalurpaq_ai_agent.service.UploadService;
import lombok.AllArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

@AllArgsConstructor
@RequestMapping("/api/admin/upload")
@RestController
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/txt")
    public Mono<Void> uploadTextFile(@RequestParam("file") FilePart file) {
        return uploadService.uploadTxt(file.filename(), file);
    }

}
