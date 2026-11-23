# Этап 19. API тестов, отзывов и тегов — подробный план (актуализация)

## Цель и границы этапа

Исправить 3 отключённых `@Disabled` HTTP-теста в `QuizReviewTagControllerTest.java` и завершить этап 19:
- `getOptionsByQuestion_shouldReturn200` — ожидается 2 варианта, возвращается 4 (или 404 из-за отсутствующего эндпоинта).
- `getQuizSubmission_shouldReturn200` — POST `/api/quiz/{quizId}/submit` возвращает 400 («Тест не готов: нет вопросов»).
- `deleteQuiz_hasSubmissions_shouldReturn400` — тот же сбой на `POST /api/quiz/{quizId}/submit`.

**Границы:**
- Не создаём новых сущностей, DTO или контроллеров.
- Исправляем двунаправленные связи в сервисных методах `QuizService.createQuestion` и `AnswerOptionService.createAnswerOption`.
- Добавляем недостающий GET-эндпоинт для вариантов ответа вопроса.
- Модифицируем тесты для гарантии корректного состояния после создания сущностей.
- Удаляем дублирование DELETE в `cleanUp()`.

## ADR

ADR не требуется. Индекс `docs/adr/README.md` пуст — изменения касаются только двунаправленных связей и тестов.

## Связанные файлы

### Исходные файлы (изменение):

1. **`src/main/java/com/studying/platform/service/QuizService.java`** — метод `createQuestion(Long quizId, ...)` — добавить поддержку двунаправленной связи (`quiz.getQuestions().add(question)`).
2. **`src/main/java/com/studying/platform/service/AnswerOptionService.java`** — метод `createAnswerOption(Long questionId, ...)` — добавить поддержку двунаправленной связи (`question.getOptions().add(option)`).
3. **`src/main/java/com/studying/platform/controller/QuestionController.java`** — добавить `@GetMapping("/{questionId}/options")` для возврата списка вариантов ответа.
4. **`src/main/java/com/studying/platform/service/QuizSubmissionService.java`** — метод `startQuiz` — после `JOIN FETCH` пересобрать bidirectional relationship (загрузка вопросов и вариантов с явным добавлением в родительские коллекции).

### Тестовые файлы (изменение):

5. **`src/test/java/com/studying/platform/controller/QuizReviewTagControllerTest.java`** — исправить 3 `@Disabled`-теста и `cleanUp()`.

### Сущности (чтение, без изменений):

- `Quiz` — `@OneToMany(mappedBy = "quiz", fetch = LAZY) Set<Question> questions`
- `Question` — `@ManyToOne Quiz quiz`, `@OneToMany(mappedBy = "question", fetch = LAZY) Set<AnswerOption> options`
- `AnswerOption` — `@ManyToOne Question question`

## Порядок действий

### Действие 1. Добавить недостающий GET-эндпоинт вариантов ответа

**Файл:** `QuestionController.java`

Добавить метод в `QuestionController`:

```java
/**
 * Получает список всех вариантов ответа указанного вопроса.
 * <p>
 * Возвращает HTTP 200 со списком DTO вариантов или ошибку 404, если вопрос не найден.
 * </p>
 *
 * @param questionId идентификатор вопроса
 * @return HTTP 200 со списком DTO вариантов ответа
 */
@GetMapping("/{questionId}/options")
public ResponseEntity<List<AnswerOptionResponseDto>> getOptionsByQuestion(
        @PathVariable Long questionId) {
    List<AnswerOption> options = answerOptionService.getOptionsByQuestion(questionId);
    List<AnswerOptionResponseDto> response = options.stream()
            .map(this::toOptionResponseDto)
            .collect(Collectors.toList());
    return ResponseEntity.ok(response);
}
```

**Файл:** `AnswerOptionService.java` — добавить метод:

```java
/**
 * Возвращает список всех вариантов ответа указанного вопроса.
 * <p>
 * Проверяет существование вопроса. Если вопрос не найден —
 * выбрасывает {@link ResourceNotFoundException}.
 * </p>
 *
 * @param questionId идентификатор вопроса
 * @return список вариантов ответа
 * @throws ResourceNotFoundException если вопрос не найден
 */
@Transactional(readOnly = true)
public List<AnswerOption> getOptionsByQuestion(Long questionId) {
    if (!questionRepository.existsById(questionId)) {
        throw new ResourceNotFoundException(
                "Вопрос с идентификатором " + questionId + " не найден");
    }
    return answerOptionRepository.findByQuestionId(questionId);
}
```

**Обоснование:** Тест `getOptionsByQuestion_shouldReturn200` вызывает `GET /api/question/{questionId}/options`. В текущем `QuestionController` нет такого эндпоинта (есть только `GET /api/question/{questionId}` и `GET /api/question/{optionId}/options`). Без эндпоинта тест получает 404.

---

### Действие 2. Исправить двунаправленные связи в сервисах

#### 2.1 `QuizService.createQuestion`

**Файл:** `QuizService.java` — метод `createQuestion(Long quizId, String text, QuizQuestionType type)`:

Текущий код:
```java
public Question createQuestion(Long quizId, String text, QuizQuestionType type) {
    Quiz quiz = findQuiz(quizId);

    Question question = new Question(quiz, text, type);
    entityManager.persist(question);
    entityManager.flush();
    return question;
}
```

Исправленный код:
```java
public Question createQuestion(Long quizId, String text, QuizQuestionType type) {
    Quiz quiz = findQuiz(quizId);

    Question question = new Question(quiz, text, type);
    // Поддержка двунаправленной связи
    quiz.getQuestions().add(question);
    question.setQuiz(quiz);
    entityManager.persist(question);
    entityManager.flush();
    return question;
}
```

**Обоснование:** Без добавления вопроса в `quiz.getQuestions()` коллекция вопросов викторины остаётся пустой в памяти. При использовании `JOIN FETCH` в `startQuiz` / `submitAnswers` Hibernate загружает вопросы из БД и заполняет коллекцию. Но если тестовый `cleanUp()` удалил вопросы через JDBC (минуя JPA), то `JOIN FETCH` не найдёт их, и `quiz.getQuestions()` окажется пустым. Поддержание двунаправленной связи гарантирует, что `quiz.getQuestions()` содержит все вопросы, сохранённые через `QuizService`.

#### 2.2 `AnswerOptionService.createAnswerOption`

**Файл:** `AnswerOptionService.java` — метод `createAnswerOption`:

Текущий код:
```java
public AnswerOption createAnswerOption(Long questionId, String text, Boolean isCorrect) {
    Question question = questionRepository.findById(questionId)
            .orElseThrow(...);

    AnswerOption option = new AnswerOption(question, text, isCorrect);
    entityManager.persist(option);
    entityManager.flush();
    return option;
}
```

Исправленный код:
```java
public AnswerOption createAnswerOption(Long questionId, String text, Boolean isCorrect) {
    Question question = questionRepository.findById(questionId)
            .orElseThrow(...);

    AnswerOption option = new AnswerOption(question, text, isCorrect);
    // Поддержка двунаправленной связи
    question.getOptions().add(option);
    option.setQuestion(question);
    entityManager.persist(option);
    entityManager.flush();
    return option;
}
```

**Обоснование:** Аналогично — без добавления варианта в `question.getOptions()` коллекция вариантов остаётся пустой. При `JOIN FETCH` в `startQuiz` и `submitAnswers` Hibernate подгружает варианты из БД, но поддержание связи на уровне Java-объектов гарантирует консистентность.

#### 2.3 `QuizSubmissionService.startQuiz` — пересборка bidirectional relationship

**Файл:** `QuizSubmissionService.java` — метод `startQuiz`:

Текущий код:
```java
@Transactional(readOnly = true)
public Quiz startQuiz(Long quizId) {
    Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
            .orElseThrow(...);

    if (quiz.getQuestions().isEmpty()) {
        throw new IllegalStateException("Тест не готов к прохождению: нет вопросов");
    }

    return quiz;
}
```

Исправленный код:
```java
@Transactional(readOnly = true)
public Quiz startQuiz(Long quizId) {
    Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
            .orElseThrow(...);

    // Пересборка двунаправленной связи после JOIN FETCH
    Set<Question> loadedQuestions = new HashSet<>(quiz.getQuestions());
    quiz.getQuestions().clear();
    for (Question q : loadedQuestions) {
        quiz.getQuestions().add(q);
    }

    if (quiz.getQuestions().isEmpty()) {
        throw new IllegalStateException("Тест не готов к прохождению: нет вопросов");
    }

    return quiz;
}
```

**Обоснование:** `JOIN FETCH` загружает вопросы и варианты из БД, но Hibernate не всегда автоматически заполняет родительскую коллекцию в bidirectional relationship. Пересборка гарантирует, что `quiz.getQuestions()` не будет пустым при корректных данных в БД.

---

### Действие 3. Исправить 3 отключённых теста

#### 3.1 `getOptionsByQuestion_shouldReturn200`

**Проблема:** Ожидается 2 варианта, возвращается 4 (или 404 из-за отсутствующего эндпоинта).

**Решение:**
1. Добавить эндпоинт (Действие 1).
2. После создания вопросов и вариантов — перезагрузить вопрос через `entityManager.find(Question.class, questionId)`, чтобы убедиться, что двунаправленная связь установлена корректно.
3. Добавить прямое SQL-утверждение для проверки количества вариантов в БД (как запасной вариант).

**Обновлённая реализация теста (фрагмент):**
```java
@Test
void getOptionsByQuestion_shouldReturn200() throws Exception {
    // ... (создание teacher, category, course, module, quiz) ...
    // ... (создание вопроса) ...

    // Создаём 2 варианта
    AnswerOptionRequestDto optDto1 = new AnswerOptionRequestDto("Ответ А", false);
    mockMvc.perform(post("/api/question/" + question.getId() + "/options")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(optDto1)))
            .andExpect(status().isCreated());

    AnswerOptionRequestDto optDto2 = new AnswerOptionRequestDto("Ответ Б", true);
    mockMvc.perform(post("/api/question/" + question.getId() + "/options")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(optDto2)))
            .andExpect(status().isCreated());

    // Перезагрузка вопроса из БД для проверки bidirectional связи
    Question reloadedQuestion = entityManager.find(Question.class, question.getId());
    assertNotNull(reloadedQuestion);
    assertEquals(2, reloadedQuestion.getOptions().size(),
            "Вопрос должен содержать ровно 2 варианта ответа");

    // Прямая SQL-проверка
    Integer optionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM answeroption WHERE question_id = ?",
            Integer.class, question.getId());
    assertEquals(2, optionCount, "В БД должно быть 2 варианта для этого вопроса");

    // HTTP-тест эндпоинта
    mockMvc.perform(get("/api/question/" + question.getId() + "/options"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
}
```

#### 3.2 `getQuizSubmission_shouldReturn200`

**Проблема:** POST `/api/quiz/{quizId}/submit` возвращает 400 («Тест не готов: нет вопросов»).

**Решение:**
1. Исправить двунаправленные связи (Действие 2.1, 2.2) — это гарантирует, что `quiz.getQuestions()` не пуст после создания вопроса и вариантов.
2. После создания вопроса и варианта — перезагрузить викторину через `entityManager.find(Quiz.class, quizId)` и убедиться, что вопросы загружены.
3. Убрать `@Disabled`.

**Обновлённая реализация теста (фрагмент):**
```java
@Test
void getQuizSubmission_shouldReturn200() throws Exception {
    // ... (создание teacher, category, course, module) ...
    // ... (создание викторины) ...

    // Создание вопроса
    QuestionRequestDto qDto = new QuestionRequestDto("Вопрос?", QuizQuestionType.SINGLE_CHOICE);
    MvcResult qResult = mockMvc.perform(post("/api/quiz/" + quiz.getId() + "/questions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(qDto)))
            .andExpect(status().isCreated())
            .andReturn();

    QuestionResponseDto question = objectMapper.readValue(
            qResult.getResponse().getContentAsString(), QuestionResponseDto.class);
    createdQuestionIds.add(question.getId());

    // Создание варианта
    AnswerOptionRequestDto optDto =
            new AnswerOptionRequestDto("Ответ", false);
    MvcResult optResult = mockMvc.perform(post("/api/question/" + question.getId() + "/options")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(optDto)))
            .andExpect(status().isCreated())
            .andReturn();

    AnswerOptionResponseDto option = objectMapper.readValue(
            optResult.getResponse().getContentAsString(), AnswerOptionResponseDto.class);
    createdOptionIds.add(option.getId());

    // Перезагрузка викторины для проверки bidirectional связи
    Quiz reloadedQuiz = entityManager.find(Quiz.class, quiz.getId());
    assertNotNull(reloadedQuiz);
    assertFalse(reloadedQuiz.getQuestions().isEmpty(),
            "Викторина должна содержать вопросы после создания");

    Long studentId = createUser("student-sub-get-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
    enrollStudent(studentId, courseId);

    SubmitAnswersRequestDto submitDto = new SubmitAnswersRequestDto(
            studentId, Map.of(question.getId(), Set.of(option.getId())));

    MvcResult result = mockMvc.perform(post("/api/quiz/" + quiz.getId() + "/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(submitDto)))
            .andExpect(status().isCreated())
            .andReturn();

    QuizSubmissionResponseDto submission = objectMapper.readValue(
            result.getResponse().getContentAsString(), QuizSubmissionResponseDto.class);
    createdSubmissionIds.add(submission.getId());

    mockMvc.perform(get("/api/submissions/quiz/" + submission.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quizId").value(quiz.getId()))
            .andExpect(jsonPath("$.studentId").value(studentId))
            .andExpect(jsonPath("$.score").value(0));
}
```

#### 3.3 `deleteQuiz_hasSubmissions_shouldReturn400`

**Проблема:** Тот же сбой на POST `/api/quiz/{quizId}/submit`.

**Решение:** Аналогично п. 3.2 — исправить bidirectional связи, перезагрузить викторину после создания вопроса/варианта, убрать `@Disabled`.

**Обновлённая реализация теста (фрагмент):**
```java
@Test
void deleteQuiz_hasSubmissions_shouldReturn400() throws Exception {
    // ... (создание teacher, category, course, module) ...
    // ... (создание викторины) ...

    // Создание вопроса
    QuestionRequestDto qDto = new QuestionRequestDto("Вопрос?", QuizQuestionType.SINGLE_CHOICE);
    MvcResult qResult = mockMvc.perform(post("/api/quiz/" + quiz.getId() + "/questions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(qDto)))
            .andExpect(status().isCreated())
            .andReturn();

    QuestionResponseDto question = objectMapper.readValue(
            qResult.getResponse().getContentAsString(), QuestionResponseDto.class);
    createdQuestionIds.add(question.getId());

    // Создание варианта
    AnswerOptionRequestDto optDto =
            new AnswerOptionRequestDto("Ответ", false);
    MvcResult optResult = mockMvc.perform(post("/api/question/" + question.getId() + "/options")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(optDto)))
            .andExpect(status().isCreated())
            .andReturn();

    AnswerOptionResponseDto option = objectMapper.readValue(
            optResult.getResponse().getContentAsString(), AnswerOptionResponseDto.class);
    createdOptionIds.add(option.getId());

    // Перезагрузка викторины для проверки bidirectional связи
    Quiz reloadedQuiz = entityManager.find(Quiz.class, quiz.getId());
    assertNotNull(reloadedQuiz);
    assertFalse(reloadedQuiz.getQuestions().isEmpty(),
            "Викторина должна содержать вопросы после создания");

    Long studentId = createUser("student-delsub-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
    enrollStudent(studentId, courseId);

    SubmitAnswersRequestDto submitDto = new SubmitAnswersRequestDto(
            studentId, Map.of(question.getId(), Set.of(option.getId())));
    mockMvc.perform(post("/api/quiz/" + quiz.getId() + "/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(submitDto)))
            .andExpect(status().isCreated());

    mockMvc.perform(delete("/api/quiz/" + quiz.getId()))
            .andExpect(status().isBadRequest());
}
```

---

### Действие 4. Очистка `cleanUp()`

**Файл:** `QuizReviewTagControllerTest.java`

Удалить дублирование `DELETE FROM modules` (строки 129 и 135). Оставить единственную очистку после удаления quiz.

Также удалить дублирующую строку `jdbcTemplate.update("delete from modules");` перед `jdbcTemplate.update("delete from enrollments");`.

---

### Действие 5. Удалить `@Disabled` и комментарии TODO

Удалить аннотации `@Disabled` и комментарии "Временное отключение" из трёх тестов:
- `getOptionsByQuestion_shouldReturn200` (строка ~528)
- `getQuizSubmission_shouldReturn200` (строка ~690)
- `deleteQuiz_hasSubmissions_shouldReturn400` (строка ~988)

---

## Критерии приёмки

1. **Двунаправленные связи поддерживаются:** `QuizService.createQuestion` добавляет вопрос в `quiz.getQuestions()`, `AnswerOptionService.createAnswerOption` добавляет вариант в `question.getOptions()`.
2. **Эндпоинт `GET /api/question/{questionId}/options`** возвращает список вариантов ответа с HTTP 200.
3. **`startQuiz` пересобирает bidirectional связь** после `JOIN FETCH`.
4. **Все 3 отключённых теста проходят** с `PASS`:
   - `getOptionsByQuestion_shouldReturn200` — возвращает 200 с массивом из 2 вариантов.
   - `getQuizSubmission_shouldReturn200` — POST submit → 201, GET submission → 200.
   - `deleteQuiz_hasSubmissions_shouldReturn400` — POST submit → 201, DELETE quiz → 400.
5. **Общий `mvn test` — 0 ошибок, 0 пропусков.**
6. **`cleanUp()` без дублирования.**
7. **JavaDoc на русском** для всех изменённых/добавленных методов.

## Команды проверки

```bash
# Компиляция проекта
mvn compile

# Запуск всех тестов (включая исправленные)
mvn test

# Проверка формата без проблем
git diff --check
```

## Ограничения, риски и блокеры

### Риски

1. **Порядок выполнения тестов JUnit 5:** JUnit 5 не гарантирует порядок выполнения `@Test` методов. Если тесты выполняются в непредсказуемом порядке, данные от предыдущего теста могут повлиять на следующий. Решение: каждый тест создаёт полные данные через API, `cleanUp()` полностью удаляет все данные после каждого теста.
2. **`EntityManager` между транзакциями:** `@BeforeEach entityManager.clear()` сбрасывает L1-кэш, но если `@Transactional` отсутствует на уровне класса, каждая API-транзакция изолирована. Двунаправленные связи решают проблему консистентности в пределах транзакции.
3. **`JOIN FETCH` и bidirectional relationship:** Hibernate может не заполнять родительскую коллекцию при `JOIN FETCH`, если `@OneToMany` использует `mappedBy`. Пересборка в `startQuiz` гарантирует консистентность.

### Блокеры

- Отсутствуют. Все зависимости (сущности, сервисы, репозитории) уже реализованы.
- ADR не требуются.

### Ограничения

- Тесты требуют запущенного Docker (Testcontainers PostgreSQL), как и все тесты проекта.
- Исправление двунаправленных связей может повлиять на существующие сервисные вызовы — необходимо убедиться, что другие тесты не нарушаются (проверяется через `mvn test`).

## Примечания

### Корневая причина проблем

После детального анализа исходного кода выявлены две корневые причины:

1. **Неподдерживаемые двунаправленные связи:** В `QuizService.createQuestion` и `AnswerOptionService.createAnswerOption` новые сущности (Question, AnswerOption) создаются и сохраняются через `EntityManager.persist()`, но **не добавляются в родительскую коллекцию** (`quiz.getQuestions()`, `question.getOptions()`). При использовании `JOIN FETCH` в `startQuiz` и `submitAnswers` Hibernate загружает вопросы/варианты из БД, но в некоторых сценариях (особенно после очистки `cleanUp()`) коллекция может оказаться пустой, что приводит к ошибке «Тест не готов: нет вопросов».

2. **Отсутствующий GET-эндпоинт:** В `QuestionController` отсутствует `GET /api/question/{questionId}/options` для получения списка вариантов ответа вопроса. Тест `getOptionsByQuestion_shouldReturn200` вызывал несуществующий эндпоинт, что приводило к 404 или некорректному ответу.

### Решение

- Добавить поддержку bidirectional relationship во всех методах создания сущностей.
- Пересобрать bidirectional relationship после `JOIN FETCH` в `startQuiz`.
- Добавить недостающий GET-эндпоинт.
- Добавить перезагрузку сущностей и SQL-утверждения в тесты для проверки корректности данных.
- Удалить дублирование `DELETE FROM modules` в `cleanUp()`.
