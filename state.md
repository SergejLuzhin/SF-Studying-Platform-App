# Текущее состояние проекта

## Текущий этап
Этап 03 — не начат.

## Выполненные этапы
- **Этап 01 (Проектирование модели):** completed. Создан `docs/data-model.md` с описанием 15 JPA-сущностей, связей, каскадов, стратегий загрузки и защиты учебной истории.
- **Этап 02 (Основа Spring Boot):** completed. Настроен Maven (Spring Boot 3.5.3, Java 23), созданы точка входа, профили `dev`/`test`, настроена конфигурация БД с переменными окружения. Тесты проходят с PostgreSQL через Testcontainers.

## Блокирующие проблемы
Нет.

## Затронутые файлы (Этап 02)
- `pom.xml` — обновлён (Spring Boot 3.5.3 parent, web, jpa, postgresql, h2 (test), testcontainers/jdbc, testcontainers/postgresql, junit-jupiter (test), spring-boot-maven-plugin)
- `src/main/java/com/studying/platform/StudyingPlatformApplication.java` — создан (точка входа, JavaDoc)
- `src/main/resources/application.yml` — создан (базовая конфигурация, активный профиль)
- `src/main/resources/application-dev.yml` — создан (профиль dev, ddl-auto: update, переменные окружения)
- `src/main/resources/application-test.yml` — создан (профиль test, jdbc:tc: PostgreSQL через Testcontainers)
- `src/test/java/com/studying/platform/StudyingPlatformApplicationTests.java` — создан (SpringBootTest, JavaDoc)

## Ключевые решения
- Spring Boot 3.5.3 (совместим с Java 23)
- Профиль `dev` по умолчанию (из `${SPRING_PROFILES_ACTIVE:dev}`)
- Параметры БД вынесены в переменные окружения (`DB_URL`, `DB_USER`, `DB_PASSWORD`)
- Тесты используют PostgreSQL через Testcontainers (`jdbc:tc:` URL) — контейнер создаётся автоматически
- H2 добавлен как резервная зависимость для тестов без Docker
- ADR не создавались (решения тривиальны для этапа настройки)

## Проверки (Этап 02)
- `mvn clean compile`: PASS
- `mvn clean test`: PASS (1 тест, 0 ошибок, реальная PostgreSQL 16.16 через Testcontainers)
- `git diff --check`: PASS (нет проблем форматирования)
- Проверка профилей (dev/test): PASS
- Переменные окружения для паролей: PASS (dev: `${DB_PASSWORD:postgres}`, test: тестовые креды)
- Testcontainers PostgreSQL: PASS (контейнер поднимается автоматически)
- `ddl-auto: update` в dev: PASS (конфигурация верна)

## Следующее действие
Начать выполнение Этапа 03 (Инфраструктура интеграционных тестов) из plan.md.
