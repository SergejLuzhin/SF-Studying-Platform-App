package com.studying.platform.dto;

/**
 * DTO для ответа с данными варианта ответа викторины.
 * <p>
 * Содержит идентификатор варианта, идентификатор вопроса,
 * текст варианта и признак правильности.
 * </p>
 *
 * @see com.studying.platform.entity.AnswerOption
 */
public class AnswerOptionResponseDto {

    /**
     * Идентификатор варианта ответа.
     */
    private Long id;

    /**
     * Идентификатор вопроса, которому принадлежит вариант.
     */
    private Long questionId;

    /**
     * Текст варианта ответа.
     */
    private String text;

    /**
     * Признак правильности варианта ответа.
     */
    private Boolean isCorrect;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public AnswerOptionResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id          идентификатор варианта
     * @param questionId  идентификатор вопроса
     * @param text        текст варианта
     * @param isCorrect   признак правильности
     */
    public AnswerOptionResponseDto(Long id, Long questionId, String text, Boolean isCorrect) {
        this.id = id;
        this.questionId = questionId;
        this.text = text;
        this.isCorrect = isCorrect;
    }

    /**
     * Возвращает идентификатор варианта ответа.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор варианта ответа.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает идентификатор вопроса.
     *
     * @return идентификатор вопроса
     */
    public Long getQuestionId() {
        return questionId;
    }

    /**
     * Устанавливает идентификатор вопроса.
     *
     * @param questionId идентификатор вопроса
     */
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
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
