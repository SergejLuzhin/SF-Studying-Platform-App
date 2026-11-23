package com.studying.platform.controller;

import com.studying.platform.dto.ErrorResponseDto;
import com.studying.platform.dto.EnrollmentRequestDto;
import com.studying.platform.dto.EnrollmentResponseDto;
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные HTTP-тесты REST-контроллера записей {@link EnrollmentController}.
 * <p>
 * Проверяют позитивные и негативные сценарии: запись студента на курс,
 * повторную запись, отписку, получение списков студентов и курсов,
 * а также обработку несуществующих объектов.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class EnrollmentControllerTest {

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

    private final java.util.List<Long> createdUserIds = new java.util.ArrayList<>();
    private final java.util.List<Long> createdCourseIds = new java.util.ArrayList<>();

    /**
     * Очищает созданные тестом данные после каждого теста.
     */
    @AfterEach
    void cleanUp() {
        transactions.executeWithoutResult(status -> {
            jdbcTemplate.update("delete from enrollments");
            for (int i = createdCourseIds.size() - 1; i >= 0; i--) {
                jdbcTemplate.update("delete from courses where id = ?", createdCourseIds.get(i));
            }
            for (int i = createdUserIds.size() - 1; i >= 0; i--) {
                jdbcTemplate.update("delete from users where id = ?", createdUserIds.get(i));
            }
            createdUserIds.clear();
            createdCourseIds.clear();
        });
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

        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        com.studying.platform.dto.UserResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                com.studying.platform.dto.UserResponseDto.class);
        createdUserIds.add(response.getId());
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

        MvcResult result = mockMvc.perform(post("/api/categories")
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
        dto.setStartDate(java.time.LocalDate.of(2026, 1, 1));
        dto.setTeacherId(teacherId);
        dto.setCategoryId(categoryId);

        MvcResult result = mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        com.studying.platform.dto.CourseResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                com.studying.platform.dto.CourseResponseDto.class);
        createdCourseIds.add(response.getId());
        return response.getId();
    }

    /**
     * Позитивный тест: студент записывается на курс (HTTP 201).
     */
    @Test
    void enrollStudent_shouldReturn201() throws Exception {
        Long teacherId = createUser("teacher-enroll@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для записей");
        Long courseId = createCourse(teacherId, categoryId);
        Long studentId = createUser("student-enroll@test.io", UserRole.STUDENT);

        EnrollmentRequestDto dto = new EnrollmentRequestDto(studentId, courseId);

        MvcResult result = mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.courseId").value(courseId))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        EnrollmentResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                EnrollmentResponseDto.class);
        assertNotNull(response.getId(), "ID записи не должен быть null");
        assertEquals("ACTIVE", response.getStatus());
    }

    /**
     * Позитивный тест: получение списка студентов курса (HTTP 200).
     */
    @Test
    void getCourseStudents_shouldReturn200() throws Exception {
        Long teacherId = createUser("teacher-students@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для списка студентов");
        Long courseId = createCourse(teacherId, categoryId);
        Long studentId1 = createUser("student1-students@test.io", UserRole.STUDENT);
        Long studentId2 = createUser("student2-students@test.io", UserRole.STUDENT);

        // Записываем двух студентов
        EnrollmentRequestDto dto1 = new EnrollmentRequestDto(studentId1, courseId);
        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        EnrollmentRequestDto dto2 = new EnrollmentRequestDto(studentId2, courseId);
        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/enrollments/courses/" + courseId + "/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].studentId").value(studentId1))
                .andExpect(jsonPath("$[1].studentId").value(studentId2));
    }

    /**
     * Позитивный тест: получение списка курсов студента (HTTP 200).
     */
    @Test
    void getStudentCourses_shouldReturn200() throws Exception {
        Long teacherId = createUser("teacher-mycourses@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для списка курсов");
        Long courseId1 = createCourse(teacherId, categoryId);
        Long courseId2 = createCourse(teacherId, categoryId);
        Long studentId = createUser("student-mycourses@test.io", UserRole.STUDENT);

        // Записываем на оба курса
        EnrollmentRequestDto dto1 = new EnrollmentRequestDto(studentId, courseId1);
        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        EnrollmentRequestDto dto2 = new EnrollmentRequestDto(studentId, courseId2);
        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/enrollments/users/" + studentId + "/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /**
     * Позитивный тест: отписка студента от курса (HTTP 204).
     */
    @Test
    void dropEnrollment_shouldReturn204() throws Exception {
        Long teacherId = createUser("teacher-drop@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для отписки");
        Long courseId = createCourse(teacherId, categoryId);
        Long studentId = createUser("student-drop@test.io", UserRole.STUDENT);

        // Сначала записываем
        EnrollmentRequestDto enrollDto = new EnrollmentRequestDto(studentId, courseId);
        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollDto)))
                .andExpect(status().isCreated());

        // Затем отписываем
        mockMvc.perform(delete("/api/enrollments")
                        .param("studentId", studentId.toString())
                        .param("courseId", courseId.toString()))
                .andExpect(status().isNoContent());

        // Проверка: активная запись больше не найдена
        mockMvc.perform(get("/api/enrollments/courses/" + courseId + "/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * Негативный тест: преподаватель не может записаться на курс (HTTP 404).
     */
    @Test
    void enrollTeacher_shouldReturn404() throws Exception {
        Long teacherId = createUser("teacher-enroll-fail@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для неудачной записи");
        Long courseId = createCourse(teacherId, categoryId);

        EnrollmentRequestDto dto = new EnrollmentRequestDto(teacherId, courseId);

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: повторная запись студента на тот же курс (HTTP 400).
     */
    @Test
    void enrollStudent_duplicate_shouldReturn400() throws Exception {
        Long teacherId = createUser("teacher-dup@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для дубликата");
        Long courseId = createCourse(teacherId, categoryId);
        Long studentId = createUser("student-dup@test.io", UserRole.STUDENT);

        // Первая запись
        EnrollmentRequestDto dto = new EnrollmentRequestDto(studentId, courseId);
        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // Повторная запись
        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Негативный тест: отписка при отсутствии записи (HTTP 404).
     */
    @Test
    void dropEnrollment_nonExistent_shouldReturn404() throws Exception {
        Long studentId = createUser("student-drop-nx@test.io", UserRole.STUDENT);

        mockMvc.perform(delete("/api/enrollments")
                        .param("studentId", studentId.toString())
                        .param("courseId", "99999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: запись с несуществующим студентом (HTTP 404).
     */
    @Test
    void enrollStudent_nonExistent_shouldReturn404() throws Exception {
        Long teacherId = createUser("teacher-nx@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для несуществующего");
        Long courseId = createCourse(teacherId, categoryId);

        EnrollmentRequestDto dto = new EnrollmentRequestDto(99999L, courseId);

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: запись на несуществующий курс (HTTP 404).
     */
    @Test
    void enrollStudent_nonExistentCourse_shouldReturn404() throws Exception {
        Long studentId = createUser("student-nx-course@test.io", UserRole.STUDENT);

        EnrollmentRequestDto dto = new EnrollmentRequestDto(studentId, 99999L);

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка формата ошибки при неудачной записи.
     */
    @Test
    void enrollStudent_errorResponse_shouldHaveCorrectFormat() throws Exception {
        Long studentId = createUser("student-error@test.io", UserRole.STUDENT);

        EnrollmentRequestDto dto = new EnrollmentRequestDto(studentId, 99999L);

        MvcResult result = mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andReturn();

        ErrorResponseDto errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponseDto.class);
        assertNotNull(errorResponse.getMessage());
        assertNotNull(errorResponse.getPath());
    }
}
