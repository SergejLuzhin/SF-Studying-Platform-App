# syntax=docker/dockerfile:1

FROM eclipse-temurin:23-jre AS build

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD nc -z localhost 8080 || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
