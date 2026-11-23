# Checkpoint

## Текущее состояние

Этап 16 «Основа REST API» завершён. Реализованы DTO, глобальный обработчик ошибок, `CategoryService` и REST-контроллеры для пользователей и категорий.

## Проверки

- **PASS:** `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn test -Dtest=RestApiTests` — 27 тестов, ошибок и пропусков нет.
- **PASS:** `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn test` — 324 теста, ошибок и пропусков нет (297 существующих + 27 новых).
- **PASS:** `git diff --check` — без проблем форматирования.

## Изменённые и новые файлы

- `src/main/java/com/studying/platform/dto/` — 6 DTO-классов: `UserCreateDto`, `UserUpdateDto`, `UserResponseDto`, `CategoryCreateDto`, `CategoryResponseDto`, `ErrorResponseDto`.
- `src/main/java/com/studying/platform/exception/GlobalExceptionHandler.java` — глобальный обработчик ошибок (404, 400, 500).
- `src/main/java/com/studying/platform/service/CategoryService.java` — новый сервис категорий.
- `src/main/java/com/studying/platform/controller/UserController.java` — REST-контроллер пользователей.
- `src/main/java/com/studying/platform/controller/CategoryController.java` — REST-контроллер категорий.
- `src/main/java/com/studying/platform/service/UserService.java` — добавлены `createUser(UserCreateDto)` и `deleteUser(Long)`.
- `src/test/java/com/studying/platform/RestApiTests.java` — 27 интеграционных тестов REST API.

## Ключевые решения

- Формат ошибок — единый `ErrorResponseDto` с полями `timestamp`, `status`, `error`, `message`, `path`.
- `IllegalStateException` (нарушение бизнес-правил) обрабатывается как 400 Bad Request.
- `UserUpdateDto` пустой — сущность `User` не имеет обновляемых полей в текущей модели.
- ADR не создавался — использованы стандартные паттерны Spring Boot REST.

## Открытые вопросы и следующий шаг

Следующий этап — 17 «API курсов и содержания» (`pending`); перед выполнением подготовить его подробный план по правилам проекта.
