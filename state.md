# Текущее состояние проекта

## Текущий этап
Этап 07 — не начат.

## Выполненные этапы
- **Этап 01 (Проектирование модели):** completed. Создан `docs/data-model.md` с описанием 15 JPA-сущностей, связей, каскадов, стратегий загрузки и защиты учебной истории.
- **Этап 02 (Основа Spring Boot):** completed. Настроен Maven (Spring Boot 3.5.3, Java 23), созданы точка входа, профили `dev`/`test`, настроена конфигурация БД с переменными окружения.
- **Этап 03 (Инфраструктура интеграционных тестов):** completed. Расширен `StudyingPlatformApplicationTests.java`: 6 тестовых методов проверяют загрузку Spring-контекста с профилем `test`, наличие DataSource и EntityManagerFactory, живое соединение с PostgreSQL через Testcontainers (`SELECT 1`), активный профиль, изоляцию тестовой базы от рабочей и значение `ddl-auto: create-drop`. Docker поднимает PostgreSQL 16.16 автоматически.
- **Этап 04 (Пользователи и профили):** completed. Созданы сущности `User`, `Profile`, `UserRole`, репозитории `UserRepository` и `ProfileRepository`, связь 1:1 (владелец — Profile, LAZY). 11 интеграционных тестов (позитивные + негативные сценарии).
- **Этап 05 (Категории и курсы):** completed. Созданы сущности `Category`, `Course` (с обратными связями `modules`, `enrollments`, `reviews`, `tags`), `Enrollment`, `CourseReview`, `Tag`, `Module`, `EnrollmentStatus`, репозитории `CategoryRepository` и `CourseRepository`. 15 интеграционных тестов (позитивные + негативные сценарии).
- **Этап 06 (Модули и уроки):** completed. Создана сущность `Lesson`, добавлена коллекция `lessons` в `Module`, созданы репозитории `ModuleRepository` и `LessonRepository`, 16 интеграционных тестов (позитивные + негативные сценарии).

## Блокирующие проблемы
Нет.

## Затронутые файлы (Этап 06)
- `src/main/java/com/studying/platform/entity/Lesson.java` — создание (сущность Lesson с полями id, title, content, videoUrl, module @ManyToOne LAZY).
- `src/main/java/com/studying/platform/entity/Module.java` — изменение (добавлено поле lessons Set<Lesson> с @OneToMany LAZY, cascade PERSIST/MERGE/REMOVE, orphanRemoval=true; getter/setter; обновлён JavaDoc класса).
- `src/main/java/com/studying/platform/repository/ModuleRepository.java` — создание (JpaRepository<Module, Long> + findByCourseIdAndOrderIndex).
- `src/main/java/com/studying/platform/repository/LessonRepository.java` — создание (JpaRepository<Lesson, Long> + findByModuleId).
- `src/test/java/com/studying/platform/ModuleAndLessonTests.java` — создание (16 интеграционных тестов: сохранение модуля/урока, каскадное сохранение, каскадное удаление курса/модуля, orphanRemoval, негативные сценарии null, чтение структуры, обновление).
- `stages/stage-06.md` — создание детального плана этапа.
- `plan.md` — статус Этапа 06 обновлён на `completed`.

## Ключевые решения
- `Module.lessons` — @OneToMany с `orphanRemoval=true` и каскадами PERSIST/MERGE/REMOVE. Полностью аналогично `Course.modules`.
- `Lesson.module` — @ManyToOne(fetch = LAZY) с явно указанным LAZY (по стандарту JPA ManyToOne EAGER, но проект стремится к LAZY).
- `Lesson.title` — nullable = false, length = 200; `content` и `videoUrl` — nullable.
- `ModuleRepository.findByCourseIdAndOrderIndex` возвращает `List<Module>` (может быть несколько модулей с одинаковым orderIndex).
- `LessonRepository.findByModuleId` возвращает `List<Lesson>` — уроки модуля.
- Все тесты используют `@DataJpaTest` + `EntityManager` (H2 embedded), как и тесты этапов 04–05.

## Проверки (Этап 06)
- `mvn clean compile -q`: PASS
- `mvn test -q`: PASS (Tests run: 48, Failures: 0, Errors: 0, Skipped: 0)
  - `ModuleAndLessonTests`: 16 тестов (PASS)
  - `CategoryAndCourseTests`: 15 тестов (PASS)
  - `UserAndProfileTests`: 11 тестов (PASS)
  - `StudyingPlatformApplicationTests`: 6 тестов (PASS)
- `git diff --check`: PASS (нет проблем форматирования)
- Проверка JavaDoc на русском: PASS
- Проверка английских идентификаторов: PASS
- Критерии приёмки (plan.md): все 11 — PASS

## Следующее действие
Начать выполнение Этапа 07 (Управление курсом) из plan.md — сервисы создания и изменения курса, добавления модулей и уроков, получения полной структуры.
