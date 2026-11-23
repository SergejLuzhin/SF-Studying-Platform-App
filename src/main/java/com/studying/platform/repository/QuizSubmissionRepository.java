package com.studying.platform.repository;

import com.studying.platform.entity.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий для сущности «Результат прохождения теста» учебной платформы.
 * <p>
 * Обеспечивает стандартные CRUD-операции и выборку результатов
 * по студенту и викторине.
 * </p>
 *
 * @see QuizSubmission
 */
public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {

    /**
     * Находит все результаты прохождения викторин для заданного студента.
     *
     * @param studentId идентификатор студента
     * @return список результатов студента
     */
    List<QuizSubmission> findByStudentId(Long studentId);

    /**
     * Находит все результаты прохождения для заданной викторины.
     *
     * @param quizId идентификатор викторины
     * @return список результатов по викторине
     */
    List<QuizSubmission> findByQuizId(Long quizId);
}
