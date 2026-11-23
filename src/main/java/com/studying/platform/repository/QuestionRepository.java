package com.studying.platform.repository;

import com.studying.platform.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
