package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import com.jvmd.digitalurpaq_ai_agent.model.RetrievalDocument;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ProcessedQuery;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ScoredDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class SearchStrategy {

    private final ReactiveElasticsearchOperations operations;

    public SearchStrategy(ReactiveElasticsearchOperations operations) {
        this.operations = operations;
    }

    public Flux<ScoredDocument> hybridSearch(ProcessedQuery query, int topK) {
        String searchText = query.getSearchQuery();
        String category = query.category().getRussianName();
        
        log.info("Executing hybrid search for: '{}', category: {}, topK: {}", searchText, category, topK);

        Mono<List<ScoredDocument>> fuzzyMatch = fuzzyMatchSearch(searchText, category);
        Mono<List<ScoredDocument>> categoryMatch = categoryBasedSearch(category);


        return Flux.merge(fuzzyMatch, categoryMatch)
                .flatMapIterable(list -> list)
                .collectList()
                .flatMapMany(allResults -> {

                    List<ScoredDocument> merged = mergeAndRankResults(allResults);
                    
                    log.info("Found {} unique documents after merging", merged.size());
                    

                    return Flux.fromIterable(merged)
                            .take(topK);
                });
    }

    private Mono<List<ScoredDocument>> fuzzyMatchSearch(String searchText, String category) {
        if (searchText == null || searchText.isBlank()) {
            return Mono.just(List.of());
        }

        String safeText = sanitizeSearchText(searchText);
        
        Criteria criteria = new Criteria()
                .or(new Criteria("name").matches(safeText))
                .or(new Criteria("text").matches(safeText));

        return executeSearch(criteria, 1.0);
    }

    private Mono<List<ScoredDocument>> categoryBasedSearch(String category) {
        if (category == null || category.isBlank() || category.equals("общее")) {
            return Mono.just(List.of());
        }


        Criteria criteria = new Criteria()
                .or(new Criteria("name").matches(category))
                .or(new Criteria("text").matches(category));

        return executeSearch(criteria, 0.5);
    }

    private Mono<List<ScoredDocument>> executeSearch(Criteria criteria, double weight) {
        CriteriaQuery query = new CriteriaQuery(criteria);
        
        return operations.search(query, RetrievalDocument.class)
                .map(hit -> new ScoredDocument(
                        hit.getContent(),
                        calculateScore(hit, weight)
                ))
                .collectList()
                .onErrorResume(e -> {
                    log.error("Search error: {}", e.getMessage());
                    return Mono.just(List.of());
                });
    }

    private double calculateScore(SearchHit<RetrievalDocument> hit, double weight) {

        float baseScore = hit.getScore();

        double weightedScore = baseScore * weight;

        RetrievalDocument doc = hit.getContent();
        double lengthBonus = Math.min(doc.getText().length() / 1000.0, 1.0) * 0.1;
        
        return weightedScore + lengthBonus;
    }

    private List<ScoredDocument> mergeAndRankResults(List<ScoredDocument> results) {

        var scoreMap = new java.util.HashMap<String, ScoredDocument>();
        
        for (ScoredDocument doc : results) {
            String id = doc.document().getId();
            if (scoreMap.containsKey(id)) {

                ScoredDocument existing = scoreMap.get(id);
                double newScore = existing.score() + doc.score() * 0.5;
                scoreMap.put(id, new ScoredDocument(existing.document(), newScore));
            } else {
                scoreMap.put(id, doc);
            }
        }
        

        List<ScoredDocument> merged = new ArrayList<>(scoreMap.values());
        merged.sort(Comparator.comparingDouble(ScoredDocument::score).reversed());
        
        return merged;
    }

    private String sanitizeSearchText(String text) {

        return text.replaceAll("[\"*\\[\\]{}()?]", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

}
