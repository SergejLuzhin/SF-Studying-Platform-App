package com.studying.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Сущность «Решение» учебной платформы.
 * <p>
 * Представляет отправленное студентом решение учебного задания.
 * Содержит текст ответа, момент отправки и (опционально) оценку
 * с комментарием преподавателя. Связи с заданием и студентом —
 * «многие-к-одному», загружаются лениво.
 * На уровне базы данных защищено уникальным составным ограничением:
 * один студент — одно решение на задание.
 * </p>
 *
 * @see Assignment
 * @see User
 */
@Entity
@Table(name = "submissions", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(columnNames = {"assignment_id", "student_id"})
})
public class Submission {

    /**
     * Идентификатор решения. Генерируется автоматически базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Задание, на которое отправлено данное решение.
     * Владелец связи «многие-к-одному». Загружается лениво.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_submission_assignment"))
    private Assignment assignment;

    /**
     * Студент, отправивший данное решение.
     * Владелец связи «многие-к-одному». Загружается лениво.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_submission_student"))
    private User student;

    /**
     * Дата и время отправки решения. Обязательно для заполнения.
     */
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    /**
     * Текст ответа студента. Хранится в формате TEXT. Обязательно для заполнения.
     * Без {@code @Lob} — согласно требованиям проекта.
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Оценка, выставленная преподавателем. Необязательно — null до проверки.
     */
    @Column(name = "score")
    private Integer score;

    /**
     * Комментарий преподавателя к решению. Необязательно.
     */
    @Column
    private String feedback;

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public Submission() {
    }

    /**
     * Создаёт решение с заданными параметрами.
     *
     * @param assignment задание, на которое отправлено решение
     * @param student    студент, отправивший решение
     * @param content    текст ответа студента
     */
    public Submission(Assignment assignment, User student, String content) {
        this.assignment = assignment;
        this.student = student;
        this.content = content;
        this.submittedAt = LocalDateTime.now();
    }

    /**
     * Создаёт решение с заданными параметрами, включая оценку и комментарий.
     *
     * @param assignment задание, на которое отправлено решение
     * @param student    студент, отправивший решение
     * @param content    текст ответа студента
     * @param score      оценка, выставленная преподавателем
     * @param feedback   комментарий преподавателя
     */
    public Submission(Assignment assignment, User student, String content,
                      Integer score, String feedback) {
        this.assignment = assignment;
        this.student = student;
        this.content = content;
        this.submittedAt = LocalDateTime.now();
        this.score = score;
        this.feedback = feedback;
    }

    /**
     * Возвращает идентификатор решения.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор решения.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает задание, на которое отправлено данное решение.
     *
     * @return задание
     */
    public Assignment getAssignment() {
        return assignment;
    }

    /**
     * Устанавливает задание, на которое отправлено данное решение.
     *
     * @param assignment задание
     */
    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    /**
     * Возвращает студента, отправившего данное решение.
     *
     * @return студент
     */
    public User getStudent() {
        return student;
    }

    /**
     * Устанавливает студента, отправившего данное решение.
     *
     * @param student студент
     */
    public void setStudent(User student) {
        this.student = student;
    }

    /**
     * Возвращает дату и время отправки решения.
     *
     * @return дата и время отправки
     */
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    /**
     * Устанавливает дату и время отправки решения.
     *
     * @param submittedAt дата и время отправки
     */
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    /**
     * Возвращает текст ответа студента.
     *
     * @return текст ответа
     */
    public String getContent() {
        return content;
    }

    /**
     * Устанавливает текст ответа студента.
     *
     * @param content текст ответа
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Возвращает оценку, выставленную преподавателем.
     *
     * @return оценка (может быть {@code null})
     */
    public Integer getScore() {
        return score;
    }

    /**
     * Устанавливает оценку, выставленную преподавателем.
     *
     * @param score оценка
     */
    public void setScore(Integer score) {
        this.score = score;
    }

    /**
     * Возвращает комментарий преподавателя к решению.
     *
     * @return комментарий (может быть {@code null})
     */
    public String getFeedback() {
        return feedback;
    }

    /**
     * Устанавливает комментарий преподавателя к решению.
     *
     * @param feedback комментарий
     */
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
