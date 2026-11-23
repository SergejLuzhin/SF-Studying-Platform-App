package com.studying.platform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO для отправки решения студента на учебное задание через REST API.
 * <p>
 * Содержит идентификатор задания, идентификатор студента и текст ответа.
 * Валидация выполняется с помощью Jakarta Bean Validation до момента
 * передачи данных в сервисный слой.
 * </p>
 */
public class SubmissionRequestDto {

    /**
     * Идентификатор задания, на которое отправляется решение.
     */
    @NotNull(message = "Идентификатор задания не может быть пустым")
    private Long assignmentId;

    /**
     * Идентификатор студента, отправляющего решение.
     */
    @NotNull(message = "Идентификатор студента не может быть пустым")
    private Long studentId;

    /**
     * Текст ответа студента. Обязательно для заполнения.
     */
    @NotNull(message = "Текст ответа не может быть пустым")
    @Size(max = 50000, message = "Текст ответа не должен превышать 50000 символов")
    private String content;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public SubmissionRequestDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param assignmentId идентификатор задания
     * @param studentId    идентификатор студента
     * @param content      текст ответа студента
     */
    public SubmissionRequestDto(Long assignmentId, Long studentId, String content) {
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.content = content;
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
}
