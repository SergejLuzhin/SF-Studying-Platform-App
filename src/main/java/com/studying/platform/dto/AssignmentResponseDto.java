package com.studying.platform.dto;

import java.time.LocalDate;

/**
 * DTO для ответа REST API с данными задания.
 * <p>
 * Содержит идентификатор задания, идентификатор урока, название,
 * описание, срок выполнения и максимальный балл.
 * Используется в телах успешных ответов GET-запросов на задания.
 * </p>
 */
public class AssignmentResponseDto {

    /**
     * Идентификатор задания.
     */
    private Long id;

    /**
     * Идентификатор урока, которому принадлежит задание.
     */
    private Long lessonId;

    /**
     * Название задания.
     */
    private String title;

    /**
     * Подробное описание задания.
     */
    private String description;

    /**
     * Дата окончания выполнения задания.
     */
    private LocalDate dueDate;

    /**
     * Максимальный балл за выполнение задания.
     */
    private Integer maxScore;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public AssignmentResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id          идентификатор задания
     * @param lessonId    идентификатор урока
     * @param title       название задания
     * @param description описание задания
     * @param dueDate     срок выполнения
     * @param maxScore    максимальный балл
     */
    public AssignmentResponseDto(Long id, Long lessonId, String title,
                                 String description, LocalDate dueDate,
                                 Integer maxScore) {
        this.id = id;
        this.lessonId = lessonId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.maxScore = maxScore;
    }

    /**
     * Возвращает идентификатор задания.
     *
     * @return идентификатор задания
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор задания.
     *
     * @param id идентификатор задания
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает идентификатор урока.
     *
     * @return идентификатор урока
     */
    public Long getLessonId() {
        return lessonId;
    }

    /**
     * Устанавливает идентификатор урока.
     *
     * @param lessonId идентификатор урока
     */
    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    /**
     * Возвращает название задания.
     *
     * @return название задания
     */
    public String getTitle() {
        return title;
    }

    /**
     * Устанавливает название задания.
     *
     * @param title название задания
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Возвращает описание задания.
     *
     * @return описание задания
     */
    public String getDescription() {
        return description;
    }

    /**
     * Устанавливает описание задания.
     *
     * @param description описание задания
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Возвращает дату окончания выполнения задания.
     *
     * @return дата окончания
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Устанавливает дату окончания выполнения задания.
     *
     * @param dueDate дата окончания
     */
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Возвращает максимальный балл за задание.
     *
     * @return максимальный балл
     */
    public Integer getMaxScore() {
        return maxScore;
    }

    /**
     * Устанавливает максимальный балл за задание.
     *
     * @param maxScore максимальный балл
     */
    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }
}
