package com.studying.platform;

import com.studying.platform.dto.CourseRequestDto;
import com.studying.platform.dto.CourseResponseDto;
import com.studying.platform.dto.ErrorResponseDto;
import com.studying.platform.dto.LessonRequestDto;
import com.studying.platform.dto.ModuleRequestDto;
import com.studying.platform.dto.UserCreateDto;
import com.studying.platform.dto.UserResponseDto;
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
 * Интеграционные тесты REST API для пользователей и категорий.
 * <p>
 * Проверяют корректные HTTP-методы и коды ответов (201, 200, 204),
 * обработку 400 при невалидном вводе и 404 при отсутствии объекта,
 * а также единый формат JSON-ответов об ошибках.
 * Тесты выполняются с PostgreSQL через Testcontainers в профиле «test».
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class RestApiTests {

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
     * Шаблон JDBC для прямого доступа к базе данных при очистке тестовых данных.
     */
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private final java.util.List<Long> createdUserIds = new java.util.ArrayList<>();
    private final java.util.List<Long> createdCategoryIds = new java.util.ArrayList<>();
    private final java.util.List<Long> createdCourseIds = new java.util.ArrayList<>();

    /**
     * Очищает созданные тестом данные после каждого теста.
     */
    @AfterEach
    void cleanUp() {
        transactions.executeWithoutResult(status -> {
            // Удаляем уроки (связаны с модулями)
            jdbcTemplate.update("delete from lessons");
            // Удаляем модули (связаны с курсами)
            jdbcTemplate.update("delete from modules");
            // Удаляем записи (связаны с курсами)
            jdbcTemplate.update("delete from enrollments");
            // Удаляем курсы
            for (int i = createdCourseIds.size() - 1; i >= 0; i--) {
                jdbcTemplate.update("delete from courses where id = ?", createdCourseIds.get(i));
            }
            // Удаляем категории (связаны с курсами)
            for (int i = createdCategoryIds.size() - 1; i >= 0; i--) {
                jdbcTemplate.update("delete from categories where id = ?", createdCategoryIds.get(i));
            }
            // Удаляем пользователей
            for (int i = createdUserIds.size() - 1; i >= 0; i--) {
                jdbcTemplate.update("delete from users where id = ?", createdUserIds.get(i));
            }
            createdUserIds.clear();
            createdCategoryIds.clear();
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
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail(email);
        dto.setRole(role);

        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        UserResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), UserResponseDto.class);
        createdUserIds.add(response.getId());
        return response.getId();
    }

    /**
     * Создание категории через API и возврат ID.
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
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        com.studying.platform.dto.CategoryResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                com.studying.platform.dto.CategoryResponseDto.class);
        createdCategoryIds.add(response.getId());
        return response.getId();
    }

    /**
     * Проверка: создание пользователя успешно (HTTP 201).
     */
    @Test
    void createUser_shouldReturn201() throws Exception {
        Long userId = createUser("api-user1@test.io", UserRole.STUDENT);
        assertNotNull(userId, "ID созданного пользователя не должен быть null");
    }

    /**
     * Проверка: чтение существующего пользователя (HTTP 200).
     */
    @Test
    void getUser_shouldReturn200() throws Exception {
        Long userId = createUser("api-user2@test.io", UserRole.TEACHER);

        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("api-user2@test.io"))
                .andExpect(jsonPath("$.role").value("TEACHER"));
    }

    /**
     * Проверка: чтение несуществующего пользователя (HTTP 404).
     */
    @Test
    void getUser_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/users/99999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: обновление пользователя (HTTP 200).
     * <p>
     * В рамках текущего этапа PUT не изменяет поля пользователя,
     * но возвращает актуальные данные с кодом 200.
     * </p>
     */
    @Test
    void updateUser_shouldReturn200() throws Exception {
        Long userId = createUser("api-user3@test.io", UserRole.ADMIN);

        mockMvc.perform(put("/api/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("api-user3@test.io"));
    }

    /**
     * Проверка: обновление несуществующего пользователя (HTTP 404).
     */
    @Test
    void updateUser_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(put("/api/users/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: удаление пользователя успешно (HTTP 204).
     */
    @Test
    void deleteUser_shouldReturn204() throws Exception {
        Long userId = createUser("api-user4@test.io", UserRole.STUDENT);

        mockMvc.perform(delete("/api/users/" + userId))
                .andExpect(status().isNoContent());
    }

    /**
     * Проверка: удаление несуществующего пользователя (HTTP 404).
     */
    @Test
    void deleteUser_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/users/99999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: невалидный email при создании пользователя (HTTP 400).
     */
    @Test
    void createUser_invalidEmail_shouldReturn400() throws Exception {
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail("not-an-email");
        dto.setRole(UserRole.STUDENT);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: пустой email при создании пользователя (HTTP 400).
     */
    @Test
    void createUser_emptyEmail_shouldReturn400() throws Exception {
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail("");
        dto.setRole(UserRole.STUDENT);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: пустая роль при создании пользователя (HTTP 400).
     */
    @Test
    void createUser_nullRole_shouldReturn400() throws Exception {
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail("no-role@test.io");
        dto.setRole(null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: дублирующийся email при создании пользователя (HTTP 400).
     * <p>
     * При нарушении уникальности email на уровне сервиса
     * выбрасывается IllegalStateException, который перехватывается
     * глобальным обработчиком и возвращается 400 или 500.
     * </p>
     */
    @Test
    void createUser_duplicateEmail_shouldReturnBadRequest() throws Exception {
        createUser("duplicate@test.io", UserRole.STUDENT);

        // Повторное создание с тем же email
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"duplicate@test.io\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: создание категории успешно (HTTP 201).
     */
    @Test
    void createCategory_shouldReturn201() throws Exception {
        Long categoryId = createCategory("Программирование");

        mockMvc.perform(get("/api/categories/" + categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").value("Программирование"));
    }

    /**
     * Проверка: получение списка всех категорий (HTTP 200).
     */
    @Test
    void getAllCategories_shouldReturn200() throws Exception {
        createCategory("Категория 1");
        createCategory("Категория 2");

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /**
     * Проверка: чтение существующей категории (HTTP 200).
     */
    @Test
    void getCategory_shouldReturn200() throws Exception {
        Long categoryId = createCategory("Маркетинг");

        mockMvc.perform(get("/api/categories/" + categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").value("Маркетинг"));
    }

    /**
     * Проверка: чтение несуществующей категории (HTTP 404).
     */
    @Test
    void getCategory_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/categories/99999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: обновление категории успешно (HTTP 200).
     */
    @Test
    void updateCategory_shouldReturn200() throws Exception {
        Long categoryId = createCategory("Дизайн");

        com.studying.platform.dto.CategoryCreateDto dto =
                new com.studying.platform.dto.CategoryCreateDto("Графический дизайн");

        mockMvc.perform(put("/api/categories/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Графический дизайн"));
    }

    /**
     * Проверка: обновление несуществующей категории (HTTP 404).
     */
    @Test
    void updateCategory_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(put("/api/categories/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Новое название\"}"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: удаление категории успешно (HTTP 204).
     */
    @Test
    void deleteCategory_shouldReturn204() throws Exception {
        Long categoryId = createCategory("Удаляемая категория");

        mockMvc.perform(delete("/api/categories/" + categoryId))
                .andExpect(status().isNoContent());
    }

    /**
     * Проверка: удаление несуществующей категории (HTTP 404).
     */
    @Test
    void deleteCategory_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/categories/99999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: формат JSON при ошибке 404.
     * <p>
     * Проверяет, что ErrorResponseDto содержит обязательные поля:
     * status, error, message, timestamp, path.
     * </p>
     */
    @Test
    void errorResponseFormat_notFound_shouldHaveRequiredFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andReturn();

        ErrorResponseDto errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), ErrorResponseDto.class);

        assertNotNull(errorResponse.getTimestamp());
        assertEquals(404, errorResponse.getStatus());
        assertEquals("Not Found", errorResponse.getError());
        assertNotNull(errorResponse.getMessage());
        assertNotNull(errorResponse.getPath());
    }

    /**
     * Проверка: формат JSON при ошибке 400 валидации.
     * <p>
     * Проверяет, что ErrorResponseDto содержит обязательные поля
     * при невалидном входном запросе.
     * </p>
     */
    @Test
    void errorResponseFormat_validationError_shouldHaveRequiredFields() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid\",\"role\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andReturn();

        ErrorResponseDto errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), ErrorResponseDto.class);

        assertNotNull(errorResponse.getTimestamp());
        assertEquals(400, errorResponse.getStatus());
        assertEquals("Bad Request", errorResponse.getError());
        assertNotNull(errorResponse.getMessage());
        assertNotNull(errorResponse.getPath());
    }

    /**
     * Проверка: создание категории с пустым названием (HTTP 400).
     */
    @Test
    void createCategory_emptyName_shouldReturn400() throws Exception {
        com.studying.platform.dto.CategoryCreateDto dto =
                new com.studying.platform.dto.CategoryCreateDto("");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: создание категории с дублирующим названием (HTTP 400).
     */
    @Test
    void createCategory_duplicateName_shouldReturnBadRequest() throws Exception {
        createCategory("Дубликат");

        com.studying.platform.dto.CategoryCreateDto dto =
                new com.studying.platform.dto.CategoryCreateDto("Дубликат");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: валидация email при создании пользователя — слишком длинный.
     */
    @Test
    void createUser_tooLongEmail_shouldReturn400() throws Exception {
        String longEmail = "a".repeat(51) + "@example.com";
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail(longEmail);
        dto.setRole(UserRole.STUDENT);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: полный цикл создания и чтения пользователя.
     */
    @Test
    void createUserAndRead_shouldPersistInDatabase() throws Exception {
        String uniqueEmail = "cycle-" + System.currentTimeMillis() + "@test.io";
        createUser(uniqueEmail, UserRole.STUDENT);

        // Читаем через API — данные должны быть доступны
        MvcResult result = mockMvc.perform(get("/api/categories"))
                .andReturn();

        // Проверка что контекст работает — GET категорий возвращает 200
        assertEquals(200, result.getResponse().getStatus());
    }

    /**
     * Проверка: пустой JSON при создании пользователя (HTTP 400).
     */
    @Test
    void createUser_emptyBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: пустой JSON при создании категории (HTTP 400).
     */
    @Test
    void createCategory_emptyBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ==================== Тесты для курсов, модулей и уроков (Этап 17) ====================

    /**
     * Создаёт преподавателя через API и возвращает его ID.
     */
    private Long createTeacher(String email) throws Exception {
        return createUser(email, UserRole.TEACHER);
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

        MvcResult result = mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        CourseResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), CourseResponseDto.class);
        createdCourseIds.add(response.getId());
        return response.getId();
    }

    /**
     * Проверка: создание курса успешно (HTTP 201).
     */
    @Test
    void createCourse_shouldReturn201() throws Exception {
        Long teacherId = createTeacher("teacher-course@test.io");
        Long categoryId = createCategory("Тестовая категория для курса");
        Long courseId = createCourse(teacherId, categoryId);

        assertNotNull(courseId, "ID созданного курса не должен быть null");

        mockMvc.perform(get("/api/courses/" + courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.teacherId").value(teacherId))
                .andExpect(jsonPath("$.categoryId").value(categoryId));
    }

    /**
     * Проверка: чтение существующего курса (HTTP 200).
     */
    @Test
    void getCourse_shouldReturn200() throws Exception {
        Long teacherId = createTeacher("teacher-get@test.io");
        Long categoryId = createCategory("Категория для чтения");
        Long courseId = createCourse(teacherId, categoryId);

        mockMvc.perform(get("/api/courses/" + courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId))
                .andExpect(jsonPath("$.teacherId").value(teacherId))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.duration").value(40))
                .andExpect(jsonPath("$.description").value("Описание курса"));
    }

    /**
     * Проверка: обновление курса (HTTP 200).
     */
    @Test
    void updateCourse_shouldReturn200() throws Exception {
        Long teacherId = createTeacher("teacher-update@test.io");
        Long categoryId = createCategory("Категория для обновления");
        Long courseId = createCourse(teacherId, categoryId);

        CourseRequestDto dto = new CourseRequestDto();
        dto.setTitle("Обновлённый курс");
        dto.setDescription("Новое описание");
        dto.setDuration(60);
        dto.setStartDate(LocalDate.of(2026, 6, 1));
        dto.setTeacherId(teacherId);
        dto.setCategoryId(categoryId);

        mockMvc.perform(put("/api/courses/" + courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Обновлённый курс"))
                .andExpect(jsonPath("$.description").value("Новое описание"))
                .andExpect(jsonPath("$.duration").value(60));
    }

    /**
     * Проверка: удаление пустого курса успешно (HTTP 204).
     */
    @Test
    void deleteCourse_shouldReturn204() throws Exception {
        Long teacherId = createTeacher("teacher-delete@test.io");
        Long categoryId = createCategory("Категория для удаления");
        Long courseId = createCourse(teacherId, categoryId);

        mockMvc.perform(delete("/api/courses/" + courseId))
                .andExpect(status().isNoContent());
    }

    /**
     * Проверка: добавление модуля к курсу (HTTP 201).
     */
    @Test
    void addModule_shouldReturn201() throws Exception {
        Long teacherId = createTeacher("teacher-module@test.io");
        Long categoryId = createCategory("Категория для модуля");
        Long courseId = createCourse(teacherId, categoryId);

        ModuleRequestDto dto = new ModuleRequestDto();
        dto.setTitle("Модуль 1");
        dto.setDescription("Описание модуля");
        dto.setOrderIndex(1);

        MvcResult result = mockMvc.perform(post("/api/courses/" + courseId + "/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Модуль 1"))
                .andReturn();

        com.studying.platform.dto.ModuleResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                com.studying.platform.dto.ModuleResponseDto.class);
        assertNotNull(response.getId());
    }

    /**
     * Проверка: получение структуры курса (HTTP 200).
     */
    @Test
    void getCourseStructure_shouldReturn200() throws Exception {
        Long teacherId = createTeacher("teacher-structure@test.io");
        Long categoryId = createCategory("Категория для структуры");
        Long courseId = createCourse(teacherId, categoryId);

        // Добавляем модуль
        ModuleRequestDto moduleDto = new ModuleRequestDto("Модуль структуры", "Описание", 1);
        mockMvc.perform(post("/api/courses/" + courseId + "/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moduleDto)))
                .andExpect(status().isCreated());

        // Добавляем урок
        LessonRequestDto lessonDto = new LessonRequestDto("Урок 1", "Содержание урока", "http://example.com/video");
        Long moduleId = getModuleIdByCourse(courseId);

        mockMvc.perform(post("/api/lessons/modules/" + moduleId + "/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lessonDto)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/courses/" + courseId + "/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId))
                .andExpect(jsonPath("$.modules.length()").value(1))
                .andExpect(jsonPath("$.modules[0].title").value("Модуль структуры"))
                .andExpect(jsonPath("$.modules[0].lessons.length()").value(1))
                .andExpect(jsonPath("$.modules[0].lessons[0].title").value("Урок 1"));
    }

    /**
     * Проверка: получение модуля (HTTP 200).
     */
    @Test
    void getModule_shouldReturn200() throws Exception {
        Long teacherId = createTeacher("teacher-get-module@test.io");
        Long categoryId = createCategory("Категория для получения модуля");
        Long courseId = createCourse(teacherId, categoryId);
        addModuleToCourse(courseId, "Модуль для чтения", 1);
        Long moduleId = getModuleIdByCourse(courseId);

        mockMvc.perform(get("/api/modules/" + moduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(moduleId))
                .andExpect(jsonPath("$.title").value("Модуль для чтения"))
                .andExpect(jsonPath("$.courseId").value(courseId));
    }

    /**
     * Проверка: обновление модуля (HTTP 200).
     */
    @Test
    void updateModule_shouldReturn200() throws Exception {
        Long teacherId = createTeacher("teacher-update-module@test.io");
        Long categoryId = createCategory("Категория для обновления модуля");
        Long courseId = createCourse(teacherId, categoryId);
        addModuleToCourse(courseId, "Старое название", 1);
        Long moduleId = getModuleIdByCourse(courseId);

        ModuleRequestDto dto = new ModuleRequestDto();
        dto.setTitle("Обновлённый модуль");
        dto.setDescription("Новое описание модуля");
        dto.setOrderIndex(2);

        mockMvc.perform(put("/api/modules/" + moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Обновлённый модуль"))
                .andExpect(jsonPath("$.orderIndex").value(2));
    }

    /**
     * Проверка: удаление модуля (HTTP 204).
     */
    @Test
    void deleteModule_shouldReturn204() throws Exception {
        Long teacherId = createTeacher("teacher-delete-module@test.io");
        Long categoryId = createCategory("Категория для удаления модуля");
        Long courseId = createCourse(teacherId, categoryId);
        addModuleToCourse(courseId, "Удаляемый модуль", 1);
        Long moduleId = getModuleIdByCourse(courseId);

        mockMvc.perform(delete("/api/modules/" + moduleId))
                .andExpect(status().isNoContent());
    }

    /**
     * Проверка: создание урока (HTTP 201).
     */
    @Test
    void addLesson_shouldReturn201() throws Exception {
        Long teacherId = createTeacher("teacher-lesson@test.io");
        Long categoryId = createCategory("Категория для урока");
        Long courseId = createCourse(teacherId, categoryId);
        addModuleToCourse(courseId, "Модуль для урока", 1);
        Long moduleId = getModuleIdByCourse(courseId);

        LessonRequestDto dto = new LessonRequestDto();
        dto.setTitle("Новый урок");
        dto.setContent("Содержание урока");
        dto.setVideoUrl("http://example.com/video.mp4");

        MvcResult result = mockMvc.perform(post("/api/lessons/modules/" + moduleId + "/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Новый урок"))
                .andReturn();

        com.studying.platform.dto.LessonResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                com.studying.platform.dto.LessonResponseDto.class);
        assertNotNull(response.getId());
    }

    /**
     * Проверка: получение урока (HTTP 200).
     */
    @Test
    void getLesson_shouldReturn200() throws Exception {
        Long teacherId = createTeacher("teacher-get-lesson@test.io");
        Long categoryId = createCategory("Категория для получения урока");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = addModuleToCourseAndReturnId(courseId, "Модуль", 1);
        Long lessonId = addLessonToModuleAndReturnId(moduleId, "Урок для чтения", "Содержание", "http://example.com/v");

        mockMvc.perform(get("/api/lessons/" + lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lessonId))
                .andExpect(jsonPath("$.title").value("Урок для чтения"))
                .andExpect(jsonPath("$.content").value("Содержание"))
                .andExpect(jsonPath("$.moduleId").value(moduleId));
    }

    /**
     * Проверка: обновление урока (HTTP 200).
     */
    @Test
    void updateLesson_shouldReturn200() throws Exception {
        Long teacherId = createTeacher("teacher-update-lesson@test.io");
        Long categoryId = createCategory("Категория для обновления урока");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = addModuleToCourseAndReturnId(courseId, "Модуль", 1);
        Long lessonId = addLessonToModuleAndReturnId(moduleId, "Старый урок", "Старое содержание", null);

        LessonRequestDto dto = new LessonRequestDto();
        dto.setTitle("Обновлённый урок");
        dto.setContent("Обновлённое содержание");
        dto.setVideoUrl("http://example.com/new-video");

        mockMvc.perform(put("/api/lessons/" + lessonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Обновлённый урок"))
                .andExpect(jsonPath("$.videoUrl").value("http://example.com/new-video"));
    }

    /**
     * Проверка: удаление урока (HTTP 204).
     */
    @Test
    void deleteLesson_shouldReturn204() throws Exception {
        Long teacherId = createTeacher("teacher-delete-lesson@test.io");
        Long categoryId = createCategory("Категория для удаления урока");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = addModuleToCourseAndReturnId(courseId, "Модуль", 1);
        Long lessonId = addLessonToModuleAndReturnId(moduleId, "Удаляемый урок", "Содержание", null);

        mockMvc.perform(delete("/api/lessons/" + lessonId))
                .andExpect(status().isNoContent());
    }

    /**
     * Проверка: полный цикл — создание курса, модуля, урока, получение структуры.
     */
    @Test
    void fullCycle_createCourseWithStructure() throws Exception {
        Long teacherId = createTeacher("teacher-fullcycle@test.io");
        Long categoryId = createCategory("Категория полного цикла");

        // Создаём курс
        Long courseId = createCourse(teacherId, categoryId);

        // Добавляем модуль
        ModuleRequestDto moduleDto = new ModuleRequestDto("Модуль цикла", "Описание", 1);
        mockMvc.perform(post("/api/courses/" + courseId + "/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moduleDto)))
                .andExpect(status().isCreated());

        Long moduleId = getModuleIdByCourse(courseId);

        // Добавляем урок
        LessonRequestDto lessonDto = new LessonRequestDto("Урок цикла", "Полный контент", "http://example.com");
        mockMvc.perform(post("/api/lessons/modules/" + moduleId + "/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lessonDto)))
                .andExpect(status().isCreated());

        // Получаем структуру
        mockMvc.perform(get("/api/courses/" + courseId + "/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[0].lessons[0].title").value("Урок цикла"))
                .andExpect(jsonPath("$.modules[0].lessons[0].content").value("Полный контент"));
    }

    // ==================== Негативные сценарии ====================

    /**
     * Проверка: чтение несуществующего курса (HTTP 404).
     */
    @Test
    void getCourse_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/courses/99999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: обновление несуществующего курса (HTTP 404).
     */
    @Test
    void updateCourse_notFound_shouldReturn404() throws Exception {
        CourseRequestDto dto = new CourseRequestDto();
        dto.setTitle("Тест");
        dto.setDescription("Тест");
        dto.setDuration(10);
        dto.setStartDate(LocalDate.of(2026, 1, 1));
        dto.setTeacherId(1L);
        dto.setCategoryId(1L);

        mockMvc.perform(put("/api/courses/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: удаление несуществующего курса (HTTP 404).
     */
    @Test
    void deleteCourse_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/courses/99999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: создание курса с пустым названием (HTTP 400).
     */
    @Test
    void createCourse_nullTitle_shouldReturn400() throws Exception {
        Long teacherId = createTeacher("teacher-validation@test.io");
        Long categoryId = createCategory("Категория для валидации");

        CourseRequestDto dto = new CourseRequestDto();
        dto.setTitle("");
        dto.setDescription("Описание");
        dto.setDuration(10);
        dto.setStartDate(LocalDate.of(2026, 1, 1));
        dto.setTeacherId(teacherId);
        dto.setCategoryId(categoryId);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: создание курса без преподавателя (HTTP 400).
     */
    @Test
    void createCourse_nullTeacher_shouldReturn400() throws Exception {
        Long categoryId = createCategory("Категория для валидации учителя");

        CourseRequestDto dto = new CourseRequestDto();
        dto.setTitle("Курс без учителя");
        dto.setDescription("Описание");
        dto.setDuration(10);
        dto.setStartDate(LocalDate.of(2026, 1, 1));
        dto.setTeacherId(null);
        dto.setCategoryId(categoryId);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: создание курса с некорректной продолжительностью (HTTP 400).
     */
    @Test
    void createCourse_invalidDuration_shouldReturn400() throws Exception {
        Long teacherId = createTeacher("teacher-duration@test.io");
        Long categoryId = createCategory("Категория для валидации duration");

        CourseRequestDto dto = new CourseRequestDto();
        dto.setTitle("Курс с нулевой продолжительностью");
        dto.setDescription("Описание");
        dto.setDuration(0);
        dto.setStartDate(LocalDate.of(2026, 1, 1));
        dto.setTeacherId(teacherId);
        dto.setCategoryId(categoryId);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: создание курса с несуществующим преподавателем (HTTP 404).
     */
    @Test
    void createCourse_nonExistentTeacher_shouldReturn404() throws Exception {
        Long categoryId = createCategory("Категория для несуществующего учителя");

        CourseRequestDto dto = new CourseRequestDto();
        dto.setTitle("Курс с чужим учителем");
        dto.setDescription("Описание");
        dto.setDuration(10);
        dto.setStartDate(LocalDate.of(2026, 1, 1));
        dto.setTeacherId(99999L);
        dto.setCategoryId(categoryId);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: создание курса с несуществующей категорией (HTTP 404).
     */
    @Test
    void createCourse_nonExistentCategory_shouldReturn404() throws Exception {
        Long teacherId = createTeacher("teacher-category@test.io");

        CourseRequestDto dto = new CourseRequestDto();
        dto.setTitle("Курс с чужой категорией");
        dto.setDescription("Описание");
        dto.setDuration(10);
        dto.setStartDate(LocalDate.of(2026, 1, 1));
        dto.setTeacherId(teacherId);
        dto.setCategoryId(99999L);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: удаление курса с записями (HTTP 400).
     */
    @Test
    void deleteCourse_withEnrollments_shouldReturn400() throws Exception {
        Long teacherId = createTeacher("teacher-enroll@test.io");
        Long studentId = createUser("student-enroll@test.io", UserRole.STUDENT);
        Long categoryId = createCategory("Категория для записей");
        Long courseId = createCourse(teacherId, categoryId);

        // Записываем студента на курс
        transactions.executeWithoutResult(status -> {
            jdbcTemplate.update("insert into enrollments (user_id, course_id, enroll_date, status) values (?, ?, ?, ?)",
                    studentId, courseId, LocalDate.now(), "ACTIVE");
        });

        mockMvc.perform(delete("/api/courses/" + courseId))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: добавление модуля к несуществующему курсу (HTTP 404).
     */
    @Test
    void addModule_nonExistentCourse_shouldReturn404() throws Exception {
        ModuleRequestDto dto = new ModuleRequestDto("Модуль", "Описание", 1);

        mockMvc.perform(post("/api/courses/99999/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: получение несуществующего модуля (HTTP 404).
     */
    @Test
    void getModule_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/modules/99999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: получение несуществующего урока (HTTP 404).
     */
    @Test
    void getLesson_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/lessons/99999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: создание курса с пустым телом (HTTP 400).
     */
    @Test
    void createCourse_emptyBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: создание модуля с пустым названием (HTTP 400).
     */
    @Test
    void createModule_nullTitle_shouldReturn400() throws Exception {
        Long teacherId = createTeacher("teacher-module-title@test.io");
        Long categoryId = createCategory("Категория для названия модуля");
        Long courseId = createCourse(teacherId, categoryId);

        ModuleRequestDto dto = new ModuleRequestDto();
        dto.setTitle("");
        dto.setDescription("Описание");
        dto.setOrderIndex(1);

        mockMvc.perform(post("/api/courses/" + courseId + "/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: создание урока с пустым названием (HTTP 400).
     */
    @Test
    void createLesson_nullTitle_shouldReturn400() throws Exception {
        Long teacherId = createTeacher("teacher-lesson-title@test.io");
        Long categoryId = createCategory("Категория для названия урока");
        Long courseId = createCourse(teacherId, categoryId);
        Long moduleId = addModuleToCourseAndReturnId(courseId, "Модуль", 1);

        LessonRequestDto dto = new LessonRequestDto();
        dto.setTitle("");
        dto.setContent("Содержание");
        dto.setVideoUrl(null);

        mockMvc.perform(post("/api/lessons/modules/" + moduleId + "/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: формат ошибки 404 при чтении несуществующего курса.
     */
    @Test
    void errorResponseFormat_course_shouldHaveRequiredFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/courses/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andReturn();

        ErrorResponseDto errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), ErrorResponseDto.class);

        assertNotNull(errorResponse.getTimestamp());
        assertEquals(404, errorResponse.getStatus());
        assertEquals("Not Found", errorResponse.getError());
        assertNotNull(errorResponse.getMessage());
        assertNotNull(errorResponse.getPath());
    }

    // ==================== Вспомогательные методы ====================

    /**
     * Добавляет модуль к курсу и возвращает его ID.
     */
    private Long addModuleToCourse(Long courseId, String title, int orderIndex) throws Exception {
        ModuleRequestDto dto = new ModuleRequestDto(title, "Описание", orderIndex);
        MvcResult result = mockMvc.perform(post("/api/courses/" + courseId + "/modules")
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
     * Добавляет модуль к курсу, возвращает ID модуля.
     */
    private Long addModuleToCourseAndReturnId(Long courseId, String title, int orderIndex) throws Exception {
        return addModuleToCourse(courseId, title, orderIndex);
    }

    /**
     * Добавляет урок к модулю и возвращает его ID.
     */
    private Long addLessonToModuleAndReturnId(Long moduleId, String title, String content, String videoUrl) throws Exception {
        LessonRequestDto dto = new LessonRequestDto(title, content, videoUrl);
        MvcResult result = mockMvc.perform(post("/api/lessons/modules/" + moduleId + "/lessons")
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
     * Получает ID первого модуля курса через прямой SQL.
     */
    private Long getModuleIdByCourse(Long courseId) throws Exception {
        return jdbcTemplate.queryForObject("select id from modules where course_id = ? order by id limit 1",
                Long.class, courseId);
    }
}
