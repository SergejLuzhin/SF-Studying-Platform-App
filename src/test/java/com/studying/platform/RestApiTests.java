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

    /** Идентификаторы ресурсов, созданных текущим тестом. */
    private final com.studying.platform.HttpFixtureTracker fixtureTracker =
            new com.studying.platform.HttpFixtureTracker();

    /**
     * Проверяет безопасные ответы на ошибки JSON, параметров и отсутствующие ресурсы.
     *
     * @throws Exception если выполнение HTTP-запроса завершилось ошибкой
     */
    @Test
    void clientErrorsUseConsistentSafeResponses() throws Exception {
        for (String body : new String[]{"{", "{\"email\":\"test@example.com\",\"role\":\"UNKNOWN\"}",
                "{\"email\":[],\"role\":\"STUDENT\"}"}) {
            perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("Некорректное тело запроса"))
                    .andExpect(jsonPath("$.path").value("/api/users"));
        }
        perform(get("/api/users/not-a-number")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/users/not-a-number"));
        perform(delete("/api/enrollments")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
        perform(get("/api/unknown-route")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/unknown-route"));
        perform(get("/api/users/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        perform(patch("/api/users/1")).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    /**
     * Проверяет отсутствие SQL и имени ограничения в ответе на нарушение уникальности.
     *
     * @throws Exception если выполнение HTTP-запроса завершилось ошибкой
     */
    @Test
    void databaseIntegrityErrorDoesNotExposeDetails() throws Exception {
        String body = "{\"name\":\"tag-error-" + java.util.UUID.randomUUID() + "\"}";
        perform(post("/api/tags").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        perform(post("/api/tags").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Нарушение целостности данных"))
                .andExpect(jsonPath("$.path").value("/api/tags"));
    }


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
     * Создание категории через API и возврат ID.
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
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        com.studying.platform.dto.CategoryResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                com.studying.platform.dto.CategoryResponseDto.class);
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

        perform(get("/api/users/" + userId))
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
        perform(get("/api/users/99999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверяет сохранение нового email и роли в ответе, отдельном GET и базе данных.
     */
    @Test
    void updateUser_shouldReturn200() throws Exception {
        Long userId = createUser("api-user3@test.io", UserRole.ADMIN);

        perform(put("/api/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"updated@test.io\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("updated@test.io"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
        assertUserState(userId, "updated@test.io", "STUDENT");
    }

    /**
     * Проверка: обновление несуществующего пользователя (HTTP 404).
     */
    @Test
    void updateUser_notFound_shouldReturn404() throws Exception {
        perform(put("/api/users/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@test.io\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isNotFound());
    }

    /** Проверяет, что сохранение прежнего email допускается при изменении роли. */
    @Test
    void updateUser_sameEmail_shouldSucceed() throws Exception {
        Long userId = createUser("same-update@test.io", UserRole.STUDENT);
        perform(put("/api/users/" + userId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"same-update@test.io\",\"role\":\"TEACHER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("same-update@test.io"))
                .andExpect(jsonPath("$.role").value("TEACHER"));
        assertUserState(userId, "same-update@test.io", "TEACHER");
    }

    /** Проверяет отклонение неверных данных и конфликта email без изменения обоих пользователей. */
    @Test
    void updateUser_invalidData_shouldNotModifyUsers() throws Exception {
        Long userId = createUser("original-update@test.io", UserRole.STUDENT);
        Long otherId = createUser("occupied-update@test.io", UserRole.ADMIN);
        for (String body : java.util.List.of(
                "{}",
                "{\"email\":\"\",\"role\":\"TEACHER\"}",
                "{\"email\":\"not-an-email\",\"role\":\"TEACHER\"}",
                "{\"email\":\"" + "a".repeat(51) + "@test.io\",\"role\":\"TEACHER\"}",
                "{\"email\":\"new-update@test.io\",\"role\":null}",
                "{\"email\":\"occupied-update@test.io\",\"role\":\"TEACHER\"}")) {
            perform(put("/api/users/" + userId).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
            assertUserState(userId, "original-update@test.io", "STUDENT");
            assertUserState(otherId, "occupied-update@test.io", "ADMIN");
        }
    }

    /**
     * Проверяет пользовательские данные через отдельный GET и новую транзакцию базы.
     *
     * @param id идентификатор пользователя
     * @param email ожидаемый адрес
     * @param role ожидаемая роль
     * @throws Exception если HTTP-запрос или проверка не выполнены
     */
    private void assertUserState(Long id, String email, String role) throws Exception {
        perform(get("/api/users/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.role").value(role));
        transactions.executeWithoutResult(status -> {
            Map<String, Object> row = jdbcTemplate.queryForMap("select login, role from users where id = ?", id);
            assertEquals(email, row.get("login"));
            assertEquals(role, row.get("role"));
        });
    }

    /**
     * Проверка: удаление пользователя успешно (HTTP 204).
     */
    @Test
    void deleteUser_shouldReturn204() throws Exception {
        Long userId = createUser("api-user4@test.io", UserRole.STUDENT);

        perform(delete("/api/users/" + userId))
                .andExpect(status().isNoContent());
    }

    /**
     * Проверка: удаление несуществующего пользователя (HTTP 404).
     */
    @Test
    void deleteUser_notFound_shouldReturn404() throws Exception {
        perform(delete("/api/users/99999"))
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

        perform(post("/api/users")
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

        perform(post("/api/users")
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

        perform(post("/api/users")
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
        perform(post("/api/users")
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

        perform(get("/api/categories/" + categoryId))
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

        perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /**
     * Проверка: чтение существующей категории (HTTP 200).
     */
    @Test
    void getCategory_shouldReturn200() throws Exception {
        Long categoryId = createCategory("Маркетинг");

        perform(get("/api/categories/" + categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").value("Маркетинг"));
    }

    /**
     * Проверка: чтение несуществующей категории (HTTP 404).
     */
    @Test
    void getCategory_notFound_shouldReturn404() throws Exception {
        perform(get("/api/categories/99999"))
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

        perform(put("/api/categories/" + categoryId)
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
        perform(put("/api/categories/99999")
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

        perform(delete("/api/categories/" + categoryId))
                .andExpect(status().isNoContent());
    }

    /**
     * Проверка: удаление несуществующей категории (HTTP 404).
     */
    @Test
    void deleteCategory_notFound_shouldReturn404() throws Exception {
        perform(delete("/api/categories/99999"))
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
        MvcResult result = perform(get("/api/users/99999"))
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
        MvcResult result = perform(post("/api/users")
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

        perform(post("/api/categories")
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

        perform(post("/api/categories")
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

        perform(post("/api/users")
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
        MvcResult result = perform(get("/api/categories"))
                .andReturn();

        // Проверка что контекст работает — GET категорий возвращает 200
        assertEquals(200, result.getResponse().getStatus());
    }

    /**
     * Проверка: пустой JSON при создании пользователя (HTTP 400).
     */
    @Test
    void createUser_emptyBody_shouldReturn400() throws Exception {
        perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: пустой JSON при создании категории (HTTP 400).
     */
    @Test
    void createCategory_emptyBody_shouldReturn400() throws Exception {
        perform(post("/api/categories")
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
     * Проверка: создание курса успешно (HTTP 201).
     */
    @Test
    void createCourse_shouldReturn201() throws Exception {
        Long teacherId = createTeacher("teacher-course@test.io");
        Long categoryId = createCategory("Тестовая категория для курса");
        Long courseId = createCourse(teacherId, categoryId);

        assertNotNull(courseId, "ID созданного курса не должен быть null");

        perform(get("/api/courses/" + courseId))
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

        perform(get("/api/courses/" + courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId))
                .andExpect(jsonPath("$.teacherId").value(teacherId))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.duration").value(40))
                .andExpect(jsonPath("$.description").value("Описание курса"));
    }

    /**
     * Проверяет полное обновление курса, включая преподавателя и категорию, через HTTP и БД.
     */
    @Test
    void updateCourse_shouldReturn200() throws Exception {
        Long teacherId = createTeacher("teacher-update@test.io");
        Long newTeacherId = createTeacher("teacher-update-new@test.io");
        Long categoryId = createCategory("Категория для обновления");
        Long newCategoryId = createCategory("Новая категория для обновления");
        Long courseId = createCourse(teacherId, categoryId);

        CourseRequestDto dto = new CourseRequestDto();
        dto.setTitle("Обновлённый курс");
        dto.setDescription("Новое описание");
        dto.setDuration(60);
        dto.setStartDate(LocalDate.of(2026, 6, 1));
        dto.setTeacherId(newTeacherId);
        dto.setCategoryId(newCategoryId);

        perform(put("/api/courses/" + courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Обновлённый курс"))
                .andExpect(jsonPath("$.description").value("Новое описание"))
                .andExpect(jsonPath("$.duration").value(60))
                .andExpect(jsonPath("$.startDate").value("2026-06-01"))
                .andExpect(jsonPath("$.teacherId").value(newTeacherId))
                .andExpect(jsonPath("$.categoryId").value(newCategoryId))
                .andExpect(jsonPath("$.categoryName").value("Новая категория для обновления"));

        perform(get("/api/courses/" + courseId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Обновлённый курс"))
                .andExpect(jsonPath("$.description").value("Новое описание"))
                .andExpect(jsonPath("$.duration").value(60))
                .andExpect(jsonPath("$.startDate").value("2026-06-01"))
                .andExpect(jsonPath("$.teacherId").value(newTeacherId))
                .andExpect(jsonPath("$.categoryId").value(newCategoryId));
        transactions.executeWithoutResult(status -> {
            Map<String, Object> row = courseState(courseId);
            assertEquals("Обновлённый курс", row.get("title"));
            assertEquals("Новое описание", row.get("description"));
            assertEquals(60, row.get("duration"));
            assertEquals(LocalDate.of(2026, 6, 1), ((java.sql.Date) row.get("start_date")).toLocalDate());
            assertEquals(newTeacherId, ((Number) row.get("teacher_id")).longValue());
            assertEquals(newCategoryId, ((Number) row.get("category_id")).longValue());
        });
        dto.setDescription(null);
        perform(put("/api/courses/" + courseId).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").isEmpty());
        perform(get("/api/courses/" + courseId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.description").isEmpty());
        transactions.executeWithoutResult(status -> assertNull(courseState(courseId).get("description")));
    }

    /** Проверяет отклонение ошибочных полных PUT без изменения любых полей курса. */
    @Test
    void updateCourse_invalidInput_shouldPreserveCourse() throws Exception {
        Long teacherId = createTeacher("teacher-update-invalid@test.io");
        Long studentId = createUser("student-update-invalid@test.io", UserRole.STUDENT);
        Long categoryId = createCategory("Категория для ошибочного обновления");
        Long courseId = createCourse(teacherId, categoryId);
        Map<String, Object> original = transactions.execute(status -> courseState(courseId));
        var initial = objectMapper.readTree(perform(get("/api/courses/" + courseId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        Map<String, Object> valid = new java.util.HashMap<>(Map.of(
                "title", "Другое название", "description", "Другое описание", "duration", 20,
                "startDate", "2027-03-01", "teacherId", teacherId, "categoryId", categoryId));
        for (String field : java.util.List.of("teacherId", "categoryId", "duration")) {
            Map<String, Object> body = new java.util.HashMap<>(valid);
            body.put(field, field.equals("duration") ? 0 : 999999999L);
            assertRejectedCourseUpdate(courseId, body, original, initial, field.equals("duration") ? 400 : 404);
        }
        Map<String, Object> wrongRole = new java.util.HashMap<>(valid);
        wrongRole.put("teacherId", studentId);
        assertRejectedCourseUpdate(courseId, wrongRole, original, initial, 404);
        Map<String, Object> negativeDuration = new java.util.HashMap<>(valid);
        negativeDuration.put("duration", -1);
        assertRejectedCourseUpdate(courseId, negativeDuration, original, initial, 400);
        for (String field : java.util.List.of("title", "duration", "startDate", "teacherId", "categoryId")) {
            Map<String, Object> body = new java.util.HashMap<>(valid);
            body.remove(field);
            assertRejectedCourseUpdate(courseId, body, original, initial, 400);
        }
    }

    /**
     * Проверяет статус ошибочного PUT и неизменность курса через HTTP и новую транзакцию.
     *
     * @param courseId идентификатор курса
     * @param body тело ошибочного запроса
     * @param original исходное состояние базы
     * @param initial исходный HTTP-ответ
     * @param expectedStatus ожидаемый HTTP-статус
     * @throws Exception если запрос или проверка не выполнены
     */
    private void assertRejectedCourseUpdate(Long courseId, Map<String, Object> body,
                                            Map<String, Object> original,
                                            com.fasterxml.jackson.databind.JsonNode initial,
                                            int expectedStatus) throws Exception {
        perform(put("/api/courses/" + courseId).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))).andExpect(status().is(expectedStatus));
        var after = objectMapper.readTree(perform(get("/api/courses/" + courseId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals(initial, after);
        assertEquals(original, transactions.execute(status -> courseState(courseId)));
    }

    /**
     * Читает сохраняемые поля курса для проверки в транзакции вызывающего кода.
     *
     * @param courseId идентификатор курса
     * @return значения полей курса из PostgreSQL
     */
    private Map<String, Object> courseState(Long courseId) {
        return jdbcTemplate.queryForMap("select title, description, duration, start_date, teacher_id, category_id from courses where id = ?", courseId);
    }

    /**
     * Проверка: удаление пустого курса успешно (HTTP 204).
     */
    @Test
    void deleteCourse_shouldReturn204() throws Exception {
        Long teacherId = createTeacher("teacher-delete@test.io");
        Long categoryId = createCategory("Категория для удаления");
        Long courseId = createCourse(teacherId, categoryId);

        perform(delete("/api/courses/" + courseId))
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

        MvcResult result = perform(post("/api/courses/" + courseId + "/modules")
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
        perform(post("/api/courses/" + courseId + "/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moduleDto)))
                .andExpect(status().isCreated());

        // Добавляем урок
        LessonRequestDto lessonDto = new LessonRequestDto("Урок 1", "Содержание урока", "http://example.com/video");
        Long moduleId = getModuleIdByCourse(courseId);

        perform(post("/api/lessons/modules/" + moduleId + "/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lessonDto)))
                .andExpect(status().isCreated());

        perform(get("/api/courses/" + courseId + "/structure"))
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

        perform(get("/api/modules/" + moduleId))
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

        perform(put("/api/modules/" + moduleId)
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

        perform(delete("/api/modules/" + moduleId))
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

        MvcResult result = perform(post("/api/lessons/modules/" + moduleId + "/lessons")
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

        perform(get("/api/lessons/" + lessonId))
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

        perform(put("/api/lessons/" + lessonId)
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

        perform(delete("/api/lessons/" + lessonId))
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
        perform(post("/api/courses/" + courseId + "/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moduleDto)))
                .andExpect(status().isCreated());

        Long moduleId = getModuleIdByCourse(courseId);

        // Добавляем урок
        LessonRequestDto lessonDto = new LessonRequestDto("Урок цикла", "Полный контент", "http://example.com");
        perform(post("/api/lessons/modules/" + moduleId + "/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lessonDto)))
                .andExpect(status().isCreated());

        // Получаем структуру
        perform(get("/api/courses/" + courseId + "/structure"))
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
        perform(get("/api/courses/99999"))
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

        perform(put("/api/courses/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: удаление несуществующего курса (HTTP 404).
     */
    @Test
    void deleteCourse_notFound_shouldReturn404() throws Exception {
        perform(delete("/api/courses/99999"))
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

        perform(post("/api/courses")
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

        perform(post("/api/courses")
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

        perform(post("/api/courses")
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

        perform(post("/api/courses")
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

        perform(post("/api/courses")
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
            Long id = jdbcTemplate.queryForObject(
                    "insert into enrollments (user_id, course_id, enroll_date, status) values (?, ?, ?, ?) returning id",
                    Long.class, studentId, courseId, LocalDate.now(), "ACTIVE");
            fixtureTracker.track("enrollments", id);
        });

        perform(delete("/api/courses/" + courseId))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: добавление модуля к несуществующему курсу (HTTP 404).
     */
    @Test
    void addModule_nonExistentCourse_shouldReturn404() throws Exception {
        ModuleRequestDto dto = new ModuleRequestDto("Модуль", "Описание", 1);

        perform(post("/api/courses/99999/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: получение несуществующего модуля (HTTP 404).
     */
    @Test
    void getModule_notFound_shouldReturn404() throws Exception {
        perform(get("/api/modules/99999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: получение несуществующего урока (HTTP 404).
     */
    @Test
    void getLesson_notFound_shouldReturn404() throws Exception {
        perform(get("/api/lessons/99999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверка: создание курса с пустым телом (HTTP 400).
     */
    @Test
    void createCourse_emptyBody_shouldReturn400() throws Exception {
        perform(post("/api/courses")
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

        perform(post("/api/courses/" + courseId + "/modules")
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

        perform(post("/api/lessons/modules/" + moduleId + "/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверка: формат ошибки 404 при чтении несуществующего курса.
     */
    @Test
    void errorResponseFormat_course_shouldHaveRequiredFields() throws Exception {
        MvcResult result = perform(get("/api/courses/99999"))
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
     * Получает ID первого модуля курса через прямой SQL.
     */
    private Long getModuleIdByCourse(Long courseId) throws Exception {
        return jdbcTemplate.queryForObject("select id from modules where course_id = ? order by id limit 1",
                Long.class, courseId);
    }
}
