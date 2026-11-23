package com.studying.platform.dto;

import com.studying.platform.entity.UserRole;

/**
 * DTO для ответа REST API с данными пользователя.
 * <p>
 * Содержит идентификатор, адрес электронной почты и роль пользователя.
 * Используется в телах успешных ответов GET-запросов.
 * </p>
 */
public class UserResponseDto {

    /**
     * Идентификатор пользователя.
     */
    private Long id;

    /**
     * Адрес электронной почты пользователя.
     */
    private String email;

    /**
     * Роль пользователя в системе.
     */
    private UserRole role;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public UserResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id    идентификатор пользователя
     * @param email адрес электронной почты
     * @param role  роль пользователя
     */
    public UserResponseDto(Long id, String email, UserRole role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    /**
     * Возвращает идентификатор пользователя.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор пользователя.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
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
