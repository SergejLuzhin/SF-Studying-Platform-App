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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * Сущность «Модуль курса» учебной платформы.
 * <p>
 * Тематический раздел внутри курса, содержащий уроки и, опционально, тест.
 * Связь с курсом — «многие-к-одному». Владелец связи — {@link Course}.
 * </p>
 *
 * @see Course
 */
@Entity
@Table(name = "modules")
public class Module {

    /**
     * Идентификатор модуля. Генерируется автоматически базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название модуля. Обязательно для заполнения.
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Описание модуля. Хранится в формате TEXT.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Порядковый номер модуля в курсе. Обязательно для заполнения.
     */
    @Column(nullable = false)
    private Integer orderIndex;

    /**
     * Курс, которому принадлежит данный модуль. Владелец связи «многие-к-одному».
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_module_course"))
    private Course course;

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public Module() {
    }

    /**
     * Создаёт модуль с заданными параметрами.
     *
     * @param title       название модуля
     * @param description описание модуля
     * @param orderIndex  порядковый номер в курсе
     * @param course      курс, которому принадлежит модуль
     */
    public Module(String title, String description, Integer orderIndex, Course course) {
        this.title = title;
        this.description = description;
        this.orderIndex = orderIndex;
        this.course = course;
    }

    /**
     * Возвращает идентификатор модуля.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор модуля.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
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
     * Возвращает порядковый номер модуля в курсе.
     *
     * @return порядковый номер
     */
    public Integer getOrderIndex() {
        return orderIndex;
    }

    /**
     * Устанавливает порядковый номер модуля в курсе.
     *
     * @param orderIndex порядковый номер
     */
    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    /**
     * Возвращает курс, которому принадлежит данный модуль.
     *
     * @return курс
     */
    public Course getCourse() {
        return course;
    }

    /**
     * Устанавливает курс, которому принадлежит данный модуль.
     *
     * @param course курс
     */
    public void setCourse(Course course) {
        this.course = course;
    }
}
