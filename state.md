# Checkpoint

## Текущее состояние

Этап 17 «API курсов и содержания» завершён. Реализованы REST-контроллеры для курсов, модулей и уроков, соответствующие DTO, методы сервиса для CRUD операций модулей и уроков.

## Проверки

- **PASS:** `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn test` — 354 теста, ошибок и пропусков нет (324 существующих + 30 новых для этапов 16-17).
- **PASS:** `git diff --check` — без проблем форматирования.

## Изменённые и новые файлы

**Новые контроллеры (3):**
- `src/main/java/com/studying/platform/controller/CourseController.java` — CRUD курсов, структура, добавление модулей.
- `src/main/java/com/studying/platform/controller/ModuleController.java` — CRUD модулей.
- `src/main/java/com/studying/platform/controller/LessonController.java` — CRUD уроков, создание урока в модуле.

**Новые DTO (8):**
- `src/main/java/com/studying/platform/dto/CourseRequestDto.java` — создание/обновление курса.
- `src/main/java/com/studying/platform/dto/CourseResponseDto.java` — ответ с данными курса.
- `src/main/java/com/studying/platform/dto/CourseStructureResponseDto.java` — вложенная структура курса с модулями и уроками.
- `src/main/java/com/studying/platform/dto/ModuleRequestDto.java` — создание/обновление модуля.
- `src/main/java/com/studying/platform/dto/ModuleResponseDto.java` — ответ с данными модуля.
- `src/main/java/com/studying/platform/dto/LessonRequestDto.java` — создание/обновление урока.
- `src/main/java/com/studying/platform/dto/LessonResponseDto.java` — ответ с данными урока.

**Изменённые файлы (3):**
- `src/main/java/com/studying/platform/service/CourseService.java` — добавлены методы: `findCourse`, `findModule`, `findLesson`, `updateModule`, `deleteModule`, `updateLesson`, `deleteLesson`.
- `src/main/java/com/studying/platform/exception/GlobalExceptionHandler.java` — добавлен обработчик `IllegalArgumentException` → 400 (для валидации данных, например duration < 1).
- `src/test/java/com/studying/platform/RestApiTests.java` — добавлены 30 интеграционных HTTP-тестов (позитивные, негативные сценарии, полный цикл), обновлён `cleanUp()` для корректного удаления модулей/уроков.

## Ключевые решения

- Эндпоинт создания урока: `POST /api/lessons/modules/{moduleId}/lessons` — вложенный ресурс.
- Структура курса: `GET /api/courses/{id}/structure` — возвращает вложенный JSON с модулями и уроками.
- `IllegalArgumentException` (некорректные данные, например duration=0) обрабатывается как 400 Bad Request (新增 обработчик в GlobalExceptionHandler).
- Порядок очистки в `cleanUp()`: уроки → модули → записи → курсы → категории → пользователи.

## Открытые вопросы и следующий шаг

Следующий этап — 18 «API записи и заданий» (`pending`); перед выполнением подготовить его подробный план по правилам проекта.
