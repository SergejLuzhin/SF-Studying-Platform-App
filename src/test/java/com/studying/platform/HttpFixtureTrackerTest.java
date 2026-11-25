package com.studying.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Проверяет изоляцию очистки HTTP-фикстур на PostgreSQL после фиксации транзакций. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class HttpFixtureTrackerTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactions;

    /** Проверяет удаление своих данных и сохранение контрольных после фактической очистки. */
    @Test
    void cleanupPreservesOtherFixtures() throws Exception {
        HttpFixtureTracker controlTracker = new HttpFixtureTracker();
        HttpFixtureTracker currentTracker = new HttpFixtureTracker();
        try {
            Map<String, Long> control = createFixture(controlTracker);
            Map<String, Long> current = createFixture(currentTracker);
            currentTracker.clean(jdbc, transactions);
            transactions.executeWithoutResult(status -> {
                for (String table : current.keySet()) {
                    assertEquals(0, count(table, current.get(table)), table);
                    assertEquals(1, count(table, control.get(table)), table);
                }
                assertEquals(0, jdbc.queryForObject("select count(*) from course_tag where course_id = ? and tag_id = ?",
                        Integer.class, current.get("courses"), current.get("tags")));
                assertEquals(1, jdbc.queryForObject("select count(*) from course_tag where course_id = ? and tag_id = ?",
                        Integer.class, control.get("courses"), control.get("tags")));
            });
        } finally {
            try {
                currentTracker.clean(jdbc, transactions);
            } finally {
                controlTracker.clean(jdbc, transactions);
            }
        }
    }

    /**
     * Создаёт полный набор взаимосвязанных фикстур через HTTP.
     *
     * @param tracker реестр созданных записей
     * @return идентификаторы по именам таблиц
     * @throws Exception при ошибке запроса или чтения ответа
     */
    private Map<String, Long> createFixture(HttpFixtureTracker tracker) throws Exception {
        String unique = UUID.randomUUID().toString();
        Map<String, Long> ids = new LinkedHashMap<>();
        long teacher = create(tracker, "/api/users", Map.of("email", "t-" + unique + "@test.io", "role", "TEACHER"));
        long student = create(tracker, "/api/users", Map.of("email", "s-" + unique + "@test.io", "role", "STUDENT"));
        ids.put("users", student);
        long category = create(tracker, "/api/categories", Map.of("name", unique));
        ids.put("categories", category);
        long course = create(tracker, "/api/courses", Map.of("title", "Курс", "description", "Описание",
                "duration", 10, "startDate", "2026-01-01", "teacherId", teacher, "categoryId", category));
        ids.put("courses", course);
        long module = create(tracker, "/api/courses/" + course + "/modules", Map.of("title", "Модуль", "orderIndex", 1));
        ids.put("modules", module);
        long lesson = create(tracker, "/api/lessons/modules/" + module + "/lessons", Map.of("title", "Урок", "content", "Текст"));
        ids.put("lessons", lesson);
        ids.put("enrollments", create(tracker, "/api/enrollments", Map.of("studentId", student, "courseId", course)));
        long assignment = create(tracker, "/api/assignments/lessons/" + lesson + "/assignments",
                Map.of("lessonId", lesson, "title", "Задание", "description", "Описание", "maxScore", 100));
        ids.put("assignments", assignment);
        ids.put("submissions", create(tracker, "/api/submissions",
                Map.of("assignmentId", assignment, "studentId", student, "content", "Решение")));
        long quiz = create(tracker, "/api/quiz", Map.of("moduleId", module, "title", "Тест"));
        ids.put("quiz", quiz);
        long question = create(tracker, "/api/quiz/" + quiz + "/questions", Map.of("text", "Вопрос", "type", "SINGLE_CHOICE"));
        ids.put("question", question);
        long option = create(tracker, "/api/question/" + question + "/options", Map.of("text", "Ответ", "isCorrect", true));
        ids.put("answeroption", option);
        ids.put("quizsubmission", create(tracker, "/api/quiz/" + quiz + "/submit",
                Map.of("studentId", student, "answers", Map.of(Long.toString(question), new long[]{option}))));
        ids.put("course_reviews", create(tracker, "/api/course-reviews",
                Map.of("studentId", student, "courseId", course, "rating", 5, "comment", "Отзыв")));
        long tag = create(tracker, "/api/tags", Map.of("name", unique));
        ids.put("tags", tag);
        transactions.executeWithoutResult(status -> jdbc.update("insert into course_tag (course_id, tag_id) values (?, ?)", course, tag));
        return ids;
    }

    /**
     * Создаёт ресурс и регистрирует его до проверки HTTP-статуса.
     *
     * @param tracker реестр текущего набора
     * @param path адрес создания
     * @param body тело запроса
     * @return идентификатор ресурса
     * @throws Exception при ошибке запроса или разбора ответа
     */
    private long create(HttpFixtureTracker tracker, String path, Object body) throws Exception {
        MvcResult result = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andDo(response -> tracker.record(response, mapper))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").longValue();
    }

    /**
     * Подсчитывает записи контрольной или тестовой фикстуры.
     *
     * @param table проверяемая таблица из созданного набора
     * @param id идентификатор записи
     * @return число найденных записей
     */
    private int count(String table, long id) {
        return jdbc.queryForObject("select count(*) from " + table + " where id = ?", Integer.class, id);
    }
}
