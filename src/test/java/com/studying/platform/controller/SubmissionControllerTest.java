package com.studying.platform.controller;

import com.studying.platform.dto.AssignmentRequestDto;
import com.studying.platform.dto.AssignmentResponseDto;
import com.studying.platform.dto.CategoryCreateDto;
import com.studying.platform.dto.CategoryResponseDto;
import com.studying.platform.dto.CourseRequestDto;
import com.studying.platform.dto.CourseResponseDto;
import com.studying.platform.dto.EnrollmentRequestDto;
import com.studying.platform.dto.ErrorResponseDto;
import com.studying.platform.dto.GradeSubmissionRequestDto;
import com.studying.platform.dto.LessonRequestDto;
import com.studying.platform.dto.LessonResponseDto;
import com.studying.platform.dto.ModuleRequestDto;
import com.studying.platform.dto.ModuleResponseDto;
import com.studying.platform.dto.SubmissionRequestDto;
import com.studying.platform.dto.SubmissionResponseDto;
import com.studying.platform.dto.UserCreateDto;
import com.studying.platform.dto.UserResponseDto;
import com.studying.platform.entity.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные HTTP-тесты REST-контроллера решений {@link SubmissionController}.
 * <p>
 * Проверяют позитивные и негативные сценарии: отправку решения студентом,
 * оценивание преподавателем, получение списков решений, а также обработку
 * несуществующих объектов, повторных отправок и некорректных оценок.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SubmissionControllerTest {

    /**
     * Счётчик для генерации уникальных имён категорий.
     */
    private static final AtomicLong catCounter = new AtomicLong(0);

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

    /** Идентификаторы ресурсов, созданных текущим тестом. */
    private final com.studying.platform.HttpFixtureTracker fixtureTracker =
            new com.studying.platform.HttpFixtureTracker();


    /**
     * Очищает созданные тестом данные после каждого теста.
     */
    @AfterEach
    void cleanUp() {
        fixtureTracker.clean(jdbcTemplate, transactions);
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
     */
    private Long createUser(String email, UserRole role) throws Exception {
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail(email);
        dto.setRole(role);

        MvcResult result = perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        UserResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), UserResponseDto.class);
        return response.getId();
    }

    /**
     * Создаёт категорию через API и возвращает её ID.
     * <p>
     * Использует уникальное имя с инкрементальным счётчиком, чтобы избежать
     * конфликтов при повторных запусках в рамках одной JVM.
     * </p>
     */
    private Long createCategory(String baseName) throws Exception {
        String name = baseName + "-" + catCounter.incrementAndGet();
        CategoryCreateDto dto = new CategoryCreateDto(name);

        MvcResult result = perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        CategoryResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), CategoryResponseDto.class);
        return response.getId();
    }

    /**
     * Создаёт курс через API и возвращает его ID.
     */
    private Long createCourse(Long teacherId, Long categoryId) throws Exception {
        CourseRequestDto dto = new CourseRequestDto();
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

        CourseResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), CourseResponseDto.class);
        return response.getId();
    }

    /**
     * Добавляет модуль к курсу и возвращает его ID.
     */
    private Long addModuleToCourse(Long courseId, String title, int orderIndex) throws Exception {
        ModuleRequestDto dto = new ModuleRequestDto(title, "Описание", orderIndex);

        MvcResult result = perform(post("/api/courses/" + courseId + "/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        ModuleResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ModuleResponseDto.class);
        return response.getId();
    }

    /**
     * Добавляет урок к модулю и возвращает его ID.
     */
    private Long addLessonToModule(Long moduleId, String title, String content, String videoUrl) throws Exception {
        LessonRequestDto dto = new LessonRequestDto(title, content, videoUrl);

        MvcResult result = perform(post("/api/lessons/modules/" + moduleId + "/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        LessonResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), LessonResponseDto.class);
        return response.getId();
    }

    /**
     * Создаёт задание через API и возвращает его ID.
     */
    private Long createAssignment(Long lessonId, String title, Integer maxScore) throws Exception {
        AssignmentRequestDto dto = new AssignmentRequestDto();
        dto.setLessonId(lessonId);
        dto.setTitle(title);
        dto.setDescription("Описание задания");
        dto.setDueDate(LocalDate.of(2026, 6, 1));
        dto.setMaxScore(maxScore);

        MvcResult result = perform(post("/api/assignments/lessons/" + lessonId + "/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        AssignmentResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AssignmentResponseDto.class);
        return response.getId();
    }

    /**
     * Записывает студента на курс через API.
     */
    private void enrollStudent(Long studentId, Long courseId) throws Exception {
        EnrollmentRequestDto dto = new EnrollmentRequestDto();
        dto.setStudentId(studentId);
        dto.setCourseId(courseId);

        perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    /**
     * Создаёт полную структуру: преподаватель, курс, модуль, урок, задание.
     */
    private Long[] createCourseWithLessonAndAssignment() throws Exception {
        Long teacherId = createUser("teacher-submission-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для решений");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = addModuleToCourse(courseId, "Модуль", 1);
        Long lessonId = addLessonToModule(moduleId, "Урок", "Содержание", null);
        Long assignmentId = createAssignment(lessonId, "Задание", 100);
        return new Long[]{teacherId, courseId, lessonId, assignmentId};
    }

    // ==================== Позитивные сценарии ====================

    /**
     * Позитивный тест: студент отправляет решение (HTTP 201).
     */
    @Test
    void submitSolution_shouldReturn201() throws Exception {
        Long[] data = createCourseWithLessonAndAssignment();
        Long studentId = createUser("student-submission-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        Long courseId = data[1];
        Long assignmentId = data[3];

        enrollStudent(studentId, courseId);

        SubmissionRequestDto dto = new SubmissionRequestDto();
        dto.setAssignmentId(assignmentId);
        dto.setStudentId(studentId);
        dto.setContent("Мое решение задачи");

        MvcResult result = perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.assignmentId").value(assignmentId))
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.content").value("Мое решение задачи"))
                .andExpect(jsonPath("$.score").isEmpty())
                .andReturn();

        SubmissionResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                SubmissionResponseDto.class);
        assertNotNull(response.getId());
        assertNull(response.getScore());
    }

    /**
     * Позитивный тест: преподаватель оценивает решение (HTTP 200).
     */
    @Test
    void gradeSubmission_shouldReturn200() throws Exception {
        Long[] data = createCourseWithLessonAndAssignment();
        Long teacherId = data[0];
        Long studentId = createUser("student-grade-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        Long courseId = data[1];
        Long assignmentId = data[3];

        enrollStudent(studentId, courseId);

        SubmissionRequestDto submitDto = new SubmissionRequestDto();
        submitDto.setAssignmentId(assignmentId);
        submitDto.setStudentId(studentId);
        submitDto.setContent("Решение для оценивания");

        MvcResult submitResult = perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isCreated())
                .andReturn();

        SubmissionResponseDto submitted = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(),
                SubmissionResponseDto.class);

        GradeSubmissionRequestDto gradeDto = new GradeSubmissionRequestDto();
        gradeDto.setScore(85);
        gradeDto.setFeedback("Хорошая работа");

        perform(patch("/api/submissions/" + submitted.getId())
                        .param("teacherId", teacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gradeDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(85))
                .andExpect(jsonPath("$.feedback").value("Хорошая работа"));
    }

    /**
     * Позитивный тест: получение списка решений по заданию (HTTP 200).
     */
    @Test
    void getSubmissionsByAssignment_shouldReturn200() throws Exception {
        Long[] data = createCourseWithLessonAndAssignment();
        Long studentId1 = createUser("student-list1-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        Long studentId2 = createUser("student-list2-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        Long courseId = data[1];
        Long assignmentId = data[3];

        enrollStudent(studentId1, courseId);
        enrollStudent(studentId2, courseId);

        SubmissionRequestDto dto1 = new SubmissionRequestDto();
        dto1.setAssignmentId(assignmentId);
        dto1.setStudentId(studentId1);
        dto1.setContent("Решение 1");
        perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        SubmissionRequestDto dto2 = new SubmissionRequestDto();
        dto2.setAssignmentId(assignmentId);
        dto2.setStudentId(studentId2);
        dto2.setContent("Решение 2");
        perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated());

        perform(get("/api/submissions/assignments/" + assignmentId + "/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /**
     * Позитивный тест: получение списка решений по студенту (HTTP 200).
     */
    @Test
    void getSubmissionsByStudent_shouldReturn200() throws Exception {
        Long[] data1 = createCourseWithLessonAndAssignment();
        Long[] data2 = createCourseWithLessonAndAssignment();
        Long studentId = createUser("student-my-submissions-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);

        enrollStudent(studentId, data1[1]);
        enrollStudent(studentId, data2[1]);

        SubmissionRequestDto dto1 = new SubmissionRequestDto();
        dto1.setAssignmentId(data1[3]);
        dto1.setStudentId(studentId);
        dto1.setContent("Решение на курс 1");
        perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        SubmissionRequestDto dto2 = new SubmissionRequestDto();
        dto2.setAssignmentId(data2[3]);
        dto2.setStudentId(studentId);
        dto2.setContent("Решение на курс 2");
        perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated());

        perform(get("/api/submissions/users/" + studentId + "/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ==================== Негативные сценарии ====================

    /**
     * Негативный тест: отправка решения на несуществующее задание (HTTP 404).
     */
    @Test
    void submitSolution_nonExistentAssignment_shouldReturn404() throws Exception {
        Long studentId = createUser("student-nx-assign-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);

        SubmissionRequestDto dto = new SubmissionRequestDto();
        dto.setAssignmentId(99999L);
        dto.setStudentId(studentId);
        dto.setContent("Решение");

        perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: отправка решения студентом, не записанным на курс (HTTP 404).
     */
    @Test
    void submitSolution_notEnrolled_shouldReturn404() throws Exception {
        Long[] data = createCourseWithLessonAndAssignment();
        Long studentId = createUser("student-not-enrolled-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        Long assignmentId = data[3];

        SubmissionRequestDto dto = new SubmissionRequestDto();
        dto.setAssignmentId(assignmentId);
        dto.setStudentId(studentId);
        dto.setContent("Решение");

        perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: повторная отправка решения (HTTP 400).
     */
    @Test
    void submitSolution_duplicate_shouldReturn400() throws Exception {
        Long[] data = createCourseWithLessonAndAssignment();
        Long studentId = createUser("student-dup-submit-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        Long courseId = data[1];
        Long assignmentId = data[3];

        enrollStudent(studentId, courseId);

        SubmissionRequestDto dto = new SubmissionRequestDto();
        dto.setAssignmentId(assignmentId);
        dto.setStudentId(studentId);
        dto.setContent("Решение 1");

        perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Негативный тест: отрицательная оценка (HTTP 400).
     */
    @Test
    void gradeSubmission_negativeScore_shouldReturn400() throws Exception {
        Long[] data = createCourseWithLessonAndAssignment();
        Long teacherId = data[0];
        Long studentId = createUser("student-neg-grade-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        Long courseId = data[1];
        Long assignmentId = data[3];

        enrollStudent(studentId, courseId);

        SubmissionRequestDto submitDto = new SubmissionRequestDto();
        submitDto.setAssignmentId(assignmentId);
        submitDto.setStudentId(studentId);
        submitDto.setContent("Решение");

        MvcResult submitResult = perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isCreated())
                .andReturn();

        SubmissionResponseDto submitted = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(),
                SubmissionResponseDto.class);

        GradeSubmissionRequestDto gradeDto = new GradeSubmissionRequestDto();
        gradeDto.setScore(-1);
        gradeDto.setFeedback(null);

        perform(patch("/api/submissions/" + submitted.getId())
                        .param("teacherId", teacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gradeDto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Негативный тест: оценка выше maxScore (HTTP 400).
     */
    @Test
    void gradeSubmission_aboveMaxScore_shouldReturn400() throws Exception {
        Long[] data = createCourseWithLessonAndAssignment();
        Long teacherId = data[0];
        Long studentId = createUser("student-high-grade-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        Long courseId = data[1];
        Long assignmentId = data[3];

        enrollStudent(studentId, courseId);

        SubmissionRequestDto submitDto = new SubmissionRequestDto();
        submitDto.setAssignmentId(assignmentId);
        submitDto.setStudentId(studentId);
        submitDto.setContent("Решение");

        MvcResult submitResult = perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isCreated())
                .andReturn();

        SubmissionResponseDto submitted = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(),
                SubmissionResponseDto.class);

        GradeSubmissionRequestDto gradeDto = new GradeSubmissionRequestDto();
        gradeDto.setScore(150);
        gradeDto.setFeedback(null);

        perform(patch("/api/submissions/" + submitted.getId())
                        .param("teacherId", teacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gradeDto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Негативный тест: оценка не-преподавателем (HTTP 400).
     */
    @Test
    void gradeSubmission_byNonTeacher_shouldReturn400() throws Exception {
        Long[] data = createCourseWithLessonAndAssignment();
        Long studentId = createUser("student-grade-as-student-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        Long courseId = data[1];
        Long assignmentId = data[3];

        enrollStudent(studentId, courseId);

        SubmissionRequestDto submitDto = new SubmissionRequestDto();
        submitDto.setAssignmentId(assignmentId);
        submitDto.setStudentId(studentId);
        submitDto.setContent("Решение");

        MvcResult submitResult = perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isCreated())
                .andReturn();

        SubmissionResponseDto submitted = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(),
                SubmissionResponseDto.class);

        GradeSubmissionRequestDto gradeDto = new GradeSubmissionRequestDto();
        gradeDto.setScore(80);
        gradeDto.setFeedback("Комментарий");

        perform(patch("/api/submissions/" + submitted.getId())
                        .param("teacherId", studentId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gradeDto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Негативный тест: оценка несуществующего решения (HTTP 404).
     */
    @Test
    void gradeSubmission_nonExistent_shouldReturn404() throws Exception {
        Long teacherId = createUser("teacher-gradenx-" + System.currentTimeMillis() + "@test.io", UserRole.TEACHER);

        GradeSubmissionRequestDto gradeDto = new GradeSubmissionRequestDto();
        gradeDto.setScore(80);
        gradeDto.setFeedback("Комментарий");

        perform(patch("/api/submissions/99999")
                        .param("teacherId", teacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gradeDto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: получение списка решений несуществующего задания (HTTP 404).
     */
    @Test
    void getSubmissionsByAssignment_nonExistent_shouldReturn404() throws Exception {
        perform(get("/api/submissions/assignments/99999/submissions"))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: получение списка решений несуществующего студента (HTTP 404).
     */
    @Test
    void getSubmissionsByStudent_nonExistent_shouldReturn404() throws Exception {
        perform(get("/api/submissions/users/99999/submissions"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка формата ошибки при повторной отправке решения.
     */
    @Test
    void submitSolution_duplicate_errorResponse_shouldHaveCorrectFormat() throws Exception {
        Long[] data = createCourseWithLessonAndAssignment();
        Long studentId = createUser("student-dup-error-" + System.currentTimeMillis() + "@test.io", UserRole.STUDENT);
        Long courseId = data[1];
        Long assignmentId = data[3];

        enrollStudent(studentId, courseId);

        SubmissionRequestDto dto = new SubmissionRequestDto();
        dto.setAssignmentId(assignmentId);
        dto.setStudentId(studentId);
        dto.setContent("Решение");

        perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        MvcResult result = perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andReturn();

        ErrorResponseDto errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponseDto.class);
        assertNotNull(errorResponse.getMessage());
        assertNotNull(errorResponse.getPath());
    }
}
