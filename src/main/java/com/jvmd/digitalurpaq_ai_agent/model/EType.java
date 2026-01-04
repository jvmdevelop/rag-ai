package com.jvmd.digitalurpaq_ai_agent.model;

public enum EType {
    USER("Пользователь"),
    AI_HELPER("DIGITAL URPAQ помошник")
    ;

    public final String name;


    EType(String name) {
        this.name = name;
    }
}

