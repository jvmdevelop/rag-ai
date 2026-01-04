package com.jvmd.digitalurpaq_ai_agent.service.rag.model;

public enum QueryCategory {
        SCHEDULE("расписание"),
        ROOMS("кабинеты"),
        TEACHERS("учителя"),
        DIRECTIONS("направления"),
        CONTACTS("контакты"),
        GENERAL("общее");

        private final String russianName;

        QueryCategory(String russianName) {
            this.russianName = russianName;
        }

        public String getRussianName() {
            return russianName;
        }
    }