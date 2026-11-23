package com.studying.platform.repository;

import com.studying.platform.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    /**
     * Находит викторину с предварительно загруженными вопросами и вариантами ответов.
     *
     * @param id идентификатор викторины
     * @return опциональный объект викторины с вопросами
     */
    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.questions qn LEFT JOIN FETCH qn.options WHERE q.id = :id")
    Optional<Quiz> findByIdWithQuestions(Long id);
}
