package com.jvmd.digitalurpaq_ai_agent.llm.langchain;

import com.jvmd.digitalurpaq_ai_agent.model.RetrievalDocument;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ProcessedQuery;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.QueryCategory;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.ScoredDocument;
import com.jvmd.digitalurpaq_ai_agent.service.rag.util.SearchStrategy;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrpaqTools {

    private final SearchStrategy searchStrategy;

    private static final ZoneId ALMATY_TZ = ZoneId.of("Asia/Almaty");
    private static final int TOOL_SEARCH_LIMIT = 3;

    @Tool("Найти информацию о расписании занятий по запросу. Используй для вопросов о времени, днях недели, сменах.")
    public String searchSchedule(String query) {
        log.info("[Tool] searchSchedule called: {}", query);
        return executeSearch(query, QueryCategory.SCHEDULE);
    }

    @Tool("Найти информацию об образовательных программах, кружках и направлениях обучения.")
    public String searchPrograms(String programName) {
        log.info("[Tool] searchPrograms called: {}", programName);
        return executeSearch(programName, QueryCategory.DIRECTIONS);
    }

    @Tool("Найти информацию о педагогах и учителях Digital Urpaq.")
    public String searchTeachers(String teacherName) {
        log.info("[Tool] searchTeachers called: {}", teacherName);
        return executeSearch(teacherName, QueryCategory.TEACHERS);
    }

    @Tool("Найти информацию о кабинетах, лабораториях и аудиториях Digital Urpaq.")
    public String getRoomInfo(String roomQuery) {
        log.info("[Tool] getRoomInfo called: {}", roomQuery);
        return executeSearch(roomQuery, QueryCategory.ROOMS);
    }

    @Tool("Получить контактную информацию Digital Urpaq: телефоны, адрес, email, время работы.")
    public String getContactInfo() {
        log.info("[Tool] getContactInfo called");
        return executeSearch("контакты телефон адрес", QueryCategory.CONTACTS);
    }

    @Tool("Получить текущую дату и время в Алматы (UTC+5). Используй для вопросов о текущем времени или дне недели.")
    public String getCurrentDateTime() {
        ZonedDateTime now = ZonedDateTime.now(ALMATY_TZ);
        String formatted = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy, HH:mm", new java.util.Locale("ru")));
        log.info("[Tool] getCurrentDateTime: {}", formatted);
        return "Текущее время в Алматы: " + formatted;
    }

    @Tool("Выполнить общий поиск по базе знаний Digital Urpaq по любому запросу.")
    public String searchKnowledgeBase(String query) {
        log.info("[Tool] searchKnowledgeBase called: {}", query);
        return executeSearch(query, QueryCategory.GENERAL);
    }

    private String executeSearch(String query, QueryCategory category) {
        try {
            ProcessedQuery processedQuery = new ProcessedQuery(query, query.toLowerCase(), category, query);
            List<ScoredDocument> results = searchStrategy.hybridSearch(processedQuery, TOOL_SEARCH_LIMIT)
                    .collectList()
                    .block();

            if (results == null || results.isEmpty()) {
                return "Информация по запросу '" + query + "' не найдена в базе знаний.";
            }

            return results.stream()
                    .map(doc -> String.format("**%s** (релевантность: %.2f)\n%s",
                            doc.getName(), doc.score(),
                            truncate(doc.getText(), 300)))
                    .collect(Collectors.joining("\n\n---\n\n"));

        } catch (Exception e) {
            log.error("[Tool] Search error for query '{}': {}", query, e.getMessage());
            return "Ошибка при поиске: " + e.getMessage();
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
