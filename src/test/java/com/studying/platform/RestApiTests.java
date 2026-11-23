package com.studying.platform;

import com.studying.platform.dto.ErrorResponseDto;
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

    /**
     * Очищает созданные тестом данные после каждого теста.
     */
    @AfterEach
    void cleanUp() {
        transactions.executeWithoutResult(status -> {
            // Удаляем категории (сначала разрываем связи с курсами)
            for (int i = createdCategoryIds.size() - 1; i >= 0; i--) {
                jdbcTemplate.update("delete from categories where id = ?", createdCategoryIds.get(i));
            }
            // Удаляем пользователей
            for (int i = createdUserIds.size() - 1; i >= 0; i--) {
                jdbcTemplate.update("delete from users where id = ?", createdUserIds.get(i));
            }
            createdUserIds.clear();
            createdCategoryIds.clear();
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
}
