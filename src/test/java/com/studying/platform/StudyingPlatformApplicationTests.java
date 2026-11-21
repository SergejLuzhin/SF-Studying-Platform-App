package com.studying.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

/**
 * Базовый тест инициализации Spring Boot-контекста.
 * <p>
 * Запускает контекст с профилем «test». PostgreSQL поднимается
 * автоматически через Testcontainers (URL {@code jdbc:tc:}).
 * Проверяет, что Spring Boot корректно загружает контекст с указанными
 * зависимостями и может создать {@code DataSource}.
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
        org.junit.jupiter.api.Assertions.assertNotNull(dataSource);
    }
}
