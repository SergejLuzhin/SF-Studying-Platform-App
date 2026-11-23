# Checkpoint

## Текущий этап

Следующий этап — 11 «Проверка работ» (`pending`).

## Последние изменения

- Реализована сущность `Submission` (Решение) с полями: id, assignment (ManyToOne LAZY), student (ManyToOne LAZY), submittedAt, content (TEXT без @Lob), score (Integer, nullable), feedback (String, nullable).
- Добавлен составной уникальный constraint `UNIQUE(assignment_id, student_id)` на уровне БД.
- Добавлена связь `@OneToMany submissions` в `Assignment` (fetch = LAZY, без каскадов).
- Создан `SubmissionRepository` с методами: `findByAssignmentId`, `findByStudentId`, `findByAssignmentIdAndStudentId`.
- Создан `SubmissionService.submitSolution()` с проверками: существование задания, запись студента на курс (ACTIVE), отсутствие повторного решения.
- Созданы 17 интеграционных тестов `SubmissionTests` (7 позитивных, 10 негативных).
- Обновлён `PostgresSchemaTests`: добавлена таблица `submissions`, PK {id}, 2 FK, UNIQUE {assignment_id, student_id}.
- Обновлён `TestDataCleanupHelper`: добавлена поддержка удаления `Submission` (первый шаг очистки).
- Обновлены все вызовы `clearTestEntities` в существующих тестах.

## Проверки

- **PASS:** компиляция проекта и тестов (`mvn compile test-compile`).
- **PASS:** `git diff --check` — без ошибок форматирования.
- **PASS:** полный прогон всех тестов: 182 теста, 0 ошибок, 0 пропусков.
- **PASS:** `SubmissionTests` — 17 тестов, 0 ошибок.
- **PASS:** `PostgresSchemaTests` — 16 тестов, 0 ошибок.
- Для повторного запуска на отдельной базе: `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn verify -Dspring.datasource.url=jdbc:postgresql://localhost:5432/platform_test`. Без этого параметра тестовый профиль использует Testcontainers. Тестовая схема работает с `create-drop`; важные данные в ней хранить нельзя.

## База данных

Использовать только `studying_platform_v2` для приложения и `platform_test` либо Testcontainers для тестов. Миграция прежних данных не требуется.
