package com.studying.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Сущность «Профиль пользователя» учебной платформы.
 * <p>
 * Хранит дополнительную информацию о пользователе: отображаемое имя,
 * краткую биографию и ссылку на аватар. Связана с пользователем
 * отношением «один-к-одному»: каждая запись в {@code profiles}
 * принадлежит ровно одному пользователю.
 * </p>
 */
@Entity
@Table(name = "profiles")
public class Profile {

    /**
     * Идентификатор профиля. Генерируется автоматически базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Отображаемое имя пользователя. Обязательно для заполнения.
     */
    @Column(nullable = false, length = 100)
    private String displayName;

    /**
     * Краткая биография пользователя. Хранится в формате TEXT.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String bio;

    /**
     * Ссылка на изображение аватара пользователя.
     */
    @Column(length = 500)
    private String avatarUrl;

    /**
     * Ссылка на связанного пользователя. Владелец связи «один-к-одному».
     * Внешний ключ {@code user_id} имеет ограничение уникальности,
     * обеспечивая корректную связь 1:1.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false,
            foreignKey = @ForeignKey(name = "fk_profile_user"))
    private User user;

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public Profile() {
    }

    /**
     * Создаёт профиль, привязанный к указанному пользователю.
     *
     * @param user        связанный пользователь
     * @param displayName отображаемое имя
     */
    public Profile(User user, String displayName) {
        this.user = user;
        this.displayName = displayName;
    }

    /**
     * Возвращает идентификатор профиля.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор профиля.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает отображаемое имя профиля.
     *
     * @return отображаемое имя
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Устанавливает отображаемое имя профиля.
     *
     * @param displayName отображаемое имя
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Возвращает биографию профиля.
     *
     * @return биография (может быть {@code null})
     */
    public String getBio() {
        return bio;
    }

    /**
     * Устанавливает биографию профиля.
     *
     * @param bio биография
     */
    public void setBio(String bio) {
        this.bio = bio;
    }

    /**
     * Возвращает ссылку на аватар профиля.
     *
     * @return URL аватара (может быть {@code null})
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * Устанавливает ссылку на аватар профиля.
     *
     * @param avatarUrl URL аватара
     */
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /**
     * Возвращает связанного пользователя.
     *
     * @return пользователь
     */
    public User getUser() {
        return user;
    }

    /**
     * Устанавливает связанного пользователя.
     *
     * @param user пользователь
     */
    public void setUser(User user) {
        this.user = user;
    }
}
