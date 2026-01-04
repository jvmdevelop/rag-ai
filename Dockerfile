FROM gradle:8.5-jdk21 AS build
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

COPY src ./src

RUN gradle build -x test --no-daemon


FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
COPY compose.yaml .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
