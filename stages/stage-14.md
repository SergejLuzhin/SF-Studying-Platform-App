# Этап 14. Отзывы и теги — подробный план

## 1. Цель и границы этапа

Дополнить существующие сущности `CourseReview`, `Tag` и связь курсов с тегами недостающими репозиториями, сервисами и бизнес-логикой. Не создавать сущности заново — они уже реализованы в предыдущих этапах.

**В границы этапа входит:**
- Создание `TagRepository` для работы с тегами.
- Добавление методов управления тегами (`addTagsToCourse`, `removeTagsFromCourse`) в существующий `CourseService`.
- Создание `CourseReviewService` с методами создания, чтения и валидации отзывов.
- Создание интеграционных тестов для `CourseReviewRepository` и `CourseReviewService` (минимум 10 содержательных тестов).
- Обновление `PostgresSchemaTests` при необходимости.

**Из границ этапа НЕ выходит:**
- Создание / изменение сущностей `CourseReview`, `Tag`, `Course`.
- REST API-эндпоинты для отзывов и тегов (этап 19).
- Изменение других сервисов и сущностей.

## 2. Критерии приёмки

- [ ] **CR1.** Отзывы можно создавать через `CourseReviewService.createReview()`.
- [ ] **CR2.** Отзывы можно читать по курсу (`getReviewsByCourse`) и по студенту (`getReviewsByStudent`).
- [ ] **CR3.** Рейтинг отзыва — целое число в диапазоне 1–5; значение вне диапазона вызывает `IllegalArgumentException`.
- [ ] **CR4.** Только студент (роль `STUDENT`) может оставить отзыв.
- [ ] **CR5.** Только записанный на курс студент (статус `ACTIVE`) может оставить отзыв.
- [ ] **CR6.** Один отзыв от одного студента на один курс — повторная попытка вызывает `IllegalStateException`.
- [ ] **CR7.** Теги сохраняются и удаляются через `CourseService.addTagsToCourse()` / `removeTagsFromCourse()`.
- [ ] **CR8.** Удаление тега из курса не удаляет сам тег и не затрагивает другие курсы, использующие этот тег.
- [ ] **CR9.** Создание дублирующего тега (с тем же именем) отклоняется (`DataIntegrityViolationException` / `ConstraintViolationException`).

## 3. Исходное состояние

### Сущности (уже реализованы, НЕ трогать)

| Файл | Описание |
|------|----------|
| `entity/CourseReview.java` | id, rating (Integer, nullable=false), comment (TEXT), createdAt (LocalDateTime, nullable=false), course (ManyToOne LAZY), student (ManyToOne LAZY), uniqueConstraint(student_id, course_id). Конструктор по умолчанию + конструктор со всеми полями. Полные getter/setter с JavaDoc. |
| `entity/Tag.java` | id, name (String, unique=true, nullable=false, length=50). Конструктор по умолчанию + конструктор с name. Полные getter/setter с JavaDoc. |
| `entity/Course.java` | Содержит `Set<CourseReview> reviews` (@OneToMany mappedBy="course", LAZY) и `Set<Tag> tags` (@ManyToMany LAZY, cascade PERSIST+MERGE, join table course_tag). |
| `entity/User.java` + `entity/UserRole.java` | Роль пользователя: `STUDENT`, `TEACHER`, `ADMIN`. |
| `entity/Enrollment.java` + `entity/EnrollmentStatus.java` | Статусы записи: `ACTIVE`, `DROPPED`. |

### Репозитории

| Файл | Статус |
|------|--------|
| `repository/CourseReviewRepository.java` | СУЩЕСТВУЕТ — пустой интерфейс, расширяет `JpaRepository<CourseReview, Long>`. |
| `repository/TagRepository.java` | **НЕ СУЩЕСТВУЕТ** — нужно создать. |
| `repository/CourseRepository.java` | СУЩЕСТВУЕТ — `findByTitle`, `existsById`. Методов для тегов нет. |
| `repository/EnrollmentRepository.java` | СУЩЕСТВУЕТ — методы `findByUserIdAndCourseId`, `findByUserIdAndCourseIdAndStatus`, `findByUserIdAndStatus`, `findByCourseIdAndStatus`, `countByCourseIdAndStatus`. |

### Сервисы

| Файл | Статус |
|------|--------|
| `service/CourseReviewService.java` | **НЕ СУЩЕСТВУЕТ** — нужно создать. |
| `service/CourseService.java` | СУЩЕСТВУЕТ — методы создания курса, добавления модулей/уроков/заданий, обновления, получения структуры. Методов для тегов нет. |
| `service/EnrollmentService.java` | СУЩЕСТВУЕТ — `enrollStudent`, `dropEnrollment`, `getEnrollmentStatus`. Паттерн: проверка существования, роли, активной записи. |
| `service/QuizSubmissionService.java`, `service/SubmissionService.java`, `service/UserService.java` | СУЩЕСТВУЮТ — не затрагиваются. |

### Исключения

| Файл | Описание |
|------|----------|
| `exception/ResourceNotFoundException.java` | Проверенное исключение RuntimeException для отсутствующих сущностей. |

### Тесты

| Файл | Статус |
|------|--------|
| `PostgresSchemaTests.java` | Таблицы `tags`, `course_reviews`, `course_tag` уже в списке. PK, FK, уникальные ограничения (`tags.name`, `course_reviews.student_id+course_id`) уже проверены. **Обновлять не нужно** — всё корректно. |
| Отдельные тесты для CourseReview / Tag | **НЕ СУЩЕСТВУЮТ** — нужно создать. |

### Утилиты

| Файл | Описание |
|------|----------|
| `TestDataCleanupHelper.java` | Включает удаление `CourseReview` по ID курсов (строка 84–88): `DELETE FROM CourseReview cr WHERE cr.course.id IN (:ids)`. Поддерживает этап. |

### Зависимости (pom.xml)

Spring Boot 3.5.3, Spring Data JPA, PostgreSQL, Testcontainers, JUnit 5 — всё уже настроено. Новые зависимости не требуются.

### ADR

Индекс `docs/adr/README.md` пуст (нет утверждённых ADR, влияющих на этап).

## 4. Последовательные шаги реализации

### Шаг 14.1. Создать `TagRepository`

**Файл:** `src/main/java/com/studying/platform/repository/TagRepository.java`

- Интерфейс, расширяет `JpaRepository<Tag, Long>`.
- JavaDoc на русском, описывающий назначение репозитория и стандартные CRUD-операции.

### Шаг 14.2. Добавить методы управления тегами в `CourseService`

**Файл:** `src/main/java/com/studying/platform/service/CourseService.java`

Добавить приватные и публичные методы:

1. **`addTagsToCourse(Long courseId, Set<Long> tagIds)`** — привязывает существующие теги к курсу. Проверяет существование курса и каждого тега. Использует `@Transactional` (унаследованный от класса).
2. **`removeTagsFromCourse(Long courseId, Set<Long> tagIds)`** — отвязывает теги от курса. Не удаляет теги из БД (только разрыв связи в `course_tag`). Проверяет существование курса и каждого тега.
3. **`getCourseTags(Long courseId)`** (опционально) — возвращает список тегов курса.

Реализация:
- Получение курса через `courseRepository.findById(courseId)`.
- Получение тегов через `tagRepository.findAllById(tagIds)`.
- Для добавления: `course.getTags().addAll(tags)`.
- Для удаления: `course.getTags().removeAll(tags)`.
- Каскады `PERSIST+MERGE` на `@ManyToMany` в `Course.tags` автоматически обрабатывают запись в `course_tag`.
- Использовать `entityManager.flush()` для сохранения.

### Шаг 14.3. Добавить зависимости `TagRepository` в `CourseService`

- Добавить поле `private final TagRepository tagRepository;`.
- Обновить конструктор: добавить `TagRepository tagRepository` в параметры.
- Сохранить существующие параметры конструктора в том же порядке, добавив `tagRepository` в конец.

### Шаг 14.4. Создать `CourseReviewService`

**Файл:** `src/main/java/com/studying/platform/service/CourseReviewService.java`

Методы с `@Transactional` на уровне класса:

1. **`createReview(Long studentId, Long courseId, Integer rating, String comment)`** — создаёт новый отзыв.
   - Проверяет существование студента через `userRepository.findById(studentId)` → `ResourceNotFoundException`.
   - Проверяет роль студента: `student.getRole() == UserRole.STUDENT` → `ResourceNotFoundException`.
   - Проверяет существование курса через `courseRepository.findById(courseId)` → `ResourceNotFoundException`.
   - Проверяет активную запись на курс: `enrollmentRepository.findByUserIdAndCourseIdAndStatus(...)` → `ResourceNotFoundException` если нет активной записи.
   - Валидирует рейтинг: `rating >= 1 && rating <= 5` → `IllegalArgumentException`.
   - Проверяет, что отзыв ещё не оставлен: `courseReviewRepository.findByStudentIdAndCourseId(...)` → `IllegalStateException`.
   - Создаёт `CourseReview` через конструктор, сохраняет через `entityManager.persist(review)` + `flush()`.
2. **`getReviewsByCourse(Long courseId)`** — возвращает список отзывов курса.
   - Проверяет существование курса.
   - Возвращает список через репозиторий (нужен метод `findByCourseId` в `CourseReviewRepository`).
3. **`getReviewsByStudent(Long studentId)`** — возвращает список отзывов студента.
   - Проверяет существование студента.
   - Возвращает список через репозиторий (нужен метод `findByStudentId` в `CourseReviewRepository`).

### Шаг 14.5. Добавить кастомные методы в `CourseReviewRepository`

**Файл:** `src/main/java/com/studying/platform/repository/CourseReviewRepository.java`

Добавить методы Spring Data JPA:
- `Optional<CourseReview> findByStudentIdAndCourseId(Long studentId, Long courseId)` — для проверки уникальности отзыва.
- `List<CourseReview> findByCourseId(Long courseId)` — для получения отзывов курса.
- `List<CourseReview> findByStudentId(Long studentId)` — для получения отзывов студента.

### Шаг 14.6. Добавить зависимости в `CourseReviewService`

- `CourseRepository`, `UserRepository`, `EnrollmentRepository`, `CourseReviewRepository`, `EntityManager` — через конструктор.

### Шаг 14.7. Создать интеграционные тесты

**Файл:** `src/test/java/com/studying/platform/CourseReviewAndTagTests.java`

Шаблон: `@DataJpaTest`, `@ActiveProfiles("test")`, `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`, `@Import({CourseReviewService.class, TagRepository.class})` (или `@Import(CourseReviewService.class)` — `TagRepository` будет автоматически обнаружен в `@DataJpaTest`, т.к. находится в пакете `com.studying.platform.repository`).

Методы-тесты (минимум 10):

#### Позитивные тесты (5–6):

1. **`createReview_shouldSucceed_forEnrolledStudent`** — студент записан на курс, оставляет отзыв с рейтингом 4 и комментарием. Проверяет создание отзыва, сохранение в БД.
2. **`createReview_shouldSucceed_withMinimumRating`** — рейтинг 1, пустой комментарий.
3. **`createReview_shouldSucceed_withMaximumRating`** — рейтинг 5, длинный комментарий.
4. **`getReviewsByCourse_shouldReturnList`** — несколько отзывов на один курс, проверяет список и количество.
5. **`getReviewsByStudent_shouldReturnList`** — один студент оставляет отзывы на разные курсы.
6. **`addTagsToCourse_shouldLinkTags`** — создаёт курс и теги, привязывает теги к курсу через `CourseService.addTagsToCourse`. Проверяет наличие тегов в курсе.
7. **`removeTagsFromCourse_shouldUnlinkOnly`** — два курса с общим тегом, удаляет тег из одного курса, проверяет что тег остался у другого и сам тег в БД не удалён.

#### Негативные тесты (5–6):

8. **`createReview_nonExistentStudent_shouldThrow`** — несуществующий ID студента.
9. **`createReview_teacherCannotReview`** — преподаватель (TEACHER) не может оставить отзыв.
10. **`createReview_adminCannotReview`** — администратор (ADMIN) не может оставить отзыв.
11. **`createReview_nonEnrolledStudent_shouldThrow`** — студент не записан на курс (нет активной записи).
12. **`createReview_studentAlreadyReviewed_shouldThrow`** — студент уже оставил отзыв на курс.
13. **`createReview_invalidRatingTooLow_shouldThrow`** — рейтинг 0.
14. **`createReview_invalidRatingTooHigh_shouldThrow`** — рейтинг 6.
15. **`createReview_nonExistentCourse_shouldThrow`** — несуществующий ID курса.

### Шаг 14.8. Проверить `PostgresSchemaTests`

Таблицы `tags`, `course_reviews`, `course_tag` уже включены. PK, FK и уникальные ограничения уже проверены. **Изменения не требуются.**

## 5. Требования к тестам

### Минимальный набор тестов: 10 содержательных интеграционных тестов

| № | Название теста | Сценарий | Ожидаемый результат |
|---|---------------|----------|---------------------|
| 1 | `createReview_shouldSucceed_forEnrolledStudent` | Позитивный: запись, отзыв | Созданная запись в БД |
| 2 | `createReview_withRatingOne` | Позитивный: минимальный рейтинг | Рейтинг 1 сохранён |
| 3 | `createReview_withRatingFive` | Позитивный: максимальный рейтинг | Рейтинг 5 сохранён |
| 4 | `getReviewsByCourse_shouldReturnAll` | Позитивный: выборка по курсу | Все отзывы курса возвращены |
| 5 | `getReviewsByStudent_shouldReturnAll` | Позитивный: выборка по студенту | Все отзывы студента возвращены |
| 6 | `addTagsToCourse_shouldLinkTags` | Позитивный: привязка тегов | Теги связаны с курсом |
| 7 | `removeTagsFromCourse_shouldNotDeleteTag` | Позитивный: удаление связи | Тег не удалён, связь разорвана |
| 8 | `createReview_nonEnrolledStudent_shouldThrow` | Негативный: нет записи | `ResourceNotFoundException` |
| 9 | `createReview_teacherCannotReview` | Негативный: неверная роль | `ResourceNotFoundException` |
| 10 | `createReview_invalidRating_shouldThrow` | Негативный: рейтинг вне диапазона | `IllegalArgumentException` |
| 11 | `createReview_duplicate_shouldThrow` | Негативный: дублирование отзыва | `IllegalStateException` |
| 12 | `createReview_nonExistentObjects_shouldThrow` | Негативный: несуществующие ID | `ResourceNotFoundException` |

### Стилистические требования к тестам

- Имена тестов в формате `methodName_shouldExpectedBehavior` или `methodName_negativeCondition_shouldThrow`.
- JavaDoc на русском для каждого тестового метода, описывающий сценарий и ожидаемое поведение.
- Разделение на подготовка / выполнение / проверка (шаблон Given-When-Then в комментариях).
- Использование `TestDataCleanupHelper` для очистки тестовых данных, если тесты зависят друг от друга (в рамках одного теста — очистка не нужна, т.к. `create-drop`).
- Проверка сохранения в БД через `repository.findById()` после создания через сервис.

## 6. Команды проверки

### Компиляция проекта

```bash
mvn compile -q
```

### Запуск тестов

```bash
mvn test -Dtest=CourseReviewAndTagTests -q
```

### Запуск всех тестов (полная проверка)

```bash
mvn test -q
```

### Запуск проверок схемы

```bash
mvn test -Dtest=PostgresSchemaTests -q
```

**Критерии прохождения:** все команды возвращают код выхода 0, без ошибок компиляции и падений тестов.

## 7. Ограничения, риски и блокеры

### Ограничения

- Сущности `CourseReview`, `Tag`, `Course` не изменяются — их структура зафиксирована в предыдущих этапах.
- REST API для отзывов и тегов не реализуется (этап 19).
- Каскады на `Course.tags` ограничены `PERSIST+MERGE` — удаление курса не удаляет связанные теги.
- Студент не может сменить отзыв (только создать один раз). Изменение отзыва не входит в этап.

### Риски

- **Конфликт с `TestDataCleanupHelper`**: helper уже содержит удаление `CourseReview` по курсу (строка 84–88). Если тесты создают отзывы, cleanup-хелпер должен корректно работать — нужно убедиться, что список `reviewCourseIds` передаётся правильно.
- **Синхронизация `PostgresSchemaTests`**: если при реализации потребуется изменить маппинг (например, добавить/убрать колонку или ограничение), нужно обновить `PostgresSchemaTests`. На данном этапе маппинг не меняется.
- **Порядок удаления в cleanup**: отзывы зависят от курса, поэтому удаление отзывов должно идти до удаления курсов — порядок в `TestDataCleanupHelper` уже корректен.

### Блокеры

- Нет утверждённых ADR, блокирующих этап.
- Сущности уже реализованы, репозитории существуют.
- Тестовая инфраструктура (Testcontainers, профиль `test`) работает (подтверждено этапами 3–13).
- **ADR_REQUIRED не требуется** — этап не содержит существенных новых архитектурных решений.

### Статус этапа: `in_progress` → `completed`
