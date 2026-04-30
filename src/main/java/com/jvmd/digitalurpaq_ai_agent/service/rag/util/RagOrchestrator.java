package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import com.jvmd.digitalurpaq_ai_agent.llm.Llm7Client;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.*;
import lombok.extern.slf4j.Slf4j;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ValidationIssue;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * RAG Pipeline Orchestrator — 7-step retrieval-augmented generation pipeline.
 *
 * Steps:
 *  1. QueryExpander   — expand query with domain synonyms (improves recall)
 *  2. QueryProcessor  — categorize and extract keywords
 *  3. CacheService    — check file-based persistent cache
 *  4. SearchStrategy  — hybrid ES search (fuzzy + category)
 *  5. DocumentReranker — LLM-based relevance reranking
 *  6. ContextBuilder  — build structured context + prompt
 *  7. LLM call        — generate final answer
 */
@Slf4j
@Service
public class RagOrchestrator {

    private final QueryExpander queryExpander;
    private final QueryProcessor queryProcessor;
    private final SearchStrategy searchStrategy;
    private final DocumentReranker documentReranker;
    private final ContextBuilder contextBuilder;
    private final ResponseValidator responseValidator;
    private final CacheService cacheService;
    private final Llm7Client llm7Client;
    private final RagMetrics metrics;

    private static final int TOP_K_DOCUMENTS = 7; // retrieve more, reranker picks the best
    private static final int TOP_K_AFTER_RERANK = 4;
    private static final int MAX_RETRIES = 2;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    public RagOrchestrator(QueryExpander queryExpander,
                           QueryProcessor queryProcessor,
                           SearchStrategy searchStrategy,
                           DocumentReranker documentReranker,
                           ContextBuilder contextBuilder,
                           ResponseValidator responseValidator,
                           CacheService cacheService,
                           Llm7Client llm7Client,
                           RagMetrics metrics) {
        this.queryExpander = queryExpander;
        this.queryProcessor = queryProcessor;
        this.searchStrategy = searchStrategy;
        this.documentReranker = documentReranker;
        this.contextBuilder = contextBuilder;
        this.responseValidator = responseValidator;
        this.cacheService = cacheService;
        this.llm7Client = llm7Client;
        this.metrics = metrics;
    }

    public Mono<RagResponse> processQuery(String userQuery) {
        long startTime = System.currentTimeMillis();
        log.info("=== RAG Pipeline START: {} ===", userQuery);

        // Step 1: Query Expansion (synchronous, fast, no I/O)
        String expandedQuery = queryExpander.expand(userQuery);

        return Mono.just(expandedQuery)
                // Step 2: Query processing + categorization (with cache)
                .flatMap(query -> {
                    log.debug("[Step 2] Processing query");
                    return cacheService.getOrComputeQuery(
                            query,
                            queryProcessor.processQuery(query)
                    );
                })
                // Step 3: Hybrid ES search (with cache)
                .flatMap(processedQuery -> {
                    log.debug("[Step 3] Searching documents: category={}", processedQuery.category());
                    return cacheService.getOrComputeSearch(
                            processedQuery.getSearchQuery(),
                            searchStrategy.hybridSearch(processedQuery, TOP_K_DOCUMENTS).collectList()
                    ).map(docs -> new QueryWithDocs(processedQuery, docs));
                })
                // Step 4: Document Reranking (LLM-based, skip if empty)
                .flatMap(queryWithDocs -> {
                    if (queryWithDocs.documents().isEmpty()) {
                        return Mono.just(queryWithDocs);
                    }
                    log.debug("[Step 4] Reranking {} documents", queryWithDocs.documents().size());
                    return documentReranker.rerank(userQuery, queryWithDocs.documents())
                            .map(reranked -> {
                                List<ScoredDocument> topK = reranked.stream()
                                        .limit(TOP_K_AFTER_RERANK)
                                        .toList();
                                return new QueryWithDocs(queryWithDocs.query(), topK);
                            });
                })
                // Step 5: Context assembly
                .flatMap(queryWithDocs -> {
                    log.debug("[Step 5] Building context from {} documents", queryWithDocs.documents().size());
                    if (queryWithDocs.documents().isEmpty()) {
                        return Mono.just(new ContextWithQuery(
                                queryWithDocs.query(),
                                "Информация не найдена",
                                queryWithDocs.documents()
                        ));
                    }
                    String context = contextBuilder.buildContext(queryWithDocs.documents(), queryWithDocs.query());
                    return Mono.just(new ContextWithQuery(queryWithDocs.query(), context, queryWithDocs.documents()));
                })
                // Step 6: LLM response generation
                .flatMap(contextWithQuery -> {
                    log.debug("[Step 6] Generating response");
                    return generateResponse(
                            contextWithQuery.context(),
                            userQuery,
                            contextWithQuery.query()
                    ).map(response -> new ResponseWithContext(response, contextWithQuery.query(), contextWithQuery.documents()));
                })
                // Step 7: Response validation
                .flatMap(responseWithContext -> {
                    log.debug("[Step 7] Validating response");
                    ValidationResult validation = responseValidator.validate(
                            responseWithContext.response(),
                            responseWithContext.query().originalQuery()
                    );
                    if (!validation.isValid()) {
                        log.warn("[Step 7] Validation issue: {}", validation.issue());
                        metrics.recordValidationFailure(validation.issue());
                    }
                    return Mono.just(new RagResponse(
                            validation.processedResponse(),
                            responseWithContext.query(),
                            responseWithContext.documents(),
                            validation.isValid(),
                            validation.issue()
                    ));
                })
                .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofSeconds(1))
                        .filter(t -> !(t instanceof IllegalArgumentException))
                        .doBeforeRetry(s -> {
                            log.warn("[RAG] Retry attempt {}", s.totalRetries() + 1);
                            metrics.recordRetry();
                        }))
                .timeout(TIMEOUT)
                .doOnSuccess(r -> {
                    long ms = System.currentTimeMillis() - startTime;
                    log.info("=== RAG Pipeline DONE in {}ms ===", ms);
                    metrics.recordSuccess(ms);
                })
                .doOnError(e -> {
                    long ms = System.currentTimeMillis() - startTime;
                    log.error("=== RAG Pipeline FAILED in {}ms: {} ===", ms, e.getMessage());
                    metrics.recordFailure(e);
                })
                .onErrorResume(e -> {
                    log.error("Fatal RAG error, returning error response", e);
                    return Mono.just(createErrorResponse(userQuery, e));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<String> generateResponse(String context, String userQuery, ProcessedQuery processedQuery) {
        String prompt = contextBuilder.buildPrompt(context, userQuery, processedQuery);
        log.debug("[RAG] LLM prompt length: {} chars", prompt.length());

        return llm7Client.chat(prompt)
                .timeout(Duration.ofSeconds(60))
                .onErrorResume(e -> {
                    log.error("[RAG] LLM error: {}", e.getMessage());
                    if (!context.isBlank() && !context.contains("не найден")) {
                        return Mono.just("На основе найденной информации:\n\n" +
                                context.substring(0, Math.min(500, context.length())) +
                                "\n\n(Полный ответ не был сгенерирован из-за технической ошибки)");
                    }
                    return Mono.just("Извините, произошла ошибка при генерации ответа. Попробуйте ещё раз.");
                });
    }

    private RagResponse createErrorResponse(String query, Throwable error) {
        String msg = error instanceof java.util.concurrent.TimeoutException
                ? "Превышено время ожидания. Попробуйте упростить запрос."
                : "Ошибка при обработке запроса. Попробуйте позже.";
        return new RagResponse(
                "Извините, " + msg,
                new ProcessedQuery(query, "", QueryCategory.GENERAL, ""),
                List.of(),
                false,
                ValidationIssue.EMPTY_RESPONSE
        );
    }

    // Convenience record for internal use
    public record RagResponse(
            String answer,
            ProcessedQuery query,
            List<ScoredDocument> documents,
            boolean valid,
            ValidationIssue validationIssue
    ) {}
}
