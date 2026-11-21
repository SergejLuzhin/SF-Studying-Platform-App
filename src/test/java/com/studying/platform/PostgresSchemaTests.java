package com.studying.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для проверки схемы базы данных PostgreSQL.
 * <p>
 * Проверяют наличие реализованных таблиц, первичных ключей, внешних ключей
 * и уникальных ограничений через метаданные JDBC (information_schema).
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
     * lessons, enrollments, tags, course_reviews, course_tag.
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
                "tags", "course_reviews", "course_tag"
        };

        try (Connection connection = dataSource.getConnection()) {
            ResultSet tables = connection.getMetaData().getTables(
                    null, currentSchema(connection), null, new String[]{"TABLE"});

            java.util.Set<String> actualTables = new java.util.HashSet<>();
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
     * Проверяет наличие первичных ключей для каждой таблицы.
     * <p>
     * У всех таблиц, кроме {@code course_tag}, первичный ключ — одиночный {@code id}.
     * Для {@code course_tag} первичный ключ составной из {@code course_id} и {@code tag_id}.
     * </p>
     *
     * @param dataSource автоматически настроенный Spring DataSource
     * @throws Exception если возникла ошибка при выполнении запроса
     */
    @Test
    void primaryKeysExist(@Autowired DataSource dataSource) throws Exception {
        // Таблицы с одиночным PK "id"
        String[] singlePkTables = {"users", "profiles", "categories", "courses",
                "modules", "lessons", "enrollments", "tags", "course_reviews"};

        try (Connection connection = dataSource.getConnection()) {
            for (String tableName : singlePkTables) {
                ResultSet pkRs = connection.getMetaData().getPrimaryKeys(
                        null, currentSchema(connection), tableName);

                boolean found = false;
                while (pkRs.next()) {
                    String columnName = pkRs.getString("COLUMN_NAME").toLowerCase();
                    if ("id".equals(columnName)) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found,
                        "Таблица «" + tableName + "» должна иметь первичный ключ «id». " +
                                "Проверяется через getPrimaryKeys() JDBC metadata.");
            }

            // course_tag — составной первичный ключ (course_id, tag_id)
            ResultSet courseTagPk = connection.getMetaData().getPrimaryKeys(
                    null, currentSchema(connection), "course_tag");

            java.util.Set<String> courseTagPkColumns = new java.util.HashSet<>();
            while (courseTagPk.next()) {
                courseTagPkColumns.add(courseTagPk.getString("COLUMN_NAME").toLowerCase());
            }

            assertTrue(courseTagPkColumns.contains("course_id"),
                    "course_tag должна иметь «course_id» в первичном ключе");
            assertTrue(courseTagPkColumns.contains("tag_id"),
                    "course_tag должна иметь «tag_id» в первичном ключе");
        }
    }

    /**
     * Проверяет наличие внешних ключей для всех реализованных связей.
     * <p>
     * Проверяются FK: profiles.user_id → users.id, courses.teacher_id → users.id,
     * courses.category_id → categories.id, modules.course_id → courses.id,
     * lessons.module_id → modules.id, enrollments.user_id → users.id,
     * enrollments.course_id → courses.id, course_reviews.course_id → courses.id,
     * course_reviews.student_id → users.id, course_tag.course_id → courses.id,
     * course_tag.tag_id → tags.id.
     * </p>
     *
     * @param dataSource автоматически настроенный Spring DataSource
     * @throws Exception если возникла ошибка при выполнении запроса
     */
    @Test
    void foreignKeysExist(@Autowired DataSource dataSource) throws Exception {
        // Каждый элемент: [таблица-владелец FK, колонка-FK, таблица-цель]
        // getExportedKeys возвращает в формате PKTable.PKColumn->FKTable
        // поэтому проверяем: users.id->profiles, users.id->courses, categories.id->courses и т.д.
        String[][] expectedFks = {
                {"users", "id", "profiles"},
                {"users", "id", "courses"},
                {"categories", "id", "courses"},
                {"courses", "id", "modules"},
                {"modules", "id", "lessons"},
                {"users", "id", "enrollments"},
                {"courses", "id", "enrollments"},
                {"courses", "id", "course_reviews"},
                {"users", "id", "course_reviews"},
                {"courses", "id", "course_tag"},
                {"tags", "id", "course_tag"}
        };

        try (Connection connection = dataSource.getConnection()) {
            ResultSet fks = connection.getMetaData().getExportedKeys(
                    null, currentSchema(connection), null);

            java.util.Set<String> actualFks = new java.util.HashSet<>();
            while (fks.next()) {
                String pkTable = fks.getString("PKTABLE_NAME").toLowerCase();
                String pkColumn = fks.getString("PKCOLUMN_NAME").toLowerCase();
                String fkTable = fks.getString("FKTABLE_NAME").toLowerCase();
                actualFks.add(pkTable + "." + pkColumn + "->" + fkTable);
            }

            for (String[] expected : expectedFks) {
                String expectedKey = expected[0] + "." + expected[1] + "->" + expected[2];
                assertTrue(actualFks.contains(expectedKey),
                        "Внешний ключ " + expectedKey +
                                " должен существовать. Найдены: " + actualFks);
            }
        }
    }

    /**
     * Проверяет уникальные ограничения на уровне базы данных.
     * <p>
     * Ожидаемые уникальные ограничения:
     * <ul>
     *   <li>{@code users.login} — уникальность логина/email</li>
     *   <li>{@code profiles.user_id} — уникальность связи 1:1</li>
     *   <li>{@code enrollments(user_id, course_id)} — уникальность пары студент-курс</li>
     *   <li>{@code tags.name} — уникальность имени тега</li>
     * </ul>
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

            // Получаем все ограничения типа UNIQUE
            ResultSet constraints = stmt.executeQuery(
                    "SELECT tc.constraint_name, tc.table_name, kcu.column_name " +
                            "FROM information_schema.table_constraints tc " +
                            "JOIN information_schema.key_column_usage kcu " +
                            "  ON tc.constraint_name = kcu.constraint_name " +
                            "    AND tc.table_schema = kcu.table_schema " +
                            "WHERE tc.constraint_type = 'UNIQUE' " +
                            "  AND tc.table_schema = '" + schema + "' " +
                            "ORDER BY tc.table_name, kcu.ordinal_position");

            java.util.Map<String, java.util.Set<String>> uniqueCols =
                    new java.util.LinkedHashMap<>();
            while (constraints.next()) {
                String table = constraints.getString("table_name").toLowerCase();
                String column = constraints.getString("column_name").toLowerCase();
                uniqueCols.computeIfAbsent(table, k -> new java.util.HashSet<>()).add(column);
            }

            // Проверяем каждое ожидаемое уникальное ограничение
            assertTrue(uniqueCols.getOrDefault("users", java.util.Set.of())
                    .contains("login"),
                    "Уникальное ограничение users.login должно существовать. Найдены: " + uniqueCols);

            assertTrue(uniqueCols.getOrDefault("profiles", java.util.Set.of())
                    .contains("user_id"),
                    "Уникальное ограничение profiles.user_id должно существовать. Найдены: " + uniqueCols);

            assertTrue(uniqueCols.getOrDefault("tags", java.util.Set.of())
                    .contains("name"),
                    "Уникальное ограничение tags.name должно существовать. Найдены: " + uniqueCols);

            java.util.Set<String> enrollmentUqColumns =
                    uniqueCols.getOrDefault("enrollments", java.util.Set.of());
            assertTrue(enrollmentUqColumns.contains("user_id"),
                    "Уникальное ограничение enrollments должно содержать user_id");
            assertTrue(enrollmentUqColumns.contains("course_id"),
                    "Уникальное ограничение enrollments должно содержать course_id");
        }
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
     * Возвращает имя текущей схемы PostgreSQL.
     *
     * @param соединение JDBC
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
