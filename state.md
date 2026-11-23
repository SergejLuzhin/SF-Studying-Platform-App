# Checkpoint

## Текущий этап

Следующий этап — 16 «Основа REST API» (`pending`).

## Последние изменения

### Этап 15. Удаление и целостность (completed)

- Добавлены методы подсчёта в репозитории:
  - `SubmissionRepository` — `countByAssignmentIdIn(Collection<Long>)` для проверки наличия решений по списку заданий.
  - `QuizSubmissionRepository` — `countByQuizIdIn(Collection<Long>)` для проверки наличия результатов тестов по списку викторин.
  - `CourseRepository` — `countEnrollmentsByCourseId(Long)` и `countReviewsByCourseId(Long)` для проверки записей и отзывов.
  - `EnrollmentRepository` — `countByCourseId(Long)` для подсчёта всех записей на курс (независимо от статуса).
  - `CourseReviewRepository` — `countByCourseId(Long)` для подсчёта отзывов курса.
- В `CourseService` добавлен метод `deleteCourse(Long)` с полной проверкой целостности:
  - Собираются ID заданий из всех модулей/уроков и ID викторин из модулей.
  - Проверяются: наличие решений, результатов тестов, записей (ACTIVE и DROPPED), отзывов.
  - При наличии любой учебной истории — выбрасывается `IllegalStateException`.
  - При отсутствии истории — курс удаляется каскадом (модули и уроки удаляются через `orphanRemoval`).
  - Преподаватель, категория и теги не затрагиваются.
- Обновлён конструктор `CourseService` (8 → 13 параметров).
- Обновлён `CourseManagementTests.java` — добавлены новые репозитории в `setUp()`.
- Создан `CourseDeletionTests.java` — 12 интеграционных тестов (3 позитивных, 6 негативных, 3 на защиту общих объектов).

## Проверки

- **PASS:** компиляция проекта и тестов (`mvn compile test-compile`).
- **PASS:** `git diff --check` — без ошибок форматирования.
- **PASS:** полный прогон всех тестов: 263 теста, 0 ошибок, 0 пропусков.
  - `CourseDeletionTests` — 12 тестов, все прошли.
  - `CourseManagementTests` — обновлены зависимости, все прошли.
  - Остальные существующие тесты — без изменений, прошли.

## База данных

Использовать только `studying_platform_v2` для приложения и `platform_test` либо Testcontainers для тестов. Миграция прежних данных не требуется.
