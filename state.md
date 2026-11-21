# Текущее состояние проекта

## Текущий этап
Этап 04 — не начат.

## Выполненные этапы
- **Этап 01 (Проектирование модели):** completed. Создан `docs/data-model.md` с описанием 15 JPA-сущностей, связей, каскадов, стратегий загрузки и защиты учебной истории.
- **Этап 02 (Основа Spring Boot):** completed. Настроен Maven (Spring Boot 3.5.3, Java 23), созданы точка входа, профили `dev`/`test`, настроена конфигурация БД с переменными окружения.
- **Этап 03 (Инфраструктура интеграционных тестов):** completed. Расширен `StudyingPlatformApplicationTests.java`: 6 тестовых методов проверяют загрузку Spring-контекста с профилем `test`, наличие DataSource и EntityManagerFactory, живое соединение с PostgreSQL через Testcontainers (`SELECT 1`), активный профиль, изоляцию тестовой базы от рабочей и значение `ddl-auto: create-drop`. Docker поднимает PostgreSQL 16.16 автоматически.

## Блокирующие проблемы
Нет.

## Затронутые файлы (Этап 03)
- `src/test/java/com/studying/platform/StudyingPlatformApplicationTests.java` — расширен: внедрение `EntityManagerFactory`, `Environment`, добавление 5 новых тестовых методов (всего 6), JavaDoc на русском для каждого метода.
- `plan.md` — статус Этапа 03 обновлён на `completed`.

## Ключевые решения
- Тесты используют `@Autowired` в параметрах `@Test`-методов (автовнедрение Spring 6.1+) для `DataSource`, `LocalContainerEntityManagerFactoryBean`, `Environment`.
- Свойство `ddl-auto` читается через `Environment.getProperty("spring.jpa.hibernate.ddl-auto")`.
- URL базы проверяется на отсутствие `studying_platform` (Testcontainers трансформирует `jdbc:tc:` URL, поэтому имя `studying_platform_test` не появляется в финальной строке подключения).
- ADR не создавались (решения тривиальны).

## Проверки (Этап 03)
- `mvn clean compile -q`: PASS
- `mvn test -q`: PASS (Tests run: 6, Failures: 0, Errors: 0, Skipped: 0)
- `git diff --check`: PASS (нет проблем форматирования)
- Docker/Testcontainers PostgreSQL: PASS (postgres:16.16, Ryuk запущен, HikariPool подключён)
- Проверка 6.1–6.8 (критерии приёмки): PASS (все 8 подтверждены)

## Следующее действие
Начать выполнение Этапа 04 (Пользователи и профили) из plan.md.
