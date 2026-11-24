package com.studying.platform.controller;

import com.studying.platform.dto.CourseReviewRequestDto;
import com.studying.platform.dto.CourseReviewResponseDto;
import com.studying.platform.dto.EnrollmentRequestDto;
import com.studying.platform.dto.ErrorResponseDto;
import com.studying.platform.dto.QuestionRequestDto;
import com.studying.platform.dto.QuestionResponseDto;
import com.studying.platform.dto.QuizRequestDto;
import com.studying.platform.dto.QuizResponseDto;
import com.studying.platform.dto.QuizSubmissionResponseDto;
import com.studying.platform.dto.StartQuizResponseDto;
import com.studying.platform.dto.SubmitAnswersRequestDto;
import com.studying.platform.dto.TagRequestDto;
import com.studying.platform.dto.TagResponseDto;
import com.studying.platform.entity.QuizQuestionType;
import com.studying.platform.entity.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные HTTP-тесты REST-контроллеров викторин, отзывов и тегов.
 * <p>
 * Проверяют позитивные и негативные сценарии: создание викторин и вопросов,
 * прохождение тестов, отзывы, теги, а также обработку ошибок валидации
 * и бизнес-правил.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class QuizReviewTagControllerTest {

    /**
     * Mock-контроллер для отправки HTTP-запросов.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Сериализатор JSON.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Шаблон транзакций для очистки данных после каждого теста.
     */
    @Autowired
    private TransactionTemplate transactions;

    /**
     * JDBC-шаблон для прямого доступа к базе данных при очистке.
     */
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * JPA-менеджер для сброса кэша сущностей после каждого теста.
     */
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    /** Идентификаторы ресурсов, созданных текущим тестом. */
    private final com.studying.platform.HttpFixtureTracker fixtureTracker =
            new com.studying.platform.HttpFixtureTracker();


    /**
     * Очищает кэш Hibernate перед каждым тестом, чтобы данные из EntityGraph
     * предыдущих тестов не влияли на текущий тест.
     */
    @BeforeEach
    void clearEntityManager() {
        entityManager.clear();
    }

    /**
     * Сбрасывает кэш Hibernate и очищает созданные данные после каждого теста.
     */
    @AfterEach
    void cleanUp() {
        fixtureTracker.clean(jdbcTemplate, transactions);
        entityManager.clear();
    }

    /**
     * Выполняет запрос и сразу регистрирует созданный ресурс до проверок ответа.
     *
     * @param request запрос MockMvc
     * @return результат для последующих проверок
     * @throws Exception если выполнение запроса или чтение идентификатора завершилось ошибкой
     */
    private org.springframework.test.web.servlet.ResultActions perform(
            org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        return mockMvc.perform(request).andDo(result -> fixtureTracker.record(result, objectMapper));
    }

    /**
     * Создаёт пользователя через API и возвращает его ID.
     *
     * @param email адрес электронной почты
     * @param role  роль пользователя
     * @return идентификатор созданного пользователя
     */
    private Long createUser(String email, UserRole role) throws Exception {
        com.studying.platform.dto.UserCreateDto dto =
                new com.studying.platform.dto.UserCreateDto();
        dto.setEmail(email);
        dto.setRole(role);

        MvcResult result = perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        com.studying.platform.dto.UserResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                com.studying.platform.dto.UserResponseDto.class);
        return response.getId();
    }

    /**
     * Создаёт категорию через API и возвращает её ID.
     *
     * @param name название категории
     * @return идентификатор созданной категории
     */
    private Long createCategory(String name) throws Exception {
        com.studying.platform.dto.CategoryCreateDto dto =
                new com.studying.platform.dto.CategoryCreateDto(name);

        MvcResult result = perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        com.studying.platform.dto.CategoryResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                com.studying.platform.dto.CategoryResponseDto.class);
        return response.getId();
    }

    /**
     * Создаёт курс через API и возвращает его ID.
     *
     * @param teacherId  идентификатор преподавателя
     * @param categoryId идентификатор категории
     * @return идентификатор созданного курса
     */
    private Long createCourse(Long teacherId, Long categoryId) throws Exception {
        com.studying.platform.dto.CourseRequestDto dto =
                new com.studying.platform.dto.CourseRequestDto();
        dto.setTitle("Курс " + System.currentTimeMillis());
        dto.setDescription("Описание курса");
        dto.setDuration(40);
        dto.setStartDate(LocalDate.of(2026, 1, 1));
        dto.setTeacherId(teacherId);
        dto.setCategoryId(categoryId);

        MvcResult result = perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        com.studying.platform.dto.CourseResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                com.studying.platform.dto.CourseResponseDto.class);
        return response.getId();
    }

    /**
     * Создаёт модуль через API и возвращает его ID.
     *
     * @param courseId идентификатор курса
     * @param orderIndex порядковый номер модуля
     * @return идентификатор созданного модуля
     */
    private Long createModule(Long courseId, int orderIndex) throws Exception {
        com.studying.platform.dto.ModuleRequestDto dto =
                new com.studying.platform.dto.ModuleRequestDto();
        dto.setTitle("Модуль " + orderIndex);
        dto.setDescription("Описание модуля");
        dto.setOrderIndex(orderIndex);

        MvcResult result = perform(post("/api/courses/" + courseId + "/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        com.studying.platform.dto.ModuleResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                com.studying.platform.dto.ModuleResponseDto.class);
        return response.getId();
    }

    /**
     * Записывает студента на курс.
     *
     * @param studentId  идентификатор студента
     * @param courseId   идентификатор курса
     */
    private void enrollStudent(Long studentId, Long courseId) throws Exception {
        EnrollmentRequestDto dto = new EnrollmentRequestDto(studentId, courseId);
        perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    // ==================== Позитивные сценарии ====================

    /**
     * Позитивный тест: создание викторины (HTTP 201).
     */
    @Test
    void createQuiz_shouldReturn201() throws Exception {
        Long teacherId = createUser("teacher-quiz-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория викторин");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto dto = new QuizRequestDto(moduleId, "Тест по модулю 1", 30);

        MvcResult result = perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.moduleId").value(moduleId))
                .andExpect(jsonPath("$.title").value("Тест по модулю 1"))
                .andExpect(jsonPath("$.timeLimit").value(30))
                .andExpect(jsonPath("$.questionCount").value(0))
                .andReturn();

        QuizResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                QuizResponseDto.class);
        assertNotNull(response.getId());
    }

    /**
     * Позитивный тест: получение викторины (HTTP 200).
     */
    @Test
    void getQuiz_shouldReturn200() throws Exception {
        Long teacherId = createUser("teacher-quiz-get-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для получения");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto createDto = new QuizRequestDto(moduleId, "Получаемый тест", 60);
        MvcResult createResult = perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuizResponseDto created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                QuizResponseDto.class);

        perform(get("/api/quiz/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Получаемый тест"))
                .andExpect(jsonPath("$.timeLimit").value(60))
                .andExpect(jsonPath("$.questionCount").value(0));
    }

    /**
     * Позитивный тест: обновление викторины (HTTP 200).
     */
    @Test
    void updateQuiz_shouldReturn200() throws Exception {
        Long teacherId = createUser("teacher-quiz-upd-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория обновления");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto createDto = new QuizRequestDto(moduleId, "Старое название", 30);
        MvcResult createResult = perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuizResponseDto created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                QuizResponseDto.class);

        QuizRequestDto updateDto = new QuizRequestDto(moduleId, "Новое название", 45);
        perform(put("/api/quiz/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Новое название"))
                .andExpect(jsonPath("$.timeLimit").value(45));
    }

    /**
     * Позитивный тест: удаление викторины (HTTP 204).
     */
    @Test
    void deleteQuiz_shouldReturn204() throws Exception {
        Long teacherId = createUser("teacher-quiz-del-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория удаления");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto dto = new QuizRequestDto(moduleId, "Удаляемый тест", 30);
        MvcResult result = perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuizResponseDto created = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                QuizResponseDto.class);

        perform(delete("/api/quiz/" + created.getId()))
                .andExpect(status().isNoContent());
    }

    /**
     * Позитивный тест: создание вопроса (HTTP 201).
     */
    @Test
    void createQuestion_shouldReturn201() throws Exception {
        Long teacherId = createUser("teacher-q-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория вопросов");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto quizDto = new QuizRequestDto(moduleId, "Тест с вопросами", 30);
        MvcResult quizResult = perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuizResponseDto quiz = objectMapper.readValue(
                quizResult.getResponse().getContentAsString(),
                QuizResponseDto.class);

        QuestionRequestDto qDto = new QuestionRequestDto("Вопрос 1?", QuizQuestionType.SINGLE_CHOICE);
        MvcResult qResult = perform(post("/api/quiz/" + quiz.getId() + "/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.text").value("Вопрос 1?"))
                .andExpect(jsonPath("$.type").value("SINGLE_CHOICE"))
                .andReturn();

        QuestionResponseDto question = objectMapper.readValue(
                qResult.getResponse().getContentAsString(),
                QuestionResponseDto.class);
    }

    /**
     * Позитивный тест: получение списка вопросов викторины (HTTP 200).
     */
    @Test
    void getQuestionsByQuiz_shouldReturn200() throws Exception {
        Long teacherId = createUser("teacher-qs-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория списка вопросов");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto quizDto = new QuizRequestDto(moduleId, "Тест для вопросов", 30);
        MvcResult quizResult = perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuizResponseDto quiz = objectMapper.readValue(
                quizResult.getResponse().getContentAsString(),
                QuizResponseDto.class);

        // Создаём 2 вопроса
        QuestionRequestDto qDto1 = new QuestionRequestDto("Вопрос A?", QuizQuestionType.SINGLE_CHOICE);
        perform(post("/api/quiz/" + quiz.getId() + "/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qDto1)))
                .andExpect(status().isCreated());

        QuestionRequestDto qDto2 = new QuestionRequestDto("Вопрос B?", QuizQuestionType.MULTIPLE_CHOICE);
        perform(post("/api/quiz/" + quiz.getId() + "/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qDto2)))
                .andExpect(status().isCreated());

        perform(get("/api/quiz/" + quiz.getId() + "/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /**
     * Позитивный тест: создание варианта ответа (HTTP 201).
     */
    @Test
    void createAnswerOption_shouldReturn201() throws Exception {
        Long teacherId = createUser("teacher-opt-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория вариантов");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto quizDto = new QuizRequestDto(moduleId, "Тест с вариантами", 30);
        MvcResult quizResult = perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuizResponseDto quiz = objectMapper.readValue(
                quizResult.getResponse().getContentAsString(),
                QuizResponseDto.class);

        QuestionRequestDto qDto = new QuestionRequestDto("Вопрос с вариантами?", QuizQuestionType.SINGLE_CHOICE);
        MvcResult qResult = perform(post("/api/quiz/" + quiz.getId() + "/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuestionResponseDto question = objectMapper.readValue(
                qResult.getResponse().getContentAsString(),
                QuestionResponseDto.class);

        com.studying.platform.dto.AnswerOptionRequestDto optDto =
                new com.studying.platform.dto.AnswerOptionRequestDto("Правильный ответ", true);
        MvcResult optResult = perform(post("/api/question/" + question.getId() + "/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(optDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.text").value("Правильный ответ"))
                .andExpect(jsonPath("$.isCorrect").value(true))
                .andReturn();

        com.studying.platform.dto.AnswerOptionResponseDto option = objectMapper.readValue(
                optResult.getResponse().getContentAsString(),
                com.studying.platform.dto.AnswerOptionResponseDto.class);
    }

    /**
     * Позитивный тест: получение вариантов ответа вопроса (HTTP 200).
     */
    @Test
    void getOptionsByQuestion_shouldReturn200() throws Exception {
        Long teacherId = createUser("teacher-opts-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для вариантов");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto quizDto = new QuizRequestDto(moduleId, "Тест для вариантов", 30);
        MvcResult quizResult = perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuizResponseDto quiz = objectMapper.readValue(
                quizResult.getResponse().getContentAsString(),
                QuizResponseDto.class);

        QuestionRequestDto qDto = new QuestionRequestDto("Вопрос с вариантами?", QuizQuestionType.SINGLE_CHOICE);
        MvcResult qResult = perform(post("/api/quiz/" + quiz.getId() + "/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuestionResponseDto question = objectMapper.readValue(
                qResult.getResponse().getContentAsString(),
                QuestionResponseDto.class);

        // Создаём 2 варианта
        com.studying.platform.dto.AnswerOptionRequestDto optDto1 =
                new com.studying.platform.dto.AnswerOptionRequestDto("Ответ А", false);
        perform(post("/api/question/" + question.getId() + "/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(optDto1)))
                .andExpect(status().isCreated());

        com.studying.platform.dto.AnswerOptionRequestDto optDto2 =
                new com.studying.platform.dto.AnswerOptionRequestDto("Ответ Б", true);
        perform(post("/api/question/" + question.getId() + "/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(optDto2)))
                .andExpect(status().isCreated());

        perform(get("/api/question/" + question.getId() + "/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /**
     * Позитивный тест: получение теста для прохождения (HTTP 200).
     */
    @Test
    void startQuiz_shouldReturn200_withQuestions() throws Exception {
        Long teacherId = createUser("teacher-start-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория старта");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto quizDto = new QuizRequestDto(moduleId, "Тест для старта", 30);
        MvcResult quizResult = perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuizResponseDto quiz = objectMapper.readValue(
                quizResult.getResponse().getContentAsString(),
                QuizResponseDto.class);

        QuestionRequestDto qDto = new QuestionRequestDto("Стартовый вопрос?", QuizQuestionType.SINGLE_CHOICE);
        perform(post("/api/quiz/" + quiz.getId() + "/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qDto)))
                .andExpect(status().isCreated());

        perform(get("/api/quiz/" + quiz.getId() + "/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(quiz.getId()))
                .andExpect(jsonPath("$.title").value("Тест для старта"))
                .andExpect(jsonPath("$.timeLimit").value(30))
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions.length()").value(1))
                .andExpect(jsonPath("$.questions[0].text").value("Стартовый вопрос?"))
                .andExpect(jsonPath("$.questions[0].options").isArray());
    }

    /**
     * Позитивный тест: прохождение теста с правильными ответами (HTTP 201).
     */
    @Test
    void submitAnswers_shouldReturn201() throws Exception {
        Long teacherId = createUser("teacher-sub-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория сдачи");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto quizDto = new QuizRequestDto(moduleId, "Тест для сдачи", 30);
        MvcResult quizResult = perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuizResponseDto quiz = objectMapper.readValue(
                quizResult.getResponse().getContentAsString(),
                QuizResponseDto.class);

        QuestionRequestDto qDto = new QuestionRequestDto("Правильный?", QuizQuestionType.SINGLE_CHOICE);
        MvcResult qResult = perform(post("/api/quiz/" + quiz.getId() + "/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuestionResponseDto question = objectMapper.readValue(
                qResult.getResponse().getContentAsString(),
                QuestionResponseDto.class);

        com.studying.platform.dto.AnswerOptionRequestDto optDto =
                new com.studying.platform.dto.AnswerOptionRequestDto("Ответ", true);
        MvcResult optResult = perform(post("/api/question/" + question.getId() + "/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(optDto)))
                .andExpect(status().isCreated())
                .andReturn();

        com.studying.platform.dto.AnswerOptionResponseDto option = objectMapper.readValue(
                optResult.getResponse().getContentAsString(),
                com.studying.platform.dto.AnswerOptionResponseDto.class);

        Long studentId = createUser("student-sub-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        enrollStudent(studentId, courseId);

        SubmitAnswersRequestDto submitDto = new SubmitAnswersRequestDto(
                studentId,
                Map.of(question.getId(), Set.of(option.getId())));

        MvcResult result = perform(post("/api/quiz/" + quiz.getId() + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.score").value(100))
                .andReturn();

        QuizSubmissionResponseDto submission = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                QuizSubmissionResponseDto.class);
    }

    /**
     * Позитивный тест: получение результата теста (HTTP 200).
     */
    @Test
    void getQuizSubmission_shouldReturn200() throws Exception {
        Long teacherId = createUser("teacher-sub-get-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория получения результатов");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto quizDto = new QuizRequestDto(moduleId, "Тест для получения", 30);
        MvcResult quizResult = perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuizResponseDto quiz = objectMapper.readValue(
                quizResult.getResponse().getContentAsString(),
                QuizResponseDto.class);

        QuestionRequestDto qDto = new QuestionRequestDto("Вопрос?", QuizQuestionType.SINGLE_CHOICE);
        MvcResult qResult = perform(post("/api/quiz/" + quiz.getId() + "/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuestionResponseDto question = objectMapper.readValue(
                qResult.getResponse().getContentAsString(),
                QuestionResponseDto.class);

        com.studying.platform.dto.AnswerOptionRequestDto optDto =
                new com.studying.platform.dto.AnswerOptionRequestDto("Ответ", true);
        MvcResult optResult = perform(post("/api/question/" + question.getId() + "/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(optDto)))
                .andExpect(status().isCreated())
                .andReturn();

        com.studying.platform.dto.AnswerOptionResponseDto option = objectMapper.readValue(
                optResult.getResponse().getContentAsString(),
                com.studying.platform.dto.AnswerOptionResponseDto.class);

        Long studentId = createUser("student-sub-get-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        enrollStudent(studentId, courseId);

        SubmitAnswersRequestDto submitDto = new SubmitAnswersRequestDto(
                studentId,
                Map.of(question.getId(), Set.of(option.getId())));

        MvcResult result = perform(post("/api/quiz/" + quiz.getId() + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuizSubmissionResponseDto submission = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                QuizSubmissionResponseDto.class);

        perform(get("/api/submissions/quiz/" + submission.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizId").value(quiz.getId()))
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.score").value(100));
    }

    /**
     * Позитивный тест: создание отзыва (HTTP 201).
     */
    @Test
    void createReview_shouldReturn201() throws Exception {
        Long teacherId = createUser("teacher-r-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория отзывов");
        Long courseId = createCourse(teacherId, categoryId);
        Long studentId = createUser("student-r-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        enrollStudent(studentId, courseId);

        CourseReviewRequestDto dto = new CourseReviewRequestDto(studentId, courseId, 5, "Отличный курс!");

        MvcResult result = perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Отличный курс!"))
                .andReturn();

        CourseReviewResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CourseReviewResponseDto.class);
    }

    /**
     * Позитивный тест: получение отзывов курса (HTTP 200).
     */
    @Test
    void getReviewsByCourse_shouldReturn200() throws Exception {
        Long teacherId = createUser("teacher-rs-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория списка отзывов");
        Long courseId = createCourse(teacherId, categoryId);
        Long studentId1 = createUser("student-rs1-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        Long studentId2 = createUser("student-rs2-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        enrollStudent(studentId1, courseId);
        enrollStudent(studentId2, courseId);

        CourseReviewRequestDto dto1 = new CourseReviewRequestDto(studentId1, courseId, 4, "Хороший курс");
        perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        CourseReviewRequestDto dto2 = new CourseReviewRequestDto(studentId2, courseId, 5, "Превосходно!");
        perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated());

        perform(get("/api/course-reviews/course/" + courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /**
     * Позитивный тест: создание тега (HTTP 201).
     */
    @Test
    void createTag_shouldReturn201() throws Exception {
        TagRequestDto dto = new TagRequestDto("Java");

        MvcResult result = perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Java"))
                .andReturn();

        TagResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TagResponseDto.class);
    }

    /**
     * Позитивный тест: получение списка тегов (HTTP 200).
     */
    @Test
    void getTags_shouldReturn200() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString();
        TagRequestDto dto1 = new TagRequestDto("Java-" + suffix);
        perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        TagRequestDto dto2 = new TagRequestDto("Hibernate-" + suffix);
        perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated());

        perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", org.hamcrest.Matchers.hasItems(dto1.getName(), dto2.getName())));
    }

    /**
     * Позитивный тест: получение тега (HTTP 200).
     */
    @Test
    void getTag_shouldReturn200() throws Exception {
        TagRequestDto dto = new TagRequestDto("Spring");
        MvcResult result = perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        TagResponseDto tag = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TagResponseDto.class);

        perform(get("/api/tags/" + tag.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Spring"));
    }

    /**
     * Позитивный тест: обновление тега (HTTP 200).
     */
    @Test
    void updateTag_shouldReturn200() throws Exception {
        TagRequestDto createDto = new TagRequestDto("JavaSE");
        MvcResult result = perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn();

        TagResponseDto tag = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TagResponseDto.class);

        TagRequestDto updateDto = new TagRequestDto("JavaEE");
        perform(put("/api/tags/" + tag.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("JavaEE"));
    }

    /**
     * Позитивный тест: удаление тега (HTTP 204).
     */
    @Test
    void deleteTag_shouldReturn204() throws Exception {
        TagRequestDto dto = new TagRequestDto("ToDelete");
        MvcResult result = perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        TagResponseDto tag = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TagResponseDto.class);

        perform(delete("/api/tags/" + tag.getId()))
                .andExpect(status().isNoContent());
    }

    // ==================== Негативные сценарии ====================

    /**
     * Негативный тест: создание викторины с несуществующим модулем (HTTP 404).
     */
    @Test
    void createQuiz_nonExistentModule_shouldReturn404() throws Exception {
        QuizRequestDto dto = new QuizRequestDto(99999L, "Тест без модуля", 30);

        perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: создание второго теста для того же модуля (HTTP 400).
     */
    @Test
    void createQuiz_duplicateModule_shouldReturn400() throws Exception {
        Long teacherId = createUser("teacher-dupq-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория дубликата викторин");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto dto1 = new QuizRequestDto(moduleId, "Первый тест", 30);
        perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        QuizRequestDto dto2 = new QuizRequestDto(moduleId, "Второй тест", 30);
        perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Негативный тест: создание викторины с пустым названием (HTTP 400).
     */
    @Test
    void createQuiz_emptyTitle_shouldReturn400() throws Exception {
        Long teacherId = createUser("teacher-qval-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория валидации викторин");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto dto = new QuizRequestDto(moduleId, "", 30);

        perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Негативный тест: удаление викторины с результатами (HTTP 400).
     */
    @Test
    void deleteQuiz_hasSubmissions_shouldReturn400() throws Exception {
        Long teacherId = createUser("teacher-delsub-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория удаления с результатами");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto quizDto = new QuizRequestDto(moduleId, "Тест с результатом", 30);
        MvcResult quizResult = perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuizResponseDto quiz = objectMapper.readValue(
                quizResult.getResponse().getContentAsString(),
                QuizResponseDto.class);

        QuestionRequestDto qDto = new QuestionRequestDto("Вопрос?", QuizQuestionType.SINGLE_CHOICE);
        MvcResult qResult = perform(post("/api/quiz/" + quiz.getId() + "/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuestionResponseDto question = objectMapper.readValue(
                qResult.getResponse().getContentAsString(),
                QuestionResponseDto.class);

        com.studying.platform.dto.AnswerOptionRequestDto optDto =
                new com.studying.platform.dto.AnswerOptionRequestDto("Ответ", true);
        MvcResult optResult = perform(post("/api/question/" + question.getId() + "/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(optDto)))
                .andExpect(status().isCreated())
                .andReturn();

        com.studying.platform.dto.AnswerOptionResponseDto option = objectMapper.readValue(
                optResult.getResponse().getContentAsString(),
                com.studying.platform.dto.AnswerOptionResponseDto.class);

        Long studentId = createUser("student-delsub-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        enrollStudent(studentId, courseId);

        SubmitAnswersRequestDto submitDto = new SubmitAnswersRequestDto(
                studentId,
                Map.of(question.getId(), Set.of(option.getId())));
        perform(post("/api/quiz/" + quiz.getId() + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isCreated());

        perform(delete("/api/quiz/" + quiz.getId()))
                .andExpect(status().isBadRequest());
    }

    /**
     * Негативный тест: прохождение несуществующего теста (HTTP 404).
     */
    @Test
    void startQuiz_nonExistent_shouldReturn404() throws Exception {
        perform(get("/api/quiz/99999/start"))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: отправка ответов на несуществующий тест (HTTP 404).
     */
    @Test
    void submitAnswers_nonExistentQuiz_shouldReturn404() throws Exception {
        SubmitAnswersRequestDto dto = new SubmitAnswersRequestDto(1L, Map.of());
        perform(post("/api/quiz/99999/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: прохождение теста без записи на курс (HTTP 404).
     */
    @Test
    void submitAnswers_notEnrolled_shouldReturn404() throws Exception {
        Long teacherId = createUser("teacher-noenr-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория без записи");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto quizDto = new QuizRequestDto(moduleId, "Тест для проверки записи", 30);
        MvcResult quizResult = perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizDto)))
                .andExpect(status().isCreated()).andReturn();
        long quizId = objectMapper.readTree(quizResult.getResponse().getContentAsString()).get("id").longValue();

        Long studentId = createUser("student-noenr-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);

        SubmitAnswersRequestDto dto = new SubmitAnswersRequestDto(studentId, Map.of());
        perform(post("/api/quiz/" + quizId + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: чужой студент в ответе (HTTP 404).
     */
    @Test
    void submitAnswers_wrongStudent_shouldReturn404() throws Exception {
        Long teacherId = createUser("teacher-wrng-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория чужого студента");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto quizDto = new QuizRequestDto(moduleId, "Тест для проверки студента", 30);
        MvcResult quizResult = perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizDto)))
                .andExpect(status().isCreated()).andReturn();
        long quizId = objectMapper.readTree(quizResult.getResponse().getContentAsString()).get("id").longValue();

        Long studentId = createUser("student-wrng-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        Long otherStudentId = createUser("student-other-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        enrollStudent(studentId, courseId);

        SubmitAnswersRequestDto dto = new SubmitAnswersRequestDto(otherStudentId, Map.of());
        perform(post("/api/quiz/" + quizId + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: создание отзыва на несуществующий курс (HTTP 404).
     */
    @Test
    void createReview_nonExistentCourse_shouldReturn404() throws Exception {
        Long studentId = createUser("student-revnx-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);

        CourseReviewRequestDto dto = new CourseReviewRequestDto(studentId, 99999L, 5, "Комментарий");

        perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: рейтинг вне диапазона (HTTP 400).
     */
    @Test
    void createReview_ratingOutOfRange_shouldReturn400() throws Exception {
        Long teacherId = createUser("teacher-revrng-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория диапазона рейтинга");
        Long courseId = createCourse(teacherId, categoryId);
        Long studentId = createUser("student-revrng-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        enrollStudent(studentId, courseId);

        CourseReviewRequestDto dto = new CourseReviewRequestDto(studentId, courseId, 0, "Плохой рейтинг");

        perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Негативный тест: повторный отзыв студента (HTTP 400).
     */
    @Test
    void createReview_duplicate_shouldReturn400() throws Exception {
        Long teacherId = createUser("teacher-revdup-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория дубликата отзывов");
        Long courseId = createCourse(teacherId, categoryId);
        Long studentId = createUser("student-revdup-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        enrollStudent(studentId, courseId);

        CourseReviewRequestDto dto = new CourseReviewRequestDto(studentId, courseId, 5, "Первый отзыв");
        perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        CourseReviewRequestDto dto2 = new CourseReviewRequestDto(studentId, courseId, 4, "Повторный отзыв");
        perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Негативный тест: отзыв от преподавателя (HTTP 404).
     */
    @Test
    void createReview_byNonStudent_shouldReturn404() throws Exception {
        Long teacherId = createUser("teacher-revnt-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория отзыва преподавателя");
        Long courseId = createCourse(teacherId, categoryId);

        CourseReviewRequestDto dto = new CourseReviewRequestDto(teacherId, courseId, 5, "Отзыв преподавателя");

        perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: создание тега с дублирующимся именем (HTTP 400).
     */
    @Test
    void createTag_duplicateName_shouldReturn400() throws Exception {
        TagRequestDto dto1 = new TagRequestDto("UniqueTag");
        perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        TagRequestDto dto2 = new TagRequestDto("UniqueTag");
        perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Негативный тест: удаление несуществующей викторины (HTTP 404).
     */
    @Test
    void deleteQuiz_nonExistent_shouldReturn404() throws Exception {
        perform(delete("/api/quiz/99999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: тест без вопросов (HTTP 400).
     */
    @Test
    void startQuiz_emptyQuiz_shouldReturn400() throws Exception {
        Long teacherId = createUser("teacher-qempty-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория пустого теста");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = createModule(courseId, 1);

        QuizRequestDto quizDto = new QuizRequestDto(moduleId, "Пустой тест", 30);
        MvcResult result = perform(post("/api/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizDto)))
                .andExpect(status().isCreated())
                .andReturn();

        QuizResponseDto quiz = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                QuizResponseDto.class);

        perform(get("/api/quiz/" + quiz.getId() + "/start"))
                .andExpect(status().isBadRequest());
    }

    // ==================== Формат ошибок ====================

    /**
     * Проверка формата ответа об ошибке.
     */
    @Test
    void errorResponseFormat_shouldMatchErrorResponseDto() throws Exception {
        MvcResult result = perform(get("/api/quiz/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andReturn();

        ErrorResponseDto errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponseDto.class);
        assertNotNull(errorResponse.getMessage());
        assertNotNull(errorResponse.getPath());
        assertNotNull(errorResponse.getTimestamp());
        assertEquals(404, errorResponse.getStatus());
    }
}
