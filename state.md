# Checkpoint

## Текущий этап
**Пункт 2 плана исправлений: Проверить реальное создание схемы** (`completed`)

## Завершённый этап — Этап 08: Запись на курс (`completed`)

### Готовое:
- Обновлён `User` (`com.studying.platform.entity`) — добавлена связь `@OneToMany(mappedBy = "user", fetch = FetchType.LAZY) Set<Enrollment> enrollments` с JavaDoc, getter/setter.
- Создан `EnrollmentRepository` (`com.studying.platform.repository`) — `JpaRepository<Enrollment, Long>` с 4 кастомными методами: `findByUserIdAndCourseIdAndStatus`, `findByUserIdAndStatus`, `findByCourseIdAndStatus`, `countByCourseIdAndStatus`.
- Создан `EnrollmentService` (`com.studying.platform.service`) — сервисный слой с методами:
  - `enrollStudent(studentId, courseId)` — запись студента на курс (валидация роли STUDENT, проверка отсутствия активной записи)
  - `dropEnrollment(studentId, courseId)` — отписка (смена статуса ACTIVE → DROPPED)
  - `getStudentCourses(studentId)` — получение активных курсов студента
  - `getCourseStudents(courseId)` — получение активных студентов курса
  - `getEnrollmentStatus(studentId, courseId)` — получение статуса записи
- Созданы тесты `EnrollmentTests` — 11 интеграционных тестов (позитивные + негативные сценарии).

### Затронутые файлы:
- `src/main/java/com/studying/platform/entity/User.java` (изменён)
- `src/main/java/com/studying/platform/repository/EnrollmentRepository.java` (новый)
- `src/main/java/com/studying/platform/service/EnrollmentService.java` (новый)
- `src/test/java/com/studying/platform/EnrollmentTests.java` (новый)
- `stages/stage-08.md` (план этапа)

### Результаты проверок:
- `git diff --check`: PASS (нет проблем с форматированием)
- `mvn test`: 69 тестов PASS, 0 FAIL, 0 ERROR
  - EnrollmentTests: 11 PASS (новые)
  - CategoryAndCourseTests: 15 PASS
  - ModuleAndLessonTests: 16 PASS
  - CourseManagementTests: 10 PASS
  - UserAndProfileTests: 11 PASS
  - StudyingPlatformApplicationTests: 6 PASS
- `stage-tester`: STATUS: PASS

### Следующий шаг:
Пункт 1 плана исправлений — перевод интеграционных тестов на PostgreSQL через Testcontainers.

---

## Выполнено — Пункт 1: PostgreSQL для всех интеграционных тестов

### Изменённые файлы:
- `pom.xml` — удалена зависимость `com.h2database:h2`
- `src/test/java/com/studying/platform/CategoryAndCourseTests.java` — добавлены `@ActiveProfiles("test")`, `@AutoConfigureTestDatabase(replace = NONE)`, проверка `databaseIsPostgreSQL()`
- `src/test/java/com/studying/platform/UserAndProfileTests.java` — добавлены `@ActiveProfiles("test")`, `@AutoConfigureTestDatabase(replace = NONE)`, проверка `databaseIsPostgreSQL()`
- `src/test/java/com/studying/platform/ModuleAndLessonTests.java` — добавлены `@ActiveProfiles("test")`, `@AutoConfigureTestDatabase(replace = NONE)`, проверка `databaseIsPostgreSQL()`
- `src/test/java/com/studying/platform/CourseManagementTests.java` — добавлены `@ActiveProfiles("test")`, `@AutoConfigureTestDatabase(replace = NONE)`, проверка `databaseIsPostgreSQL()`
- `src/test/java/com/studying/platform/EnrollmentTests.java` — добавлены `@ActiveProfiles("test")`, `@AutoConfigureTestDatabase(replace = NONE)`, проверка `databaseIsPostgreSQL()`
- `src/test/java/com/studying/platform/StudyingPlatformApplicationTests.java` — добавлена проверка `databaseIsPostgreSQL()`

### Результаты проверок:
- `mvn test -Dtest=StudyingPlatformApplicationTests,UserAndProfileTests`: 19 PASS, 0 FAIL — PASS
- `mvn verify`: 75 PASS, 0 FAIL, 0 ERROR — PASS

### Следующий шаг:
Пункт 2 — проверить реальное создание схемы.

---

## Выполнено — Пункт 2: Проверка схемы и ограничений

### Созданный файл:
- `src/test/java/com/studying/platform/PostgresSchemaTests.java` — новый тестовый класс с 9 проверками схемы

### Проверяемые элементы:
- **Соединение**: `SELECT 1`, активный профиль `test`, продукт БД — PostgreSQL
- **Таблицы**: users, profiles, categories, courses, modules, lessons, enrollments, tags, course_reviews, course_tag
- **Первичные ключи**: одиночный `id` для 9 таблиц; составной `(course_id, tag_id)` для course_tag
- **Внешние ключи**: 11 FK (profiles→users, courses→users, courses→categories, modules→courses, lessons→modules, enrollments→users, enrollments→courses, course_reviews→courses, course_reviews→users, course_tag→courses, course_tag→tags)
- **Уникальные ограничения**: users.login, profiles.user_id, enrollments(user_id+course_id), tags.name
- **Конфигурация**: `ddl-auto=create-drop` в тесте, изоляция тестовой БД от рабочей

### Результаты проверок:
- `mvn test -Dtest=PostgresSchemaTests`: 9 PASS, 0 FAIL — PASS
- `mvn verify`: 84 PASS, 0 FAIL, 0 ERROR — PASS
- `git diff --check`: PASS

### Следующий шаг:
Пункт 3 — привести пользователя к согласованной модели (удалить пароль, добавить email).
