# Checkpoint

## Текущий этап

Следующий этап — 12 «Структура тестов» (`pending`).

## Последние изменения

### Этап 11. Проверка работ (completed)

- Добавлено поле `UserRepository` в `SubmissionService` и обновлён конструктор.
- Реализован метод `gradeSubmission(submissionId, score, feedback, teacherId)`:
  - Проверка существования решения и преподавателя.
  - Валидация диапазона: `0 ≤ score ≤ maxScore` задания (при `maxScore == null` — только `score ≥ 0`).
  - Обновление полей `score` и `feedback` решения.
- Реализованы методы получения списков:
  - `getSubmissionsByAssignment(assignmentId)` — с проверкой существования задания.
  - `getSubmissionsByStudent(studentId)` — с проверкой существования студента.
- Добавлено 13 интеграционных тестов в `SubmissionTests.java` (5 позитивных + 2 выборки списков + 6 негативных сценариев).

## Проверки

- **PASS:** компиляция проекта и тестов (`mvn compile test-compile`).
- **PASS:** `git diff --check` — без ошибок форматирования.
- **PASS:** полный прогон всех тестов: 195 тестов, 0 ошибок, 0 пропусков.
- **PASS:** независимая проверка этапа 11 субагентом `stage-tester`.
- Для повторного запуска на отдельной базе: `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn verify -Dspring.datasource.url=jdbc:postgresql://localhost:5432/platform_test`. Без этого параметра тестовый профиль использует Testcontainers. Тестовая схема работает с `create-drop`; важные данные в ней хранить нельзя.

## База данных

Использовать только `studying_platform_v2` для приложения и `platform_test` либо Testcontainers для тестов. Миграция прежних данных не требуется.
