package com.studying.platform.dto;

/**
 * DTO для ответа REST API с данными модуля курса.
 * <p>
 * Содержит идентификатор, название, описание, порядковый номер и идентификатор курса.
 * Используется в телах успешных ответов GET-запросов.
 * </p>
 */
public class ModuleResponseDto {

    /**
     * Идентификатор модуля.
     */
    private Long id;

    /**
     * Название модуля.
     */
    private String title;

    /**
     * Описание модуля.
     */
    private String description;

    /**
     * Порядковый номер модуля в курсе.
     */
    private Integer orderIndex;

    /**
     * Идентификатор курса, которому принадлежит модуль.
     */
    private Long courseId;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public ModuleResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id        идентификатор модуля
     * @param title     название модуля
     * @param description описание модуля
     * @param orderIndex порядковый номер
     * @param courseId  идентификатор курса
     */
    public ModuleResponseDto(Long id, String title, String description,
                             Integer orderIndex, Long courseId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.orderIndex = orderIndex;
        this.courseId = courseId;
    }

    /**
     * Возвращает идентификатор модуля.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор модуля.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает название модуля.
     *
     * @return название модуля
     */
    public String getTitle() {
        return title;
    }

    /**
     * Устанавливает название модуля.
     *
     * @param title название модуля
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Возвращает описание модуля.
     *
     * @return описание модуля
     */
    public String getDescription() {
        return description;
    }

    /**
     * Устанавливает описание модуля.
     *
     * @param description описание модуля
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Возвращает порядковый номер модуля.
     *
     * @return порядковый номер
     */
    public Integer getOrderIndex() {
        return orderIndex;
    }

    /**
     * Устанавливает порядковый номер модуля.
     *
     * @param orderIndex порядковый номер
     */
    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
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
