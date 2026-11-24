# Checkpoint

## Текущее состояние

Завершён Этап 22: «Docker и автоматизация». Все предыдущие этапы (1–21) подтверждены.

## Что реализовано в Этапе 22

- **`Dockerfile`** — одностадийная сборка Spring Boot-приложения (eclipse-temurin:23-jre, EXPOSE 8080, HEALTHCHECK через `nc`, ENTRYPOINT `java -jar`).
- **`docker-compose.yml`** — сервисы `postgres` (postgres:16, healthcheck через `pg_isready`) и `app` (build из Dockerfile, depends_on postgres service_healthy, порт 8080:8080, изолированная сеть `studying-platform`).
- **`application-docker.yml`** — профиль `docker`: URL БД `jdbc:postgresql://postgres:5432/studying_platform`, `ddl-auto: update`.
- **`.github/workflows/ci.yml`** — GitHub Actions: checkout, JDK 23 Temurin, `mvn -B clean compile`, `mvn -B test -Ptest` с Testcontainers.
- **`DockerInfrastructureTest`** — 16 интеграционных тестов на верификацию файлов и сборки Maven.

## Итоговые проверки

- **PASS:** `mvn test -Dtest=DockerInfrastructureTest` — 16 тестов, 0 ошибок
- **PASS:** `mvn test` — 482 теста, 0 ошибок, 0 пропусков
- **PASS:** `mvn clean compile` — BUILD SUCCESS, 97 source files, 0 errors
- **PASS:** `git diff --check` — нет проблем с форматированием
- **PASS:** независимая проверка `stage-tester` — STATUS: PASS

## Затронутые файлы

- NEW: `Dockerfile`
- NEW: `docker-compose.yml`
- NEW: `src/main/resources/application-docker.yml`
- NEW: `.github/workflows/ci.yml`
- NEW: `src/test/java/com/studying/platform/DockerInfrastructureTest.java`
- NEW: `stages/stage-22.md`
- MOD: `plan.md` (статус этапа 22)

## ADR

Значимых архитектурных решений не требовалось. Инфраструктурные файлы (Dockerfile, docker-compose, CI) — стандартная практика, ADR не создавались.

## Следующий шаг

Этап 23: Документация — `pending`.
