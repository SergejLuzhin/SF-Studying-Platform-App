# Текущее состояние проекта

## Текущий этап
Этап 05 — не начат.

## Выполненные этапы
- **Этап 01 (Проектирование модели):** completed. Создан `docs/data-model.md` с описанием 15 JPA-сущностей, связей, каскадов, стратегий загрузки и защиты учебной истории.
- **Этап 02 (Основа Spring Boot):** completed. Настроен Maven (Spring Boot 3.5.3, Java 23), созданы точка входа, профили `dev`/`test`, настроена конфигурация БД с переменными окружения.
- **Этап 03 (Инфраструктура интеграционных тестов):** completed. Расширен `StudyingPlatformApplicationTests.java`: 6 тестовых методов проверяют загрузку Spring-контекста с профилем `test`, наличие DataSource и EntityManagerFactory, живое соединение с PostgreSQL через Testcontainers (`SELECT 1`), активный профиль, изоляцию тестовой базы от рабочей и значение `ddl-auto: create-drop`. Docker поднимает PostgreSQL 16.16 автоматически.
- **Этап 04 (Пользователи и профили):** completed. Созданы сущности `User`, `Profile`, `UserRole`, репозитории `UserRepository` и `ProfileRepository`, связь 1:1 (владелец — Profile, LAZY). 11 интеграционных тестов (позитивные + негативные сценарии).

## Блокирующие проблемы
Нет.

## Затронутые файлы (Этап 04)
- `src/main/java/com/studying/platform/entity/UserRole.java` — создание (enum ролей: STUDENT, TEACHER, ADMIN).
- `src/main/java/com/studying/platform/entity/User.java` — создание (сущность User с полями id, login, password, role, profile).
- `src/main/java/com/studying/platform/entity/Profile.java` — создание (сущность Profile с полями id, displayName, bio, avatarUrl, user; владелец связи 1:1).
- `src/main/java/com/studying/platform/repository/UserRepository.java` — создание (JpaRepository + findByLogin).
- `src/main/java/com/studying/platform/repository/ProfileRepository.java` — создание (JpaRepository + findByUserId).
- `src/test/java/com/studying/platform/UserAndProfileTests.java` — создание (11 интеграционных тестов: сохранение, чтение, поиск, уникальность, валидация, обновление, удаление).
- `stages/stage-04.md` — создание детального плана этапа.
- `plan.md` — статус Этапа 04 обновлён на `completed`.

## Ключевые решения
- Сущности размещены в пакетах `com.studying.platform.entity` и `com.studying.platform.repository`.
- Связь 1:1 между User и Profile: владелец — `Profile` (содержит `@JoinColumn(user_id, unique=true, nullable=false)`), обратная сторона — `User` (`mappedBy="user"`).
- `@OneToOne(fetch = FetchType.LAZY)` на обеих сторонах для демонстрации ленивой загрузки.
- Тесты используют `@DataJpaTest` (H2) для быстрых проверок; тестовая PostgreSQL через Testcontainers также доступна через существующий `StudyingPlatformApplicationTests`.

## Проверки (Этап 04)
- `mvn clean compile -q`: PASS
- `mvn test -q`: PASS (Tests run: 17, Failures: 0, Errors: 0, Skipped: 0)
  - `UserAndProfileTests`: 11 тестов (PASS)
  - `StudyingPlatformApplicationTests`: 6 тестов (PASS)
- `git diff --check`: PASS (нет проблем форматирования)
- Проверка JavaDoc на русском: PASS
- Проверка английских идентификаторов: PASS
- Проверка схемы БД (таблицы users, profiles, внешние ключи, уникальность): PASS
- Критерии приёмки (plan.md): PASS

## Следующее действие
Начать выполнение Этапа 05 (Категории и курсы) из plan.md.
