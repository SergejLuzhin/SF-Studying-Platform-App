# Checkpoint

## Текущий этап
**Пункт 7 плана исправлений: Доказать каскадное сохранение и повторное чтение** (`completed`)

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

---

## Выполнено — Пункт 5: Исправить списки курсов и студентов вне транзакции

### Изменённые файлы:
- **`src/main/java/com/studying/platform/repository/EnrollmentRepository.java`** — добавлен `@EntityGraph(attributePaths = "course")` к `findByUserIdAndStatus` и `@EntityGraph(attributePaths = "user")` к `findByCourseIdAndStatus`; обновлён JavaDoc
- **`src/main/java/com/studying/platform/service/EnrollmentService.java`** — в `getStudentCourses` и `getCourseStudents` добавлена проверка существования student/course через `existsById` с выбросом `ResourceNotFoundException`; исправлены комментарии на точные; сохранена ленивая загрузка коллекций по умолчанию
- **`src/main/java/com/studying/platform/repository/UserRepository.java`** — добавлен метод `existsById(Long id)`
- **`src/main/java/com/studying/platform/repository/CourseRepository.java`** — добавлен метод `existsById(Long id)`

### Созданный файл:
- **`src/test/java/com/studying/platform/EnrollmentServiceTransactionTests.java`** — 8 интеграционных тестов через `@SpringBootTest` с `@Transactional(propagation = Propagation.NOT_SUPPORTED)`:
  - `getStudentCourses_fieldsAccessibleOutsideTransaction` — 2 курса студента, проверка `TransactionSynchronizationManager.isActualTransactionActive() == false`, чтение `getId()` и `getTitle()`
  - `getCourseStudents_fieldsAccessibleOutsideTransaction` — 2 студента курса, проверка транзакции закрыта, чтение `getId()` и `getEmail()`
  - `getCourseStudents_filtersOutDroppedStudents` — один активный, один отписанный; возвращается только активный
  - `getStudentCourses_emptyListForExistingStudentWithoutEnrollments` — пустой список для существующего студента
  - `getCourseStudents_emptyListForExistingCourseWithoutEnrollments` — пустой список для существующего курса
  - `getStudentCourses_nonExistentStudent_shouldThrow` — `ResourceNotFoundException`
  - `getCourseStudents_nonExistentCourse_shouldThrow` — `ResourceNotFoundException`
  - `getStudentCourses_contentCorrectRegardlessOfOrder` — проверка по содержимому, а не по порядку

### Результаты:
- `mvn test -Dtest=EnrollmentServiceTransactionTests`: **8 PASS** — PASS
- `mvn verify`: **115 PASS**, 0 FAIL — PASS
- `git diff --check`: **PASS**

### Следующий шаг:
Пункт 6 — исправить повторную запись и статус после отписки.

---

## Выполнено — Пункт 6: Исправить повторную запись и статус после отписки

### Изменённые файлы:
- **`src/main/java/com/studying/platform/repository/EnrollmentRepository.java`** — добавлен метод `findByUserIdAndCourseId(Long userId, Long courseId)` для поиска любой записи независимо от статуса; обновлён JavaDoc
- **`src/main/java/com/studying/platform/service/EnrollmentService.java`** — в `enrollStudent`: проверка любой существующей записи (ACTIVE или DROPPED) вместо только ACTIVE; сообщение об ошибке изменено на «уже был записан»; в `getEnrollmentStatus`: поиск без фильтра статуса — возвращает ACTIVE, DROPPED или null; обновлён JavaDoc обоих методов
- **`src/test/java/com/studying/platform/EnrollmentTests.java`** — обновлён `getEnrollmentStatus_shouldReturnCorrectStatus`: после отписки ожидается `DROPPED` вместо `null`; добавлены 2 новых теста: `enrollStudent_afterDrop_shouldThrow` (запись → отписка → повторная запись бросает `IllegalStateException`, статус остаётся DROPPED) и `uniqueConstraint_userIdCourseId_shouldPreventDuplicate` (обход сервиса, проверка ограничения БД через `PersistenceException`)

### Результаты:
- `mvn test -Dtest=EnrollmentTests`: **14 PASS** (12 существующих + 2 новых) — PASS
- `mvn verify`: **117 PASS**, 0 FAIL — PASS
- `git diff --check`: **PASS**

### Следующий шаг:
Пункт 7 — доказать каскадное сохранение и повторное чтение.

---

## Выполнено — Пункт 7: Доказать каскадное сохранение и повторное чтение

### Изменённые файлы:
- **`src/test/java/com/studying/platform/ModuleAndLessonTests.java`** — полностью переписаны:
  - `testCascadeSaveCourseWithModulesAndLessons`: сохраняются общие категория и преподаватель, создаётся курс с 2 модулями и 3 уроками, заполняются двусторонние связи, вызывается `persist(course)` **один раз**, после `flush` и `clear` контекста загружаются курс, модули и уроки заново — проверяются количество, поля, внешние ключи
  - `testCascadeDeleteCourseRemovesModulesAndLessons`: запоминаются ID курса, 2 модулей, 2 уроков, преподавателя и категории; после удаления проверяется отсутствие курса/модулей/уроков и **сохранение** общих объектов
- **`src/test/java/com/studying/platform/CategoryAndCourseTests.java`** — добавлен `clear()` после сохранения и перед чтением в `saveCategoryAndCourse_shouldPersistAndRetrieve` и `updateCourse_shouldChangeData`
- **`src/test/java/com/studying/platform/UserAndProfileTests.java`** — добавлен `clear()` перед чтением профиля и пользователя в `saveUserWithProfile_shouldPersistAndRetrieve`

### Созданный файл:
- **`src/test/java/com/studying/platform/CourseServiceTransactionTests.java`** — 4 интеграционных теста через `@SpringBootTest` с `@Transactional(propagation = NOT_SUPPORTED)`:
  - `getCourseStructure_fieldsAccessibleOutsideTransaction` — создание данных через `TransactionTemplate`, вызов `getCourseStructure`, проверка `TransactionSynchronizationManager.isActualTransactionActive() == false`, чтение скалярных полей
  - `getCourseStructure_multipleModulesAndLessons` — курс с 2 модулями и 3 уроками, проверка общего количества
  - `getCourseStructure_nonExistentCourse_shouldThrow` — `ResourceNotFoundException`
  - `getCourseStructure_emptyStructure` — пустая структура без модулей

### Результаты:
- `mvn test -Dtest=ModuleAndLessonTests,CategoryAndCourseTests,UserAndProfileTests,CourseServiceTransactionTests`: **57 PASS** — PASS
- `mvn verify`: **121 PASS**, 0 FAIL — PASS
- `git diff --check`: **PASS**

### Следующий шаг:
Пункт 8 — проверить продолжительность курса.
