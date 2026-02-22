# syntax=docker/dockerfile:1

# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Gradle wrapper + build scripts first to leverage Docker layer caching
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts build.gradle.kts
COPY settings.gradle.kts settings.gradle.kts

# Ensure wrapper is executable
RUN chmod +x ./gradlew

# Copy source
COPY src src

# Build boot jar (skip tests for faster container builds; Render can run tests separately if desired)
RUN ./gradlew clean bootJar -x test


# --- Run stage ---
FROM eclipse-temurin:21-jre

WORKDIR /app

# Render sets PORT; Spring reads it via server.port=${PORT:1107}
ENV PORT=1107

COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 1107

CMD ["java", "-jar", "/app/app.jar"]
