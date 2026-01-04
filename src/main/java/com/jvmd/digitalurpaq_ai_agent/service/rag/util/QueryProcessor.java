package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ProcessedQuery;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.QueryCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class QueryProcessor {

    public Mono<ProcessedQuery> processQuery(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            return Mono.just(new ProcessedQuery(userQuery, "", QueryCategory.GENERAL, ""));
        }

        return Mono.fromCallable(() -> {
            String lowerQuery = userQuery.toLowerCase();
            QueryCategory category = determineCategory(lowerQuery);
            String keywords = extractKeywords(lowerQuery);
            
            log.info("Processed query - Category: {}, Keywords: {}", category, keywords);
            
            return new ProcessedQuery(
                    userQuery,
                    lowerQuery,
                    category,
                    keywords
            );
        })
        .onErrorResume(e -> {
            log.error("Error processing query: {}", e.getMessage());
            return Mono.just(new ProcessedQuery(userQuery, "", QueryCategory.GENERAL, userQuery));
        });
    }

    private QueryCategory determineCategory(String metadata) {
        String lowerMetadata = metadata.toLowerCase();
        
        if (lowerMetadata.contains("расписание") || lowerMetadata.contains("звонк")) {
            return QueryCategory.SCHEDULE;
        } else if (lowerMetadata.contains("кабинет") || lowerMetadata.contains("лаборатор")) {
            return QueryCategory.ROOMS;
        } else if (lowerMetadata.contains("учител") || lowerMetadata.contains("педагог") || lowerMetadata.contains("преподават")) {
            return QueryCategory.TEACHERS;
        } else if (lowerMetadata.contains("направлен") || lowerMetadata.contains("кружок") || lowerMetadata.contains("секци")) {
            return QueryCategory.DIRECTIONS;
        } else if (lowerMetadata.contains("контакт") || lowerMetadata.contains("телефон") || lowerMetadata.contains("адрес")) {
            return QueryCategory.CONTACTS;
        }
        
        return QueryCategory.GENERAL;
    }

    private String extractKeywords(String metadata) {
        if (metadata.contains("[Ключевые слова]:")) {
            String[] parts = metadata.split("\\[Ключевые слова\\]:");
            if (parts.length > 1) {
                String keywordsPart = parts[1].split("\\[")[0].trim();
                return keywordsPart;
            }
        }
        return metadata;
    }
}
