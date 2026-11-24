package com.studying.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Сквозная приёмка REST-сценария на PostgreSQL без открытой веб-сессии и внешней транзакции.
 */
@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class RestAcceptanceTest {
    /** HTTP-клиент тестового контекста. */
    @Autowired private MockMvc mvc;
    /** Сериализатор тел запросов и ответов. */
    @Autowired private ObjectMapper mapper;
    /** Средство адресной проверки и очистки данных. */
    @Autowired private JdbcTemplate jdbc;
    /** Шаблон транзакции очистки фикстур. */
    @Autowired private TransactionTemplate transactions;
    /** Реестр ресурсов, созданных тестом. */
    private final HttpFixtureTracker fixtures = new HttpFixtureTracker();

    /**
     * Удаляет только ресурсы, созданные текущим сценарием.
     */
    @AfterEach
    void cleanUp() {
        fixtures.clean(jdbc, transactions);
    }

    /**
     * Проверяет учебный сценарий и защиту сохранённой истории через реальные HTTP-эндпоинты.
     *
     * @throws Exception при ошибке HTTP-запроса или разбора ответа
     */
    @Test
    void completeLearningScenarioWithoutOpenSession() throws Exception {
        String suffix = UUID.randomUUID().toString();
        long teacher = create("/api/users", Map.of("email", "t-" + suffix + "@test.io", "role", "TEACHER"));
        long replacement = create("/api/users", Map.of("email", "r-" + suffix + "@test.io", "role", "TEACHER"));
        long student = create("/api/users", Map.of("email", "s-" + suffix + "@test.io", "role", "STUDENT"));
        long category = create("/api/categories", Map.of("name", suffix));
        Map<String, Object> courseBody = Map.of("title", "Курс", "description", "Описание", "duration", 10,
                "startDate", "2026-01-01", "teacherId", teacher, "categoryId", category);
        long course = create("/api/courses", courseBody);
        mvc.perform(put("/api/courses/{id}", course).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("title", "Курс", "description", "Описание",
                        "duration", 10, "startDate", "2026-01-01", "teacherId", replacement,
                        "categoryId", category))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.teacherId").value(replacement));
        long module = create("/api/courses/" + course + "/modules", Map.of("title", "Модуль", "orderIndex", 1));
        long lesson = create("/api/lessons/modules/" + module + "/lessons",
                Map.of("title", "Урок", "content", "Материал"));
        Map<String, Long> enrollment = Map.of("studentId", student, "courseId", course);
        create("/api/enrollments", enrollment);
        mvc.perform(get("/api/enrollments/courses/{id}/students", course))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(student));
        mvc.perform(get("/api/enrollments/users/{id}/courses", student))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(course));
        mvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(enrollment))).andExpect(status().isBadRequest());
        long assignment = create("/api/assignments/lessons/" + lesson + "/assignments",
                Map.of("title", "Задание", "maxScore", 100));
        Map<String, Object> submissionBody = Map.of("assignmentId", assignment, "studentId", student, "content", "Решение");
        long submission = create("/api/submissions", submissionBody);
        mvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(submissionBody))).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/submissions/{id}", submission).param("teacherId", Long.toString(replacement))
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":101}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/submissions/{id}", submission).param("teacherId", Long.toString(replacement))
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":85,\"feedback\":\"Хорошо\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.score").value(85));
        long quiz = create("/api/quiz", Map.of("moduleId", module, "title", "Тест"));
        mvc.perform(get("/api/quiz/{id}/start", quiz)).andExpect(status().isBadRequest());
        long question = create("/api/quiz/" + quiz + "/questions",
                Map.of("text", "Вопрос", "type", "SINGLE_CHOICE"));
        long correct = create("/api/question/" + question + "/options", Map.of("text", "Да", "isCorrect", true));
        create("/api/question/" + question + "/options", Map.of("text", "Нет", "isCorrect", false));
        MvcResult start = mvc.perform(get("/api/quiz/{id}/start", quiz)).andExpect(status().isOk()).andReturn();
        JsonNode questions = mapper.readTree(start.getResponse().getContentAsString()).get("questions");
        assertEquals(question, questions.get(0).get("id").longValue());
        assertEquals(2, questions.get(0).get("options").size());
        questions.get(0).get("options").forEach(option -> {
            assertEquals(2, option.size());
            assertFalse(option.has("isCorrect"));
        });
        long result = create("/api/quiz/" + quiz + "/submit",
                Map.of("studentId", student, "answers", Map.of(Long.toString(question), new long[]{correct})));
        assertEquals(100, jdbc.queryForObject("select score from quizsubmission where id = ?", Integer.class, result));
        long tag = create("/api/tags", Map.of("name", suffix));
        mvc.perform(post("/api/courses/{id}/tags", course).contentType(MediaType.APPLICATION_JSON)
                .content("[" + tag + "]")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/courses/{id}", course)).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/question/{id}", question)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tags/{id}", tag)).andExpect(status().isOk());
        mvc.perform(get("/api/users/{id}", replacement)).andExpect(status().isOk());
        mvc.perform(get("/api/categories/{id}", category)).andExpect(status().isOk());
        mvc.perform(get("/api/users/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Создаёт и сразу регистрирует фикстуру до проверки результата.
     *
     * @param path путь создания
     * @param body данные запроса
     * @return идентификатор созданного ресурса
     * @throws Exception при ошибке запроса или разбора ответа
     */
    private long create(String path, Object body) throws Exception {
        MvcResult result = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
                .andDo(response -> fixtures.record(response, mapper))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").longValue();
    }
}
