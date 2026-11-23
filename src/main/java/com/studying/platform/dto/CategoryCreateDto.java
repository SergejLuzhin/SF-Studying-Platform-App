package com.studying.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для создания новой категории курса через REST API.
 * <p>
 * Содержит обязательное поле «название» категории.
 * Валидация выполняется с помощью Jakarta Bean Validation до момента
 * передачи данных в сервисный слой.
 * </p>
 */
public class CategoryCreateDto {

    /**
     * Название категории курса.
     */
    @NotBlank(message = "Название категории не может быть пустым")
    @Size(max = 100, message = "Название категории не должно превышать 100 символов")
    private String name;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public CategoryCreateDto() {
    }

    /**
     * Создаёт DTO с заданным названием категории.
     *
     * @param name название категории
     */
    public CategoryCreateDto(String name) {
        this.name = name;
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
