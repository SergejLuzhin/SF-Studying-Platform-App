package com.studying.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO для создания или обновления курса через REST API.
 * <p>
 * Содержит обязательные поля: название, продолжительность, дата начала,
 * идентификатор преподавателя и идентификатор категории.
 * Валидация выполняется с помощью Jakarta Bean Validation до момента
 * передачи данных в сервисный слой.
 * </p>
 */
public class CourseRequestDto {

    /**
     * Название курса. Обязательно для заполнения.
     */
    @NotBlank(message = "Название курса не может быть пустым")
    @Size(max = 200, message = "Название курса не должно превышать 200 символов")
    private String title;

    /**
     * Подробное описание курса. Может быть пустым или null.
     */
    private String description;

    /**
     * Продолжительность курса в часах. Должна быть не менее 1.
     */
    @NotNull(message = "Продолжительность курса не может быть пустой")
    private Integer duration;

    /**
     * Дата начала курса. Обязательно для заполнения.
     */
    @NotNull(message = "Дата начала курса не может быть пустой")
    private LocalDate startDate;

    /**
     * Идентификатор преподавателя, ведущего курс.
     */
    @NotNull(message = "Идентификатор преподавателя не может быть пустым")
    private Long teacherId;

    /**
     * Идентификатор категории курса.
     */
    @NotNull(message = "Идентификатор категории не может быть пустым")
    private Long categoryId;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public CourseRequestDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param title       название курса
     * @param description описание курса
     * @param duration    продолжительность в часах
     * @param startDate   дата начала курса
     * @param teacherId   идентификатор преподавателя
     * @param categoryId  идентификатор категории
     */
    public CourseRequestDto(String title, String description, Integer duration,
                            LocalDate startDate, Long teacherId, Long categoryId) {
        this.title = title;
        this.description = description;
        this.duration = duration;
        this.startDate = startDate;
        this.teacherId = teacherId;
        this.categoryId = categoryId;
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
}
