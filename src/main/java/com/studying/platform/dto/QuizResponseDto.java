package com.studying.platform.dto;

/**
 * DTO для ответа с данными викторины.
 * <p>
 * Содержит идентификатор викторины, идентификатор модуля, название,
 * лимит времени и количество вопросов.
 * </p>
 *
 * @see com.studying.platform.entity.Quiz
 */
public class QuizResponseDto {

    /**
     * Идентификатор викторины.
     */
    private Long id;

    /**
     * Идентификатор модуля, которому принадлежит викторина.
     */
    private Long moduleId;

    /**
     * Название викторины.
     */
    private String title;

    /**
     * Лимит времени на прохождение викторины в минутах.
     */
    private Integer timeLimit;

    /**
     * Количество вопросов в викторине.
     */
    private Integer questionCount;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public QuizResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id            идентификатор викторины
     * @param moduleId      идентификатор модуля
     * @param title         название викторины
     * @param timeLimit     лимит времени в минутах
     * @param questionCount количество вопросов
     */
    public QuizResponseDto(Long id, Long moduleId, String title, Integer timeLimit, Integer questionCount) {
        this.id = id;
        this.moduleId = moduleId;
        this.title = title;
        this.timeLimit = timeLimit;
        this.questionCount = questionCount;
    }

    /**
     * Возвращает идентификатор викторины.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор викторины.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает идентификатор модуля.
     *
     * @return идентификатор модуля
     */
    public Long getModuleId() {
        return moduleId;
    }

    /**
     * Устанавливает идентификатор модуля.
     *
     * @param moduleId идентификатор модуля
     */
    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
    }

    /**
     * Возвращает название викторины.
     *
     * @return название викторины
     */
    public String getTitle() {
        return title;
    }

    /**
     * Устанавливает название викторины.
     *
     * @param title название викторины
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Возвращает лимит времени на прохождение викторины в минутах.
     *
     * @return лимит времени в минутах
     */
    public Integer getTimeLimit() {
        return timeLimit;
    }

    /**
     * Устанавливает лимит времени на прохождение викторины в минутах.
     *
     * @param timeLimit лимит времени в минутах
     */
    public void setTimeLimit(Integer timeLimit) {
        this.timeLimit = timeLimit;
    }

    /**
     * Возвращает количество вопросов викторины.
     *
     * @return количество вопросов
     */
    public Integer getQuestionCount() {
        return questionCount;
    }

    /**
     * Устанавливает количество вопросов викторины.
     *
     * @param questionCount количество вопросов
     */
    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }
}
