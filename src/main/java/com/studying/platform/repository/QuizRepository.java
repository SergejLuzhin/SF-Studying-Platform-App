package com.studying.platform.repository;

import com.studying.platform.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий для сущности «Викторина» учебной платформы.
 * <p>
 * Обеспечивает стандартные CRUD-операции и поиск викторины по модулю
 * для проверки ограничения «один модуль — один тест».
 * </p>
 *
 * @see Quiz
 */
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    /**
     * Находит викторину, привязанную к заданному модулю.
     *
     * @param moduleId идентификатор модуля
     * @return опциональный объект викторины
     */
    Optional<Quiz> findByModuleId(Long moduleId);
}
