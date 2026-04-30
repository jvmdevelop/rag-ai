plugins {
    java
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.jvmd"
version = "0.0.1-SNAPSHOT"
description = "digitalurpaq_ai_agent"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["springAiVersion"] = "1.0.3"
extra["langchain4jVersion"] = "0.36.0"

springBoot {
    buildInfo()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
    // ── Production-ready (Senior must-have) ───────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-actuator")       // health, metrics, info
    implementation("org.springframework.boot:spring-boot-starter-validation")     // Jakarta Bean Validation
    implementation("org.springframework.boot:spring-boot-starter-cache")          // @Cacheable, @CacheEvict
    implementation("org.springframework.boot:spring-boot-starter-aop")            // @Aspect, @Around
    implementation("org.springframework.retry:spring-retry")                      // @Retryable, @Recover

    // ── Caching ───────────────────────────────────────────────────────────────
    implementation("com.github.ben-manes.caffeine:caffeine")                      // high-perf in-memory L1 cache

    // ── Metrics & Observability ───────────────────────────────────────────────
    implementation("io.micrometer:micrometer-registry-prometheus")                // Prometheus scraping
    implementation("io.micrometer:micrometer-observation")                        // Observation API

    // ── Document parsing ─────────────────────────────────────────────────────
    implementation("org.apache.pdfbox:pdfbox:2.0.33")

    // ── JSON ─────────────────────────────────────────────────────────────────
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // ── LangChain4j ──────────────────────────────────────────────────────────
    implementation("dev.langchain4j:langchain4j-spring-boot-starter:${property("langchain4jVersion")}")
    implementation("dev.langchain4j:langchain4j-open-ai-spring-boot-starter:${property("langchain4jVersion")}")
    implementation("dev.langchain4j:langchain4j:${property("langchain4jVersion")}")

    // ── AWS S3 (Storj) ───────────────────────────────────────────────────────
    implementation("software.amazon.awssdk:auth:2.41.0")
    implementation("software.amazon.awssdk:s3:2.41.0")
    runtimeOnly("software.amazon.awssdk:bom:2.40.16")

    // ── Misc ─────────────────────────────────────────────────────────────────
    implementation("me.paulschwarz:spring-dotenv:3.0.0")

    // ── Developer tools ───────────────────────────────────────────────────────
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // ── Tests ─────────────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:elasticsearch")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
