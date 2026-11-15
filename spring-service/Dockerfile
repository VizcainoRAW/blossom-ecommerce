# Build stage: tests are skipped here because the integration tests need Docker themselves; run them with ./gradlew check.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Wrapper and build files first so the Gradle distribution and dependencies stay cached between code changes.
COPY gradlew settings.gradle build.gradle lombok.config ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null

COPY src src
RUN ./gradlew bootJar --no-daemon -q && cp build/libs/*.jar app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --no-create-home spring
USER spring
COPY --from=build /app/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
