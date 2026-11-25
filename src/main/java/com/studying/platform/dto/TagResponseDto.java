package com.studying.platform.dto;

/**
 * DTO для ответа с данными тега.
 * <p>
 * Содержит идентификатор и название тега.
 * </p>
 *
 * @see com.studying.platform.entity.Tag
 */
public class TagResponseDto {

    /**
     * Идентификатор тега.
     */
    private Long id;

    /**
     * Название тега.
     */
    private String name;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public TagResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id   идентификатор тега
     * @param name название тега
     */
    public TagResponseDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Возвращает идентификатор тега.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор тега.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает название тега.
     *
     * @return название тега
     */
    public String getName() {
        return name;
    }

    /**
     * Устанавливает название тега.
     *
     * @param name название тега
     */
    public void setName(String name) {
        this.name = name;
    }
}
