package com.studying.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO для создания отзыва о курсе.
 * <p>
 * Содержит идентификаторы студента и курса, рейтинг (1–5) и опциональный комментарий.
 * </p>
 *
 * @see com.studying.platform.entity.CourseReview
 */
public class CourseReviewRequestDto {

    /**
     * Идентификатор студента, оставляющего отзыв.
     */
    @NotNull(message = "Идентификатор студента не может быть null")
    private Long studentId;

    /**
     * Идентификатор курса, о котором оставлен отзыв.
     */
    @NotNull(message = "Идентификатор курса не может быть null")
    private Long courseId;

    /**
     * Рейтинг курса от 1 до 5. Обязательно для заполнения.
     */
    @NotNull(message = "Рейтинг не может быть null")
    @Min(value = 1, message = "Рейтинг должен быть от 1 до 5")
    @Max(value = 5, message = "Рейтинг должен быть от 1 до 5")
    private Integer rating;

    /**
     * Текстовый комментарий отзыва. Может быть null.
     */
    private String comment;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public CourseReviewRequestDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param studentId идентификатор студента
     * @param courseId  идентификатор курса
     * @param rating    рейтинг курса (1–5)
     * @param comment   текстовый комментарий
     */
    public CourseReviewRequestDto(Long studentId, Long courseId, Integer rating, String comment) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.rating = rating;
        this.comment = comment;
    }

    /**
     * Возвращает идентификатор студента.
     *
     * @return идентификатор студента
     */
    public Long getStudentId() {
        return studentId;
    }

    /**
     * Устанавливает идентификатор студента.
     *
     * @param studentId идентификатор студента
     */
    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    /**
     * Возвращает идентификатор курса.
     *
     * @return идентификатор курса
     */
    public Long getCourseId() {
        return courseId;
    }

    /**
     * Устанавливает идентификатор курса.
     *
     * @param courseId идентификатор курса
     */
    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    /**
     * Возвращает рейтинг курса.
     *
     * @return рейтинг (1–5)
     */
    public Integer getRating() {
        return rating;
    }

    /**
     * Устанавливает рейтинг курса.
     *
     * @param rating рейтинг (1–5)
     */
    public void setRating(Integer rating) {
        this.rating = rating;
    }

    /**
     * Возвращает текстовый комментарий отзыва.
     *
     * @return комментарий
     */
    public String getComment() {
        return comment;
    }

    /**
     * Устанавливает текстовый комментарий отзыва.
     *
     * @param comment комментарий
     */
    public void setComment(String comment) {
        this.comment = comment;
    }
}
