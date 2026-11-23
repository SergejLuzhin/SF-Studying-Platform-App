# Checkpoint

## Текущий этап

Следующий этап — 10 «Сдача решений» (`pending`). Перед выполнением создать подробный план `stages/stage-10.md` по правилам проекта.

## Последние изменения

- Реализована сущность `Assignment` (Задание) с полями: id, lesson (FK), title, description (TEXT без @Lob), dueDate, maxScore.
- Добавлена связь `@OneToMany` в `Lesson` для коллекции `assignments` (`fetch = LAZY`, без каскадов — безопасно для этапа 10).
- Создан `AssignmentRepository` с методом `findByLessonId(Long lessonId)`.
- В `CourseService` добавлены методы: `createAssignment`, `updateAssignment`, `deleteAssignment`, `getAssignmentsByLessonId`.
- Обновлены проверки схемы PostgreSQL: таблица `assignments`, PK `{id}`, FK `lesson_id -> lessons.id`.
- Созданы 11 интеграционных тестов `AssignmentServiceTests` (CRUD, ограничения, TEXT-поле, каскады).
- Исправлена ошибка в `CourseManagementTests` — добавлен `AssignmentRepository` в конструктор `CourseService`.
- Исправлен тест `testTextStorageViaJpaAndSql` — заменён прямой `Connection` на `JdbcTemplate` для работы в транзакции `@DataJpaTest`.

## Проверки

- **PASS:** компиляция проекта и тестов (`mvn compile test-compile`).
- **PASS:** `git diff --check` — без ошибок форматирования.
- **PASS:** полный прогон всех тестов: 165 тестов, 0 ошибок, 0 пропусков.
- **PASS:** `AssignmentServiceTests` — 11 тестов, 0 ошибок.
- **PASS:** `PostgresSchemaTests` — 16 тестов, 0 ошибок.
- Для повторного запуска на отдельной базе: `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn verify -Dspring.datasource.url=jdbc:postgresql://localhost:5432/platform_test`. Без этого параметра тестовый профиль использует Testcontainers. Тестовая схема работает с `create-drop`; важные данные в ней хранить нельзя.

## База данных

Использовать только `studying_platform_v2` для приложения и `platform_test` либо Testcontainers для тестов. Миграция прежних данных не требуется.
