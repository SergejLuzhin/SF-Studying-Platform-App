package com.studying.platform.dto;

import com.studying.platform.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO для обновления данных пользователя через REST API.
 * <p>
 * Для полного обновления обязательны корректный email длиной до 50 символов
 * и роль пользователя. Пароль не передаётся.
 * </p>
 */
public class UserUpdateDto {

    @NotBlank(message = "Адрес электронной почты не может быть пустым")
    @Email(message = "Адрес электронной почты должен быть корректным")
    @Size(max = 50, message = "Адрес электронной почты не должен превышать 50 символов")
    private String email;

    @NotNull(message = "Роль пользователя не может быть пустой")
    private UserRole role;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public UserUpdateDto() {
    }

    /**
     * Возвращает новый адрес электронной почты.
     *
     * @return адрес электронной почты
     */
    public String getEmail() {
        return email;
    }

    /**
     * Устанавливает новый адрес электронной почты.
     *
     * @param email адрес электронной почты
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Возвращает новую роль пользователя.
     *
     * @return роль пользователя
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * Устанавливает новую роль пользователя.
     *
     * @param role роль пользователя
     */
    public void setRole(UserRole role) {
        this.role = role;
    }
}
