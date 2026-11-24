package com.studying.platform.controller;

import com.studying.platform.dto.AssignmentRequestDto;
import com.studying.platform.dto.AssignmentResponseDto;
import com.studying.platform.dto.ErrorResponseDto;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные HTTP-тесты REST-контроллера заданий {@link AssignmentController}.
 * <p>
 * Проверяют позитивные и негативные сценарии: создание, чтение, обновление,
 * удаление заданий, получение списка заданий урока, а также обработку
 * несуществующих объектов и валидацию входных данных.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AssignmentControllerTest {

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
     * Добавляет модуль к курсу и возвращает его ID.
     */
    private Long addModuleToCourse(Long courseId, String title, int orderIndex) throws Exception {
        com.studying.platform.dto.ModuleRequestDto dto =
                new com.studying.platform.dto.ModuleRequestDto(title, "Описание", orderIndex);

        MvcResult result = perform(post("/api/courses/" + courseId + "/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        com.studying.platform.dto.ModuleResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                com.studying.platform.dto.ModuleResponseDto.class);
        return response.getId();
    }

    /**
     * Добавляет урок к модулю и возвращает его ID.
     */
    private Long addLessonToModule(Long moduleId, String title, String content, String videoUrl) throws Exception {
        com.studying.platform.dto.LessonRequestDto dto =
                new com.studying.platform.dto.LessonRequestDto(title, content, videoUrl);

        MvcResult result = perform(post("/api/lessons/modules/" + moduleId + "/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        com.studying.platform.dto.LessonResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                com.studying.platform.dto.LessonResponseDto.class);
        return response.getId();
    }

    /**
     * Проверяет выбор урока из пути, совместимость и неизменность родителя при обновлении.
     *
     * @throws Exception если HTTP-запрос или чтение ответа завершились ошибкой
     */
    @Test
    void assignmentLessonComesFromPathAndCannotBeChanged() throws Exception {
        Long teacher = createUser("teacher-lesson-path@test.io", UserRole.TEACHER);
        Long category = createCategory("Категория проверки урока");
        Long course = createCourse(teacher, category);
        Long module = addModuleToCourse(course, "Модуль", 1);
        Long lesson = addLessonToModule(module, "Первый урок", "Содержание", null);
        Long otherLesson = addLessonToModule(module, "Второй урок", "Содержание", null);
        String path = "/api/assignments/lessons/" + lesson + "/assignments";

        MvcResult created = perform(post(path).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Без ID\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.lessonId").value(lesson))
                .andReturn();
        Long assignment = objectMapper.readValue(created.getResponse().getContentAsString(),
                AssignmentResponseDto.class).getId();
        perform(post(path).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Совпадает\",\"lessonId\":" + lesson + "}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.lessonId").value(lesson));
        int count = jdbcTemplate.queryForObject("select count(*) from assignments where lesson_id = ?",
                Integer.class, lesson);
        perform(post(path).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Не совпадает\",\"lessonId\":" + otherLesson + "}"))
                .andExpect(status().isBadRequest());
        assertEquals(count, jdbcTemplate.queryForObject(
                "select count(*) from assignments where lesson_id = ?", Integer.class, lesson));
        perform(post("/api/assignments/lessons/{id}/assignments", Long.MAX_VALUE)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Нет урока\"}"))
                .andExpect(status().isNotFound());
        perform(put("/api/assignments/{id}", assignment).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Обновлено\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lessonId").value(lesson));
        perform(put("/api/assignments/{id}", assignment).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Без переноса\",\"lessonId\":" + otherLesson + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lessonId").value(lesson));
        assertEquals(lesson, jdbcTemplate.queryForObject(
                "select lesson_id from assignments where id = ?", Long.class, assignment));
    }

    /**
     * Позитивный тест: создание задания для урока (HTTP 201).
     */
    @Test
    void createAssignment_shouldReturn201() throws Exception {
        Long teacherId = createUser("teacher-assign@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для заданий");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = addModuleToCourse(courseId, "Модуль", 1);
        Long lessonId = addLessonToModule(moduleId, "Урок", "Содержание", null);

        AssignmentRequestDto dto = new AssignmentRequestDto();
        dto.setLessonId(lessonId);
        dto.setTitle("Задание 1");
        dto.setDescription("Описание задания");
        dto.setDueDate(LocalDate.of(2026, 6, 1));
        dto.setMaxScore(100);

        MvcResult result = perform(post("/api/assignments/lessons/" + lessonId + "/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Задание 1"))
                .andExpect(jsonPath("$.lessonId").value(lessonId))
                .andExpect(jsonPath("$.maxScore").value(100))
                .andReturn();

        AssignmentResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AssignmentResponseDto.class);
        assertNotNull(response.getId());
    }

    /**
     * Позитивный тест: получение задания по ID (HTTP 200).
     */
    @Test
    void getAssignment_shouldReturn200() throws Exception {
        Long teacherId = createUser("teacher-get-assign@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для чтения задания");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = addModuleToCourse(courseId, "Модуль", 1);
        Long lessonId = addLessonToModule(moduleId, "Урок", "Содержание", null);

        Long assignmentId = createAssignment(lessonId, "Читаемое задание", 50);

        perform(get("/api/assignments/" + assignmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assignmentId))
                .andExpect(jsonPath("$.title").value("Читаемое задание"))
                .andExpect(jsonPath("$.lessonId").value(lessonId))
                .andExpect(jsonPath("$.maxScore").value(50));
    }

    /**
     * Позитивный тест: обновление задания (HTTP 200).
     */
    @Test
    void updateAssignment_shouldReturn200() throws Exception {
        Long teacherId = createUser("teacher-upd-assign@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для обновления задания");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = addModuleToCourse(courseId, "Модуль", 1);
        Long lessonId = addLessonToModule(moduleId, "Урок", "Содержание", null);

        Long assignmentId = createAssignment(lessonId, "Старое название", 50);

        AssignmentRequestDto dto = new AssignmentRequestDto();
        dto.setLessonId(lessonId);
        dto.setTitle("Обновлённое название");
        dto.setDescription("Обновлённое описание");
        dto.setDueDate(LocalDate.of(2026, 7, 1));
        dto.setMaxScore(75);

        perform(put("/api/assignments/" + assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Обновлённое название"))
                .andExpect(jsonPath("$.description").value("Обновлённое описание"))
                .andExpect(jsonPath("$.dueDate").isNotEmpty())
                .andExpect(jsonPath("$.maxScore").value(75));
    }

    /**
     * Позитивный тест: удаление задания (HTTP 204).
     */
    @Test
    void deleteAssignment_shouldReturn204() throws Exception {
        Long teacherId = createUser("teacher-del-assign@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для удаления задания");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = addModuleToCourse(courseId, "Модуль", 1);
        Long lessonId = addLessonToModule(moduleId, "Урок", "Содержание", null);

        Long assignmentId = createAssignment(lessonId, "Удаляемое задание", 50);

        perform(delete("/api/assignments/" + assignmentId))
                .andExpect(status().isNoContent());

        // Проверка: задание больше не найдено
        perform(get("/api/assignments/" + assignmentId))
                .andExpect(status().isNotFound());
    }

    /**
     * Позитивный тест: получение списка заданий урока (HTTP 200).
     */
    @Test
    void getAssignmentsByLesson_shouldReturn200() throws Exception {
        Long teacherId = createUser("teacher-list-assign@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для списка заданий");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = addModuleToCourse(courseId, "Модуль", 1);
        Long lessonId = addLessonToModule(moduleId, "Урок", "Содержание", null);

        createAssignment(lessonId, "Задание 1", 50);
        createAssignment(lessonId, "Задание 2", 75);

        perform(get("/api/assignments/lessons/" + lessonId + "/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Задание 1"))
                .andExpect(jsonPath("$[1].title").value("Задание 2"));
    }

    /**
     * Негативный тест: создание задания для несуществующего урока (HTTP 404).
     */
    @Test
    void createAssignment_nonExistentLesson_shouldReturn404() throws Exception {
        AssignmentRequestDto dto = new AssignmentRequestDto();
        dto.setLessonId(99999L);
        dto.setTitle("Задание");
        dto.setDescription("Описание");
        dto.setDueDate(LocalDate.of(2026, 6, 1));
        dto.setMaxScore(100);

        perform(post("/api/assignments/lessons/99999/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: обновление несуществующего задания (HTTP 404).
     */
    @Test
    void updateAssignment_nonExistent_shouldReturn404() throws Exception {
        AssignmentRequestDto dto = new AssignmentRequestDto();
        dto.setLessonId(1L);
        dto.setTitle("Обновлённое");
        dto.setDescription("Новое");
        dto.setDueDate(LocalDate.of(2026, 7, 1));
        dto.setMaxScore(80);

        perform(put("/api/assignments/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: удаление несуществующего задания (HTTP 404).
     */
    @Test
    void deleteAssignment_nonExistent_shouldReturn404() throws Exception {
        perform(delete("/api/assignments/99999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Негативный тест: создание задания с пустым названием (HTTP 400).
     */
    @Test
    void createAssignment_emptyTitle_shouldReturn400() throws Exception {
        Long teacherId = createUser("teacher-val-assign@test.io", UserRole.TEACHER);
        Long categoryId = createCategory("Категория для валидации задания");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = addModuleToCourse(courseId, "Модуль", 1);
        Long lessonId = addLessonToModule(moduleId, "Урок", "Содержание", null);

        AssignmentRequestDto dto = new AssignmentRequestDto();
        dto.setLessonId(lessonId);
        dto.setTitle("");
        dto.setDescription("Описание");
        dto.setDueDate(LocalDate.of(2026, 6, 1));
        dto.setMaxScore(100);

        perform(post("/api/assignments/lessons/" + lessonId + "/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Негативный тест: получение списка заданий несуществующего урока (HTTP 404).
     */
    @Test
    void getAssignmentsByLesson_nonExistent_shouldReturn404() throws Exception {
        perform(get("/api/assignments/lessons/99999/assignments"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка формата ошибки при получении несуществующего задания.
     */
    @Test
    void getAssignment_errorResponse_shouldHaveCorrectFormat() throws Exception {
        MvcResult result = perform(get("/api/assignments/99999"))
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
                result.getResponse().getContentAsString(),
                AssignmentResponseDto.class);
        return response.getId();
    }
}
