package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import com.jvmd.digitalurpaq_ai_agent.llm.Llm7Client;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class RagOrchestrator {

    private final QueryProcessor queryProcessor;
    private final SearchStrategy searchStrategy;
    private final ContextBuilder contextBuilder;
    private final ResponseValidator responseValidator;
    private final CacheService cacheService;
    private final Llm7Client llm7Client;
    private final RagMetrics metrics;

    private static final int TOP_K_DOCUMENTS = 5;
    private static final int MAX_RETRIES = 2;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    public RagOrchestrator(QueryProcessor queryProcessor,
                          SearchStrategy searchStrategy,
                          ContextBuilder contextBuilder,
                          ResponseValidator responseValidator,
                          CacheService cacheService,
                          Llm7Client llm7Client,
                          RagMetrics metrics) {
        this.queryProcessor = queryProcessor;
        this.searchStrategy = searchStrategy;
        this.contextBuilder = contextBuilder;
        this.responseValidator = responseValidator;
        this.cacheService = cacheService;
        this.llm7Client = llm7Client;
        this.metrics = metrics;
    }

    public Mono<RagResponse> processQuery(String userQuery) {
        long startTime = System.currentTimeMillis();
        
        log.info("=== RAG Pipeline Started for query: {} ===", userQuery);

        return Mono.just(userQuery)
                .flatMap(query -> {
                    log.info("Step 1: Processing query");
                    return cacheService.getOrComputeQuery(
                            query,
                            queryProcessor.processQuery(query)
                    );
                })
                .flatMap(processedQuery -> {
                    log.info("Step 2: Searching documents for category: {}", 
                            processedQuery.category());
                    
                    return cacheService.getOrComputeSearch(
                            processedQuery.getSearchQuery(),
                            searchStrategy.hybridSearch(processedQuery, TOP_K_DOCUMENTS)
                                    .collectList()
                    ).map(docs -> new QueryWithDocs(processedQuery, docs));
                })
                .flatMap(queryWithDocs -> {
                    log.info("Step 3: Building context from {} documents", 
                            queryWithDocs.documents().size());
                    
                    if (queryWithDocs.documents().isEmpty()) {
                        return Mono.just(new ContextWithQuery(
                                queryWithDocs.query(),
                                "Информация не найдена",
                                queryWithDocs.documents()
                        ));
                    }
                    
                    String context = contextBuilder.buildContext(
                            queryWithDocs.documents(),
                            queryWithDocs.query()
                    );
                    
                    return Mono.just(new ContextWithQuery(
                            queryWithDocs.query(),
                            context,
                            queryWithDocs.documents()
                    ));
                })
                .flatMap(contextWithQuery -> {
                    log.info("Step 4: Generating response");
                    
                    return generateResponse(
                            contextWithQuery.context(),
                            contextWithQuery.query().originalQuery(),
                            contextWithQuery.query()
                    ).map(response -> new ResponseWithContext(
                            response,
                            contextWithQuery.query(),
                            contextWithQuery.documents()
                    ));
                })
                .flatMap(responseWithContext -> {
                    log.info("Step 5: Validating response");
                    
                    ValidationResult validation = responseValidator.validate(
                            responseWithContext.response(),
                            responseWithContext.query().originalQuery()
                    );
                    
                    if (!validation.isValid()) {
                        log.warn("Response validation failed: {}", validation.issue());
                        metrics.recordValidationFailure(validation.issue());
                    }
                    
                    String finalResponse = validation.isValid() 
                            ? validation.processedResponse()
                            : validation.processedResponse();
                    
                    return Mono.just(new RagResponse(
                            finalResponse,
                            responseWithContext.query(),
                            responseWithContext.documents(),
                            validation.isValid(),
                            validation.issue()
                    ));
                })
                .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofSeconds(1))
                        .filter(throwable -> !(throwable instanceof IllegalArgumentException))
                        .doBeforeRetry(signal -> {
                            log.warn("Retrying RAG pipeline, attempt: {}", signal.totalRetries() + 1);
                            metrics.recordRetry();
                        })
                )
                .timeout(TIMEOUT)
                .doOnSuccess(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("=== RAG Pipeline Completed in {}ms ===", duration);
                    metrics.recordSuccess(duration);
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("=== RAG Pipeline Failed after {}ms: {} ===", 
                            duration, error.getMessage());
                    metrics.recordFailure(error);
                })
                .onErrorResume(error -> {
                    log.error("Fatal error in RAG pipeline", error);
                    return Mono.just(createErrorResponse(userQuery, error));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<String> generateResponse(String context, String userQuery, 
                                         ProcessedQuery processedQuery) {
        String prompt = contextBuilder.buildPrompt(context, userQuery, processedQuery);
        
        log.debug("Calling LLM7.io with prompt length: {}", prompt.length());
        
        return llm7Client.chat(prompt)
                .timeout(Duration.ofSeconds(60))
                .onErrorResume(e -> {
                    log.error("Error generating response: {}", e.getMessage(), e);
                    
                    if (!context.isBlank()) {
                        String fallback = "На основе найденной информации:\n\n" + 
                                         context.substring(0, Math.min(500, context.length())) + 
                                         "\n\n(Полный ответ не был сгенерирован из-за технической ошибки)";
                        return Mono.just(fallback);
                    }
                    
                    return Mono.just("Извините, произошла ошибка при генерации ответа. Попробуйте еще раз.");
                });
    }

    private RagResponse createErrorResponse(String query, Throwable error) {
        String errorMessage = "Извините, произошла ошибка при обработке вашего запроса. ";

        if (error instanceof java.util.concurrent.TimeoutException) {
            errorMessage += "Превышено время ожидания. Попробуйте упростить запрос.";
        } else {
            errorMessage += "Пожалуйста, попробуйте еще раз позже.";
        }
        
        return new RagResponse(
                errorMessage,
                new ProcessedQuery(query, "", QueryCategory.GENERAL, ""),
                List.of(),
                false,
                ResponseValidator.ValidationIssue.EMPTY_RESPONSE
        );
    }

}
