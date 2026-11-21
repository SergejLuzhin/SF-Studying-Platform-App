# Checkpoint

## Текущий этап
**Пункт 4 плана исправлений: Добавить создание пользователя через сервис** (`completed`)

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
- `stage-tester`: STATUS: PASS

### Следующий шаг:
Пункт 1 плана исправлений — перевод интеграционных тестов на PostgreSQL через Testcontainers.

---

## Выполнено — Пункт 1: PostgreSQL для всех интеграционных тестов

### Изменённые файлы:
- `pom.xml` — удалена зависимость `com.h2database:h2`
- 5 тестовых классов с `@DataJpaTest` — добавлены `@ActiveProfiles("test")`, `@AutoConfigureTestDatabase(replace = NONE)`, проверка `databaseIsPostgreSQL()`
- `StudyingPlatformApplicationTests` — добавлена проверка `databaseIsPostgreSQL()`

### Результаты: 75 PASS

### Следующий шаг:
Пункт 2 — проверить реальное создание схемы.

---

## Выполнено — Пункт 2: Проверка схемы и ограничений

### Созданный файл:
- `src/test/java/com/studying/platform/PostgresSchemaTests.java` — 9 проверок схемы через JDBC metadata

### Проверяемые элементы:
- 10 таблиц, PK (10), FK (11), уникальные ограничения (4), ddl-auto, изоляция БД

### Результаты: 84 PASS

### Следующий шаг:
Пункт 3 — привести пользователя к согласованной модели.

---

## Выполнено — Пункт 3: Привести пользователя к согласованной модели

### Изменённые файлы:
- **`src/main/java/com/studying/platform/entity/User.java`** — удалено поле `password`; поле `login` переименовано в `email` (физическая колонка `users.login` сохранена); добавлены `@NotBlank`, `@Email`, `@Size(max=50)` на email, `@NotNull` на role; конструктор `User(String email, UserRole role)`; обновлён JavaDoc
- **`src/main/java/com/studying/platform/repository/UserRepository.java`** — `findByLogin(String login)` → `findByEmail(String email)`
- **`pom.xml`** — добавлен `spring-boot-starter-validation`
- **`src/test/java/com/studying/platform/CategoryAndCourseTests.java`** — обновлены конструкторы `new User(email, role)`, `getLogin()` → `getEmail()`, исключения `PropertyValueException` → `ConstraintViolationException` для PostgreSQL
- **`src/test/java/com/studying/platform/UserAndProfileTests.java`** — обновлены конструкторы, `findByLogin` → `findByEmail`, `getLogin()` → `getEmail()`, `getPassword()` удалён; добавлены тесты flush/clear/reload; 7 новых тестов валидации (null/empty/whitespace/invalid/long email, null role)
- **`src/test/java/com/studying/platform/ModuleAndLessonTests.java`** — обновлён `createTeacher(email)` helper; исключения `PropertyValueException` → `ConstraintViolationException`
- **`src/test/java/com/studying/platform/EnrollmentTests.java`** — обновлены все ~25 вызовов `new User(email, role)`
- **`src/test/java/com/studying/platform/CourseManagementTests.java`** — обновлены `createTeacher(email)` и `createStudent(email)` helpers

### Результаты:
- `mvn test -Dtest=UserAndProfileTests`: 20 PASS — PASS
- `mvn verify`: **92 PASS**, 0 FAIL — PASS
- `git diff --check`: **PASS**

### Блокер существующей базы:
Колонка `users.password NOT NULL` сохранится при `ddl-auto=update` — вставка без пароля в старую схему завершится ошибкой. Тесты используют отдельную контейнерную БД (`create-drop`), поэтому не затронуты. Для использования старой рабочей БД требуется согласованная миграция.

### Следующий шаг:
Пункт 4 — добавить `UserService` с конструкторным внедрением и транзакционным методом `createUser`.

---

## Выполнено — Пункт 4: Добавить создание пользователя через сервис

### Созданные файлы:
- **`src/main/java/com/studying/platform/service/UserService.java`** — сервисный слой с методами:
  - `createUser(email, role)` — создание пользователя с валидацией через Jakarta Bean Validation, проверкой дубликата email и сохранением через `saveAndFlush`
  - `getUser(userId)` — чтение пользователя по ID с выбросом `ResourceNotFoundException` при отсутствии
- **`src/test/java/com/studying/platform/UserServiceTests.java`** — 15 интеграционных тестов через `@SpringBootTest`:
  - Позитивные: создание STUDENT, создание TEACHER, чтение существующего, чтение вне транзакции, email макс. длины (50 символов)
  - Валидация: null email, пустой email, пробельный email, некорректный формат, email > 50 символов, null role — все бросают `ConstraintViolationException`
  - Дубликат: повторный email бросает `IllegalStateException`
  - Отсутствие: `getUser(99999L)` бросает `ResourceNotFoundException`
  - Интеграция: преподаватель из UserService используется для создания курса, студент из UserService записывается через EnrollmentService

### Результаты:
- `mvn test -Dtest=UserServiceTests`: **15 PASS** — PASS
- `mvn verify`: **107 PASS**, 0 FAIL — PASS
- `git diff --check`: **PASS**

### Следующий шаг:
Пункт 5 — исправить списки курсов и студентов вне транзакции.
