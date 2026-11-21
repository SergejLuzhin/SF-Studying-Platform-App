package com.studying.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Базовый тест инициализации Spring Boot-контекста.
 * <p>
 * Запускает контекст с профилем «test». PostgreSQL поднимается
 * автоматически через Testcontainers (URL {@code jdbc:tc:}).
 * Проверяет, что Spring Boot корректно загружает контекст с указанными
 * зависимостями, создаёт DataSource, EntityManagerFactory, а также
 * гарантирует изоляцию тестовой базы от рабочей («dev»).
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
class StudyingPlatformApplicationTests {

    /**
     * Проверка успешной инициализации контекста Spring Boot.
     * <p>
     * Успешное выполнение теста означает, что:
     * <ul>
     *   <li>Spring Boot успешно загрузил конфигурацию профиля «test»;</li>
     *   <li>Testcontainers поднял контейнер PostgreSQL;</li>
     *   <li>Spring Data JPA корректно настроил DataSource и EntityManagerFactory.</li>
     * </ul>
     * </p>
     *
     * @param dataSource автоматически настроенный Spring DataSource;
     *                   проверяется, что не {@code null}
     */
    @Test
    void contextLoads(@Autowired DataSource dataSource) {
        // Контекст должен загрузиться без исключений, DataSource — не пустой
        assertNotNull(dataSource);
    }

    /**
     * Проверка наличия настроенного EntityManagerFactory (JPA-слой инициализирован).
     * <p>
     * Успешное внедрение {@code EntityManagerFactory} через {@code @Autowired}
     * и проверка на {@code null} подтверждают, что Spring Data JPA
     * корректно обработал конфигурацию Hibernate и подключился к тестовой БД.
     * </p>
     *
     * @param entityManagerFactory автоматически настроенный Spring EntityManagerFactory;
     *                             проверяется, что не {@code null}
     */
    @Test
    void entityManagerFactoryIsConfigured(@Autowired org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        // EntityManagerFactory должен быть создан Spring Boot при наличии spring-boot-starter-data-jpa
        assertNotNull(entityManagerFactory, "EntityManagerFactory должен быть настроен Spring Boot");
    }

    /**
     * Проверка живого соединения с контейнеризированной PostgreSQL.
     * <p>
     * Получает соединение из DataSource и выполняет простой SQL-запрос
     * {@code SELECT 1} для подтверждения, что Testcontainers запустил
     * рабочий контейнер PostgreSQL и Spring может взаимодействовать с ним.
     * </p>
     *
     * @param dataSource автоматически настроенный Spring DataSource;
     *                   через него открывается соединение и выполняется запрос
     * @throws Exception если возникла ошибка при получении или использовании соединения
     */
    @Test
    void testLiveConnectionToPostgres(@Autowired DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {

            assertTrue(resultSet.next(), "Запрос SELECT 1 должен вернуть строку");
            assertEquals(1, resultSet.getInt(1), "Результат SELECT 1 должен быть равен 1");
        }
    }

    /**
     * Проверка активного тестового профиля.
     * <p>
     * Убеждается, что в момент запуска теста активен профиль «test»
     * (а не «dev» или другой), что гарантирует использование
     * конфигурации Testcontainers вместо рабочей базы.
     * </p>
     *
     * @param environment внедрённый Spring Environment для получения активных профилей
     */
    @Test
    void activeProfileIsTest(@Autowired Environment environment) {
        // Проверяем, что активный профиль — "test"
        String[] activeProfiles = environment.getActiveProfiles();
        assertArrayEquals(new String[]{"test"}, activeProfiles,
                "Активный профиль должен быть «test», получены: " + String.join(", ", activeProfiles));
    }

    /**
     * Проверка имени тестовой базы данных (изоляция от рабочей).
     * <p>
     * Профиль «test» использует Testcontainers JDBC URL
     * {@code jdbc:tc:postgresql:16:///studying_platform_test}, который
     * гарантированно не указывает на рабочую базу «studying_platform».
     * Реальный URL после трансформации Testcontainers (например,
     * {@code jdbc:postgresql://localhost:55532/test}) проверяется на
     * отсутствие имени рабочей базы.
     * </p>
     *
     * @param dataSource автоматически настроенный Spring DataSource;
     *                   через него извлекается мета-данные JDBC-соединения
     * @throws Exception если возникла ошибка при получении мета-данных соединения
     */
    @Test
    void testDatabaseIsIsolatedFromDev(@Autowired DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            // Testcontainers трансформирует jdbc:tc: URL; проверяем отсутствие рабочей базы
            assertFalse(url.contains("studying_platform"),
                    "URL не должен указывать на рабочую базу «studying_platform», получено: " + url);
            // URL должен быть валидным jdbc:postgresql
            assertTrue(url.startsWith("jdbc:postgresql://"),
                    "JDBC URL должен начинаться с jdbc:postgresql://, получено: " + url);
        }
    }

    /**
     * Проверка значения параметра hibernate hbm2ddl.auto.
     * <p>
     * В профиле «test» установлено {@code ddl-auto: create-drop}
     * в файле {@code application-test.yml}. Данный тест проверяет,
     * что конфигурация Hibernate корректно загружена Spring Boot.
     * Свойство доступно через ключ {@code spring.jpa.hibernate.ddl-auto}.
     * </p>
     *
     * @param environment внедрённый Spring Environment для чтения свойств конфигурации
     */
    @Test
    void ddlAutoIsCreateDrop(@Autowired Environment environment) {
        // Свойство ddl-auto читается как spring.jpa.hibernate.ddl-auto
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        assertEquals("create-drop", ddlAuto,
                "ddl-auto в профиле «test» должен быть «create-drop», получено: " + ddlAuto);
    }
}
