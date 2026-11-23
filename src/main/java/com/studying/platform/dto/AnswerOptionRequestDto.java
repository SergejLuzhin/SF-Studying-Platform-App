package com.studying.platform.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO для создания и обновления варианта ответа викторины.
 * <p>
 * Содержит текст варианта и признак правильности ответа.
 * </p>
 *
 * @see com.studying.platform.entity.AnswerOption
 */
public class AnswerOptionRequestDto {

    /**
     * Текст варианта ответа. Обязательно для заполнения.
     */
    @NotBlank(message = "Текст варианта ответа не может быть пустым")
    private String text;

    /**
     * Признак правильности варианта ответа.
     */
    private Boolean isCorrect;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public AnswerOptionRequestDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param text      текст варианта ответа
     * @param isCorrect признак правильности
     */
    public AnswerOptionRequestDto(String text, Boolean isCorrect) {
        this.text = text;
        this.isCorrect = isCorrect;
    }

    /**
     * Возвращает текст варианта ответа.
     *
     * @return текст варианта
     */
    public String getText() {
        return text;
    }

    /**
     * Устанавливает текст варианта ответа.
     *
     * @param text текст варианта
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Возвращает признак правильности варианта ответа.
     *
     * @return признак правильности
     */
    public Boolean getIsCorrect() {
        return isCorrect;
    }

    /**
     * Устанавливает признак правильности варианта ответа.
     *
     * @param isCorrect признак правильности
     */
    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
}
