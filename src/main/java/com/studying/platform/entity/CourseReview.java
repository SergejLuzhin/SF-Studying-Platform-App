package com.studying.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Сущность «Отзыв о курсе» учебной платформы.
 * <p>
 * Позволяет студенту оставить оценку и текстовый комментарий о курсе.
 * Один отзыв от одного студента на один курс (ограничение уникальности).
 * Связь с курсом и пользователем — «многие-к-одному».
 * </p>
 *
 * @see User
 * @see Course
 */
@Entity
@Table(name = "course_reviews", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(columnNames = {"student_id", "course_id"})
})
public class CourseReview {

    /**
     * Идентификатор отзыва. Генерируется автоматически базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Рейтинг курса от 1 до 5. Обязательно для заполнения.
     */
    @Column(nullable = false)
    private Integer rating;

    /**
     * Текстовый комментарий отзыва. Хранится в формате TEXT.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String comment;

    /**
     * Дата и время создания отзыва. Обязательно для заполнения.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Курс, о котором оставлен отзыв.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_review_course"))
    private Course course;

    /**
     * Студент, оставивший отзыв.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_review_student"))
    private User student;

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public CourseReview() {
    }

    /**
     * Создаёт отзыв о курсе.
     *
     * @param rating    рейтинг от 1 до 5
     * @param comment   текстовый комментарий
     * @param createdAt дата и время создания
     * @param course    курс
     * @param student   студент
     */
    public CourseReview(Integer rating, String comment, LocalDateTime createdAt,
                        Course course, User student) {
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.course = course;
        this.student = student;
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

    /**
     * Возвращает курс, о котором оставлен отзыв.
     *
     * @return курс
     */
    public Course getCourse() {
        return course;
    }

    /**
     * Устанавливает курс, о котором оставлен отзыв.
     *
     * @param course курс
     */
    public void setCourse(Course course) {
        this.course = course;
    }

    /**
     * Возвращает студента, оставившего отзыв.
     *
     * @return студент
     */
    public User getStudent() {
        return student;
    }

    /**
     * Устанавливает студента, оставившего отзыв.
     *
     * @param student студент
     */
    public void setStudent(User student) {
        this.student = student;
    }
}
