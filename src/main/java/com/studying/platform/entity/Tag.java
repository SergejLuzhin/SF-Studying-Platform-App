package com.studying.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Сущность «Тег» учебной платформы.
 * <p>
 * Метка для классификации курсов по ключевым темам (например, «Java», «Hibernate», «Beginner»).
 * Название тега уникально. Связь с курсами — «многие-ко-многим» через таблицу {@code course_tag}.
 * </p>
 *
 * @see Course
 */
@Entity
@Table(name = "tags")
public class Tag {

    /**
     * Идентификатор тега. Генерируется автоматически базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название тега. Уникальное и обязательное для заполнения.
     */
    @Column(unique = true, nullable = false, length = 50)
    private String name;

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public Tag() {
    }

    /**
     * Создаёт тег с заданным названием.
     *
     * @param name название тега
     */
    public Tag(String name) {
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
