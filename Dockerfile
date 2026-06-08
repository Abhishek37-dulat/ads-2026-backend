# syntax=docker/dockerfile:1

# ---- build stage ----
FROM gradle:9.1.0-jdk25 AS build
WORKDIR /home/gradle/src
# warm the dependency cache first
COPY settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon > /dev/null 2>&1 || true
# build the boot jar
COPY src ./src
RUN gradle bootJar --no-daemon

# ---- runtime stage ----
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd -r -u 1001 relay
COPY --from=build /home/gradle/src/build/libs/relay-backend.jar app.jar
USER relay
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "/app/app.jar"]
