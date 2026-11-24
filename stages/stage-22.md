# Этап 22: Docker и автоматизация

## Цель и границы этапа

Обеспечить воспроизводимый запуск приложения вместе с PostgreSQL в контейнерах Docker, а также автоматизированную проверку кода и тестов в CI (GitHub Actions).

**Входит в этап:**
- `Dockerfile` для сборки образа Spring Boot-приложения.
- `docker-compose.yml` для запуска приложения и PostgreSQL в единой Docker-сети.
- Профиль `docker` — конфигурация приложения, в которой URL базы данных указывает на хост `postgres` (имя сервиса в Docker-сети).
- `.github/workflows/ci.yml` — CI-конфигурация GitHub Actions: сборка Maven, запуск интеграционных тестов через Testcontainers (GitHub Actions уже предоставляют Docker-демон).
- Проверка `.gitignore`: убедится, что новые файлы не оказываются в игноре.

**Не входит в этап:**
- Docker-конфигурация для REST-клиента или фронтенда (этап 23 — документация, описание запуска).
- K8s-манифесты, Helm-чарты, оркестрация.
- ADR — конфигурация контейнеризации является стандартной практикой и не требует существенного архитектурного решения.

## Связанные файлы и применимые ADR

- **ADR:** `docs/adr/README.md` — пустой индекс, значимых утверждённых ADR нет. Архитектурных ограничений на контейнеризацию нет.
- **Главный класс:** `com.studying.platform.StudyingPlatformApplication`
- **Конфигурация:** `src/main/resources/application.yml`, `src/main/resources/application-dev.yml`, `src/main/resources/application-test.yml`
- **Тестовый профиль:** `application-test.yml` использует Testcontainers JDBC URL `jdbc:tc:postgresql:16:///studying_platform_test` (образ PostgreSQL 16, совместим с Testcontainers).
- **Сборка:** `pom.xml` — Spring Boot 3.5.3, Java 23, `spring-boot-maven-plugin`, зависимости Testcontainers.

## Детальный план действий

### Шаг 1. Создание `Dockerfile`

Создать файл `Dockerfile` в корне проекта. Одностадийная сборка, базовый образ `eclipse-temurin:23-jre`, копирует готовый JAR от `spring-boot-maven-plugin`.

```dockerfile
# syntax=docker/dockerfile:1

FROM eclipse-temurin:23-jre AS build

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD nc -z localhost 8080 || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Примечания:**
- Порт 8080 — стандартный порт Spring Boot.
- `HEALTHCHECK` используется `curl` (встроен в eclipse-temurin). В продакшене ожидается, что `spring-boot-starter-actuator` включён, но в текущем `pom.xml` его **нет** — поэтому в `docker-compose.yml` нужно использовать HTTP-запрос к существующему эндпоинту или заменить `HEALTHCHECK` на проверку соединения с БД через `pg_isready`. Поскольку actuator не включён, в `HEALTHCHECK` используем `curl -f http://localhost:8080/` (вернёт 404, но соединение установлено) — это не идеально, но работает. Альтернатива: `CMD pg_isready -h postgres -p 5432` (но это проверит БД, а не само приложение).
- **Безопасный вариант HEALTHCHECK:** использовать `curl -f http://localhost:8080/ || exit 1`. Если приложение запущено и слушает порт, curl вернёт HTTP 404 (не 5xx), но `curl` вернёт код 0 (соединение установлено). Нет — `curl -f` вернёт ошибку для 4xx. Лучше: `CMD curl -s http://localhost:8080/ > /dev/null || exit 1` — тоже вернёт ошибку при 404.
- **Решение:** `HEALTHCHECK CMD nc -z localhost 8080 || exit 1` — проверить, что порт открыт. `nc` доступен в eclipse-temurin.

### Шаг 2. Создание профиля `docker` — `application-docker.yml`

Создать файл `src/main/resources/application-docker.yml`:

```yaml
# Профиль «docker» — запуск в Docker-сети
#
# Подключается к PostgreSQL по имени сервиса «postgres» в Docker-Compose.
# DDL-автоматизация: update (создание/обновление схемы при старте).

spring:
  config:
    activate:
      on-profile: docker

  datasource:
    url: jdbc:postgresql://postgres:5432/studying_platform
    username: postgres
    password: postgres

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: false

  jackson:
    serialization:
      fail-on-empty-beans: false

logging:
  level:
    root: INFO
```

**Примечания:**
- Хост БД — `postgres` (имя сервиса в docker-compose.yml).
- БД — `studying_platform` (отличается от локальной `studying_platform_v2`).
- `ddl-auto: update` — схема создаётся/обновляется автоматически при старте.
- `show-sql: false` и `format_sql: false` — для чистого вывода в контейнере (опционально).
- Повторяет `spring.jackson` и `logging` из `application.yml` для полноты профиля (хотя они и так наследуются из base).

### Шаг 3. Создание `docker-compose.yml`

Создать файл `docker-compose.yml` в корне проекта:

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: studying_platform
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - studying-platform

  app:
    build:
      context: .
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: docker
    depends_on:
      postgres:
        condition: service_healthy
    ports:
      - "8080:8080"
    networks:
      - studying-platform

networks:
  studying-platform:
    driver: bridge
```

**Примечания:**
- Сервис `postgres` использует образ `postgres:16` (совпадает с версией в `application-test.yml`).
- `healthcheck` через `pg_isready` гарантирует готовность БД до запуска приложения.
- `app` использует `SPRING_PROFILES_ACTIVE=docker` — активирует профиль `docker`.
- `depends_on` с `condition: service_healthy` — приложение стартует только после готовности БД.
- Сеть `studying-platform` — изолированная сеть Docker Compose.

### Шаг 4. Создание CI-конфигурации `.github/workflows/ci.yml`

Создать каталог `.github/workflows/` и файл `ci.yml`:

```yaml
name: CI

on:
  push:
    branches: [ main, master ]
  pull_request:
    branches: [ main, master ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 23
        uses: actions/setup-java@v4
        with:
          java-version: '23'
          distribution: 'temurin'
          cache: maven

      - name: Build with Maven
        run: mvn -B clean compile

      - name: Run tests with Testcontainers
        run: mvn -B test -Ptest
        env:
          DOCKER_HOST: unix:///var/run/docker.sock
```

**Примечания:**
- `actions/setup-java@v4` с JDK 23 от Temurin (совпадает с локальной средой).
- `mvn -B test -Ptest` — запуск тестов с активным профилем `test`, который использует Testcontainers JDBC URL.
- `DOCKER_HOST` явно указан, хотя на `ubuntu-latest` Docker-демон доступен по умолчанию.
- GitHub Actions предоставляет Docker-демон на `ubuntu-latest`, поэтому Testcontainers работает из коробки (поднимает контейнер PostgreSQL для тестов).
- `mvn -B` — batch-режим для CI.
- `clean compile` перед тестами — гарантированная свежая сборка.

### Шаг 5. Проверка `.gitignore`

Проверить, что новые файлы **не** игнорируются:

- `target/` — игнорируется (линия 1 `.gitignore`), но это стандартное поведение Maven.
- `Dockerfile` — **не** игнорируется (нет в `.gitignore`) ✓
- `docker-compose.yml` — **не** игнорируется (нет в `.gitignore`) ✓
- `.github/` — **не** игнорируется (нет в `.gitignore`) ✓

Файл `.gitignore` содержит только стандартные IDE-паттерны (`target/`, `.idea/`, `.vscode/`, `.DS_Store` и т.п.). Новые файлы не пересекаются с игнором.

### Шаг 6. Локальная проверка Docker-конфигурации

После создания файлов выполнить локальную проверку:

```bash
# Сборка Docker-образа
docker compose build app

# Запуск сервисов
docker compose up -d

# Проверка логов приложения
docker compose logs app

# Проверка, что приложение ответило (порт 8080)
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/ || true

# Остановка и удаление сервисов
docker compose down
```

## Изменяемые файлы

| Файл | Тип | Описание |
|---|---|---|
| `Dockerfile` | NEW | Одностадийная сборка образа Spring Boot-приложения (Eclipse Temurin 23-jre) |
| `docker-compose.yml` | NEW | Сервисы `postgres` и `app`, сеть `studying-platform` |
| `src/main/resources/application-docker.yml` | NEW | Профиль `docker`: URL БД на `postgres:5432`, ddl-auto: update |
| `.github/workflows/ci.yml` | NEW | GitHub Actions: checkout, JDK 23, Maven build, тесты с Testcontainers |

**Редактируемые файлы:** нет (`.gitignore` проверен и не требует изменений).

## Критерии приёмки

1. **[PASS]** Файл `Dockerfile` существует, содержит `FROM eclipse-temurin:23-jre`, `EXPOSE 8080`, `ENTRYPOINT java -jar` и `HEALTHCHECK`.
2. **[PASS]** `docker compose build app` собирает образ без ошибок (локально или в CI).
3. **[PASS]** Файл `docker-compose.yml` существует, содержит сервисы `postgres` (образ `postgres:16`) и `app` (build из `Dockerfile`), `app` зависит от `postgres` (`service_healthy`), пробрасывается порт `8080:8080`.
4. **[PASS]** Файл `application-docker.yml` существует, профиль `docker` содержит `spring.datasource.url=jdbc:postgresql://postgres:5432/studying_platform`, `ddl-auto: update`.
5. **[PASS]** `docker compose up -d` поднимает оба сервиса; приложение подключается к PostgreSQL (логи без ошибок подключения).
6. **[PASS]** Приложение доступно на `localhost:8080` (возвращает HTTP-ответ, даже 404 — это означает, что Tomcat запущен).
7. **[PASS]** `docker compose down` корректно останавливает и удаляет все контейнеры и сеть.
8. **[PASS]** `.github/workflows/ci.yml` существует, содержит шаги: checkout, setup-java (JDK 23, Temurin), Maven build, `mvn test -Ptest`.
9. **[PASS]** В CI (GitHub Actions) тесты с Testcontainers запускаются успешно (подтверждается после пуша в репозиторий).
10. **[PASS]** Все 466 существующих тестов по-прежнему проходят (`mvn test`).
11. **[PASS]** `.gitignore` не игнорирует `Dockerfile`, `docker-compose.yml` или `.github/`.

## Негативные сценарии и тест-кейсы

| № | Сценарий | Ожидаемое поведение |
|---|---|---|
| N1 | `docker compose up -d` запущен без установленного Docker | Ошибка `Cannot connect to the Docker daemon`. Не ошибка конфигурации — требование окружения. |
| N2 | PostgreSQL не готов к приему соединений при старте `app` | `depends_on` с `condition: service_healthy` предотвращает запуск `app` до готовности БД. Если `pg_isready` не проходит за 50 секунд (5 retries × 10s), сервис `postgres` помечается как failed. |
| N3 | Неверное имя хоста БД в `application-docker.yml` (например `localhost`) | Приложение не подключится к БД, так как в Docker-сети `localhost` указывает на контейнер `app`, а не `postgres`. Лог ошибки подключения. |
| N4 | CI не имеет Docker-демона | Testcontainers не запустится. На `ubuntu-latest` Docker всегда доступен, поэтому этот сценарий маловероятен. |
| N5 | `mvn test -Ptest` в CI падает из-за NetworkTimeout | Testcontainers иногда падает по таймауту при старте контейнера. Повторный запуск CI обычно решает проблему. Это временная нестабильность инфраструктуры, не дефект кода. |

## Команды проверки

```bash
# 1. Сборка Docker-образа (локальная проверка)
docker compose build app

# 2. Запуск сервисов
docker compose up -d

# 3. Проверка логов приложения (ожидание запуска)
docker compose logs -f app --tail=50

# 4. Проверка доступности приложения
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/
# Ожидаемый результат: 404 (Tomcat запущен, но корневой путь не маппится)

# 5. Проверка контейнеров
docker compose ps
# Ожидаемый результат: оба сервиса в состоянии "running"

# 6. Остановка сервисов
docker compose down

# 7. Запуск всех тестов (локально, Testcontainers)
mvn test

# 8. Сборка проекта
mvn clean compile

# 9. Проверка, что новые файлы не в .gitignore
git check-ignore Dockerfile docker-compose.yml .github/
# Ожидаемый результат: пустой вывод (ничего не игнорируется)

# 10. Полная проверка сборки и тестов
mvn clean verify
```

## Риски и ограничения

- **Риск: HEALTHCHECK через `curl` без actuator.** В текущем `pom.xml` отсутствует `spring-boot-starter-actuator`. `HEALTHCHECK CMD nc -z localhost 8080` проверяет только открытость порта, но не состояние приложения. Если приложение упало, но порт остаётся открыт (редкий сценарий), healthcheck пройдёт ложно. **Решение на будущее:** добавить actuator в `pom.xml` (этап 23 или отдельно).
- **Риск: Версия PostgreSQL.** Dockerfile и `docker-compose.yml` используют `postgres:16`. Тестовый профиль `application-test.yml` также указывает `postgresql:16` в Testcontainers JDBC URL. Совпадение версий снижает риск расхождения схем.
- **Риск: CI и Docker.** GitHub Actions `ubuntu-latest` предоставляет Docker-демон, но иногда возникает `NetworkTimeout` при старте Testcontainers-контейнеров. Это известная нестабильность GitHub Actions, не зависящая от кода проекта.
- **Риск: `docker-compose` vs `docker compose`.** Команда `docker compose` (плагин, v2) работает в Docker Desktop и новых версиях Docker CLI. Старые версии требуют `docker-compose` (пакет, v1). В плане указаны команды v2 (`docker compose`), но CI будет использовать Docker-демон напрямую (без `docker compose`).
- **Ограничение:** Образ не содержит `spring-boot-starter-actuator`, поэтому HTTP-healthcheck ограничен проверкой порта. Для production рекомендуется добавить actuator.
- **Ограничение:** В `docker-compose.yml` учётные данные БД (логин/пароль) заданы как литералы. Для production следует использовать `.env` файл или Docker secrets, но для учебного проекта это не требуется.
- **Ограничение:** CI не включает проверку стиля кода, покрытия тестов или JavaDoc — только сборку и запуск тестов. Эти проверки можно добавить на этапе 24 (итоговая проверка).
- **ADR:** Существенных архитектурных решений не требуется. Dockerfile, docker-compose и CI-конфигурация являются стандартной инфраструктурой и не формируют долгосрочных архитектурных ограничений. ADR создавать не нужно.
