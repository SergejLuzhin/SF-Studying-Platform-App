package com.studying.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO для создания или обновления модуля курса через REST API.
 * <p>
 * Содержит обязательные поля: название и порядковый номер модуля.
 * Валидация выполняется с помощью Jakarta Bean Validation до момента
 * передачи данных в сервисный слой.
 * </p>
 */
public class ModuleRequestDto {

    /**
     * Название модуля. Обязательно для заполнения.
     */
    @NotBlank(message = "Название модуля не может быть пустым")
    @Size(max = 200, message = "Название модуля не должно превышать 200 символов")
    private String title;

    /**
     * Описание модуля. Может быть пустым или null.
     */
    private String description;

    /**
     * Порядковый номер модуля в курсе. Обязательно для заполнения.
     */
    @NotNull(message = "Порядковый номер модуля не может быть пустым")
    private Integer orderIndex;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public ModuleRequestDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param title       название модуля
     * @param description описание модуля
     * @param orderIndex  порядковый номер
     */
    public ModuleRequestDto(String title, String description, Integer orderIndex) {
        this.title = title;
        this.description = description;
        this.orderIndex = orderIndex;
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
}
