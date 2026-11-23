package com.studying.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Сущность «Задание» учебной платформы.
 * <p>
 * Представляет учебное задание, привязанное к конкретному уроку.
 * Содержит описание, срок выполнения и максимальный балл.
 * Связь с уроком — «многие-к-одному», загружается лениво.
 * </p>
 *
 * @see Lesson
 * @see Submission
 */
@Entity
@Table(name = "assignments")
public class Assignment {

    /**
     * Идентификатор задания. Генерируется автоматически базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Урок, которому принадлежит данное задание.
     * Владелец связи «многие-к-одному». Загружается лениво.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_assignment_lesson"))
    private Lesson lesson;

    /**
     * Название задания. Обязательно для заполнения.
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Подробное описание задания и требования к выполнению.
     * Хранится в формате TEXT. Необязательно.
     * Без {@code @Lob} — согласно требованиям проекта.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Дата окончания выполнения задания. Необязательно.
     */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /**
     * Максимальный балл за выполнение задания. Необязательно.
     */
    @Column(name = "max_score")
    private Integer maxScore;

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public Assignment() {
    }

    /**
     * Создаёт задание с заданными параметрами.
     *
     * @param lesson      урок, которому принадлежит задание
     * @param title       название задания
     * @param description описание задания
     * @param dueDate     срок выполнения
     * @param maxScore    максимальный балл
     */
    public Assignment(Lesson lesson, String title, String description,
                      LocalDate dueDate, Integer maxScore) {
        this.lesson = lesson;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.maxScore = maxScore;
    }

    /**
     * Возвращает идентификатор задания.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор задания.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает урок, которому принадлежит данное задание.
     *
     * @return урок
     */
    public Lesson getLesson() {
        return lesson;
    }

    /**
     * Устанавливает урок, которому принадлежит данное задание.
     *
     * @param lesson урок
     */
    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    /**
     * Возвращает название задания.
     *
     * @return название задания
     */
    public String getTitle() {
        return title;
    }

    /**
     * Устанавливает название задания.
     *
     * @param title название задания
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Возвращает описание задания.
     *
     * @return описание задания
     */
    public String getDescription() {
        return description;
    }

    /**
     * Устанавливает описание задания.
     *
     * @param description описание задания
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Возвращает дату окончания выполнения задания.
     *
     * @return дата окончания
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Устанавливает дату окончания выполнения задания.
     *
     * @param dueDate дата окончания
     */
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Возвращает максимальный балл за задание.
     *
     * @return максимальный балл
     */
    public Integer getMaxScore() {
        return maxScore;
    }

    /**
     * Устанавливает максимальный балл за задание.
     *
     * @param maxScore максимальный балл
     */
    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }
}
