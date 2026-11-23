package com.studying.platform.repository;

import com.studying.platform.entity.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий для сущности «Вариант ответа» викторины учебной платформы.
 * <p>
 * Обеспечивает стандартные CRUD-операции и поиск вариантов ответа,
 * относящихся к заданному вопросу.
 * </p>
 *
 * @see AnswerOption
 */
public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {

    /**
     * Находит все варианты ответа, относящиеся к заданному вопросу.
     *
     * @param questionId идентификатор вопроса
     * @return список вариантов ответа
     */
    List<AnswerOption> findByQuestionId(Long questionId);
}
