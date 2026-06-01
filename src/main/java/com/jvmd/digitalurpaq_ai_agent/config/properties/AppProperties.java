package com.jvmd.digitalurpaq_ai_agent.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        String pdfPath,
        boolean init,
        boolean useChunking,
        RateLimitProperties rateLimit
) {
    public AppProperties {
        if (frontendUrl == null) frontendUrl = "localhost:3000";
        if (pdfPath == null) pdfPath = "raspisanie.pdf";
        if (rateLimit == null) rateLimit = new RateLimitProperties(20, 60);
    }

    public record RateLimitProperties(int requestsPerMinute, int windowSeconds) {
        public RateLimitProperties {
            if (requestsPerMinute == 0) requestsPerMinute = 20;
            if (windowSeconds == 0) windowSeconds = 60;
        }
    }
}
