# Этап 13: Прохождение тестов (Detailed Plan)

> **Согласованное уточнение от 23 ноября 2026 года:** незавершённый тест можно сохранять, но проходить нельзя. Перед подсчётом проверяются наличие хотя бы одного вопроса и варианта у каждого вопроса, ровно один правильный вариант для `SINGLE_CHOICE` и хотя бы один — для `MULTIPLE_CHOICE`. При нарушении возвращается понятная ошибка, результат не сохраняется. Это уточнение заменяет прежнее правило ниже, по которому вопрос без правильных вариантов засчитывался неправильным, а пустой тест давал результат 0. Правило игнорирования чужого ID вопроса сохраняется. Исправление выполняется по `refactor-after-part2.md`, пункт 6.

## 1. Цель и границы этапа

Реализовать сущность `QuizSubmission`, сервис проверки ответов и вычисления результата, репозиторий для выборок, а также интеграционные тесты (PostgreSQL через Testcontainers) для сценариев прохождения теста, ограничений записи на курс и множественных попыток.

**Границы:**
- Создаются: `QuizSubmission` (сущность), `QuizSubmissionRepository` (репозиторий), `QuizSubmissionService` (сервис).
- Обновляется: `PostgresSchemaTests` (добавление таблицы `quizsubmission` в проверки схемы).
- Не затрагиваются: REST-контроллеры (это этап 19), остальные сущности и сервисы не изменяются.
- Не создаётся ADR: используемая модель (сохранение только итогового балла) соответствует `task.md` и `plan.md` — решение о хранении итогового результата, а не выбранных ответов, уже утверждено на этапе 01.

---

## 2. Связанные файлы и применимые ADR

### Существующие файлы (только чтение для понимания)

| Файл | Роль |
|---|---|
| `src/main/java/.../entity/Quiz.java` | Родительская сущность — викторина |
| `src/main/java/.../entity/Question.java` | Вопрос викторины |
| `src/main/java/.../entity/AnswerOption.java` | Вариант ответа (поле `isCorrect`) |
| `src/main/java/.../entity/User.java` | Студент (связь ManyToOne) |
| `src/main/java/.../entity/Enrollment.java` | Запись на курс (статус ACTIVE/DROPPED) |
| `src/main/java/.../entity/EnrollmentStatus.java` | Enum статусов записи |
| `src/main/java/.../entity/Module.java` | Связь: Quiz → Module → Course |
| `src/main/java/.../entity/Course.java` | Родительский курс |
| `src/main/java/.../entity/UserRole.java` | Enum `STUDENT` / `TEACHER` / `ADMIN` |
| `src/main/java/.../exception/ResourceNotFoundException.java` | Исключение для отсутствующих сущностей |
| `src/main/java/.../repository/QuizRepository.java` | Репозиторий викторин |
| `src/main/java/.../repository/QuestionRepository.java` | Репозиторий вопросов |
| `src/main/java/.../repository/AnswerOptionRepository.java` | Репозиторий вариантов ответа |
| `src/main/java/.../repository/EnrollmentRepository.java` | Репозиторий записей (проверка `findByUserIdAndCourseIdAndStatus`) |
| `src/main/java/.../repository/UserRepository.java` | Репозиторий пользователей |
| `src/main/java/.../service/EnrollmentService.java` | Паттерн проверки записи на курс |
| `src/main/java/.../service/SubmissionService.java` | Референсный паттерн сервисного слоя (constructor injection, `@Transactional`, `EntityManager`) |
| `src/test/java/.../PostgresSchemaTests.java` | Тесты схемы БД — обновить для `quizsubmission` |
| `src/test/java/.../QuizStructureTest.java` | Паттерн тестов викторин (`@DataJpaTest`, `@Transactional`, `@Import`) |
| `src/test/java/.../SubmissionTests.java` | Референсный паттерн сервисных тестов (helper-методы, негативные сценарии) |

### АDR

Индекс `docs/adr/README.md` пуст — существенных архитектурных решений для этого этапа не требуется. Модель хранения итогового результата (а не выбранных ответов) описана в `task.md` (строки 192–206) и согласована на этапе 01.

---

## 3. Подробные действия

### Действие 13.1 — Создание сущности `QuizSubmission`

**Файл:** `src/main/java/com/studying/platform/entity/QuizSubmission.java`

**Поля:**

| Поле | Тип | Аннотация | Комментарий |
|---|---|---|---|
| `id` | `Long` | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` | Первичный ключ |
| `quiz` | `Quiz` | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "quiz_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quizsubmission_quiz"))` | Викторина, без каскада удаления |
| `student` | `User` | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quizsubmission_student"))` | Студент, без каскада удаления |
| `score` | `Integer` | `@Column(nullable = false)` | Итоговый балл (0–N) |
| `takenAt` | `LocalDateTime` | `@Column(name = "taken_at", nullable = false)` | Дата и время прохождения |

**Важные ограничения:**
- **Без каскадных операций** на `quiz` и `student` (ни `REMOVE`, ни `ALL`). Результат не должен удаляться при удалении викторины или пользователя.
- `score` и `takenAt` — не `null`.
- Имя таблицы: `quizsubmission`.
- JavaDoc на русском для класса, каждого поля, конструктора и всех методов.

### Действие 13.2 — Создание репозитория `QuizSubmissionRepository`

**Файл:** `src/main/java/com/studying/platform/repository/QuizSubmissionRepository.java`

**Методы (кроме стандартных CRUD от `JpaRepository<QuizSubmission, Long>`):**

| Метод | Описание |
|---|---|
| `List<QuizSubmission> findByStudentId(Long studentId)` | Все результаты студента |
| `List<QuizSubmission> findByQuizId(Long quizId)` | Все результаты по викторине |

JavaDoc на русском для каждого метода.

### Действие 13.3 — Создание сервиса `QuizSubmissionService`

**Файл:** `src/main/java/com/studying/platform/service/QuizSubmissionService.java`

**Зависимости (внедряются через конструктор):**
- `QuizSubmissionRepository`
- `QuizRepository`
- `QuestionRepository`
- `AnswerOptionRepository`
- `EnrollmentRepository`
- `UserRepository`
- `EntityManager`

**Методы:**

#### `QuizSubmission startQuiz(Long quizId, Long studentId)`

Описание: подготавливает данные для прохождения (если потребуется в будущем), но основное — возвращает викторину для отображения вопросов. На этом этапе метод может быть простым getter-обёрткой, поскольку структура теста уже существует.

Логика:
1. Находит викторину по `quizId` (`QuizRepository.findById`), выбрасывает `ResourceNotFoundException` если не найдена.
2. Для будущего расширения: возвращает `Quiz` с загруженными вопросами внутри транзакции (чтобы lazy-коллекция `questions` была доступна внутри транзакции).

JavaDoc: описать возвращаемую викторину, исключения, транзакционность.

#### `QuizSubmission submitAnswers(Long quizId, Long studentId, Map<Long, Set<Long>> answers)`

Описание: проверяет ответ студента, вычисляет балл, сохраняет результат.

Логика:

1. **Валидация викторины:** `QuizRepository.findById(quizId)` → `ResourceNotFoundException` если не найдена.

2. **Проверка записи на курс:**
   - Через `quiz.getModule().getCourse().getId()` получить ID курса.
   - `EnrollmentRepository.findByUserIdAndCourseIdAndStatus(studentId, courseId, EnrollmentStatus.ACTIVE)` — если не найдена → `ResourceNotFoundException("Студент с идентификатором ... не записан на курс ...")`.
   - Загружаем курс внутри транзакции (безопасный lazy-fetch).

3. **Валидация студента:** `UserRepository.findById(studentId)` — если не найден → `ResourceNotFoundException`.

4. **Загрузка структуры теста:**
   - `QuestionRepository.findByQuizId(quizId)` — все вопросы викторины.
   - Для каждого вопроса: `AnswerOptionRepository.findByQuestionId(questionId)` — все варианты ответа.
   - Собрать маппинг: `Map<Long, Set<Long>> correctOptions` — где ключ = `questionId`, значение = набор ID правильных `AnswerOption`.

5. **Вычисление результата (scoring):**
   - Для каждого вопроса викторины:
     - Получить выбранные студентом ID: `answers.get(questionId)`.
     - Сравнить с правильными ID из `correctOptions.get(questionId)`.
     - **Точное совпадение:** `selectedSet.equals(correctSet)` — только полное совпадение засчитывается.
     - Для `SINGLE_CHOICE`: `correctSet` содержит ровно 1 ID; неправильный выбор (даже один) → не засчитывается.
     - Для `MULTIPLE_CHOICE`: студент должен выбрать **все** правильные и **никаких** неправильных; пропуск хотя бы одного правильного или выбор лишнего → не засчитывается.
   - Балл: `score = correctCount * (100 / totalQuestions)` — целочисленное деление, либо `score = (correctCount * 100) / totalQuestions`.

6. **Обработка лишних/чужих ID:**
   - Если в `answers` передан `questionId`, которого нет в викторине — этот вопрос игнорируется (не засчитывается, но и не вызывает ошибку).
   - Прежнее правило засчитывать вопрос без правильных вариантов как неправильный заменено согласованным уточнением в начале документа: неготовый тест не проходится.

7. **Сохранение результата:**
   - Создать `QuizSubmission` с полями: `quiz`, `student`, `score`, `takenAt = LocalDateTime.now()`.
   - `entityManager.persist(submission)`, `entityManager.flush()`.
   - Вернуть сохранённый объект.

**Исключения:**
- `ResourceNotFoundException` — викторина не найдена, студент не записан на курс, студент не найден.
- `IllegalArgumentException` — аргумент `answers` равен `null`.

JavaDoc на русском для каждого параметра и исключения.

### Действие 13.4 — Обновление `PostgresSchemaTests`

**Файл:** `src/test/java/com/studying/platform/PostgresSchemaTests.java`

**Изменения:**

1. **`requiredTablesExist`:** добавить `"quizsubmission"` в массив `expectedTables`.

2. **`primaryKeysExist`:** добавить `"quizsubmission", Set.of("id")` в `expectedPks`.

3. **`foreignKeysExist`:** добавить два внешних ключа:
   - `"quizsubmission", "quiz_id" → "quiz", "id"`
   - `"quizsubmission", "student_id" → "users", "id"`

4. **Не добавлять уникальных ограничений** для `quizsubmission` — их нет в модели.

5. Обновить `expectedTables` в `uniqueConstraintsExist` (если проверяются «неожиданные» таблицы) — убедиться, что `quizsubmission` не фигурирует там.

### Действие 13.5 — Создание тестов `QuizSubmissionTests`

**Файл:** `src/test/java/com/studying/platform/QuizSubmissionTests.java`

**Аннотации (шаблон, по образцу `SubmissionTests` и `QuizStructureTest`):**

```java
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Import(QuizSubmissionService.class)
class QuizSubmissionTests { ... }
```

**Помощники (helper-методы):** те же паттерны, что в `QuizStructureTest` и `SubmissionTests`:
- `createCategory(name)`, `createTeacher(email)`, `createStudent(email)`, `createCourse(title, teacher, category)`, `createModule(title, course, orderIndex)`, `createQuiz(module, title, timeLimit)`, `createQuestion(quiz, text, type)`, `createAnswerOption(question, text, isCorrect)`, `createEnrollment(user, course, status)`.

**Тест-кейсы:**

#### Позитивные сценарии

| № | Название теста | Описание |
|---|---|---|
| T1 | `submitAnswers_singleChoice_correct` | Викторина с 1 вопросом SINGLE_CHOICE, правильный ответ → score = 100 |
| T2 | `submitAnswers_multipleChoice_exactMatch` | Викторина с 1 вопросом MULTIPLE_CHOICE (2 правильных из 4), точное совпадение → score = 100 |
| T3 | `submitAnswers_mixedQuestions_partialScore` | Викторина: 2 вопроса SINGLE_CHOICE + 1 MULTIPLE_CHOICE; 2 правильных из 3 → score = 66 (целочисленное деление) |
| T4 | `submitAnswers_allCorrectFullScore` | Викторина из 3 вопросов, все ответы правильные → score = 100 |
| T5 | `submitAnswers_multipleAttempts_newRecords` | Две попытки: обе сохраняются как отдельные записи (`QuizSubmission`); счётчик записей = 2 |
| T6 | `findByStudentId_returnsResults` | После 2 попыток студента `findByStudentId` возвращает 2 записи |
| T7 | `findByQuizId_returnsResults` | После 3 попыток по викторине `findByQuizId` возвращает 3 записи |
| T8 | `submitAnswers_textStorageViaJpaAndSql` | Проверка хранения `score` через JPA и прямой SQL (аналогично SubmissionTests) |

#### Негативные сценарии

| № | Название теста | Описание |
|---|---|---|
| N1 | `submitAnswers_nonExistentQuiz_throws` | `quizId = 99999` → `ResourceNotFoundException` |
| N2 | `submitAnswers_studentNotEnrolled_throws` | Студент не записан на курс викторины → `ResourceNotFoundException` |
| N3 | `submitAnswers_afterDropEnrollment_throws` | Студент был записан, потом отписался (DROPPED) → `ResourceNotFoundException` |
| N4 | `submitAnswers_singleChoice_wrongAnswer` | SINGLE_CHOICE: неправильный вариант → score = 0 |
| N5 | `submitAnswers_multipleChoice_missingCorrect` | MULTIPLE_CHOICE: выбран 1 из 2 правильных → score = 0 (не точное совпадение) |
| N6 | `submitAnswers_multipleChoice_extraWrong` | MULTIPLE_CHOICE: выбраны все правильные + один неправильный → score = 0 (не точное совпадение) |
| N7 | `submitAnswers_foreignOptionId` | Указан ID варианта, который не принадлежит ни одному вопросу викторины → вопрос не засчитан, score = 0 для этого вопроса |
| N8 | `submitAnswers_nullAnswersMap_throws` | `answers = null` → `IllegalArgumentException` |
| N9 | `submitAnswers_nonExistentStudent_throws` | Студент не найден → `ResourceNotFoundException` |
| N10 | `quizSubmission_nullQuiz_throws` | Прямое сохранение с `quiz = null` → `ConstraintViolationException` (NOT NULL FK) |
| N11 | `quizSubmission_nullStudent_throws` | Прямое сохранение с `student = null` → `ConstraintViolationException` (NOT NULL FK) |
| N12 | `quizSubmission_nullScore_throws` | Прямое сохранение с `score = null` → `ConstraintViolationException` (NOT NULL) |
| N13 | `quizSubmission_nullTakenAt_throws` | Прямое сохранение с `takenAt = null` → `ConstraintViolationException` (NOT NULL) |

#### Проверка сохранения результата

| № | Название теста | Описание |
|---|---|---|
| S1 | `submitAnswers_savesResult` | Проверить, что `QuizSubmission` сохранён в БД: `QuizSubmissionRepository.findById` возвращает запись с правильными полями |
| S2 | `submitAnswers_takenAtIsCurrent` | `takenAt` — не `null`, в пределах текущей минуты |
| S3 | `submitAnswers_noCascadeDelete` | Сохранить `QuizSubmission`, затем удалить викторину — результат должен остаться (проверка: `QuizSubmissionRepository.count()` не изменился). **Важно:** на уровне БД FK-ограничение `quizsubmission.quiz_id → quiz.id` предотвратит удаление викторины при наличии результатов, если `ON DELETE RESTRICT` (JPA по умолчанию). Этот тест должен подтверждать отсутствие каскадного удаления на уровне JPA. |

---

## 4. Критерии приёмки

1. **Сущность `QuizSubmission`** создана с полями `id`, `quiz` (ManyToOne, без каскада удаления), `student` (ManyToOne, без каскада удаления), `score` (Integer, NOT NULL), `takenAt` (LocalDateTime, NOT NULL). Таблица `quizsubmission` существует в PostgreSQL.
2. **Репозиторий `QuizSubmissionRepository`** создан с методами `findByStudentId` и `findByQuizId`.
3. **Сервис `QuizSubmissionService`** реализован с методами `startQuiz` и `submitAnswers`.
4. **Проверка записи на курс:** незаписанный студент не может пройти тест; отписавшийся студент (DROPPED) не может пройти тест.
5. **Вычисление балла:** точное совпадение множества выбранных ID с множеством правильных ID для каждого вопроса; частичное совпадение → вопрос не засчитан.
6. **Множественные попытки:** каждый вызов `submitAnswers` создаёт новую запись `QuizSubmission`.
7. **Выборки:** `findByStudentId` и `findByQuizId` возвращают корректные списки.
8. **Тесты схемы:** `PostgresSchemaTests` обновлены и проходят.
9. **JavaDoc:** на русском языке для всех новых классов, методов, конструкторов, полей.
10. **Идентификаторы:** на английском языке.

---

## 5. Команды проверки

### Компиляция

```bash
mvn clean compile test-compile
```

### Полный прогон тестов

```bash
mvn test
```

### Проверка только схемы PostgreSQL

```bash
mvn test -Dtest=PostgresSchemaTests
```

### Проверка только тестов викторин

```bash
mvn test -Dtest=QuizStructureTest,QuizSubmissionTests
```

> **Примечание:** все команды должны выполняться из корня проекта `/Users/sergeyluzhin/Documents/Intellij IDEA/Homework/Studying-Platform`. Требуются запущенный Docker и доступ к `postgres` образу для Testcontainers.

---

## 6. Ограничения, риски и блокеры

### Ограничения
- **Каскады:** категорически запрещено настраивать `CascadeType.REMOVE` или `CascadeType.ALL` на полях `quiz` и `student` в `QuizSubmission`. Результат теста должен сохраняться независимо от удаления родительских сущностей (на уровне JPA). На уровне БД FK-ограничение по умолчанию предотвращает удаление викторины при наличии результатов.
- **Ленивая загрузка:** при проверке записи на курс используется цепочка `quiz.getModule().getCourse().getId()` — это безопасно внутри `@Transactional` метода сервиса. В тестах с `@DataJpaTest` и `@Transactional` всё также безопасно.
- **Хранение результатов:** по модели проекта хранится только итоговый балл, а не выбранные ответы. Это утверждённое решение, зафиксированное в `task.md`.

### Риски
- **Нарушение уникальности:** если потребуется запретить повторное прохождение, нужно добавить уникальное ограничение на уровне БД или сервиса. На данном этапе повторные попытки разрешены (как указано в `plan.md`: «тест можно проходить многократно»).
- **Целочисленное деление:** при расчёте `score = (correctCount * 100) / totalQuestions` возможны потери точности (например, 1/3 → 33). Альтернатива — использовать `double` или `BigDecimal`, но `task.md` указывает `int score` в модели. Оставляем `int`.
- **Связь Module → Course:** в существующих сущностях `Module` имеет `@ManyToOne(fetch = LAZY) Course`. При проверке записи на курс цепочка `quiz → module → course` должна выполняться внутри транзакции, иначе будет `LazyInitializationException`. В тестах с `@Transactional` это гарантировано.

### Блокеры
- Нет блокеров. ADR отсутствуют (индекс пустой), реализация согласована с существующими сущностями и паттернами.
- Если в процессе реализации обнаружится необходимость существенно изменить модель (например, добавить хранение выбранных ответов вместо балла), это потребует нового ADR и блокирует этап до согласования.

---

## 7. Сводный чеклист файлов для создания/изменения

| Действие | Файл | Тип |
|---|---|---|
| 13.1 | `src/main/java/.../entity/QuizSubmission.java` | Создать |
| 13.2 | `src/main/java/.../repository/QuizSubmissionRepository.java` | Создать |
| 13.3 | `src/main/java/.../service/QuizSubmissionService.java` | Создать |
| 13.4 | `src/test/java/.../PostgresSchemaTests.java` | Изменить (добавить `quizsubmission`) |
| 13.5 | `src/test/java/.../QuizSubmissionTests.java` | Создать |

**Итого:** 3 новых файла, 1 изменённый файл, ~13 тест-кейсов.
