# Checkpoint

## Текущий этап
**Пункт 15 плана исправлений: Дополнение регрессионных тестов CRUD и валидации** (`completed`)

## Завершённый этап — Этап 08: Запись на курс (`completed`)

---

## Выполнено — Пункт 11: Изоляция и безопасная очистка интеграционных тестов

### Созданные файлы:
- **`src/test/java/com/studying/platform/TestDataCleanupHelper.java`** — общий компонент Spring для адресной очистки тестовых данных в порядке зависимостей (отзывы → записи → уроки → модули → курсы → категории → пользователи). Для записей используется удаление по ID курсов и email пользователей, что позволяет очищать записи, когда их собственные ID не отслеживаются.
- **`src/test/java/com/studying/platform/TestDataCleanupHelperTests.java`** — 6 интеграционных тестов помощника: удаление отслеживаемых категорий, удаление отслеживаемых пользователей, сохранение посторонних категорий, сохранение посторонних пользователей, обработка пустых коллекций ID, удаление курса с зависимостями (модуль + уроки).

### Изменённые файлы:
- **`src/test/java/com/studying/platform/MvpFlowTests.java`** — удалён `@AfterEach` с `DELETE FROM` без WHERE; добавлены поля для отслеживания ID созданных сущностей; `@AfterEach` использует `TestDataCleanupHelper` в транзакции; добавлен тест изоляции `cleanup_shouldNotRemovePrestigeTestData`; добавлен импорт `UserRepository`; префиксы email ограничены 20 символами.
- **`src/test/java/com/studying/platform/EnrollmentServiceTransactionTests.java`** — аналогичные изменения: отслеживание ID, `TestDataCleanupHelper` для очистки, префиксы ограничены 17 символами.
- **`src/test/java/com/studying/platform/CourseServiceTransactionTests.java`** — добавлены поля `moduleIds`/`lessonIds` (Set) для поддержки нескольких модулей/уроков; `@AfterEach` собирает все ID; методы `createTeacherInTransaction`/`createCategoryInTransaction` используют `TransactionTemplate` для `persist()`; все тесты используют `@Transactional(propagation = NOT_SUPPORTED)` на уровне класса.
- **`src/test/java/com/studying/platform/UserServiceTests.java`** — удалены `deleteAll()` с подавлением исключений; добавлены поля отслеживания ID; `@AfterEach` использует `TestDataCleanupHelper`; префиксы ограничены 14 символами.

### Результаты проверок:
- `mvn test -Dtest=TestDataCleanupHelperTests`: **6 PASS** — PASS
- `mvn test -Dtest=MvpFlowTests`: **6 PASS** — PASS
- `mvn test -Dtest=EnrollmentServiceTransactionTests`: **8 PASS** — PASS
- `mvn test -Dtest=CourseServiceTransactionTests`: **4 PASS** — PASS
- `mvn test -Dtest=UserServiceTests`: **15 PASS** — PASS
- `mvn verify`: **140 PASS**, 0 FAIL, 0 ERROR — PASS
- `git diff --check`: **PASS** (нет проблем с форматированием)

### Следующий шаг:
Пункт 12 — точные проверки первичных, внешних и уникальных ключей (`completed`).

---

## Выполнено — Пункт 14: Исправление и проверка JavaDoc (`completed`)

### Изменённые файлы:
- **`src/main/java/com/studying/platform/service/EnrollmentService.java`** — добавлен импорт `org.springframework.data.jpa.repository.EntityGraph` для разрешения ссылок `{@link EntityGraph}` в JavaDoc методов `getStudentCourses` и `getCourseStudents`
- **`src/main/java/com/studying/platform/entity/User.java`** — в Javadoc поля `profile` убрано утверждение о гарантированно ленивой загрузке (`@OneToOne` по умолчанию имеет `fetch = EAGER`); описание заменено на точное
- **`src/main/java/com/studying/platform/service/CourseService.java`** — в Javadoc `addModule` и `addLesson` исправлено: убрано неверное упоминание «каскадного сохранения»; указано, что используется явный `entityManager.persist` и `flush`, без сброса контекста

### Результаты проверок:
- `mvn compile`: **успешно, 0 ошибок** — PASS
- `mvn javadoc:javadoc` (основной код): **BUILD SUCCESS**, 1 предупреждение (не связанное: `StudyingPlatformApplication` default constructor); **нет ошибок `reference not found`** — PASS
- `mvn verify`: **147 PASS**, 0 FAIL, 0 ERROR — PASS
- `git diff --check`: **PASS** (нет проблем с форматированием)
- Генерация JavaDoc для тестовых классов (`PostgresSchemaTests`): **NOT_RUN** — класс package-private, `javadoc` не может обработать non-public классы (известное ограничение JDK)

### Следующий шаг:
Пункт 15 — дополнение регрессионных тестов CRUD и валидации длительности курса.

---

## Выполнено — Пункт 15: Дополнение регрессионных тестов CRUD и валидации (`completed`)

### Изменённые файлы:
- **`src/test/java/com/studying/platform/CourseManagementTests.java`** — добавлен тест `createCourse_withNullDuration_shouldThrow`: создаёт валидные категорию и преподавателя, вызывает `createCourse` с `duration = null`, ожидает `IllegalArgumentException`, проверяет что курс не создан (`courseRepository.count() == 0`)
- **`src/test/java/com/studying/platform/ModuleAndLessonTests.java`** — в `testCascadeDeleteCourseRemovesModulesAndLessons` добавлен `entityManager.clear()` после `flush()`; проверка отсутствия курса, обоих модулей и обоих уроков выполняется из очищенного контекста
- **`src/test/java/com/studying/platform/CourseServiceTransactionTests.java`** — добавлены 2 теста через настоящий Spring-прокси:
  - `updateCourse_withZeroDuration_shouldThrow` — создаёт курс, вызывает `updateCourse` с `duration = 0`, ожидает `IllegalArgumentException`, проверяет в новой транзакции что данные unchanged
  - `updateCourse_withNegativeDuration_shouldThrow` — аналогично с `duration = -10`

### Результаты проверок:
- `mvn test -Dtest=CourseManagementTests,ModuleAndLessonTests,CourseServiceTransactionTests`: **42 PASS** (19 + 17 + 6) — PASS
- `mvn verify`: **150 PASS**, 0 FAIL, 0 ERROR — PASS
- `git diff --check`: **PASS** (нет проблем с форматированием)

### Следующий шаг:
Пункт 16 — привести документы в порядок и повторить приёмку.

---

## Выполнено — Пункт 11: Изоляция и безопасная очистка интеграционных тестов

### Изменённые файлы (сущности — удаление `@Lob`):
- **`src/main/java/com/studying/platform/entity/Course.java`** — удалён `@Lob` с поля `description`, удалён импорт `jakarta.persistence.Lob`
- **`src/main/java/com/studying/platform/entity/Module.java`** — удалён `@Lob` с поля `description`, удалён импорт
- **`src/main/java/com/studying/platform/entity/Lesson.java`** — удалён `@Lob` с поля `content`, удалён импорт
- **`src/main/java/com/studying/platform/entity/Profile.java`** — удалён `@Lob` с поля `bio`, удалён импорт
- **`src/main/java/com/studying/platform/entity/CourseReview.java`** — удалён `@Lob` с поля `comment`, удалён импорт

Во всех случаях сохранён `@Column(columnDefinition = "TEXT")` и nullable-семантика.

### Созданный файл:
- **`src/main/java/com/studying/platform/repository/CourseReviewRepository.java`** — `JpaRepository<CourseReview, Long>` для поддержки тестов

### Созданный тест:
- **`src/test/java/com/studying/platform/PostgresTextStorageTests.java`** — 3 интеграционных теста:
  - `textFieldsPersistAndReadCorrectly` — сохраняет 5 текстовых полей с кириллицей, переводом строки и > 255 символов; flush/clear; читает через JPA и SQL — все совпадают
  - `textFieldsUpdateAndReread` — обновление описания курса; подтверждение через JPA и SQL
  - `nullAndEmptyTextFields` — null и пустая строка для всех 5 полей; проверка через JPA и SQL

### Результаты проверок:
- `mvn test -Dtest=PostgresTextStorageTests`: **3 PASS** — PASS
- `mvn verify`: **147 PASS**, 0 FAIL, 0 ERROR — PASS
- `git diff --check`: **PASS**

### Блокер существующей базы:
Данные, созданные со старым маппингом `@Lob` + `columnDefinition = "TEXT"`, могут содержать OID Large Object в TEXT-колонках. Простое удаление `@Lob` или `ddl-auto=update` не преобразует существующие данные. Требуется согласованная миграция: различение OID от обычного текста, резервное копирование, преобразование и проверка целостности.

---

## Выполнено — Пункт 13: Исправление хранения текстовых полей PostgreSQL (`completed`)

---

## Выполнено — Пункт 12: Точные проверки первичных, внешних и уникальных ключей

### Изменённый файл:
- **`src/test/java/com/studying/platform/PostgresSchemaTests.java`** — полностью переработаны три метода проверки ограничений схемы.

### Выполненные изменения:

**`primaryKeysExist`** — вместо проверки наличия колонки `id` в PK теперь проверяется **точное равенство множества** колонок PK:
- Для 9 таблиц с одиночным PK — множество должно быть точно `{id}`
- Для `course_tag` — множество должно быть точно `{course_id, tag_id}`
- Лишние колонки в PK обнаруживаются и приводят к ошибке

**`foreignKeysExist`** — вместо проверки только `(pkTable.pkCol -> fkTable)` теперь проверяются **все четыре компонента** каждого FK через `getImportedKeys`:
- Таблица-владелец FK, колонка-владелец FK, целевая таблица, целевая колонка
- Формат ключа: `ownerTable.ownerColumn->refTable.refColumn`
- Проверяется как наличие всех 11 ожидаемых FK, так и отсутствие лишних
- Генерируемые Hibernate имена ограничений не сравниваются

**`uniqueConstraintsExist`** — вместо группировки колонок по таблице теперь используется **группировка по идентификатору ограничения** (`constraint_name`):
- Запрос включает `table_name`, `constraint_name`, `column_name`, `ordinal_position`
- Группировка: `table -> constraint_name -> columns` (с учётом порядка)
- Два отдельных уникальных ограничения на `user_id` и `course_id` не сливаются в одно
- Составное ограничение `enrollments(user_id, course_id)` проверяется как единый набор
- Обнаружены и добавлены дополнительные ограничения: `categories.name`, `course_reviews(student_id, course_id)`

**Негативные тесты:**
- `fkWrongOwnerColumnShouldFail` — проверяет, что несуществующий FK не проходит валидацию
- `pkExtraColumnShouldBeDetected` — подтверждает точный состав PK `users`
- `uniqueSeparateConstraintsNotMerged` — проверяет, что `enrollments` имеет составное ограничение, а не два отдельных

### Результаты проверок:
- `mvn test -Dtest=PostgresSchemaTests`: **13 PASS** (было 9) — PASS
- `mvn verify`: **144 PASS**, 0 FAIL, 0 ERROR — PASS
- `git diff --check`: **PASS**

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

---

## Выполнено — Пункт 8: Проверка длительности курса

### Изменённые файлы:
- **`src/main/java/com/studying/platform/service/CourseService.java`** — добавлен приватный метод `validateDuration(Integer duration)` с JavaDoc; вызов в `createCourse` до создания сущности; вызов в `updateCourse` только при `duration != null`; обновлён JavaDoc обоих методов

### Добавленные тесты в `CourseManagementTests.java`:
- `createCourse_withZeroDuration_shouldThrow` — duration = 0 бросает `IllegalArgumentException`
- `createCourse_withNegativeDuration_shouldThrow` — duration = -5 бросает `IllegalArgumentException`
- `createCourse_withMinimumDuration_shouldSucceed` — duration = 1 создаётся успешно
- `updateCourse_withZeroDuration_shouldThrow` — duration = 0 бросает `IllegalArgumentException`, данные не меняются
- `updateCourse_withNegativeDuration_shouldThrow` — duration = -10 бросает `IllegalArgumentException`, данные не меняются
- `updateCourse_withMinimumDurationShouldSucceed` — duration = 1 обновляется успешно
- `updateCourse_withNullDuration_shouldPreserveValue` — duration = null сохраняет прежнее значение 25

### Результаты:
- `mvn test -Dtest=CourseManagementTests`: **18 PASS** (11 существующих + 8 новых + databaseIsPostgreSQL) — PASS
- `mvn verify`: **128 PASS**, 0 FAIL — PASS
- `git diff --check`: **PASS**

### Следующий шаг:
Пункт 9 — согласовать документацию модели и фактическое состояние (`in_progress`).

---

## Выполнено — Пункт 9: Согласовать документацию модели и фактическое состояние

### Изменённый файл:
- **`docs/data-model.md`** — приведён в соответствие с фактическим кодом.

### Выполненные исправления:

**Раздел 1 — Общие сведения:**
- Добавлена пометка о статусе реализации: 9 сущностей реализованы, 6 запланированы (этапы 09–13)

**Раздел 2.1 — User:**
- Заменены `login`/`password` на `email` с указанием физической колонки `users.login`
- Добавлены аннотации `@NotBlank`, `@Email`, `@Size(max = 50)` на email, `@NotNull` на role
- Удалено поле `password` из модели
- Помечены связи с не-реализованными сущностями (Submission, QuizSubmission)

**Раздел 2.2 — Profile:**
- Удалено неверное поле `userId Long`; оставлен корректный `user User` (владелец связи)
- Исправлен `mappedBy = "profile"` на `mappedBy = "user"` на стороне User
- Указано, что связь необязательная

**Раздел 2.4 — Course каскады:**
- Исправлено: Tag имеет `PERSIST, MERGE` (а не «не для Tag»)

**Раздел 2.6 — Enrollment:**
- Поля `userId`/`courseId Long` заменены на `user User`/`course Course`
- Добавлен блок «Правила статусов»: ACTIVE → DROPPED, запрет повторной записи

**Раздел 4.1 — Каскады:**
- Исправлено: Course → Tag имеет `PERSIST, MERGE`

**Раздел 4.2 — Правила удаления:**
- Помечено `@PreRemove`-логика как «этап 15, не реализовано»
- Помечены не-реализованные сущности (Submission, QuizSubmission) в правилах удаления User и Course
- Добавлено примечание о защите через бизнес-сервисы и FK БД

**Раздел 5 — Verification Table:**
- Обновлено покрытие для пунктов 1, 2, 13, 14 с отметками ⚠️ частично
- Помечены не-реализованные сущности

**Раздел 6 — Mermaid ER:**
- Поле `login`/`password` заменено на `email` с указанием колонки `login`

**Раздел 8 — Примечания:**
- Исправлено описание ленивой загрузки: `Course.teacher` — LAZY, `Course.category` — EAGER
- Добавлена информация о `LAZY` для `@ManyToOne` реализованных сущностей

**Дополнительно:**
- Все `@ManyToOne` для реализованных сущностей (Course, Enrollment, Module, Lesson, CourseReview) приведены с указанием `fetch = FetchType.LAZY`
- Курс → Category обновлён на `fetch = FetchType.EAGER`

### Результаты:
- `mvn verify`: **128 PASS**, 0 FAIL — PASS
- `git diff --check`: **PASS** (изменён только `docs/data-model.md`, 50 добавлено / 39 удалено)
- Поиск `password`/`@PreRemove`/`mappedBy = "profile"`/`H2`: все оставшиеся употребления осмысленны

### Следующий шаг:
Пункт 10 — итоговая независимая проверка и актуализация состояния.

---

## Выполнено — Пункт 10: Итоговая независимая проверка и актуализация состояния

### Созданный файл:
- **`src/test/java/com/studying/platform/MvpFlowTests.java`** — 5 сквозных интеграционных тестов через `@SpringBootTest` с `@Transactional(propagation = Propagation.NOT_SUPPORTED)`:
  - `fullMvpFlow_createEnrollReadDropReenrollmentBlocked` — полный MVP-сценарий: создание преподавателя и студента через `UserService`, создание категории, курса, модуля и урока через `CourseService`, запись студента через `EnrollmentService`, чтение структуры и списков вне транзакции, отписка, проверка статуса `DROPPED`, пустых активных списков и запрета повторной записи
  - `userServiceIntegration_courseAndEnrollmentWork` — интеграция `UserService` с `CourseService` и `EnrollmentService`
  - `reenrollmentActiveCourse_shouldThrow` — запрет повторной записи на активный курс
  - `enrollNonExistentCourse_shouldThrow` — исключение при записи на несуществующий курс
  - `getNonExistentCourseStructure_shouldThrow` — исключение при чтении несуществующего курса

### Результаты всех проверок:
- **`java -version`**: Java 23.0.2 — PASS
- **`mvn -version`**: Maven 3.10.0, Java 23 — PASS
- **`docker info`**: Docker Desktop 28.3.2, Testcontainers version 1.21.2 — PASS
- **`mvn test -Dtest=MvpFlowTests`**: 5 PASS, 0 FAIL — PASS
- **`mvn verify`**: 133 PASS (11 классов тестов), 0 FAIL, 0 ERROR — PASS
- **`git diff --check`**: PASS (нет проблем с форматированием)
- **Проверка схемы** (PostgresSchemaTests): 9 проверок — PASS
- **Проверка каскадов** (ModuleAndLessonTests): каскадное сохранение и удаление — PASS
- **Проверка транзакций** (EnrollmentServiceTransactionTests + CourseServiceTransactionTests): данные доступны вне транзакции — PASS
- **Проверка уникальности** (EnrollmentTests + MvpFlowTests): ограничение БД и сервисная проверка — PASS
- **Негативные сценарии** (все классы): 404, валидация, дубликаты — PASS

### Итого по всем тестам:
| Класс тестов | Кол-во тестов | Статус |
|---|---|---|
| PostgresSchemaTests | 9 | PASS |
| EnrollmentServiceTransactionTests | 8 | PASS |
| StudyingPlatformApplicationTests | 7 | PASS |
| CourseServiceTransactionTests | 4 | PASS |
| UserAndProfileTests | 20 | PASS |
| CourseManagementTests | 18 | PASS |
| ModuleAndLessonTests | 17 | PASS |
| CategoryAndCourseTests | 16 | PASS |
| UserServiceTests | 15 | PASS |
| EnrollmentTests | 14 | PASS |
| MvpFlowTests | 5 | PASS |
| **Всего** | **133** | **Все PASS** |

### Блокер существующей базы:
Колонка `users.password NOT NULL` сохранится при `ddl-auto=update` — вставка без пароля в старую схему завершится ошибкой. Тесты используют отдельную контейнерную БД (`create-drop`), поэтому не затронуты. Для использования старой рабочей БД требуется согласованная миграция.

### Следующий шаг:
Переход к Этапу 09: Задания — реализация `Assignment`, репозитория и управления заданиями урока.
