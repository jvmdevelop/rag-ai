package com.jvmd.digitalurpaq_ai_agent.repo;

import com.jvmd.digitalurpaq_ai_agent.model.RetrievalDocument;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface RetrievalDocumentRepository extends ReactiveElasticsearchRepository<RetrievalDocument, String> {
}
