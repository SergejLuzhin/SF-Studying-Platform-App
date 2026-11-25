package com.studying.platform;

import com.studying.platform.seeder.DemoDataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Проверяет реальный запуск профилей разработки на изолированной PostgreSQL.
 */
class DevProfileStartupTest {

    /**
     * Проверяет отсутствие сидера в dev и повторяемость двух запусков dev с demo.
     */
    @Test
    void devAndDemoStartWithIsolatedDatabase() {
        try (PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:16")) {
            database.start();
            try (ConfigurableApplicationContext context = start(database, "dev")) {
                verifyConfiguration(context, database);
                assertTrue(context.getBeansOfType(DemoDataSeeder.class).isEmpty());
                assertEquals(0, context.getBean(JdbcTemplate.class)
                        .queryForObject("select count(*) from users", Long.class));
            }
            List<Long> initialCounts;
            try (ConfigurableApplicationContext context = start(database, "dev,demo")) {
                verifyConfiguration(context, database);
                assertEquals(1, context.getBeansOfType(DemoDataSeeder.class).size());
                initialCounts = counts(context);
                assertEquals(List.of(3L, 1L, 2L, 4L, 4L, 2L, 1L, 2L, 6L, 1L), initialCounts);
            }
            try (ConfigurableApplicationContext context = start(database, "dev,demo")) {
                verifyConfiguration(context, database);
                assertEquals(initialCounts, counts(context));
            }
        }
    }

    /**
     * Запускает приложение с параметрами отдельного контейнера и заданными профилями.
     *
     * @param database изолированная база
     * @param profiles активные профили
     * @return запущенный контекст приложения
     */
    private ConfigurableApplicationContext start(PostgreSQLContainer<?> database, String profiles) {
        return new SpringApplicationBuilder(StudyingPlatformApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.profiles.active=" + profiles,
                        "--spring.datasource.url=" + database.getJdbcUrl(),
                        "--spring.datasource.username=" + database.getUsername(),
                        "--spring.datasource.password=" + database.getPassword());
    }

    /**
     * Проверяет выбранный профиль, настройки схемы и фактический адрес подключения.
     *
     * @param context контекст приложения
     * @param database ожидаемый контейнер
     */
    private void verifyConfiguration(ConfigurableApplicationContext context, PostgreSQLContainer<?> database) {
        assertFalse(Arrays.asList(context.getEnvironment().getActiveProfiles()).contains("test"));
        assertEquals("update", context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals(database.getJdbcUrl(), context.getBean(JdbcTemplate.class).execute(
                (org.springframework.jdbc.core.ConnectionCallback<String>) connection -> connection.getMetaData().getURL()));
    }

    /**
     * Подсчитывает демонстрационные записи и маркер преподавателя.
     *
     * @param context контекст приложения
     * @return количества сущностей в фиксированном порядке
     */
    private List<Long> counts(ConfigurableApplicationContext context) {
        JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
        return List.of(
                jdbc.queryForObject("select count(*) from users", Long.class),
                jdbc.queryForObject("select count(*) from courses", Long.class),
                jdbc.queryForObject("select count(*) from modules", Long.class),
                jdbc.queryForObject("select count(*) from lessons", Long.class),
                jdbc.queryForObject("select count(*) from assignments", Long.class),
                jdbc.queryForObject("select count(*) from enrollments", Long.class),
                jdbc.queryForObject("select count(*) from quiz", Long.class),
                jdbc.queryForObject("select count(*) from question", Long.class),
                jdbc.queryForObject("select count(*) from answeroption", Long.class),
                jdbc.queryForObject("select count(*) from users where login = 'demo-teacher@platform.test'", Long.class));
    }
}
