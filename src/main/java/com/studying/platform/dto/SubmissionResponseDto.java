package com.studying.platform.dto;

import java.time.LocalDateTime;

/**
 * DTO для ответа REST API с данными решения студента.
 * <p>
 * Содержит идентификатор решения, идентификаторы задания и студента,
 * текст ответа, дату отправки, оценку и комментарий преподавателя.
 * Используется в телах успешных ответов GET-запросов на решения.
 * </p>
 */
public class SubmissionResponseDto {

    /**
     * Идентификатор решения.
     */
    private Long id;

    /**
     * Идентификатор задания.
     */
    private Long assignmentId;

    /**
     * Идентификатор студента, отправившего решение.
     */
    private Long studentId;

    /**
     * Текст ответа студента.
     */
    private String content;

    /**
     * Дата и время отправки решения.
     */
    private LocalDateTime submittedAt;

    /**
     * Оценка, выставленная преподавателем (может быть null).
     */
    private Integer score;

    /**
     * Комментарий преподавателя к решению (может быть null).
     */
    private String feedback;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public SubmissionResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id           идентификатор решения
     * @param assignmentId идентификатор задания
     * @param studentId    идентификатор студента
     * @param content      текст ответа
     * @param submittedAt  дата и время отправки
     * @param score        оценка
     * @param feedback     комментарий преподавателя
     */
    public SubmissionResponseDto(Long id, Long assignmentId, Long studentId,
                                 String content, LocalDateTime submittedAt,
                                 Integer score, String feedback) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.content = content;
        this.submittedAt = submittedAt;
        this.score = score;
        this.feedback = feedback;
    }

    /**
     * Возвращает идентификатор решения.
     *
     * @return идентификатор решения
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор решения.
     *
     * @param id идентификатор решения
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает идентификатор задания.
     *
     * @return идентификатор задания
     */
    public Long getAssignmentId() {
        return assignmentId;
    }

    /**
     * Устанавливает идентификатор задания.
     *
     * @param assignmentId идентификатор задания
     */
    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
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
     * Возвращает оценку, выставленную преподавателем.
     *
     * @return оценка (может быть null)
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
     * @return комментарий (может быть null)
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
