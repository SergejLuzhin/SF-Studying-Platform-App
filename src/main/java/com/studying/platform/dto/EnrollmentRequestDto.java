package com.studying.platform.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO для создания записи на курс через REST API.
 * <p>
 * Содержит идентификаторы студента и курса.
 * Валидация выполняется с помощью Jakarta Bean Validation до момента
 * передачи данных в сервисный слой.
 * </p>
 */
public class EnrollmentRequestDto {

    /**
     * Идентификатор студента, записывающегося на курс.
     */
    @NotNull(message = "Идентификатор студента не может быть пустым")
    private Long studentId;

    /**
     * Идентификатор курса, на который записывается студент.
     */
    @NotNull(message = "Идентификатор курса не может быть пустым")
    private Long courseId;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public EnrollmentRequestDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param studentId идентификатор студента
     * @param courseId  идентификатор курса
     */
    public EnrollmentRequestDto(Long studentId, Long courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
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
}
