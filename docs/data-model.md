# Модель данных учебной платформы

## 1. Общие сведения

Документ описывает детальную логическую и концептуальную модель данных для учебной платформы на базе Spring Boot, Hibernate/JPA и PostgreSQL. Модель включает **15 JPA-сущностей**, связи типов 1:1, 1:M и M:M, правила каскадирования, стратегии загрузки и механизмы защиты учебной истории.

**Статус реализации:** по состоянию после исправлений реализованы сущности `User`, `Profile`, `Category`, `Tag`, `Course`, `Enrollment`, `Module`, `Lesson`, `CourseReview`. Остальные шесть сущностей — `Assignment`, `Submission`, `Quiz`, `Question`, `AnswerOption`, `QuizSubmission` — относятся к планируемым этапам 09–13 и не реализованы в коде. Функциональные сервисы тегов и отзывов также относятся к будущему этапу 14.

Все поля идентифицированы, типы данных указаны с учётом PostgreSQL-совместимых JPA-маппингов. Для каждой связи определён владелец (side of relationship), тип загрузки (`FetchType`) и каскадные операции.

## 2. Перечень сущностей

### 2.1. User (Пользователь)

| Поле | Тип Java | Тип PostgreSQL | Аннотации / Ограничения |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | `@Id`, `@GeneratedValue` |
| `email` | `String` | `VARCHAR(50)` | `@Column(name = "login", unique = true, nullable = false)`, `@NotBlank`, `@Email`, `@Size(max = 50)` |
| `role` | `UserRole` | `VARCHAR(20)` | `@Enumerated(EnumType.STRING)`, `@NotNull`, `nullable = false` |

**Примечание:** Java-поле называется `email`, однако физическое имя колонки в базе данных — `users.login`. Это сделано для обратной совместимости: существующая схема содержит колонку `login`, и переименование колонки требует отдельной миграции (см. `state.md` — блокер старой базы). Значение `password` удалено из модели: аутентификация и хранение паролей не входят в объём проекта.

**Enum `UserRole`:** `STUDENT`, `TEACHER`, `ADMIN`.

**Связи:**
- `1:1` с `Profile` (владелец — `Profile`, LAZY, `mappedBy = "user"`)
- `1:M` с `Course` (через `Course.teacher`, владелец — `Course`, LAZY)
- `1:M` с `Enrollment` (владелец — `Enrollment`, LAZY)
- `1:M` с `Submission` (владелец — `Submission`, LAZY, **не реализовано**)
- `1:M` с `QuizSubmission` (владелец — `QuizSubmission`, LAZY, **не реализовано**)
- `1:M` с `CourseReview` (владелец — `CourseReview`, LAZY)

**Каскады:** нет (удаление пользователя защищено).

---

### 2.2. Profile (Профиль пользователя)

| Поле | Тип Java | Тип PostgreSQL | Аннотации / Ограничения |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | `@Id`, `@GeneratedValue` |
| `displayName` | `String` | `VARCHAR(100)` | `@Column(nullable = false)` |
| `bio` | `String` | `TEXT` | `@Lob`, `@Column(columnDefinition = "TEXT")` |
| `avatarUrl` | `String` | `VARCHAR(500)` | `@Column(length = 500)` |
| `user` | `User` | — | `@OneToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "user_id", unique = true, nullable = false, foreignKey = @ForeignKey(name = "fk_profile_user"))` |

**Примечание:** Владелец связи «один-к-одному» — `Profile`. Обратная сторона — `User.profile` с `mappedBy = "user"`. Профиль **не обязателен** для пользователя (связь необязательная).

**Каскады:** `CascadeType.PERSIST`, `CascadeType.MERGE`.

---

### 2.3. Category (Категория курса)

| Поле | Тип Java | Тип PostgreSQL | Аннотации / Ограничения |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | `@Id`, `@GeneratedValue` |
| `name` | `String` | `VARCHAR(100)` | `@Column(unique = true, nullable = false)` |

**Связи:**
- `1:M` с `Course` (владелец — `Course`, LAZY, `mappedBy = "category"`).

**Каскады:** нет.

---

### 2.4. Tag (Тег)

| Поле | Тип Java | Тип PostgreSQL | Аннотации / Ограничения |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | `@Id`, `@GeneratedValue` |
| `name` | `String` | `VARCHAR(50)` | `@Column(unique = true, nullable = false)` |

**Связи:**
- `M:M` с `Course` через таблицу `course_tag` (`@JoinTable`). Владелец — `Course`. LAZY.

**Каскады:** нет.

---

### 2.5. Course (Курс)

| Поле | Тип Java | Тип PostgreSQL | Аннотации / Ограничения |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | `@Id`, `@GeneratedValue` |
| `title` | `String` | `VARCHAR(200)` | `@Column(nullable = false)` |
| `description` | `String` | `TEXT` | `@Lob` |
| `duration` | `Integer` | `INTEGER` | Минимальное значение 1 |
| `startDate` | `LocalDate` | `DATE` | `@Column(nullable = false)` |
| `teacherId` | `Long` | `BIGINT` | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "teacher_id", nullable = false, foreignKey = @ForeignKey(name = "fk_course_teacher"))` |
| `categoryId` | `Long` | `BIGINT` | `@ManyToOne(fetch = FetchType.EAGER)`, `@JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_course_category"))` |

**Связи:**
- `M:1` с `Category` (владелец — `Course`, EAGER для категории, т.к. небольшая справочная сущность)
- `M:1` с `User` (преподаватель, владелец — `Course`, LAZY)
- `1:M` с `Module` (`@OneToMany(mappedBy = "course", fetch = FetchType.LAZY, cascade = {PERSIST, MERGE, REMOVE}, orphanRemoval = true)`)
- `1:M` с `Enrollment` (`@OneToMany(mappedBy = "course", fetch = FetchType.LAZY)`)
- `1:M` с `CourseReview` (`@OneToMany(mappedBy = "course", fetch = FetchType.LAZY)`)
- `M:M` с `Tag` (`@ManyToMany(fetch = FetchType.LAZY, cascade = {PERSIST, MERGE}), @JoinTable(name = "course_tag", joinColumns = @JoinColumn(name = "course_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))`)

**Каскады:** `REMOVE` для `Module` и дочерних (через каскад на Module); `PERSIST, MERGE` для `Tag` (через `@ManyToMany`); без каскадов для `Enrollment` и `CourseReview`.

---

### 2.6. Enrollment (Запись на курс)

| Поле | Тип Java | Тип PostgreSQL | Аннотации / Ограничения |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | `@Id`, `@GeneratedValue` |
| `user` | `User` | `BIGINT` | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "user_id", nullable = false)` |
| `course` | `Course` | `BIGINT` | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "course_id", nullable = false)` |
| `enrollDate` | `LocalDateTime` | `TIMESTAMP` | `@Column(nullable = false)` |
| `status` | `EnrollmentStatus` | `VARCHAR(20)` | `@Enumerated(EnumType.STRING)`, `nullable = false` |

**Enum `EnrollmentStatus`:** `ACTIVE`, `DROPPED`.

**Ограничение уникальности:** `@UniqueConstraint(columnNames = {"user_id", "course_id"})` — один студент может быть записан на курс один раз независимо от статуса.

**Правила статусов:**
- `ACTIVE`: студент активно записан на курс. Отписка переводит статус в `DROPPED`.
- `DROPPED`: студент отписался. Повторная запись запрещена (сервис бросает `IllegalStateException`).
- Запись не удаляется при отписке — она сохраняется как часть архива истории.

**Связи:**
- `M:1` с `User` (студент, LAZY)
- `M:1` с `Course` (курс, LAZY)

**Каскады:** нет.

---

### 2.7. Module (Модуль курса)

| Поле | Тип Java | Тип PostgreSQL | Аннотации / Ограничения |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | `@Id`, `@GeneratedValue` |
| `title` | `String` | `VARCHAR(200)` | `@Column(nullable = false)` |
| `description` | `String` | `TEXT` | `@Lob` |
| `orderIndex` | `Integer` | `INTEGER` | `@Column(nullable = false)` |
| `courseId` | `Long` | `BIGINT` | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "course_id", nullable = false, foreignKey = @ForeignKey(name = "fk_module_course"))` |

**Связи:**
- `M:1` с `Course` (владелец — `Module`, LAZY)
- `1:M` с `Lesson` (`@OneToMany(mappedBy = "module", fetch = FetchType.LAZY, cascade = {PERSIST, MERGE}, orphanRemoval = true)`)
- `1:1` с `Quiz` (`@OneToOne(mappedBy = "module", fetch = FetchType.LAZY, cascade = {PERSIST, MERGE}, orphanRemoval = true)`)

**Каскады:** `PERSIST, MERGE` для `Lesson` и `Quiz` (через `mappedBy`), `orphanRemoval = true`.

---

### 2.8. Lesson (Урок)

| Поле | Тип Java | Тип PostgreSQL | Аннотации / Ограничения |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | `@Id`, `@GeneratedValue` |
| `title` | `String` | `VARCHAR(200)` | `@Column(nullable = false)` |
| `content` | `String` | `TEXT` | `@Lob` |
| `videoUrl` | `String` | `VARCHAR(500)` | |
| `moduleId` | `Long` | `BIGINT` | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "module_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lesson_module"))` |

**Связи:**
- `M:1` с `Module` (владелец — `Lesson`, LAZY)
- `1:M` с `Assignment` (`@OneToMany(mappedBy = "lesson", fetch = FetchType.LAZY, cascade = {PERSIST, MERGE}, orphanRemoval = true)`)

**Каскады:** `PERSIST, MERGE` для `Assignment`, `orphanRemoval = true`.

---

### 2.9. Assignment (Задание)

| Поле | Тип Java | Тип PostgreSQL | Аннотации / Ограничения |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | `@Id`, `@GeneratedValue` |
| `title` | `String` | `VARCHAR(200)` | `@Column(nullable = false)` |
| `description` | `String` | `TEXT` | `@Lob` |
| `dueDate` | `LocalDateTime` | `TIMESTAMP` | `@Column(nullable = false)` |
| `maxScore` | `Integer` | `INTEGER` | `@Column(nullable = false)` |
| `lessonId` | `Long` | `BIGINT` | `@ManyToOne`, `@JoinColumn(name = "lesson_id", nullable = false, foreignKey = @ForeignKey(name = "fk_assignment_lesson"))` |

**Связи:**
- `M:1` с `Lesson` (владелец — `Assignment`, LAZY)
- `1:M` с `Submission` (`@OneToMany(mappedBy = "assignment", fetch = FetchType.LAZY)`)

**Каскады:** нет (решения являются частью учебной истории).

---

### 2.10. Submission (Решение задания)

| Поле | Тип Java | Тип PostgreSQL | Аннотации / Ограничения |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | `@Id`, `@GeneratedValue` |
| `content` | `String` | `TEXT` | `@Lob`, `nullable = false` |
| `score` | `Integer` | `INTEGER` | |
| `feedback` | `String` | `TEXT` | `@Lob` |
| `submittedAt` | `LocalDateTime` | `TIMESTAMP` | `@Column(nullable = false)` |
| `assignmentId` | `Long` | `BIGINT` | `@ManyToOne`, `@JoinColumn(name = "assignment_id", nullable = false)` |
| `studentId` | `Long` | `BIGINT` | `@ManyToOne`, `@JoinColumn(name = "student_id", nullable = false)` |

**Ограничение уникальности:** `@UniqueConstraint(columnNames = {"student_id", "assignment_id"})` — одно решение на задание от одного студента.

**Связи:**
- `M:1` с `Assignment` (владелец — `Submission`, LAZY)
- `M:1` с `User` (студент, владелец — `Submission`, LAZY)

**Каскады:** нет.

---

### 2.11. Quiz (Тест)

| Поле | Тип Java | Тип PostgreSQL | Аннотации / Ограничения |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | `@Id`, `@GeneratedValue` |
| `title` | `String` | `VARCHAR(200)` | `@Column(nullable = false)` |
| `timeLimit` | `Integer` | `INTEGER` | Время на прохождение в минутах |
| `moduleId` | `Long` | `BIGINT` | `@OneToOne`, `@JoinColumn(name = "module_id", unique = true, nullable = false, foreignKey = @ForeignKey(name = "fk_quiz_module"))` |

**Связи:**
- `1:1` с `Module` (владелец — `Quiz`, LAZY)
- `1:M` с `Question` (`@OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY, cascade = {PERSIST, MERGE}, orphanRemoval = true)`)
- `1:M` с `QuizSubmission` (`@OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY)`)

**Каскады:** `PERSIST, MERGE` для `Question`, `orphanRemoval = true`.

---

### 2.12. Question (Вопрос викторины)

| Поле | Тип Java | Тип PostgreSQL | Аннотации / Ограничения |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | `@Id`, `@GeneratedValue` |
| `text` | `String` | `TEXT` | `@Lob`, `nullable = false` |
| `type` | `QuestionType` | `VARCHAR(20)` | `@Enumerated(EnumType.STRING)`, `nullable = false` |
| `quizId` | `Long` | `BIGINT` | `@ManyToOne`, `@JoinColumn(name = "quiz_id", nullable = false, foreignKey = @ForeignKey(name = "fk_question_quiz"))` |

**Enum `QuestionType`:** `SINGLE_CHOICE`, `MULTIPLE_CHOICE`.

**Связи:**
- `M:1` с `Quiz` (владелец — `Question`, LAZY)
- `1:M` с `AnswerOption` (`@OneToMany(mappedBy = "question", fetch = FetchType.LAZY, cascade = {PERSIST, MERGE}, orphanRemoval = true)`)

**Каскады:** `PERSIST, MERGE` для `AnswerOption`, `orphanRemoval = true`.

---

### 2.13. AnswerOption (Вариант ответа)

| Поле | Тип Java | Тип PostgreSQL | Аннотации / Ограничения |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | `@Id`, `@GeneratedValue` |
| `text` | `String` | `TEXT` | `@Lob`, `nullable = false` |
| `isCorrect` | `Boolean` | `BOOLEAN` | `@Column(nullable = false)` |
| `questionId` | `Long` | `BIGINT` | `@ManyToOne`, `@JoinColumn(name = "question_id", nullable = false, foreignKey = @ForeignKey(name = "fk_answeroption_question"))` |

**Связи:**
- `M:1` с `Question` (владелец — `AnswerOption`, LAZY)

**Каскады:** нет.

---

### 2.14. QuizSubmission (Результат прохождения теста)

| Поле | Тип Java | Тип PostgreSQL | Аннотации / Ограничения |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | `@Id`, `@GeneratedValue` |
| `score` | `Integer` | `INTEGER` | `@Column(nullable = false)` |
| `takenAt` | `LocalDateTime` | `TIMESTAMP` | `@Column(nullable = false)` |
| `quizId` | `Long` | `BIGINT` | `@ManyToOne`, `@JoinColumn(name = "quiz_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quizsubmission_quiz"))` |
| `studentId` | `Long` | `BIGINT` | `@ManyToOne`, `@JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quizsubmission_student"))` |

**Связи:**
- `M:1` с `Quiz` (владелец — `QuizSubmission`, LAZY)
- `M:1` с `User` (студент, владелец — `QuizSubmission`, LAZY)

**Каскады:** нет.

---

### 2.15. CourseReview (Отзыв о курсе)

| Поле | Тип Java | Тип PostgreSQL | Аннотации / Ограничения |
|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | `@Id`, `@GeneratedValue` |
| `rating` | `Integer` | `INTEGER` | `@Column(nullable = false)`, диапазон 1–5 |
| `comment` | `String` | `TEXT` | `@Lob` |
| `createdAt` | `LocalDateTime` | `TIMESTAMP` | `@Column(nullable = false)` |
| `courseId` | `Long` | `BIGINT` | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "course_id", nullable = false, foreignKey = @ForeignKey(name = "fk_review_course"))` |
| `studentId` | `Long` | `BIGINT` | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_review_student"))` |

**Ограничение уникальности:** `@UniqueConstraint(columnNames = {"student_id", "course_id"})` — один отзыв от студента на курс.

**Связи:**
- `M:1` с `Course` (владелец — `CourseReview`, LAZY)
- `M:1` с `User` (студент, владелец — `CourseReview`, LAZY)

**Каскады:** нет.

---

## 3. Схема связей (Relationship Graph)

```
USER ───1:1──▶ PROFILE
USER ───1:M──▶ COURSE (как teacher)
USER ───1:M──▶ ENROLLMENT
USER ───1:M──▶ SUBMISSION
USER ───1:M──▶ QUIZSUBMISSION
USER ───1:M──▶ COURSEREVIEW
CATEGORY ──1:M─▶ COURSE
COURSE ───M:M── TAG  (через course_tag)
COURSE ───1:M──▶ MODULE
COURSE ───1:M──▶ ENROLLMENT
COURSE ───1:M──▶ COURSEREVIEW
MODULE ───1:M──▶ LESSON
MODULE ───1:1──▶ QUIZ (необязательная)
LESSON ───1:M──▶ ASSIGNMENT
ASSIGNMENT ──1:M─▶ SUBMISSION
QUIZ ─────1:M──▶ QUESTION
QUESTION ─1:M──▶ ANSWEROPTION
QUIZ ─────1:M──▶ QUIZSUBMISSION
```

### Владение связями (Ownership Summary)

| Связь | Владелец | Обратная сторона | FetchType (коллекция) |
|---|---|---|---|
| User ↔ Profile | Profile | User (mappedBy) | LAZY |
| User (teacher) ↔ Course | Course | User (mappedBy) | LAZY |
| Category ↔ Course | Course | Category (mappedBy) | LAZY |
| Course ↔ Module | Module | Course (mappedBy) | LAZY |
| Module ↔ Lesson | Lesson | Module (mappedBy) | LAZY |
| Module ↔ Quiz | Quiz | Module (mappedBy) | LAZY |
| Lesson ↔ Assignment | Assignment | Lesson (mappedBy) | LAZY |
| Assignment ↔ Submission | Submission | Assignment (mappedBy) | LAZY |
| Course ↔ Enrollment | Enrollment | Course (mappedBy) | LAZY |
| User ↔ Enrollment | Enrollment | User (mappedBy) | LAZY |
| Course ↔ CourseReview | CourseReview | Course (mappedBy) | LAZY |
| User ↔ CourseReview | CourseReview | User (mappedBy) | LAZY |
| Quiz ↔ Question | Question | Quiz (mappedBy) | LAZY |
| Question ↔ AnswerOption | AnswerOption | Question (mappedBy) | LAZY |
| Quiz ↔ QuizSubmission | QuizSubmission | Quiz (mappedBy) | LAZY |
| User ↔ QuizSubmission | QuizSubmission | User (mappedBy) | LAZY |
| Course ↔ Tag | Course | Tag (mappedBy) | LAZY |

---

## 4. Каскадные операции и стратегия удаления

### 4.1. Таблица каскадов

| Сущность-владелец | Связь | CascadeType | orphanRemoval |
|---|---|---|---|
| Course | → Module | PERSIST, MERGE, REMOVE | true |
| Course | → Enrollment | — | false |
| Course | → CourseReview | — | false |
| Course | ↔ Tag | PERSIST, MERGE | false |
| Module | → Lesson | PERSIST, MERGE | true |
| Module | → Quiz | PERSIST, MERGE | true |
| Lesson | → Assignment | PERSIST, MERGE | true |
| Quiz | → Question | PERSIST, MERGE | true |
| Question | → AnswerOption | PERSIST, MERGE | true |
| Profile | → User (через FK) | PERSIST, MERGE | false |

### 4.2. Правила удаления (History Protection)

#### User (Пользователь)

Удаление пользователя **запрещено**, если:
- Есть активные или архивные записи `Enrollment` (студент записан или был записан на курсы).
- Есть отправленные решения `Submission` (часть учебной истории, **не реализовано**).
- Есть результаты тестов `QuizSubmission` (**не реализовано**).
- Есть оставленные отзывы `CourseReview`.
- Есть профиль `Profile`.

> Обоснование: пользователь — общий ресурс платформы. Удаление пользователя приведёт к потере учебной истории всех связанных объектов.
>
> **Важно:** конечная защита удаления через `@PreRemove`-логику (проверка всех коллекций перед удалением) запланирована на этап 15 и на текущий момент не реализована.

#### Course (Курс)

Удаление курса **запрещено**, если:
- Есть хотя бы одна запись `Enrollment` со статусом `ACTIVE` (студент сейчас записан).
- Есть записи `Enrollment` со статусом `DROPPED` (отписавшиеся студенты — часть истории).
- Есть хотя бы одна запись `CourseReview` (отзывы — часть истории).
- Есть хотя бы одна `Submission` (решения студентов — часть истории, **не реализовано**).
- Есть хотя бы один `QuizSubmission` (результаты тестов — часть истории, **не реализовано**).

Если все вышеперечисленные коллекции пусты — курс **может быть удалён** каскадно вместе с модулями, уроками, заданиями, тестами, вопросами и вариантами ответов.

> Обоснование: курс без записей и учебной истории — пустой контейнер, его удаление безопасно. При наличии записей или истории удаление запрещено.
>
> **Важно:** защита учебной истории через `@PreRemove`-логику (проверка коллекций перед удалением) запланирована на этап 15 и на текущий момент не реализована. Фактические ограничения обеспечиваются правилами бизнес-сервисов и внешними ключами БД.

#### Enrollment (Запись на курс)

- Статус `ACTIVE`: можно отписаться (изменить статус на `DROPPED`). При отписке — **учебная история сохраняется** (Submissions, QuizSubmissions, CourseReview остаются).
- Статус `DROPPED`: запись является частью архива истории и **не подлежит удалению** через сервис записи. Можно только прочитать.

> Обоснование: отписка — не удаление. Студент, отписавшийся от курса, сохраняет все свои решения, результаты тестов и отзывы.
>
> **Важно:** конечная защита истории (невозможность удаления записей при наличии Submissions/QuizSubmissions/CourseReview) запланирована на этап 15. На текущий момент отписка переводит запись в статус `DROPPED`, но каскадное удаление сущностей этапов 09–13 с защитой истории ещё не реализовано.

#### Submission (Решение задания)

Удаление **запрещено**. Решение — часть учебной истории студента и задания. Даже при удалении курса или отписке студена решение сохраняется.

#### QuizSubmission (Результат теста)

Удаление **запрещено**. Результат теста — часть учебной истории. Студент может проходить тест многократно, каждый результат сохраняется.

---

## 5. Проверка покрытия требований (Verification Table)

| № | Требование | Сущность/связь | Покрытие |
|---|---|---|---|
| 1 | 15 JPA-сущностей (проект) | Все перечислены в п.2 | ✅ (9 реализовано, 6 запланировано) |
| 2 | Связь 1:1 | User ↔ Profile, Module ↔ Quiz | ✅ (Profile реализовано, Quiz — запланировано) |
| 3 | Связь 1:M (множественные) | Все основные ветки модели | ✅ (реализовано для существующих сущностей) |
| 4 | Связь M:M | Course ↔ Tag (join table), User ↔ Course (через Enrollment) | ✅ |
| 5 | course_tag — join table, не сущность | `@JoinTable` в `Course.tags` | ✅ |
| 6 | Enrollment — сущность-связка | Поля: id, userId, courseId, enrollDate, status | ✅ |
| 7 | Уникальность записи (студент+курс) | `@UniqueConstraint` в `Enrollment` | ✅ |
| 8 | Одно решение на задание | `@UniqueConstraint` в `Submission` | ✅ (сущность не реализована) |
| 9 | Один отзыв на курс | `@UniqueConstraint` в `CourseReview` | ✅ |
| 10 | Один тест на модуль | `@OneToOne` + `unique = true` в `Quiz.module` | ✅ (сущность не реализована) |
| 11 | LAZY для всех коллекций | Все `@OneToMany`, `@ManyToMany` — LAZY | ✅ |
| 12 | Каскады для учебных материалов | Module→Lesson, Module→Quiz, Lesson→Assignment, Quiz→Question | ✅ (для реализованных сущностей) |
| 13 | Защита учебной истории | Бизнес-правила в сервисах, внешние ключи БД; `@PreRemove` — этап 15 | ⚠️ частично (этап 15) |
| 14 | Курс с историей не удаляется | Проверка коллекций в сервисах | ⚠️ частично (этап 15) |
| 15 | Отписка не удаляет историю | Отписка → статус DROPPED, не каскад | ✅ |
| 16 | Общие объекты не удаляются | User, Category, Tag — нет каскадов | ✅ |

---

## 6. Diagram (Mermaid ER)

```mermaid
erDiagram
    USER {
        long id PK
        string email UK (column: login)
        string role
    }

    PROFILE {
        long id PK
        string displayName
        text bio
        string avatarUrl
        long user_id FK UK
    }

    CATEGORY {
        long id PK
        string name UK
    }

    TAG {
        long id PK
        string name UK
    }

    COURSE {
        long id PK
        string title
        text description
        integer duration
        date startDate
        long teacher_id FK
        long category_id FK
    }

    ENROLLMENT {
        long id PK
        long user_id FK
        long course_id FK
        timestamp enrollDate
        string status
    }

    MODULE {
        long id PK
        string title
        text description
        integer orderIndex
        long course_id FK
    }

    LESSON {
        long id PK
        string title
        text content
        string videoUrl
        long module_id FK
    }

    ASSIGNMENT {
        long id PK
        string title
        text description
        timestamp dueDate
        integer maxScore
        long lesson_id FK
    }

    SUBMISSION {
        long id PK
        text content
        integer score
        text feedback
        timestamp submittedAt
        long assignment_id FK
        long student_id FK
    }

    QUIZ {
        long id PK
        string title
        integer timeLimit
        long module_id FK UK
    }

    QUESTION {
        long id PK
        text text
        string type
        long quiz_id FK
    }

    ANSWEROPTION {
        long id PK
        text text
        boolean isCorrect
        long question_id FK
    }

    QUIZSUBMISSION {
        long id PK
        integer score
        timestamp takenAt
        long quiz_id FK
        long student_id FK
    }

    COURSEREVIEW {
        long id PK
        integer rating
        text comment
        timestamp createdAt
        long course_id FK
        long student_id FK
    }

    USER ||--o| PROFILE : has
    USER ||--o{ COURSE : teaches
    CATEGORY ||--o{ COURSE : contains
    COURSE ||--o{ MODULE : has
    MODULE ||--o{ LESSON : has
    LESSON ||--o{ ASSIGNMENT : includes
    MODULE ||--o| QUIZ : may_have
    QUIZ ||--o{ QUESTION : contains
    QUESTION ||--o{ ANSWEROPTION : has
    USER ||--o{ ENROLLMENT : enrolls
    COURSE ||--o{ ENROLLMENT : has
    USER ||--o{ SUBMISSION : submits
    ASSIGNMENT ||--o{ SUBMISSION : receives
    QUIZ ||--o{ QUIZSUBMISSION : results
    USER ||--o{ QUIZSUBMISSION : takes
    USER ||--o{ COURSEREVIEW : writes
    COURSE ||--o{ COURSEREVIEW : receives
    COURSE ||--o{ COURSE_TAG : links
    TAG ||--o{ COURSE_TAG : links
```

---

## 7. Enum-типы

### UserRole
```
STUDENT — студент, может записываться на курсы, отправлять решения, проходить тесты, оставлять отзывы.
TEACHER — преподаватель, может создавать и вести курсы, добавлять модули, уроки, задания, оценивать решения.
ADMIN — администратор (резервная роль для будущих расширений).
```

### EnrollmentStatus
```
ACTIVE — студент активно записан на курс.
DROPPED — студент отписался от курса (история сохраняется).
```

### QuestionType
```
SINGLE_CHOICE — один правильный ответ из предложенных.
MULTIPLE_CHOICE — один или несколько правильных ответов из предложенных.
```

---

## 8. Примечания по реализации

1. **Ленивая загрузка:** все коллекции (`@OneToMany`, `@ManyToMany`) используют `FetchType.LAZY`. Связи `@ManyToOne` реализованных сущностей также явно заданы как `LAZY` (кроме `Course.category`, которая загружается `EAGER` как небольшая справочная сущность). `@OneToOne` с стороны `Profile` также `LAZY`.

2. **Генерация ID:** все сущности используют `@GeneratedValue(strategy = GenerationType.IDENTITY)` с PostgreSQL sequences.

3. **Таблица course_tag:** создаётся автоматически через `@JoinTable(name = "course_tag")`. Отдельная JPA-сущность не нужна.

4. **Инициализация схемы:** `spring.jpa.hibernate.ddl-auto = update` для автоматического создания/обновления схемы на основе аннотаций.

5. **Валидация диапазонов:** для `rating` (1-5) и `maxScore` (положительное) рекомендуется валидация на уровне сервиса (не на уровне JPA).
