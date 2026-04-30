package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class QueryExpander {

    private static final Map<String, List<String>> SYNONYMS = new LinkedHashMap<>();

    static {
        SYNONYMS.put("расписание", List.of("занятия", "уроки", "расписание занятий", "когда"));
        SYNONYMS.put("время", List.of("расписание", "часы", "смена", "начало", "конец"));
        SYNONYMS.put("смена", List.of("первая смена", "вторая смена", "расписание"));
        SYNONYMS.put("звонок", List.of("расписание звонков", "перемена", "перерыв"));

        SYNONYMS.put("понедельник", List.of("пн", "начало недели"));
        SYNONYMS.put("вторник", List.of("вт"));
        SYNONYMS.put("среда", List.of("ср", "середина недели"));
        SYNONYMS.put("четверг", List.of("чт"));
        SYNONYMS.put("пятница", List.of("пт", "конец недели"));
        SYNONYMS.put("суббота", List.of("сб"));

        SYNONYMS.put("учитель", List.of("педагог", "преподаватель", "тренер", "руководитель"));
        SYNONYMS.put("педагог", List.of("учитель", "преподаватель", "наставник"));

        SYNONYMS.put("кабинет", List.of("комната", "аудитория", "класс", "зал", "лаборатория"));
        SYNONYMS.put("лаборатория", List.of("кабинет", "лаб", "лабораторный зал"));

        SYNONYMS.put("кружок", List.of("секция", "направление", "курс", "занятие", "клуб"));
        SYNONYMS.put("направление", List.of("кружок", "секция", "программа", "курс"));
        SYNONYMS.put("программирование", List.of("код", "кодинг", "разработка", "it", "информатика"));
        SYNONYMS.put("робот", List.of("робототехника", "lego", "arduino", "конструктор"));

        SYNONYMS.put("контакт", List.of("телефон", "адрес", "email", "связь", "позвонить"));
        SYNONYMS.put("телефон", List.of("номер", "контакты", "звонить"));
        SYNONYMS.put("адрес", List.of("где находится", "местоположение", "как доехать"));

        SYNONYMS.put("дети", List.of("школьники", "учащиеся", "воспитанники"));
        SYNONYMS.put("подростки", List.of("молодежь", "школьники", "юниоры"));
    }

    public String expand(String originalQuery) {
        if (originalQuery == null || originalQuery.isBlank()) return originalQuery;

        String lower = originalQuery.toLowerCase();
        Set<String> expansions = new LinkedHashSet<>();

        for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                expansions.addAll(entry.getValue());
            }
        }

        if (expansions.isEmpty()) {
            log.debug("[QueryExpander] No expansions for: {}", originalQuery);
            return originalQuery;
        }

        String expanded = originalQuery + " " + String.join(" ", expansions);
        log.info("[QueryExpander] Expanded '{}' → added {} terms", originalQuery, expansions.size());
        return expanded;
    }

}
