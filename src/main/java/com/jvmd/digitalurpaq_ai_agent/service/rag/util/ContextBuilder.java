package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ProcessedQuery;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.QueryCategory;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ScoredDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ContextBuilder {

    private static final int MAX_CONTEXT_LENGTH = 4000;
    private static final String CONTEXT_SEPARATOR = "\n---\n";

    public String buildContext(List<ScoredDocument> documents,
                               ProcessedQuery query) {
        if (documents.isEmpty()) {
            return "Информация не найдена в базе знаний.";
        }

        StringBuilder context = new StringBuilder();
        context.append("=== НАЙДЕННАЯ ИНФОРМАЦИЯ ===\n\n");

        int totalLength = 0;
        int docCount = 0;

        for (ScoredDocument scoredDoc : documents) {
            String docText = formatDocument(scoredDoc, docCount + 1);
            
            if (totalLength + docText.length() > MAX_CONTEXT_LENGTH && docCount > 0) {
                log.info("Context limit reached, using {} documents", docCount);
                break;
            }

            context.append(docText);
            context.append(CONTEXT_SEPARATOR);
            
            totalLength += docText.length();
            docCount++;
        }

        context.append("\n=== КОНЕЦ ИНФОРМАЦИИ ===\n");
        
        log.info("Built context with {} documents, total length: {}", docCount, totalLength);
        
        return context.toString();
    }

    private String formatDocument(ScoredDocument scoredDoc, int index) {
        return String.format(
                "Документ %d: %s\nРелевантность: %.2f\n\n%s",
                index,
                scoredDoc.getName(),
                scoredDoc.score(),
                scoredDoc.getText()
        );
    }

    public String buildPrompt(String context, String userQuery, ProcessedQuery processedQuery) {
        String categoryHint = getCategoryHint(processedQuery.category());
        
        return String.format("""
                Ты - AI помощник Дворца школьников "Digital Urpaq".
                
                ТВОЯ ЗАДАЧА:
                - Ответить на вопрос пользователя, используя ТОЛЬКО предоставленную информацию
                - Быть точным, конкретным и полезным
                - Если информации недостаточно, честно сказать об этом
                - Отвечать на русском языке
                
                %s
                
                %s
                
                ВОПРОС ПОЛЬЗОВАТЕЛЯ:
                %s
                
                ИНСТРУКЦИИ:
                1. Внимательно изучи найденную информацию
                2. Найди релевантные части, которые отвечают на вопрос
                3. Сформулируй четкий и полный ответ
                4. Если нужно, структурируй ответ списком или таблицей
                5. Не придумывай информацию, которой нет в документах
                
                ОТВЕТ:
                """,
                categoryHint,
                context,
                userQuery
        );
    }

    private String getCategoryHint(QueryCategory category) {
        return switch (category) {
            case SCHEDULE -> "КАТЕГОРИЯ: Расписание\nОбрати особое внимание на время, дни недели и смены.";
            case ROOMS -> "КАТЕГОРИЯ: Кабинеты и лаборатории\nОпиши оборудование и возможности помещений.";
            case TEACHERS -> "КАТЕГОРИЯ: Учителя и педагоги\nУкажи имена, квалификацию и достижения.";
            case DIRECTIONS -> "КАТЕГОРИЯ: Направления и кружки\nОпиши программы, возраст участников и условия.";
            case CONTACTS -> "КАТЕГОРИЯ: Контакты\nУкажи точные телефоны, адреса и время работы.";
            case GENERAL -> "КАТЕГОРИЯ: Общая информация\nДай полный и информативный ответ.";
        };
    }
}
