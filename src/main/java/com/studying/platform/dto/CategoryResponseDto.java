package com.studying.platform.dto;

/**
 * DTO для ответа REST API с данными категории.
 * <p>
 * Содержит идентификатор и название категории.
 * Используется в телах успешных ответов GET-запросов.
 * </p>
 */
public class CategoryResponseDto {

    /**
     * Идентификатор категории.
     */
    private Long id;

    /**
     * Название категории.
     */
    private String name;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public CategoryResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id   идентификатор категории
     * @param name название категории
     */
    public CategoryResponseDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Возвращает идентификатор категории.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор категории.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает название категории.
     *
     * @return название категории
     */
    public String getName() {
        return name;
    }

    /**
     * Устанавливает название категории.
     *
     * @param name название категории
     */
    public void setName(String name) {
        this.name = name;
    }
}
