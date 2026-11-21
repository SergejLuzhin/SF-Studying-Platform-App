package com.studying.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

import com.studying.platform.entity.Enrollment;

/**
 * Сущность «Пользователь» учебной платформы.
 * <p>
 * Представляет участника системы: студента, преподавателя или администратора.
 * Содержит уникальный адрес электронной почты (физически хранится в колонке
 * {@code users.login} для обратной совместимости), роль и необязательный профиль.
 * Связан с профилем через отношение «один-к-одному», которое загружается лениво.
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
     * Адрес электронной почты пользователя. Уникальный и обязательный для заполнения.
     * Физическое имя колонки в базе данных — {@code login} (для сохранения
     * обратной совместимости с существующей схемой).
     */
    @NotBlank(message = "Адрес электронной почты не может быть пустым")
    @Email(message = "Адрес электронной почты должен быть корректным")
    @Size(max = 50, message = "Адрес электронной почты не должен превышать 50 символов")
    @Column(name = "login", nullable = false, unique = true, length = 50)
    private String email;

    /**
     * Роль пользователя в системе.
     * Может принимать значения: STUDENT, TEACHER, ADMIN.
     */
    @NotNull(message = "Роль пользователя не может быть null")
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
     * Записи пользователя на курсы. Загружаются лениво.
     * Владелец обратной связи — сущность {@link Enrollment}.
     */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<Enrollment> enrollments = new HashSet<>();

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public User() {
    }

    /**
     * Создаёт нового пользователя с заданными параметрами.
     *
     * @param email уникальный адрес электронной почты пользователя
     * @param role  роль пользователя
     */
    public User(String email, UserRole role) {
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
     * Возвращает адрес электронной почты пользователя.
     *
     * @return адрес электронной почты
     */
    public String getEmail() {
        return email;
    }

    /**
     * Устанавливает адрес электронной почты пользователя.
     *
     * @param email адрес электронной почты
     */
    public void setEmail(String email) {
        this.email = email;
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

    /**
     * Возвращает коллекцию записей пользователя на курсы.
     *
     * @return коллекция записей
     */
    public Set<Enrollment> getEnrollments() {
        return enrollments;
    }

    /**
     * Устанавливает коллекцию записей пользователя на курсы.
     *
     * @param enrollments коллекция записей
     */
    public void setEnrollments(Set<Enrollment> enrollments) {
        this.enrollments = enrollments;
    }
}
