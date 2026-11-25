package com.studying.platform.dto;

import com.studying.platform.entity.QuizQuestionType;

/**
 * DTO для ответа с данными вопроса викторины.
 * <p>
 * Содержит идентификатор вопроса, идентификатор викторины,
 * текст вопроса, тип и количество вариантов ответа.
 * </p>
 *
 * @see com.studying.platform.entity.Question
 */
public class QuestionResponseDto {

    /**
     * Идентификатор вопроса.
     */
    private Long id;

    /**
     * Идентификатор викторины, которой принадлежит вопрос.
     */
    private Long quizId;

    /**
     * Текст вопроса.
     */
    private String text;

    /**
     * Тип вопроса.
     */
    private QuizQuestionType type;

    /**
     * Количество вариантов ответа на вопрос.
     */
    private Integer optionCount;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public QuestionResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id          идентификатор вопроса
     * @param quizId      идентификатор викторины
     * @param text        текст вопроса
     * @param type        тип вопроса
     * @param optionCount количество вариантов ответа
     */
    public QuestionResponseDto(Long id, Long quizId, String text, QuizQuestionType type, Integer optionCount) {
        this.id = id;
        this.quizId = quizId;
        this.text = text;
        this.type = type;
        this.optionCount = optionCount;
    }

    /**
     * Возвращает идентификатор вопроса.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор вопроса.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает идентификатор викторины.
     *
     * @return идентификатор викторины
     */
    public Long getQuizId() {
        return quizId;
    }

    /**
     * Устанавливает идентификатор викторины.
     *
     * @param quizId идентификатор викторины
     */
    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    /**
     * Возвращает текст вопроса.
     *
     * @return текст вопроса
     */
    public String getText() {
        return text;
    }

    /**
     * Устанавливает текст вопроса.
     *
     * @param text текст вопроса
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Возвращает тип вопроса.
     *
     * @return тип вопроса
     */
    public QuizQuestionType getType() {
        return type;
    }

    /**
     * Устанавливает тип вопроса.
     *
     * @param type тип вопроса
     */
    public void setType(QuizQuestionType type) {
        this.type = type;
    }

    /**
     * Возвращает количество вариантов ответа.
     *
     * @return количество вариантов ответа
     */
    public Integer getOptionCount() {
        return optionCount;
    }

    /**
     * Устанавливает количество вариантов ответа.
     *
     * @param optionCount количество вариантов ответа
     */
    public void setOptionCount(Integer optionCount) {
        this.optionCount = optionCount;
    }
}
