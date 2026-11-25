# Этап 01: Проектирование модели данных (Data Model Design)

## 1. Цель и границы этапа
**Цель:** Разработать и задокументировать детальную концептуальную и логическую модель данных для учебной платформы на основе требований `task.md` и `task-part2.md`.

**Границы:**
- Этап включает **только** написание документа проектирования (`docs/data-model.md`).
- Этап **не включает** написание кода (Java-классов, конфигурации, контроллеров).
- Документ должен быть достаточным для точной реализации всех 15 сущностей, связей и бизнес-правил на следующем этапе (Stage 02).

## 2. Результаты этапа (Deliverables)
- Файл `docs/data-model.md`, содержащий:
  1. Описание 15 JPA-сущностей (поля, типы, аннотации, ограничения `@Column`, `@Table`).
  2. Схему связей (1:1, 1:M, M:M) с указанием владельца (`@ManyToOne`, `@JoinColumn`).
  3. Настройки каскадирования (`CascadeType`) и стратегии загрузки (`FetchType`).
  4. Логику защиты учебной истории (`@PreRemove` условия).
  5. Таблицу верификации покрытия требований.

## 3. Детализация модели (Справочные данные для планограммиста)

На основе `task.md` и `task-part2.md` планируется следующая структура сущностей:

### 3.1. Пользователи и доступ
- **User**: `id`, `login` (unique), `password`, `role` (enum: STUDENT, TEACHER, ADMIN).
- **Profile**: `id`, `displayName`, `bio`, `userId` (unique). Связь 1:1 с User.

### 3.2. Организация курсов
- **Category**: `id`, `name`. Связь 1:M с Course.
- **Tag**: `id`, `name`. Связь M:M с Course через таблицу `course_tag`.
- **Course**: `id`, `title`, `description`, `teacherId` (FK to User), `categoryId` (FK to Category).
  - Связи: 1:M с Module, 1:M с Enrollment, 1:M с CourseReview, M:M с Tag.

### 3.3. Структура курса (Curriculum)
- **Module**: `id`, `title`, `courseId` (FK), `orderIndex`.
  - Связи: 1:M с Lesson, 1:1 с Quiz (optional, `@PrimaryKeyJoinColumn`).
- **Lesson**: `id`, `title`, `content` (text), `moduleId` (FK).
  - Связи: 1:M с Assignment.
- **Assignment**: `id`, `title`, `deadline`, `maxScore`, `lessonId` (FK).
  - Связи: 1:M с Submission.

### 3.4. Оценка и тестирование
- **Quiz**: `id`, `title`, `moduleId` (FK, unique), `maxScore`.
  - Связи: 1:M с Question, 1:M с QuizSubmission.
- **Question**: `id`, `text`, `quizId` (FK).
  - Связи: 1:M с AnswerOption.
- **AnswerOption**: `id`, `text`, `isCorrect`.
- **QuizSubmission**: `id`, `userId` (FK), `quizId` (FK), `score`, `submittedAt`.
  - Бизнес-правило: Один пользователь может иметь много записей по одному Quiz.
- **Submission**: `id`, `userId` (FK), `assignmentId` (FK), `score`, `status`, `submittedAt`.
  - Бизнес-правило: Ограничение уникальности (`@UniqueConstraint` на `userId` + `assignmentId`).

### 3.5. Взаимодействие (Enrollment & Reviews)
- **Enrollment**: `id`, `userId` (FK), `courseId` (FK), `enrollDate`, `status` (enum: ACTIVE, DROPPED).
  - Бизнес-правило: `@UniqueConstraint` на `userId` + `courseId`.
- **CourseReview**: `id`, `userId` (FK), `courseId` (FK), `rating`, `comment`.
  - Бизнес-правило: `@UniqueConstraint` на `userId` + `courseId`.

## 4. Правила удаления и защиты истории (History Protection)
Документ должен четко описывать логику `@PreRemove`:

1. **User**: Удаление запрещено, если есть связанные `Enrollment`, `Submission`, `QuizSubmission`, `CourseReview` или `Profile`.
2. **Course**: Удаление запрещено, если есть `Enrollment` (статус ACTIVE), `CourseReview`.
3. **Enrollment**: Удаление (отписка) допускается только если `status == ACTIVE`. При `DROPPED` запись становится частью архива истории и удалению не подлежит.
4. **Submission / QuizSubmission**: Не подлежат удалению (являются частью истории).
5. **Module / Lesson**: Удаление запрещено при наличии дочерних элементов (Assignments, Lessons, Enrollments).

## 5. Детальный план работ (Actions)

1. **Подготовка структуры документа**: Создать `docs/data-model.md`.
2. **Описание сущностей**: Для каждой из 15 сущностей описать:
   - Имя класса и `@Entity(name="...")`.
   - Список полей, типы, `@Column` (nullable, unique, length).
   - Специфические аннотации (`@Enumerated`, `@Lob`).
3. **Описание связей**:
   - Схематично показать граф связей (Mermaid или текстовый вид).
   - Указать `FetchType` для всех коллекций (по умолчанию LAZY).
   - Определить владельцы связей (кто хранит `@JoinColumn`).
4. **Настройка каскадов и исключений**:
   - Описать, что передается при удалении (например, `REMOVE` для Course->Module, но НЕ для Course->Enrollment).
   - Определить `orphanRemoval` там, где это уместно (например, для Questions -> AnswerOptions).
5. **Описание ограничений и проверок**:
   - `@UniqueConstraint` для Enrollment, Submission, CourseReview.
   - `@PreRemove` хуки для защиты истории.
6. **Финальная проверка (Verification)**:
   - Сверить список сущностей с требуемыми 15 шт.
   - Проверить покрытие всех связей из `task.md`.
   - Убедиться, что `course_tag` описан как `@JoinTable` без отдельной сущности.

## 6. Критерии приёмки (Acceptance Criteria)
1. Файл `docs/data-model.md` создан и существует.
2. Перечислены все 15 сущностей: `User`, `Profile`, `Course`, `Category`, `Enrollment`, `Module`, `Lesson`, `Assignment`, `Submission`, `Quiz`, `Question`, `AnswerOption`, `QuizSubmission`, `CourseReview`, `Tag`.
3. Описано отношение M:M между `Course` и `Tag` через Join Table (без сущности `CourseTag`).
4. Описано отношение M:M между `User` и `Course` через сущность `Enrollment`.
5. Указаны стратегии загрузки (`FetchType.LAZY`) для всех коллекций.
6. Определена защита от удаления записей, имеющих дочернюю активность (Enrollments, Submissions, Reviews).

## 7. АDR и Риски
- **ADR**: В проекте нет утверждённых ADR.
- **ADR_REQUIRED**: Нет. Модель полностью следует требованиям `task.md` и `task-part2.md`.
- **Риски**: Сложность описания каскадов при удалении `User` и `Course`. Требуется особая внимательность при описании `@PreRemove` логики, чтобы не заблокировать удаление активных курсов при наличии неактивной истории.

## 8. Проверки и команды
- Проверка существования файла:
  ```bash
  ls -l docs/data-model.md
  ```
- Проверка количества сущностей в тексте (должно быть ровно 15 ключевых слов-имён):
  ```bash
  grep -oE "User|Profile|Course|Category|Enrollment|Module|Lesson|Assignment|Submission|Quiz|Question|AnswerOption|QuizSubmission|CourseReview|Tag" docs/data-model.md | sort -u | wc -l
  ```
