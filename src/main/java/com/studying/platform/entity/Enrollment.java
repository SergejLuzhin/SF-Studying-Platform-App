package com.studying.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Сущность «Запись на курс» учебной платформы.
 * <p>
 * Фиксирует факт записи студента на курс с указанием даты и статуса.
 * Связь с курсом и пользователем — «многие-к-одному».
 * Ограничение уникальности: один студент может быть записан на курс один раз.
 * </p>
 *
 * @see User
 * @see Course
 */
@Entity
@Table(name = "enrollments", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(columnNames = {"user_id", "course_id"})
})
public class Enrollment {

    /**
     * Идентификатор записи. Генерируется автоматически базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Пользователь (студент), записанный на курс.
     */
    @jakarta.persistence.ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_enrollment_user"))
    private User user;

    /**
     * Курс, на который записан студент.
     */
    @jakarta.persistence.ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_enrollment_course"))
    private Course course;

    /**
     * Дата записи на курс. Обязательно для заполнения.
     */
    @Column(nullable = false)
    private LocalDateTime enrollDate;

    /**
     * Статус записи: ACTIVE — активно записан, DROPPED — отписался.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public Enrollment() {
    }

    /**
     * Создаёт запись студента на курс.
     *
     * @param user       студент
     * @param course     курс
     * @param enrollDate дата записи
     * @param status     статус записи
     */
    public Enrollment(User user, Course course, LocalDateTime enrollDate, EnrollmentStatus status) {
        this.user = user;
        this.course = course;
        this.enrollDate = enrollDate;
        this.status = status;
    }

    /**
     * Возвращает идентификатор записи.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор записи.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает пользователя (студента).
     *
     * @return пользователь
     */
    public User getUser() {
        return user;
    }

    /**
     * Устанавливает пользователя (студента).
     *
     * @param user пользователь
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Возвращает курс.
     *
     * @return курс
     */
    public Course getCourse() {
        return course;
    }

    /**
     * Устанавливает курс.
     *
     * @param course курс
     */
    public void setCourse(Course course) {
        this.course = course;
    }

    /**
     * Возвращает дату записи на курс.
     *
     * @return дата записи
     */
    public LocalDateTime getEnrollDate() {
        return enrollDate;
    }

    /**
     * Устанавливает дату записи на курс.
     *
     * @param enrollDate дата записи
     */
    public void setEnrollDate(LocalDateTime enrollDate) {
        this.enrollDate = enrollDate;
    }

    /**
     * Возвращает статус записи.
     *
     * @return статус записи
     */
    public EnrollmentStatus getStatus() {
        return status;
    }

    /**
     * Устанавливает статус записи.
     *
     * @param status статус записи
     */
    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }
}
