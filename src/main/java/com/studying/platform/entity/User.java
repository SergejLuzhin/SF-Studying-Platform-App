package com.studying.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Сущность «Пользователь» учебной платформы.
 * <p>
 * Представляет участника системы: студента, преподавателя или администратора.
 * Содержит уникальный логин, хеш пароля и роль. Связан с профилем
 * через отношение «один-к-одному», которое загружается лениво.
 * </p>
 */
@Entity
@Table(name = "users")
public class User {

    /**
     * Идентификатор пользователя. Генерируется автоматически базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальный логин (адрес электронной почты) пользователя.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String login;

    /**
     * Хеш пароля пользователя.
     * В текущей реализации сохраняется как строка без хеширования.
     */
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * Роль пользователя в системе.
     * Может принимать значения: STUDENT, TEACHER, ADMIN.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    /**
     * Профиль пользователя. Владелец связи — сущность {@link Profile}.
     * Загружается лениво для оптимизации производительности.
     */
    @OneToOne(mappedBy = "user")
    private Profile profile;

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public User() {
    }

    /**
     * Создаёт нового пользователя с заданными параметрами.
     *
     * @param login    уникальный логин пользователя
     * @param password хеш пароля
     * @param role     роль пользователя
     */
    public User(String login, String password, UserRole role) {
        this.login = login;
        this.password = password;
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
     * Возвращает уникальный логин пользователя.
     *
     * @return логин
     */
    public String getLogin() {
        return login;
    }

    /**
     * Устанавливает уникальный логин пользователя.
     *
     * @param login логин
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Возвращает хеш пароля пользователя.
     *
     * @return хеш пароля
     */
    public String getPassword() {
        return password;
    }

    /**
     * Устанавливает хеш пароля пользователя.
     *
     * @param password хеш пароля
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Возвращает роль пользователя.
     *
     * @return роль
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * Устанавливает роль пользователя.
     *
     * @param role роль
     */
    public void setRole(UserRole role) {
        this.role = role;
    }

    /**
     * Возвращает профиль пользователя.
     *
     * @return профиль (может быть {@code null})
     */
    public Profile getProfile() {
        return profile;
    }

    /**
     * Устанавливает профиль пользователя.
     *
     * @param profile профиль
     */
    public void setProfile(Profile profile) {
        this.profile = profile;
    }
}
