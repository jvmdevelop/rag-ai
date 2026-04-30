package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import com.jvmd.digitalurpaq_ai_agent.model.event.CacheInvalidatedEvent;
import com.jvmd.digitalurpaq_ai_agent.model.event.DocumentIndexedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RagEventListener {

    @Async
    @EventListener
    public void onDocumentIndexed(DocumentIndexedEvent event) {
        log.info("[Event] Document indexed: id={} name='{}' chunks={}",
                event.documentId(), event.documentName(), event.chunkCount());
    }

    @EventListener
    public void onCacheInvalidated(CacheInvalidatedEvent event) {
        log.info("[Event] Cache invalidated: namespace={} reason={}",
                event.namespace(), event.reason());
    }
}
