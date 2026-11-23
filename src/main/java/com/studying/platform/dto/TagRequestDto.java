package com.studying.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для создания тега.
 * <p>
 * Содержит название тега, которое является уникальным и обязательным для заполнения.
 * Максимальная длина — 50 символов.
 * </p>
 *
 * @see com.studying.platform.entity.Tag
 */
public class TagRequestDto {

    /**
     * Название тега. Обязательно для заполнения, максимум 50 символов.
     */
    @NotBlank(message = "Название тега не может быть пустым")
    @Size(max = 50, message = "Название тега не должно превышать 50 символов")
    private String name;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public TagRequestDto() {
    }

    /**
     * Создаёт DTO с заданным названием тега.
     *
     * @param name название тега
     */
    public TagRequestDto(String name) {
        this.name = name;
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
