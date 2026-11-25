package com.studying.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для создания и обновления викторины.
 * <p>
 * Содержит идентификатор модуля, название викторины (обязательно, до 200 символов)
 * и опциональный лимит времени в минутах.
 * </p>
 *
 * @see com.studying.platform.entity.Quiz
 */
public class QuizRequestDto {

    /**
     * Идентификатор модуля, которому принадлежит викторина.
     */
    private Long moduleId;

    /**
     * Название викторины. Обязательно для заполнения, максимум 200 символов.
     */
    @NotBlank(message = "Название викторины не может быть пустым")
    @Size(max = 200, message = "Название викторины не должно превышать 200 символов")
    private String title;

    /**
     * Лимит времени на прохождение викторины в минутах. Может быть null (без ограничения).
     */
    private Integer timeLimit;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public QuizRequestDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param moduleId  идентификатор модуля
     * @param title     название викторины
     * @param timeLimit лимит времени в минутах
     */
    public QuizRequestDto(Long moduleId, String title, Integer timeLimit) {
        this.moduleId = moduleId;
        this.title = title;
        this.timeLimit = timeLimit;
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
}
