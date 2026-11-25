package com.studying.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для проверки схемы базы данных PostgreSQL.
 * <p>
 * Проверяют наличие реализованных таблиц, точный состав первичных ключей,
 * все четыре компонента внешних ключей и уникальные ограничения,
 * сгруппированные по идентификатору ограничения, а не по таблице.
 * Тесты выполняются с реальной PostgreSQL через Testcontainers в профиле «test».
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
class PostgresSchemaTests {

    /**
     * Проверяет соединение с базой данных.
     * <p>
     * Выполняет простой запрос {@code SELECT 1} для подтверждения,
     * что приложение подключено к работающей PostgreSQL.
     * </p>
     *
     * @param dataSource автоматически настроенный Spring DataSource
     * @throws Exception если возникла ошибка при получении или использовании соединения
     */
    @Test
    void connectionTest(@Autowired DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            assertTrue(resultSet.next(), "Запрос SELECT 1 должен вернуть строку");
            assertEquals(1, resultSet.getInt(1), "Результат SELECT 1 должен быть равен 1");
        }
    }

    /**
     * Проверяет, что активен профиль «test».
     *
     * @param environment внедрённый Spring Environment для получения активных профилей
     */
    @Test
    void activeProfileIsTest(@Autowired org.springframework.core.env.Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        assertArrayEquals(new String[]{"test"}, activeProfiles,
                "Активный профиль должен быть «test», получены: " + String.join(", ", activeProfiles));
    }

    /**
     * Проверяет, что в тестах используется PostgreSQL.
     *
     * @param dataSource автоматически настроенный Spring DataSource
     * @throws Exception если возникла ошибка при получении мета-данных соединения
     */
    @Test
    void databaseIsPostgreSQL(@Autowired DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            assertEquals("PostgreSQL", productName,
                    "Тестовая база должна быть PostgreSQL, получено: " + productName);
        }
    }

    /**
     * Проверяет наличие всех реализованных таблиц в схеме.
     * <p>
     * Ожидаемые таблицы: users, profiles, categories, courses, modules,
     * lessons, enrollments, tags, course_reviews, course_tag, assignments, submissions.
     * </p>
     *
     * @param dataSource автоматически настроенный Spring DataSource
     * @throws Exception если возникла ошибка при выполнении запроса
     */
    @Test
    void requiredTablesExist(@Autowired DataSource dataSource) throws Exception {
        String[] expectedTables = {
                "users", "profiles", "categories", "courses",
                "modules", "lessons", "enrollments",
                "tags", "course_reviews", "course_tag",
                "assignments", "submissions",
                "quiz", "question", "answeroption",
                "quizsubmission"
        };

        try (Connection connection = dataSource.getConnection()) {
            ResultSet tables = connection.getMetaData().getTables(
                    null, currentSchema(connection), null, new String[]{"TABLE"});

            Set<String> actualTables = new HashSet<>();
            while (tables.next()) {
                actualTables.add(tables.getString("TABLE_NAME").toLowerCase());
            }

            for (String expectedTable : expectedTables) {
                assertTrue(actualTables.contains(expectedTable.toLowerCase()),
                        "Таблица «" + expectedTable + "» должна существовать в схеме. " +
                                "Найдены таблицы: " + actualTables);
            }
        }
    }

    /**
     * Проверяет точный состав первичных ключей для каждой таблицы.
     * <p>
     * Для таблиц с одиночным ключом проверяет, что набор колонок PK
     * в точности равен {@code {id}}. Для {@code course_tag} проверяет
     * точное равенство множеству {@code {course_id, tag_id}}.
     * Лишние колонки в PK приводят к ошибке.
     * </p>
     *
     * @param dataSource автоматически настроенный Spring DataSource
     * @throws Exception если возникла ошибка при выполнении запроса
     */
    @Test
    void primaryKeysExist(@Autowired DataSource dataSource) throws Exception {
        // Таблицы с одиночным PK {id}
        Map<String, Set<String>> expectedPks = new LinkedHashMap<>();
        expectedPks.put("users", Set.of("id"));
        expectedPks.put("profiles", Set.of("id"));
        expectedPks.put("categories", Set.of("id"));
        expectedPks.put("courses", Set.of("id"));
        expectedPks.put("modules", Set.of("id"));
        expectedPks.put("lessons", Set.of("id"));
        expectedPks.put("enrollments", Set.of("id"));
        expectedPks.put("tags", Set.of("id"));
        expectedPks.put("course_reviews", Set.of("id"));
        expectedPks.put("course_tag", Set.of("course_id", "tag_id"));
        expectedPks.put("assignments", Set.of("id"));
        expectedPks.put("submissions", Set.of("id"));
        expectedPks.put("quiz", Set.of("id"));
        expectedPks.put("question", Set.of("id"));
        expectedPks.put("answeroption", Set.of("id"));
        expectedPks.put("quizsubmission", Set.of("id"));

        try (Connection connection = dataSource.getConnection()) {
            String schema = currentSchema(connection);

            for (Map.Entry<String, Set<String>> entry : expectedPks.entrySet()) {
                String tableName = entry.getKey();
                Set<String> expectedColumns = entry.getValue();

                ResultSet pkRs = connection.getMetaData().getPrimaryKeys(
                        null, schema, tableName);

                Set<String> actualColumns = new HashSet<>();
                while (pkRs.next()) {
                    actualColumns.add(pkRs.getString("COLUMN_NAME").toLowerCase());
                }

                // Проверяем точное равенство множеств: не только наличие, но и отсутствие лишних колонок
                assertPrimaryKey(expectedColumns, actualColumns,
                        "Таблица «" + tableName + "»: ожидаемый набор колонок первичного ключа " +
                                expectedColumns + ", получено " + actualColumns);
            }
        }
    }

    /**
     * Проверяет точный состав внешних ключей.
     * <p>
     * Для каждого FK проверяет все четыре компонента:
     * таблица-владелец FK, колонка-владелец FK, целевая таблица, целевая колонка.
     * Использует JDBC {@code getImportedKeys}, который возвращает информацию
     * о внешних ключах для каждой таблицы-владельца.
     * </p>
     *
     * @param dataSource автоматически настроенный Spring DataSource
     * @throws Exception если возникла ошибка при выполнении запроса
     */
    @Test
    void foreignKeysExist(@Autowired DataSource dataSource) throws Exception {
        // Каждый элемент: [таблица-владелец FK, колонка-FK, целевая таблица, целевая колонка]
        String[][] expectedFks = {
                {"profiles", "user_id", "users", "id"},
                {"courses", "teacher_id", "users", "id"},
                {"courses", "category_id", "categories", "id"},
                {"modules", "course_id", "courses", "id"},
                {"lessons", "module_id", "modules", "id"},
                {"enrollments", "user_id", "users", "id"},
                {"enrollments", "course_id", "courses", "id"},
                {"course_reviews", "course_id", "courses", "id"},
                {"course_reviews", "student_id", "users", "id"},
                {"course_tag", "course_id", "courses", "id"},
                {"course_tag", "tag_id", "tags", "id"},
                {"assignments", "lesson_id", "lessons", "id"},
                {"submissions", "assignment_id", "assignments", "id"},
                {"submissions", "student_id", "users", "id"},
                {"quiz", "module_id", "modules", "id"},
                {"question", "quiz_id", "quiz", "id"},
                {"answeroption", "question_id", "question", "id"},
                {"quizsubmission", "quiz_id", "quiz", "id"},
                {"quizsubmission", "student_id", "users", "id"}
        };

        try (Connection connection = dataSource.getConnection()) {
            String schema = currentSchema(connection);

            // Собираем все FK через getImportedKeys:
            // для каждой таблицы возвращает FK, где эта таблица — владелец
            ResultSet fks = connection.getMetaData().getImportedKeys(
                    null, schema, null);

            // Формируем набор ожидаемых FK для быстрого сравнения
            Set<String> expectedSet = new HashSet<>();
            for (String[] fk : expectedFks) {
                expectedSet.add(fk[0] + "." + fk[1] + "->" + fk[2] + "." + fk[3]);
            }

            // Собираем все фактические FK
            Set<String> actualSet = new HashSet<>();
            while (fks.next()) {
                String fkTable = fks.getString("FKTABLE_NAME").toLowerCase();
                String fkColumn = fks.getString("FKCOLUMN_NAME").toLowerCase();
                String pkTable = fks.getString("PKTABLE_NAME").toLowerCase();
                String pkColumn = fks.getString("PKCOLUMN_NAME").toLowerCase();
                actualSet.add(fkTable + "." + fkColumn + "->" + pkTable + "." + pkColumn);
            }

            // Проверяем, что каждый ожидаемый FK найден
            for (String[] expected : expectedFks) {
                String expectedKey = expected[0] + "." + expected[1] + "->" + expected[2] + "." + expected[3];
                assertForeignKey(expectedKey, actualSet,
                        "Внешний ключ «" + expectedKey +
                                "» должен существовать. " +
                                "Ожидаемые FK: " + expectedSet +
                                ", фактические FK: " + actualSet);
            }

            // Проверяем отсутствие лишних FK (не должно быть FK, которых нет в expected)
            for (String actualKey : actualSet) {
                assertTrue(expectedSet.contains(actualKey),
                        "Неожиданный внешний ключ «" + actualKey +
                                "» обнаружен. Ожидаемые: " + expectedSet +
                                ", фактические: " + actualSet);
            }
        }
    }

    /**
     * Проверяет уникальные ограничения, сгруппированные по идентификатору ограничения.
     * <p>
     * Ограничения группируются по имени ограничения (constraint_name),
     * а не по имени таблицы. Это позволяет корректно отличать два
     * отдельных уникальных ограничения на {@code user_id} и {@code course_id}
     * от одного составного ограничения {@code (user_id, course_id)}.
     * </p>
     *
     * @param dataSource автоматически настроенный Spring DataSource
     * @throws Exception если возникла ошибка при выполнении запроса
     */
    @Test
    void uniqueConstraintsExist(@Autowired DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {

            String schema = currentSchema(connection);

            // Получаем все ограничения типа UNIQUE с таблицей, именем и колонками
            ResultSet constraints = stmt.executeQuery(
                    "SELECT tc.constraint_name, tc.table_name, kcu.column_name, kcu.ordinal_position " +
                            "FROM information_schema.table_constraints tc " +
                            "JOIN information_schema.key_column_usage kcu " +
                            "  ON tc.constraint_name = kcu.constraint_name " +
                            "    AND tc.table_schema = kcu.table_schema " +
                            "    AND tc.table_name = kcu.table_name " +
                            "WHERE tc.constraint_type = 'UNIQUE' " +
                            "  AND tc.table_schema = '" + schema + "' " +
                            "ORDER BY tc.constraint_name, kcu.ordinal_position");

            // Группируем: table_name -> constraint_name -> columns (в порядке)
            Map<String, Map<String, List<String>>> actualByTable = new LinkedHashMap<>();
            while (constraints.next()) {
                String tableName = constraints.getString("table_name").toLowerCase();
                String constraintName = constraints.getString("constraint_name").toLowerCase();
                String column = constraints.getString("column_name").toLowerCase();

                actualByTable.computeIfAbsent(tableName,
                        k -> new LinkedHashMap<>())
                        .computeIfAbsent(constraintName,
                                k -> new ArrayList<>())
                        .add(column);
            }

            // Ожидаемые ограничения: таблица -> список колонок (по одному ограничению на таблицу)
            Map<String, List<String>> expectedConstraints = new LinkedHashMap<>();
            expectedConstraints.put("users", List.of("login"));
            expectedConstraints.put("profiles", List.of("user_id"));
            expectedConstraints.put("tags", List.of("name"));
            expectedConstraints.put("categories", List.of("name"));
            expectedConstraints.put("enrollments", List.of("user_id", "course_id"));
            expectedConstraints.put("course_reviews", List.of("student_id", "course_id"));
            expectedConstraints.put("submissions", List.of("assignment_id", "student_id"));
            expectedConstraints.put("quiz", List.of("module_id"));

            // Проверяем каждое ожидаемое ограничение
            for (Map.Entry<String, List<String>> entry : expectedConstraints.entrySet()) {
                String table = entry.getKey();
                List<String> expectedCols = entry.getValue();

                Map<String, List<String>> tableConstraints = actualByTable.getOrDefault(table, Map.of());

                assertUniqueConstraint(expectedCols, tableConstraints,
                        "Уникальное ограничение на колонки " + expectedCols +
                                " таблицы «" + table + "» должно существовать. " +
                                "Фактические ограничения: " + tableConstraints);
            }

            // Проверяем отсутствие ограничений на непредусмотренных таблицах
            Set<String> expectedTables = Set.of("users", "profiles", "tags", "categories",
                    "enrollments", "course_reviews", "submissions", "quiz");
            for (String actualTable : actualByTable.keySet()) {
                assertTrue(expectedTables.contains(actualTable),
                        "Неожиданная таблица с уникальным ограничением «" + actualTable +
                                "». Ожидаемые таблицы: " + expectedTables);
            }
        }
    }

    /**
     * Проверяет, что некорректный FK (с несуществующей колонкой-владельцем) не проходит валидацию.
     * <p>
     * Имитирует проверку с неправильной колонкой-владельцем FK:
     * проверяет, что profiles.nonexistent_column -&gt; users.id действительно отсутствует.
     * </p>
     *
     * @param dataSource автоматически настроенный Spring DataSource
     * @throws Exception если возникла ошибка при выполнении запроса
     */
    @Test
    void fkWrongOwnerColumnShouldFail(@Autowired DataSource dataSource) throws Exception {
        // Симулируем проверку FK с неверной колонкой-владельцем
        // Реальный FK: profiles.user_id -> users.id
        // Проверяем, что profiles.nonexistent_column -> users.id не найден
        String wrongFkKey = "profiles.nonexistent_column->users.id";

        try (Connection connection = dataSource.getConnection()) {
            String schema = currentSchema(connection);
            ResultSet fks = connection.getMetaData().getImportedKeys(
                    null, schema, null);

            Set<String> actualSet = new HashSet<>();
            while (fks.next()) {
                String fkTable = fks.getString("FKTABLE_NAME").toLowerCase();
                String fkColumn = fks.getString("FKCOLUMN_NAME").toLowerCase();
                String pkTable = fks.getString("PKTABLE_NAME").toLowerCase();
                String pkColumn = fks.getString("PKCOLUMN_NAME").toLowerCase();
                actualSet.add(fkTable + "." + fkColumn + "->" + pkTable + "." + pkColumn);
            }

            assertFalse(actualSet.contains(wrongFkKey),
                    "FK с несуществующей колонкой-владельцем «" + wrongFkKey +
                            "» не должен быть найден. Фактические FK: " + actualSet);
        }
    }

    /**
     * Проверяет, что лишний столбец в PK обнаруживается.
     * <p>
     * Убедимся, что PK для users — именно {id}, а не {id, что-то_ещё}.
     * </p>
     *
     * @param dataSource автоматически настроенный Spring DataSource
     * @throws Exception если возникла ошибка при выполнении запроса
     */
    @Test
    void pkExtraColumnShouldBeDetected(@Autowired DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String schema = currentSchema(connection);
            ResultSet pkRs = connection.getMetaData().getPrimaryKeys(
                    null, schema, "users");
            Set<String> actualPkColumns = new HashSet<>();
            while (pkRs.next()) {
                actualPkColumns.add(pkRs.getString("COLUMN_NAME").toLowerCase());
            }
            assertEquals(Set.of("id"), actualPkColumns,
                    "PK таблицы users должен быть точно {id}");
        }
    }

    /**
     * Проверяет, что два отдельных уникальных ограничения не сливаются в одно.
     * <p>
     * enrollments имеет составное уникальное ограничение на (user_id, course_id).
     * Этот тест убеждается, что группировка по имени ограничения
     * корректно различает составное и отдельные ограничения.
     * </p>
     *
     * @param dataSource автоматически настроенный Spring DataSource
     * @throws Exception если возникла ошибка при выполнении запроса
     */
    @Test
    void uniqueSeparateConstraintsNotMerged(@Autowired DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {

            String schema = currentSchema(connection);

            // Получаем ограничения для enrollments, сгруппированные по имени
            Map<String, List<String>> constraintsByConstraintName = new LinkedHashMap<>();
            ResultSet constraints = stmt.executeQuery(
                    "SELECT tc.constraint_name, kcu.column_name, kcu.ordinal_position " +
                            "FROM information_schema.table_constraints tc " +
                            "JOIN information_schema.key_column_usage kcu " +
                            "  ON tc.constraint_name = kcu.constraint_name " +
                            "    AND tc.table_schema = kcu.table_schema " +
                            "    AND tc.table_name = kcu.table_name " +
                            "WHERE tc.constraint_type = 'UNIQUE' " +
                            "  AND tc.table_schema = '" + schema + "' " +
                            "  AND tc.table_name = 'enrollments' " +
                            "ORDER BY tc.constraint_name, kcu.ordinal_position");

            while (constraints.next()) {
                String constraintName = constraints.getString("constraint_name").toLowerCase();
                String column = constraints.getString("column_name").toLowerCase();

                constraintsByConstraintName.computeIfAbsent(constraintName,
                        k -> new ArrayList<>())
                        .add(column);
            }

            // Проверяем, что составное ограничение имеет обе колонки в одном наборе,
            // а не разбросаны по разным ограничениям
            assertTrue(constraintsByConstraintName.size() >= 1,
                    "У таблицы enrollments должно быть хотя бы одно уникальное ограничение");

            // Ищем ограничение, содержащее user_id
            List<String> userCourseConstraint = null;
            for (Map.Entry<String, List<String>> entry : constraintsByConstraintName.entrySet()) {
                if (entry.getValue().contains("user_id") && entry.getValue().contains("course_id")) {
                    userCourseConstraint = entry.getValue();
                    break;
                }
            }

            assertNotNull(userCourseConstraint,
                    "Должно существовать составное ограничение, содержащее user_id и course_id в одном наборе");
            assertEquals(2, userCourseConstraint.size(),
                    "Составное ограничение должно содержать ровно 2 колонки");
            assertTrue(userCourseConstraint.contains("user_id"),
                    "Составное ограничение должно содержать user_id");
            assertTrue(userCourseConstraint.contains("course_id"),
                    "Составное ограничение должно содержать course_id");
        }
    }

    /**
     * Проверяет точное совпадение состава первичного ключа.
     *
     * @param expected ожидаемые колонки
     * @param actual фактические колонки
     * @param message описание проверки
     */
    private void assertPrimaryKey(Set<String> expected, Set<String> actual, String message) {
        assertEquals(expected, actual, message);
    }

    /**
     * Проверяет наличие внешнего ключа с точными колонками и таблицами.
     *
     * @param expected ожидаемый ключ
     * @param actual фактические ключи
     * @param message описание проверки
     */
    private void assertForeignKey(String expected, Set<String> actual, String message) {
        assertTrue(actual.contains(expected), message);
    }

    /**
     * Проверяет наличие одного уникального ограничения с заданными колонками.
     *
     * @param expected ожидаемые колонки одного ограничения
     * @param actual ограничения, сгруппированные по имени
     * @param message описание проверки
     */
    private void assertUniqueConstraint(List<String> expected, Map<String, List<String>> actual, String message) {
        assertTrue(actual.values().stream().anyMatch(expected::equals), message);
    }

    /** Проверяет отклонение первичного ключа с дополнительной колонкой. */
    @Test
    void primaryKeyMatcherRejectsExtraColumn() {
        assertThrows(AssertionError.class,
                () -> assertPrimaryKey(Set.of("id"), Set.of("id", "extra"), "Неверный PK"));
    }

    /** Проверяет отклонение внешнего ключа с неверной колонкой владельца. */
    @Test
    void foreignKeyMatcherRejectsWrongColumn() {
        assertThrows(AssertionError.class, () -> assertForeignKey("profiles.user_id->users.id",
                Set.of("profiles.other_id->users.id"), "Неверный FK"));
    }

    /** Проверяет, что два отдельных ограничения не заменяют составное. */
    @Test
    void uniqueMatcherRejectsSeparateConstraints() {
        assertThrows(AssertionError.class, () -> assertUniqueConstraint(List.of("user_id", "course_id"),
                Map.of("first", List.of("user_id"), "second", List.of("course_id")), "Неверный UNIQUE"));
    }

    /**
     * Проверяет значение параметра hibernate hbm2ddl.auto в профиле «test».
     *
     * @param environment внедрённый Spring Environment для чтения свойств конфигурации
     */
    @Test
    void ddlAutoIsCreateDrop(@Autowired org.springframework.core.env.Environment environment) {
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        assertEquals("create-drop", ddlAuto,
                "ddl-auto в профиле «test» должен быть «create-drop», получено: " + ddlAuto);
    }

    /**
     * Проверяет изоляцию тестовой базы от рабочей.
     * <p>
     * Имя тестовой базы не должно содержать имени рабочей базы «studying_platform».
     * </p>
     *
     * @param dataSource автоматически настроенный Spring DataSource
     * @throws Exception если возникла ошибка при получении мета-данных соединения
     */
    @Test
    void testDatabaseIsIsolatedFromDev(@Autowired DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertFalse(url.contains("studying_platform"),
                    "URL не должен указывать на рабочую базу «studying_platform», получено: " + url);
            assertTrue(url.startsWith("jdbc:postgresql://"),
                    "JDBC URL должен начинаться с jdbc:postgresql://, получено: " + url);
        }
    }

    /**
     * Проверяет имя текущей схемы PostgreSQL.
     *
     * @param dataSource автоматически настроенный Spring DataSource
     * @throws Exception если возникла ошибка при выполнении запроса
     */
    @Test
    void currentSchemaNameIsPublic(@Autowired DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String schema = currentSchema(connection);
            assertEquals("public", schema,
                    "Текущая схема должна быть «public», получено: " + schema);
        }
    }

    /**
     * Возвращает имя текущей схемы PostgreSQL.
     *
     * @param connection соединение JDBC
     * @return имя схемы (обычно «public»)
     * @throws Exception если возникла ошибка при выполнении запроса
     */
    private String currentSchema(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT current_schema()")) {
            if (rs.next()) {
                return rs.getString(1);
            }
            return "public";
        }
    }
}
