package com.studying.platform.dto;

import java.time.LocalDate;

/**
 * DTO для ответа REST API с данными курса.
 * <p>
 * Содержит идентификатор, название, описание, продолжительность, дату начала,
 * идентификаторы преподавателя и категории, а также название категории.
 * Используется в телах успешных ответов GET-запросов.
 * </p>
 */
public class CourseResponseDto {

    /**
     * Идентификатор курса.
     */
    private Long id;

    /**
     * Название курса.
     */
    private String title;

    /**
     * Описание курса.
     */
    private String description;

    /**
     * Продолжительность курса в часах.
     */
    private Integer duration;

    /**
     * Дата начала курса.
     */
    private LocalDate startDate;

    /**
     * Идентификатор преподавателя.
     */
    private Long teacherId;

    /**
     * Идентификатор категории.
     */
    private Long categoryId;

    /**
     * Название категории курса.
     */
    private String categoryName;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public CourseResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id            идентификатор курса
     * @param title         название курса
     * @param description   описание курса
     * @param duration      продолжительность в часах
     * @param startDate     дата начала курса
     * @param teacherId     идентификатор преподавателя
     * @param categoryId    идентификатор категории
     * @param categoryName  название категории
     */
    public CourseResponseDto(Long id, String title, String description, Integer duration,
                             LocalDate startDate, Long teacherId, Long categoryId,
                             String categoryName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.duration = duration;
        this.startDate = startDate;
        this.teacherId = teacherId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    /**
     * Возвращает идентификатор курса.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор курса.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает название курса.
     *
     * @return название курса
     */
    public String getTitle() {
        return title;
    }

    /**
     * Устанавливает название курса.
     *
     * @param title название курса
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Возвращает описание курса.
     *
     * @return описание курса
     */
    public String getDescription() {
        return description;
    }

    /**
     * Устанавливает описание курса.
     *
     * @param description описание курса
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Возвращает продолжительность курса в часах.
     *
     * @return продолжительность
     */
    public Integer getDuration() {
        return duration;
    }

    /**
     * Устанавливает продолжительность курса в часах.
     *
     * @param duration продолжительность
     */
    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    /**
     * Возвращает дату начала курса.
     *
     * @return дата начала
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Устанавливает дату начала курса.
     *
     * @param startDate дата начала
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Возвращает идентификатор преподавателя.
     *
     * @return идентификатор преподавателя
     */
    public Long getTeacherId() {
        return teacherId;
    }

    /**
     * Устанавливает идентификатор преподавателя.
     *
     * @param teacherId идентификатор преподавателя
     */
    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    /**
     * Возвращает идентификатор категории.
     *
     * @return идентификатор категории
     */
    public Long getCategoryId() {
        return categoryId;
    }

    /**
     * Устанавливает идентификатор категории.
     *
     * @param categoryId идентификатор категории
     */
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * Возвращает название категории.
     *
     * @return название категории
     */
    public String getCategoryName() {
        return categoryName;
    }

    /**
     * Устанавливает название категории.
     *
     * @param categoryName название категории
     */
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
