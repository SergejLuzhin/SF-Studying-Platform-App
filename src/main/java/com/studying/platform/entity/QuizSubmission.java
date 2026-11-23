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
 * Сущность «Результат прохождения теста» учебной платформы.
 * <p>
 * Хранит итоговый балл студента за прохождение конкретной викторины.
 * Связь с викториной и студентом — «многие-к-одному».
 * Каскадное удаление результатов при удалении викторины или студента
 * не настраивается (результаты сохраняются).
 * </p>
 *
 * @see Quiz
 * @see User
 */
@Entity
@Table(name = "quizsubmission")
public class QuizSubmission {

    /**
     * Идентификатор результата прохождения теста. Генерируется автоматически базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Викторина, по которой был получен результат. Владелец связи «многие-к-одному».
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_quizsubmission_quiz"))
    private Quiz quiz;

    /**
     * Студент, прошедший викторину. Владелец связи «многие-к-одному».
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_quizsubmission_student"))
    private User student;

    /**
     * Итоговый балл студента за данный тест. Обязательно для заполнения.
     * Вычисляется на основе точного совпадения выбранных вариантов ответов с правильными.
     */
    @Column(nullable = false)
    private Integer score;

    /**
     * Дата и время прохождения теста. Обязательно для заполнения.
     */
    @Column(name = "taken_at", nullable = false)
    private LocalDateTime takenAt;

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public QuizSubmission() {
    }

    /**
     * Создаёт результат прохождения теста с заданными параметрами.
     *
     * @param quiz    викторина, по которой получен результат
     * @param student студент, прошедший викторину
     * @param score   итоговый балл
     * @param takenAt дата и время прохождения
     */
    public QuizSubmission(Quiz quiz, User student, Integer score, LocalDateTime takenAt) {
        this.quiz = quiz;
        this.student = student;
        this.score = score;
        this.takenAt = takenAt;
    }

    /**
     * Возвращает идентификатор результата прохождения теста.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор результата прохождения теста.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает викторину, по которой получен результат.
     *
     * @return викторина
     */
    public Quiz getQuiz() {
        return quiz;
    }

    /**
     * Устанавливает викторину, по которой получен результат.
     *
     * @param quiz викторина
     */
    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    /**
     * Возвращает студента, прошедшего викторину.
     *
     * @return студент
     */
    public User getStudent() {
        return student;
    }

    /**
     * Устанавливает студента, прошедшего викторину.
     *
     * @param student студент
     */
    public void setStudent(User student) {
        this.student = student;
    }

    /**
     * Возвращает итоговый балл за тест.
     *
     * @return балл
     */
    public Integer getScore() {
        return score;
    }

    /**
     * Устанавливает итоговый балл за тест.
     *
     * @param score балл
     */
    public void setScore(Integer score) {
        this.score = score;
    }

    /**
     * Возвращает дату и время прохождения теста.
     *
     * @return дата и время
     */
    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    /**
     * Устанавливает дату и время прохождения теста.
     *
     * @param takenAt дата и время
     */
    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }
}
