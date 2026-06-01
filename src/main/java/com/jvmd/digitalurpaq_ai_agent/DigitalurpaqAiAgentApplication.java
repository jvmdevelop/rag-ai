package com.jvmd.digitalurpaq_ai_agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableWebFlux
@EnableScheduling
@ConfigurationPropertiesScan("com.jvmd.digitalurpaq_ai_agent.config.properties")
public class DigitalurpaqAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalurpaqAiAgentApplication.class, args);
    }

}
