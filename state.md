# Checkpoint

## Текущий этап

Следующий этап — 13 «Прохождение тестов» (`pending`).

## Последние изменения

### Этап 12. Структура тестов (completed)

- Созданы сущности JPA: `Quiz`, `Question`, `AnswerOption`.
  - `Quiz` — `@OneToOne` к `Module` (уникально, владелец связи), `@OneToMany` на `Question` (PERSIST, LAZY).
  - `Question` — `@ManyToOne` к `Quiz`, `@OneToMany` на `AnswerOption` (PERSIST, LAZY). Поддержка `SINGLE_CHOICE` и `MULTIPLE_CHOICE` (несколько правильных вариантов).
  - `AnswerOption` — `@ManyToOne` к `Question`. Поле `isCorrect` определяет правильность.
- Создан enum `QuizQuestionType` (`SINGLE_CHOICE`, `MULTIPLE_CHOICE`).
- Созданы репозитории: `QuizRepository` (с методом `findByModuleId`), `QuestionRepository`, `AnswerOptionRepository`.
- Добавлены интеграционные тесты в `QuizStructureTest.java` (14 тестов): каскадное сохранение, поиск по ID модуля/викторины/вопроса, типы вопросов, ограничения уникальности и NOT NULL.
- Обновлены `PostgresSchemaTests`: добавлены новые таблицы (`quiz`, `question`, `answeroption`), внешние ключи и уникальные ограничения для `quiz.module_id`.

## Проверки

- **PASS:** компиляция проекта и тестов (`mvn clean compile test-compile`).
- **PASS:** `git diff --check` — без ошибок форматирования.
- **PASS:** полный прогон всех тестов: 209 тестов, 0 ошибок, 0 пропусков.

## База данных

Использовать только `studying_platform_v2` для приложения и `platform_test` либо Testcontainers для тестов. Миграция прежних данных не требуется.
