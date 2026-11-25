package com.studying.platform.dto;

import java.time.LocalDateTime;

/**
 * DTO для ответа с данными отзыва о курсе.
 * <p>
 * Содержит идентификатор отзыва, идентификаторы студента и курса,
 * рейтинг, комментарий и дату создания.
 * </p>
 *
 * @see com.studying.platform.entity.CourseReview
 */
public class CourseReviewResponseDto {

    /**
     * Идентификатор отзыва.
     */
    private Long id;

    /**
     * Идентификатор студента, оставившего отзыв.
     */
    private Long studentId;

    /**
     * Идентификатор курса, о котором оставлен отзыв.
     */
    private Long courseId;

    /**
     * Рейтинг курса (1–5).
     */
    private Integer rating;

    /**
     * Текстовый комментарий отзыва.
     */
    private String comment;

    /**
     * Дата и время создания отзыва.
     */
    private LocalDateTime createdAt;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public CourseReviewResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id        идентификатор отзыва
     * @param studentId идентификатор студента
     * @param courseId  идентификатор курса
     * @param rating    рейтинг
     * @param comment   комментарий
     * @param createdAt дата создания
     */
    public CourseReviewResponseDto(Long id, Long studentId, Long courseId,
                                   Integer rating, String comment, LocalDateTime createdAt) {
        this.id = id;
        this.studentId = studentId;
        this.courseId = courseId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    /**
     * Возвращает идентификатор отзыва.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор отзыва.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
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

    /**
     * Возвращает дату и время создания отзыва.
     *
     * @return дата и время
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Устанавливает дату и время создания отзыва.
     *
     * @param createdAt дата и время
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
