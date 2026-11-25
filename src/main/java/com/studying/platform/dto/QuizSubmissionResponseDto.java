package com.studying.platform.dto;

import java.time.LocalDateTime;

/**
 * DTO для ответа с результатом прохождения викторины.
 * <p>
 * Содержит идентификатор результата, идентификатор викторины,
 * идентификатор студента, итоговый балл и дату прохождения.
 * </p>
 *
 * @see com.studying.platform.entity.QuizSubmission
 */
public class QuizSubmissionResponseDto {

    /**
     * Идентификатор результата прохождения теста.
     */
    private Long id;

    /**
     * Идентификатор викторины.
     */
    private Long quizId;

    /**
     * Идентификатор студента, прошедшего викторину.
     */
    private Long studentId;

    /**
     * Итоговый балл за тест (0–100).
     */
    private Integer score;

    /**
     * Дата и время прохождения теста.
     */
    private LocalDateTime takenAt;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public QuizSubmissionResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id        идентификатор результата
     * @param quizId    идентификатор викторины
     * @param studentId идентификатор студента
     * @param score     итоговый балл
     * @param takenAt   дата и время прохождения
     */
    public QuizSubmissionResponseDto(Long id, Long quizId, Long studentId, Integer score, LocalDateTime takenAt) {
        this.id = id;
        this.quizId = quizId;
        this.studentId = studentId;
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
     * Возвращает идентификатор викторины.
     *
     * @return идентификатор викторины
     */
    public Long getQuizId() {
        return quizId;
    }

    /**
     * Устанавливает идентификатор викторины.
     *
     * @param quizId идентификатор викторины
     */
    public void setQuizId(Long quizId) {
        this.quizId = quizId;
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
