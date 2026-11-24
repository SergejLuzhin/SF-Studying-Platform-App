package com.studying.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Отслеживает созданные HTTP-запросами фикстуры и удаляет только их. */
public class HttpFixtureTracker {
    private final Map<String, Set<Long>> ids = new LinkedHashMap<>();

    /** Создаёт пустой набор идентификаторов фикстур одного теста. */
    public HttpFixtureTracker() {
    }

    /**
     * Запоминает идентификатор созданного ресурса до выполнения проверок ответа.
     *
     * @param result результат HTTP-запроса
     * @param mapper преобразователь JSON
     * @throws Exception если ответ созданного ресурса нельзя прочитать
     */
    public void record(MvcResult result, ObjectMapper mapper) throws Exception {
        if (!"POST".equals(result.getRequest().getMethod()) || result.getResponse().getStatus() != 201) {
            return;
        }
        String path = result.getRequest().getRequestURI();
        String table;
        if (path.equals("/api/users")) table = "users";
        else if (path.equals("/api/categories")) table = "categories";
        else if (path.equals("/api/courses")) table = "courses";
        else if (path.matches("/api/courses/\\d+/modules")) table = "modules";
        else if (path.matches("/api/lessons/modules/\\d+/lessons")) table = "lessons";
        else if (path.matches("/api/assignments/lessons/\\d+/assignments")) table = "assignments";
        else if (path.equals("/api/submissions")) table = "submissions";
        else if (path.equals("/api/enrollments")) table = "enrollments";
        else if (path.equals("/api/quiz")) table = "quiz";
        else if (path.matches("/api/quiz/\\d+/questions")) table = "question";
        else if (path.matches("/api/question/\\d+/options")) table = "answeroption";
        else if (path.matches("/api/quiz/\\d+/submit")) table = "quizsubmission";
        else if (path.equals("/api/course-reviews")) table = "course_reviews";
        else if (path.equals("/api/tags")) table = "tags";
        else throw new IllegalStateException("Неизвестный созданный HTTP-ресурс: " + path);
        var id = mapper.readTree(result.getResponse().getContentAsString()).get("id");
        if (id == null || !id.canConvertToLong()) {
            throw new IllegalStateException("В ответе отсутствует идентификатор: " + path);
        }
        track(table, id.longValue());
    }

    /**
     * Регистрирует идентификатор фикстуры указанной таблицы.
     *
     * @param table имя таблицы из списка поддерживаемых фикстур
     * @param id идентификатор созданной записи
     */
    public void track(String table, long id) {
        if (!Set.of("submissions", "quizsubmission", "course_reviews", "answeroption", "question", "quiz",
                "assignments", "lessons", "modules", "enrollments", "courses", "tags", "categories", "users")
                .contains(table)) {
            throw new IllegalArgumentException("Неизвестная таблица: " + table);
        }
        ids.computeIfAbsent(table, key -> new LinkedHashSet<>()).add(id);
    }

    /**
     * Удаляет зарегистрированные данные в одной транзакции в порядке внешних ключей.
     * Ошибки не подавляются; идентификаторы сбрасываются только после commit.
     *
     * @param jdbc средство выполнения SQL
     * @param transactions шаблон транзакций
     */
    public void clean(JdbcTemplate jdbc, TransactionTemplate transactions) {
        transactions.executeWithoutResult(status -> {
            for (Long courseId : ids.getOrDefault("courses", Set.of())) {
                for (Long tagId : ids.getOrDefault("tags", Set.of())) {
                    jdbc.update("delete from course_tag where course_id = ? and tag_id = ?", courseId, tagId);
                }
            }
            for (String table : new String[]{"submissions", "quizsubmission", "course_reviews", "answeroption",
                    "question", "quiz", "assignments", "lessons", "modules", "enrollments", "courses",
                    "tags", "categories", "users"}) {
                for (Long id : ids.getOrDefault(table, Set.of())) {
                    jdbc.update("delete from " + table + " where id = ?", id);
                }
            }
        });
        ids.clear();
    }
}
