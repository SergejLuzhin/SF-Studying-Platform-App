package com.studying.platform.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * Сущность «Викторина» учебной платформы.
 * <p>
 * Набор вопросов для проверки знаний по модулю курса.
 * Связь с модулем — «один-к-одному»: у каждого модуля не более одного теста.
 * Владелец связи с модулем — {@code Quiz}. Связь «один-ко-многим» на вопросы
 * с каскадной операцией PERSIST для автоматического сохранения вопросов
 * при сохранении викторины.
 * </p>
 *
 * @see Module
 * @see Question
 */
@Entity
@Table(name = "quiz")
public class Quiz {

    /**
     * Идентификатор викторины. Генерируется автоматически базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Модуль, которому принадлежит данная викторина. Владелец связи «один-к-одному».
     * У каждого модуля не более одного теста.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_quiz_module"))
    private Module module;

    /**
     * Название викторины. Обязательно для заполнения.
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Лимит времени на прохождение викторины в минутах. Может быть нулём (без ограничения).
     */
    @Column(name = "time_limit")
    private Integer timeLimit;

    /**
     * Вопросы викторины. Загружаются лениво, каскадная операция PERSIST
     * обеспечивает автоматическое сохранение вопросов при сохранении викторины.
     * Владелец обратной связи — {@link Question}.
     */
    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY,
            cascade = CascadeType.PERSIST)
    private Set<Question> questions = new HashSet<>();

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public Quiz() {
    }

    /**
     * Создаёт викторину с заданными параметрами.
     *
     * @param module    модуль, которому принадлежит викторина
     * @param title     название викторины
     * @param timeLimit лимит времени на прохождение в минутах
     */
    public Quiz(Module module, String title, Integer timeLimit) {
        this.module = module;
        this.title = title;
        this.timeLimit = timeLimit;
    }

    /**
     * Возвращает идентификатор викторины.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор викторины.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает модуль, которому принадлежит данная викторина.
     *
     * @return модуль
     */
    public Module getModule() {
        return module;
    }

    /**
     * Устанавливает модуль, которому принадлежит данная викторина.
     *
     * @param module модуль
     */
    public void setModule(Module module) {
        this.module = module;
    }

    /**
     * Возвращает название викторины.
     *
     * @return название викторины
     */
    public String getTitle() {
        return title;
    }

    /**
     * Устанавливает название викторины.
     *
     * @param title название викторины
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Возвращает лимит времени на прохождение викторины в минутах.
     *
     * @return лимит времени в минутах
     */
    public Integer getTimeLimit() {
        return timeLimit;
    }

    /**
     * Устанавливает лимит времени на прохождение викторины в минутах.
     *
     * @param timeLimit лимит времени в минутах
     */
    public void setTimeLimit(Integer timeLimit) {
        this.timeLimit = timeLimit;
    }

    /**
     * Возвращает коллекцию вопросов викторины.
     *
     * @return коллекция вопросов
     */
    public Set<Question> getQuestions() {
        return questions;
    }

    /**
     * Устанавливает коллекцию вопросов викторины.
     *
     * @param questions коллекция вопросов
     */
    public void setQuestions(Set<Question> questions) {
        this.questions = questions;
    }
}
