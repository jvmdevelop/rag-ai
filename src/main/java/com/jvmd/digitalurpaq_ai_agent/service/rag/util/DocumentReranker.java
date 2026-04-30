package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import com.jvmd.digitalurpaq_ai_agent.llm.Llm7Client;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ScoredDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.reranker.enabled", havingValue = "true", matchIfMissing = true)
public class DocumentReranker {

    private final Llm7Client llm7Client;

    private static final int RERANK_THRESHOLD = 2;
    private static final Pattern SCORE_PATTERN = Pattern.compile("DOC(\\d+):\\s*(\\d+(?:\\.\\d+)?)");

    public Mono<List<ScoredDocument>> rerank(String query, List<ScoredDocument> documents) {
        if (documents.size() <= RERANK_THRESHOLD) {
            return Mono.just(documents);
        }

        log.debug("[Reranker] Reranking {} docs for: {}", documents.size(), query);

        return llm7Client.chat(buildRerankPrompt(query, documents))
                .map(response -> parseAndRerank(response, documents))
                .onErrorResume(e -> {
                    log.warn("[Reranker] Fallback to ES scores: {}", e.getMessage());
                    return Mono.just(documents);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String buildRerankPrompt(String query, List<ScoredDocument> documents) {
        StringBuilder sb = new StringBuilder();
        sb.append("Оцени релевантность каждого документа для запроса. ")
          .append("Ответь ТОЛЬКО строкой: DOC1:N DOC2:N ... (N от 0 до 10).\n\n")
          .append("Запрос: ").append(query).append("\n\n");

        for (int i = 0; i < documents.size(); i++) {
            ScoredDocument doc = documents.get(i);
            String preview = doc.getText().length() > 150
                    ? doc.getText().substring(0, 150) + "..." : doc.getText();
            sb.append(String.format("DOC%d: %s\n%s\n\n", i + 1, doc.getName(), preview));
        }
        sb.append("Оценки:");
        return sb.toString();
    }

    private List<ScoredDocument> parseAndRerank(String llmResponse, List<ScoredDocument> docs) {
        Map<Integer, Double> scores = new HashMap<>();
        Matcher m = SCORE_PATTERN.matcher(llmResponse);
        while (m.find()) {
            int idx = Integer.parseInt(m.group(1)) - 1;
            double score = Double.parseDouble(m.group(2)) / 10.0;
            if (idx >= 0 && idx < docs.size()) scores.put(idx, score);
        }

        if (scores.isEmpty()) return docs;

        return IntStream.range(0, docs.size())
                .mapToObj(i -> {
                    double llmScore = scores.getOrDefault(i, 0.5);
                    double combined = llmScore * 0.7 + docs.get(i).score() * 0.3;
                    return new ScoredDocument(docs.get(i).document(), combined);
                })
                .sorted(Comparator.comparingDouble(ScoredDocument::score).reversed())
                .collect(Collectors.toList());
    }
}
