package com.studying.platform.dto;

import com.studying.platform.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO для создания нового пользователя через REST API.
 * <p>
 * Содержит обязательные поля: адрес электронной почты и роль пользователя.
 * Валидация выполняется с помощью Jakarta Bean Validation до момента
 * передачи данных в сервисный слой.
 * </p>
 */
public class UserCreateDto {

    /**
     * Адрес электронной почты пользователя.
     */
    @NotBlank(message = "Адрес электронной почты не может быть пустым")
    @Email(message = "Адрес электронной почты должен быть корректным")
    @Size(max = 50, message = "Адрес электронной почты не должен превышать 50 символов")
    private String email;

    /**
     * Роль пользователя в системе.
     */
    @NotNull(message = "Роль пользователя не может быть пустой")
    private UserRole role;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public UserCreateDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param email адрес электронной почты
     * @param role  роль пользователя
     */
    public UserCreateDto(String email, UserRole role) {
        this.email = email;
        this.role = role;
    }

    /**
     * Возвращает адрес электронной почты.
     *
     * @return адрес электронной почты
     */
    public String getEmail() {
        return email;
    }

    /**
     * Устанавливает адрес электронной почты.
     *
     * @param email адрес электронной почты
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Возвращает роль пользователя.
     *
     * @return роль пользователя
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * Устанавливает роль пользователя.
     *
     * @param role роль пользователя
     */
    public void setRole(UserRole role) {
        this.role = role;
    }
}
