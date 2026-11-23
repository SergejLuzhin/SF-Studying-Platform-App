package com.studying.platform.dto;

import com.studying.platform.entity.QuizQuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO для создания и обновления вопроса викторины.
 * <p>
 * Содержит текст вопроса (обязательно) и тип вопроса
 * (SINGLE_CHOICE или MULTIPLE_CHOICE).
 * </p>
 *
 * @see com.studying.platform.entity.Question
 */
public class QuestionRequestDto {

    /**
     * Текст вопроса. Обязательно для заполнения.
     */
    @NotBlank(message = "Текст вопроса не может быть пустым")
    private String text;

    /**
     * Тип вопроса. Обязательно для заполнения.
     */
    @NotNull(message = "Тип вопроса не может быть null")
    private QuizQuestionType type;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public QuestionRequestDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param text текст вопроса
     * @param type тип вопроса
     */
    public QuestionRequestDto(String text, QuizQuestionType type) {
        this.text = text;
        this.type = type;
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
}
