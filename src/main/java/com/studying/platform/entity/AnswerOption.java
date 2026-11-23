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

/**
 * Сущность «Вариант ответа» викторины учебной платформы.
 * <p>
 * Конкретный вариант ответа на вопрос теста. Поле {@code isCorrect}
 * определяет, является ли вариант правильным. Для вопросов типа
 * {@link QuizQuestionType#MULTIPLE_CHOICE} правильными могут быть
 * несколько вариантов. Связь с вопросом — «многие-к-одному».
 * </p>
 *
 * @see Question
 * @see QuizQuestionType
 */
@Entity
@Table(name = "answeroption")
public class AnswerOption {

    /**
     * Идентификатор варианта ответа. Генерируется автоматически базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Вопрос, которому принадлежит данный вариант ответа. Владелец связи «многие-к-одному».
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_answeroption_question"))
    private Question question;

    /**
     * Текст варианта ответа. Обязательно для заполнения.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    /**
     * Признак правильности варианта ответа.
     * Для вопросов типа {@link QuizQuestionType#SINGLE_CHOICE} только один вариант имеет значение {@code true}.
     * Для вопросов типа {@link QuizQuestionType#MULTIPLE_CHOICE} правильных вариантов может быть несколько.
     */
    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public AnswerOption() {
    }

    /**
     * Создаёт вариант ответа с заданными параметрами.
     *
     * @param question  вопрос, которому принадлежит вариант ответа
     * @param text      текст варианта ответа
     * @param isCorrect признак правильности варианта ответа
     */
    public AnswerOption(Question question, String text, Boolean isCorrect) {
        this.question = question;
        this.text = text;
        this.isCorrect = isCorrect;
    }

    /**
     * Возвращает идентификатор варианта ответа.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор варианта ответа.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает вопрос, которому принадлежит данный вариант ответа.
     *
     * @return вопрос
     */
    public Question getQuestion() {
        return question;
    }

    /**
     * Устанавливает вопрос, которому принадлежит данный вариант ответа.
     *
     * @param question вопрос
     */
    public void setQuestion(Question question) {
        this.question = question;
    }

    /**
     * Возвращает текст варианта ответа.
     *
     * @return текст варианта ответа
     */
    public String getText() {
        return text;
    }

    /**
     * Устанавливает текст варианта ответа.
     *
     * @param text текст варианта ответа
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Возвращает признак правильности варианта ответа.
     *
     * @return признак правильности
     */
    public Boolean getIsCorrect() {
        return isCorrect;
    }

    /**
     * Устанавливает признак правильности варианта ответа.
     *
     * @param isCorrect признак правильности
     */
    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
}
