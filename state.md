# Checkpoint

## Текущий этап

Следующий этап — 15 «Удаление и целостность» (`pending`).

## Последние изменения

### Этап 14. Отзывы и теги (completed)

- Создан репозиторий: `TagRepository` (extends JpaRepository<Tag, Long>).
- Дополнен репозиторий: `CourseReviewRepository` — добавлены методы `findByStudentIdAndCourseId`, `findByCourseId`, `findByStudentId`.
- Дополнен репозиторий: `CourseRepository` — добавлен JPQL-метод `countTagsByCourseId` для подсчёта тегов без загрузки lazy-коллекции.
- Дополнен `CourseService`: добавлены методы `addTagsToCourse(Set<Long>)`, `removeTagsFromCourse(Set<Long>)`; добавлена зависимость `TagRepository`.
- Создан сервис: `CourseReviewService` с методами `createReview`, `getReviewsByCourse`, `getReviewsByStudent`.
  - Реализована проверка: роль STUDENT, активная запись на курс (EnrollmentStatus.ACTIVE), рейтинг 1–5, уникальность отзыва.
- Добавлены интеграционные тесты: `CourseReviewAndTagTests.java` (19 тестов) — позитивные сценарии (создание отзывов, чтение по курсу/студенту, привязка/отвязка тегов) и негативные (несуществующие ID, неверная роль, отсутствие записи, дублирование, некорректный рейтинг).
- Исправлен `CourseManagementTests.java` — добавлена зависимость `TagRepository` в конструктор `CourseService`.

## Проверки

- **PASS:** компиляция проекта и тестов (`mvn clean compile test-compile`).
- **PASS:** `git diff --check` — без ошибок форматирования.
- **PASS:** полный прогон всех тестов: 251 тест, 0 ошибок, 0 пропусков.

## База данных

Использовать только `studying_platform_v2` для приложения и `platform_test` либо Testcontainers для тестов. Миграция прежних данных не требуется.
