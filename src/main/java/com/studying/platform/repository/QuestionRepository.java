package com.studying.platform.repository;

import com.studying.platform.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Репозиторий для сущности «Вопрос викторины» учебной платформы.
 * <p>
 * Обеспечивает стандартные CRUD-операции и поиск вопросов,
 * принадлежащих заданной викторине.
 * </p>
 *
 * @see Question
 */
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * Находит все вопросы, принадлежащие заданной викторине.
     *
     * @param quizId идентификатор викторины
     * @return список вопросов викторины
     */
    List<Question> findByQuizId(Long quizId);

    /**
     * Находит все вопросы викторины с предварительно загруженными вариантами ответов.
     *
     * @param quizId идентификатор викторины
     * @return список вопросов с вариантами ответов
     */
    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.options WHERE q.quiz.id = :quizId")
    List<Question> findByQuizIdWithAnswers(Long quizId);
}
