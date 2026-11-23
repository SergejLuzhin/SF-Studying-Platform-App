package com.studying.platform.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * Сущность «Вопрос викторины» учебной платформы.
 * <p>
 * Вопрос теста с текстом, типом и набором вариантов ответа.
 * Связь с викториной — «многие-к-одному». Связь «один-ко-многим»
 * на варианты ответа с каскадной операцией PERSIST.
 * Поддерживает один или несколько правильных вариантов ответа.
 * </p>
 *
 * @see Quiz
 * @see AnswerOption
 * @see QuizQuestionType
 */
@Entity
@Table(name = "question")
public class Question {

    /**
     * Идентификатор вопроса. Генерируется автоматически базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Викторина, которой принадлежит данный вопрос. Владелец связи «многие-к-одному».
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_question_quiz"))
    private Quiz quiz;

    /**
     * Текст вопроса. Обязательно для заполнения.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    /**
     * Тип вопроса: одиночный или множественный выбор. Обязательно для заполнения.
     */
    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private QuizQuestionType type;

    /**
     * Варианты ответа на данный вопрос. Загружаются лениво, каскадная операция PERSIST
     * обеспечивает автоматическое сохранение вариантов при сохранении вопроса.
     * Владелец обратной связи — {@link AnswerOption}.
     * Поддерживает один или несколько правильных вариантов (isCorrect).
     */
    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY,
            cascade = CascadeType.PERSIST)
    private Set<AnswerOption> options = new HashSet<>();

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public Question() {
    }

    /**
     * Создаёт вопрос с заданными параметрами.
     *
     * @param quiz    викторина, которой принадлежит вопрос
     * @param text    текст вопроса
     * @param type    тип вопроса
     */
    public Question(Quiz quiz, String text, QuizQuestionType type) {
        this.quiz = quiz;
        this.text = text;
        this.type = type;
    }

    /**
     * Возвращает идентификатор вопроса.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор вопроса.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает викторину, которой принадлежит данный вопрос.
     *
     * @return викторина
     */
    public Quiz getQuiz() {
        return quiz;
    }

    /**
     * Устанавливает викторину, которой принадлежит данный вопрос.
     *
     * @param quiz викторина
     */
    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    /**
     * Возвращает текст вопроса.
     *
     * @return текст вопроса
     */
    public String getText() {
        return text;
    }

    /**
     * Устанавливает текст вопроса.
     *
     * @param text текст вопроса
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Возвращает тип вопроса.
     *
     * @return тип вопроса
     */
    public QuizQuestionType getType() {
        return type;
    }

    /**
     * Устанавливает тип вопроса.
     *
     * @param type тип вопроса
     */
    public void setType(QuizQuestionType type) {
        this.type = type;
    }

    /**
     * Возвращает коллекцию вариантов ответа на данный вопрос.
     *
     * @return коллекция вариантов ответа
     */
    public Set<AnswerOption> getOptions() {
        return options;
    }

    /**
     * Устанавливает коллекцию вариантов ответа на данный вопрос.
     *
     * @param options коллекция вариантов ответа
     */
    public void setOptions(Set<AnswerOption> options) {
        this.options = options;
    }
}
