package com.studying.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа в приложение «Учебная платформа».
 * <p>
 * Использует Spring Boot для автоматической конфигурации и запуска
 * веб-приложения с поддержкой Spring Data JPA и PostgreSQL.
 * Поддерживает профили «dev» и «test» для разработки и тестирования.
 * </p>
 */
@SpringBootApplication
public class StudyingPlatformApplication {

    /**
     * Конструктор по умолчанию для точки входа в приложение.
     * Используется Spring Boot для создания экземпляра приложения.
     */
    public StudyingPlatformApplication() {
        // Точка входа — static main(), конструктор не требуется.
    }

    /**
     * Точка входа в приложение.
     * Запускает Spring Boot-приложение с автоматической конфигурацией.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(StudyingPlatformApplication.class, args);
    }
}
