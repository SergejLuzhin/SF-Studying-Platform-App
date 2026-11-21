# Checkpoint

## Текущий этап
**Этап 08: Запись на курс** (`pending`)

## Завершённый этап — Этап 07: Управление курсом (`completed`)

### Готовое:
- Класс `ResourceNotFoundException` (`com.studying.platform.exception`) — кастомное исключение для отсутствующих сущностей
- Класс `CourseService` (`com.studying.platform.service`) — сервисный слой с методами:
  - `createCourse(...)` — создание курса с валидацией роли TEACHER и существования категории
  - `updateCourse(...)` — обновление данных курса (частичное, null-safe)
  - `addModule(...)` — добавление модуля к курсу
  - `addLesson(...)` — добавление урока к модулю
  - `getCourseStructure(...)` — получение полной структуры курса (с инициализацией ленивых коллекций)
- Тесты `CourseManagementTests` — 10 интеграционных тестов (позитивные + негативные сценарии)

### Затронутые файлы:
- `src/main/java/com/studying/platform/exception/ResourceNotFoundException.java` (новый)
- `src/main/java/com/studying/platform/service/CourseService.java` (новый)
- `src/test/java/com/studying/platform/CourseManagementTests.java` (новый)
- `stages/stage-07.md` (план этапа)

### Результаты проверок:
- `git diff --check`: PASS (нет проблем с форматированием)
- `mvn test`: 58 тестов PASS, 0 FAIL, 0 ERROR
  - ModuleAndLessonTests: 16 PASS
  - CourseManagementTests: 10 PASS (новые)
  - CategoryAndCourseTests: 15 PASS
  - UserAndProfileTests: 11 PASS
  - StudyingPlatformApplicationTests: 6 PASS
- `stage-tester`: STATUS: PASS

### Следующий шаг:
Этап 08 — реализовать `Enrollment`, запись и отписку студента, списки студентов курса и курсов студента.
