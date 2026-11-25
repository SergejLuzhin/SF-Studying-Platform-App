package com.studying.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * Сущность «Категория курса» учебной платформы.
 * <p>
 * Справочная сущность для классификации курсов по тематике
 * (например, «Программирование», «Дизайн», «Маркетинг»).
 * Каждая категория уникальна и может содержать несколько курсов.
 * </p>
 */
@Entity
@Table(name = "categories")
public class Category {

    /**
     * Идентификатор категории. Генерируется автоматически базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название категории. Уникальное и обязательное для заполнения.
     */
    @Column(unique = true, nullable = false, length = 100)
    private String name;

    /**
     * Коллекция курсов, относящихся к данной категории.
     * Загружается лениво. Владелец связи — сущность {@link Course}.
     */
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private Set<Course> courses = new HashSet<>();

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public Category() {
    }

    /**
     * Создаёт категорию с заданным названием.
     *
     * @param name название категории
     */
    public Category(String name) {
        this.name = name;
    }

    /**
     * Возвращает идентификатор категории.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор категории.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
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

    /**
     * Возвращает коллекцию курсов данной категории.
     *
     * @return коллекция курсов
     */
    public Set<Course> getCourses() {
        return courses;
    }

    /**
     * Устанавливает коллекцию курсов данной категории.
     *
     * @param courses коллекция курсов
     */
    public void setCourses(Set<Course> courses) {
        this.courses = courses;
    }
}
