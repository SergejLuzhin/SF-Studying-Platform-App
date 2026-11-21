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
import jakarta.persistence.Table;

/**
 * Сущность «Урок» учебной платформы.
 * <p>
 * Представляет отдельный урок внутри модуля курса. Содержит текстовое
 * содержание, ссылку на видео и привязку к родительскому модулю.
 * Связь с модулем — «многие-к-одному», загружается лениво.
 * </p>
 *
 * @see Module
 */
@Entity
@Table(name = "lessons")
public class Lesson {

    /**
     * Идентификатор урока. Генерируется автоматически базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название урока. Обязательно для заполнения.
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Текстовое содержание урока. Хранится в формате TEXT. Необязательно.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * Ссылка на видео-материал урока. Необязательно.
     */
    @Column
    private String videoUrl;

    /**
     * Модуль, которому принадлежит данный урок. Владелец связи
     * «многие-к-одному». Загружается лениво.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_lesson_module"))
    private Module module;

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public Lesson() {
    }

    /**
     * Создаёт урок с заданными параметрами.
     *
     * @param title    название урока
     * @param content  текстовое содержание урока
     * @param videoUrl ссылка на видео-материал
     * @param module   модуль, которому принадлежит урок
     */
    public Lesson(String title, String content, String videoUrl, Module module) {
        this.title = title;
        this.content = content;
        this.videoUrl = videoUrl;
        this.module = module;
    }

    /**
     * Возвращает идентификатор урока.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор урока.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает название урока.
     *
     * @return название урока
     */
    public String getTitle() {
        return title;
    }

    /**
     * Устанавливает название урока.
     *
     * @param title название урока
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Возвращает текстовое содержание урока.
     *
     * @return содержание урока
     */
    public String getContent() {
        return content;
    }

    /**
     * Устанавливает текстовое содержание урока.
     *
     * @param content содержание урока
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Возвращает ссылку на видео-материал урока.
     *
     * @return ссылка на видео
     */
    public String getVideoUrl() {
        return videoUrl;
    }

    /**
     * Устанавливает ссылку на видео-материал урока.
     *
     * @param videoUrl ссылка на видео
     */
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    /**
     * Возвращает модуль, которому принадлежит данный урок.
     *
     * @return модуль
     */
    public Module getModule() {
        return module;
    }

    /**
     * Устанавливает модуль, которому принадлежит данный урок.
     *
     * @param module модуль
     */
    public void setModule(Module module) {
        this.module = module;
    }
}
