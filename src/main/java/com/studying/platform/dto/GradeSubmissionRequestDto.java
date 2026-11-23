package com.studying.platform.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO для выставления оценки и комментария к решению студента через REST API.
 * <p>
 * Содержит оценку и необязательный комментарий преподавателя.
 * Валидация выполняется с помощью Jakarta Bean Validation.
 * </p>
 */
public class GradeSubmissionRequestDto {

    /**
     * Оценка, выставляемая студенту. Обязательно для заполнения.
     */
    @NotNull(message = "Оценка обязательна")
    private Integer score;

    /**
     * Комментарий преподавателя к решению. Может быть пустым или null.
     */
    private String feedback;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public GradeSubmissionRequestDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param score    оценка
     * @param feedback комментарий преподавателя
     */
    public GradeSubmissionRequestDto(Integer score, String feedback) {
        this.score = score;
        this.feedback = feedback;
    }

    /**
     * Возвращает оценку.
     *
     * @return оценка
     */
    public Integer getScore() {
        return score;
    }

    /**
     * Устанавливает оценку.
     *
     * @param score оценка
     */
    public void setScore(Integer score) {
        this.score = score;
    }

    /**
     * Возвращает комментарий преподавателя.
     *
     * @return комментарий (может быть null)
     */
    public String getFeedback() {
        return feedback;
    }

    /**
     * Устанавливает комментарий преподавателя.
     *
     * @param feedback комментарий
     */
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
