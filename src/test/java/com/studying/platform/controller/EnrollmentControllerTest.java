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
@SpringBootTest(properties = "spring.jpa.open-in-view=false")
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

    /** Идентификаторы ресурсов, созданных текущим тестом. */
    private final com.studying.platform.HttpFixtureTracker fixtureTracker =
            new com.studying.platform.HttpFixtureTracker();


    /**
     * Удаляет записи только между созданными тестом пользователями и курсами,
     * затем удаляет созданные курсы, категории и пользователей.
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
        dto.setStartDate(java.time.LocalDate.of(2026, 1, 1));
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
     * Позитивный тест: студент записывается на курс (HTTP 201).
     */
    @Test
    void enrollStudent_shouldReturn201() throws Exception {
        Long teacherId = createUser("teacher-enroll@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для записей");
        Long courseId = createCourse(teacherId, categoryId);
        Long studentId = createUser("student-enroll@test.io", UserRole.STUDENT);

        EnrollmentRequestDto dto = new EnrollmentRequestDto(studentId, courseId);

        MvcResult result = perform(post("/api/enrollments")
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
        transactions.executeWithoutResult(status -> assertEquals(response.getId(), jdbcTemplate.queryForObject(
                "select id from enrollments where user_id = ? and course_id = ?", Long.class, studentId, courseId)));
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
        perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        EnrollmentRequestDto dto2 = new EnrollmentRequestDto(studentId2, courseId);
        perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated());

        perform(get("/api/enrollments/courses/" + courseId + "/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.containsInAnyOrder(studentId1.intValue(), studentId2.intValue())))
                .andExpect(jsonPath("$[*].email", org.hamcrest.Matchers.containsInAnyOrder("student1-students@test.io", "student2-students@test.io")))
                .andExpect(jsonPath("$[*].role", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("STUDENT"))));
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
        perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        EnrollmentRequestDto dto2 = new EnrollmentRequestDto(studentId, courseId2);
        perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated());

        perform(get("/api/enrollments/users/" + studentId + "/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.containsInAnyOrder(courseId1.intValue(), courseId2.intValue())))
                .andExpect(jsonPath("$[*].description", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("Описание курса"))));
    }

    /** Проверяет идентификаторы и поля ресурсов, фильтрацию ACTIVE и пустые списки без OSIV. */
    @Test
    void lists_returnOnlyActiveResourcesWithCompleteFields() throws Exception {
        Long teacher = createUser("teacher-lists@test.io", UserRole.TEACHER);
        Long category = createCategory("Категория изоляции списков");
        Long course1 = createCourse(teacher, category);
        Long course2 = createCourse(teacher, category);
        Long courseDropped = createCourse(teacher, category);
        Long emptyCourse = createCourse(teacher, category);
        Long student1 = createUser("student-lists-1@test.io", UserRole.STUDENT);
        Long student2 = createUser("student-lists-2@test.io", UserRole.STUDENT);
        Long droppedStudent = createUser("student-lists-drop@test.io", UserRole.STUDENT);
        Long emptyStudent = createUser("student-lists-empty@test.io", UserRole.STUDENT);

        long enrollment1 = enroll(student1, course1);
        if (enrollment1 == student1) {
            Long noiseCourse = createCourse(teacher, category);
            Long noiseStudent = createUser("student-lists-noise@test.io", UserRole.STUDENT);
            enroll(noiseStudent, noiseCourse);
        }
        long enrollment2 = enroll(student2, course1);
        enroll(droppedStudent, course1);
        long course2Enrollment = enroll(student1, course2);
        enroll(student1, courseDropped);
        assertTrue(enrollment1 != student1 || enrollment2 != student2,
                "Хотя бы один ID студента должен отличаться от ID его записи");
        assertTrue(enrollment1 != course1 || course2Enrollment != course2,
                "Хотя бы один ID курса должен отличаться от ID его записи");
        perform(delete("/api/enrollments").param("studentId", droppedStudent.toString())
                .param("courseId", course1.toString())).andExpect(status().isNoContent());
        perform(delete("/api/enrollments").param("studentId", student1.toString())
                .param("courseId", courseDropped.toString())).andExpect(status().isNoContent());

        transactions.executeWithoutResult(status -> assertEquals(enrollment1, jdbcTemplate.queryForObject(
                "select id from enrollments where user_id = ? and course_id = ?", Long.class, student1, course1)));
        var students = objectMapper.readTree(perform(get("/api/enrollments/courses/" + course1 + "/students"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals(2, students.size());
        java.util.Set<com.fasterxml.jackson.databind.JsonNode> actualStudents = new java.util.HashSet<>();
        students.forEach(actualStudents::add);
        assertEquals(java.util.Set.of(
                objectMapper.readTree(objectMapper.writeValueAsString(Map.of("id", student1, "email", "student-lists-1@test.io", "role", "STUDENT"))),
                objectMapper.readTree(objectMapper.writeValueAsString(Map.of("id", student2, "email", "student-lists-2@test.io", "role", "STUDENT")))), actualStudents);

        var courses = objectMapper.readTree(perform(get("/api/enrollments/users/" + student1 + "/courses"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals(2, courses.size());
        java.util.Set<com.fasterxml.jackson.databind.JsonNode> actualCourses = new java.util.HashSet<>();
        courses.forEach(actualCourses::add);
        assertEquals(java.util.Set.of(courseSummary(course1), courseSummary(course2)), actualCourses);
        perform(get("/api/enrollments/courses/" + emptyCourse + "/students"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        perform(get("/api/enrollments/users/" + emptyStudent + "/courses"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        perform(get("/api/enrollments/courses/999999999/students")).andExpect(status().isNotFound());
        perform(get("/api/enrollments/users/999999999/courses")).andExpect(status().isNotFound());
    }

    /**
     * Создаёт запись через HTTP и возвращает именно её идентификатор.
     *
     * @param studentId идентификатор студента
     * @param courseId идентификатор курса
     * @return идентификатор записи
     * @throws Exception если запрос или разбор ответа завершился ошибкой
     */
    private long enroll(Long studentId, Long courseId) throws Exception {
        MvcResult result = perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EnrollmentRequestDto(studentId, courseId))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").longValue();
    }

    /**
     * Формирует ожидаемые краткие данные курса на основе чтения базы.
     *
     * @param courseId идентификатор курса
     * @return JSON-объект с ID, названием и описанием
     * @throws Exception если данные курса не удалось преобразовать в JSON
     */
    private com.fasterxml.jackson.databind.JsonNode courseSummary(Long courseId) throws Exception {
        Map<String, Object> summary = transactions.execute(status -> {
            Map<String, Object> course = jdbcTemplate.queryForMap(
                    "select title, description from courses where id = ?", courseId);
            return Map.of("id", courseId, "title", course.get("title"), "description", course.get("description"));
        });
        return objectMapper.readTree(objectMapper.writeValueAsString(summary));
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
        perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollDto)))
                .andExpect(status().isCreated());

        // Затем отписываем
        perform(delete("/api/enrollments")
                        .param("studentId", studentId.toString())
                        .param("courseId", courseId.toString()))
                .andExpect(status().isNoContent());

        // Проверка: активная запись больше не найдена
        perform(get("/api/enrollments/courses/" + courseId + "/students"))
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

        perform(post("/api/enrollments")
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
        perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // Повторная запись
        perform(post("/api/enrollments")
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

        perform(delete("/api/enrollments")
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

        perform(post("/api/enrollments")
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

        perform(post("/api/enrollments")
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

        MvcResult result = perform(post("/api/enrollments")
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
