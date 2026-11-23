package com.studying.platform.dto;

import java.time.LocalDateTime;

/**
 * DTO для ответа REST API с данными записи на курс.
 * <p>
 * Содержит идентификатор записи, идентификаторы студента и курса,
 * статус записи и дату записи.
 * Используется в телах успешных ответов GET-запросов на записи.
 * </p>
 */
public class EnrollmentResponseDto {

    /**
     * Идентификатор записи.
     */
    private Long id;

    /**
     * Идентификатор студента.
     */
    private Long studentId;

    /**
     * Идентификатор курса.
     */
    private Long courseId;

    /**
     * Статус записи (ACTIVE или DROPPED).
     */
    private String status;

    /**
     * Дата и время записи на курс.
     */
    private LocalDateTime enrollDate;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public EnrollmentResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id        идентификатор записи
     * @param studentId идентификатор студента
     * @param courseId  идентификатор курса
     * @param status    статус записи
     * @param enrollDate дата записи
     */
    public EnrollmentResponseDto(Long id, Long studentId, Long courseId,
                                 String status, LocalDateTime enrollDate) {
        this.id = id;
        this.studentId = studentId;
        this.courseId = courseId;
        this.status = status;
        this.enrollDate = enrollDate;
    }

    /**
     * Возвращает идентификатор записи.
     *
     * @return идентификатор записи
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор записи.
     *
     * @param id идентификатор записи
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
     * Возвращает статус записи.
     *
     * @return статус записи
     */
    public String getStatus() {
        return status;
    }

    /**
     * Устанавливает статус записи.
     *
     * @param status статус записи
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Возвращает дату и время записи на курс.
     *
     * @return дата и время записи
     */
    public LocalDateTime getEnrollDate() {
        return enrollDate;
    }

    /**
     * Устанавливает дату и время записи на курс.
     *
     * @param enrollDate дата и время записи
     */
    public void setEnrollDate(LocalDateTime enrollDate) {
        this.enrollDate = enrollDate;
    }
}
