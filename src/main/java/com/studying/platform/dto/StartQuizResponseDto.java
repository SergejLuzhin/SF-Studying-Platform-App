package com.studying.platform.dto;

import java.util.List;

/**
 * DTO для ответа с викториной, подготовленной к прохождению.
 * <p>
 * Содержит основные сведения викторины и вложенный список вопросов с вариантами ответов.
 * Используется эндпоинтом GET /api/quiz/{id}/start.
 * Варианты содержат идентификаторы и текст, но не признаки правильности.
 * </p>
 *
 * @see com.studying.platform.entity.Quiz
 */
public class StartQuizResponseDto {

    /**
     * Идентификатор викторины.
     */
    private Long id;

    /**
     * Идентификатор модуля, которому принадлежит викторина.
     */
    private Long moduleId;

    /**
     * Название викторины.
     */
    private String title;

    /**
     * Лимит времени на прохождение викторины в минутах.
     */
    private Integer timeLimit;

    /**
     * Список вопросов викторины с вариантами ответов.
     */
    private List<QuestionWithOptionsDto> questions;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public StartQuizResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id        идентификатор викторины
     * @param moduleId  идентификатор модуля
     * @param title     название викторины
     * @param timeLimit лимит времени в минутах
     * @param questions список вопросов с вариантами
     */
    public StartQuizResponseDto(Long id, Long moduleId, String title, Integer timeLimit,
                                List<QuestionWithOptionsDto> questions) {
        this.id = id;
        this.moduleId = moduleId;
        this.title = title;
        this.timeLimit = timeLimit;
        this.questions = questions;
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
     * Возвращает идентификатор модуля.
     *
     * @return идентификатор модуля
     */
    public Long getModuleId() {
        return moduleId;
    }

    /**
     * Устанавливает идентификатор модуля.
     *
     * @param moduleId идентификатор модуля
     */
    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
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
     * @return лимит времени
     */
    public Integer getTimeLimit() {
        return timeLimit;
    }

    /**
     * Устанавливает лимит времени на прохождение викторины в минутах.
     *
     * @param timeLimit лимит времени
     */
    public void setTimeLimit(Integer timeLimit) {
        this.timeLimit = timeLimit;
    }

    /**
     * Возвращает список вопросов викторины с вариантами ответов.
     *
     * @return список вопросов
     */
    public List<QuestionWithOptionsDto> getQuestions() {
        return questions;
    }

    /**
     * Устанавливает список вопросов викторины с вариантами ответов.
     *
     * @param questions список вопросов
     */
    public void setQuestions(List<QuestionWithOptionsDto> questions) {
        this.questions = questions;
    }

    /**
     * Вложенный DTO для вопроса с вариантами ответов.
     */
    public static class QuestionWithOptionsDto {

        /**
         * Идентификатор вопроса.
         */
        private Long id;

        /**
         * Текст вопроса.
         */
        private String text;

        /**
         * Тип вопроса.
         */
        private com.studying.platform.entity.QuizQuestionType type;

        /**
         * Список вариантов ответа.
         */
        private List<AnswerOptionDto> options;

        /**
         * Публичный конструктор по умолчанию.
         */
        public QuestionWithOptionsDto() {
        }

        /**
         * Создаёт DTO вопроса с вариантами ответов.
         *
         * @param id      идентификатор вопроса
         * @param text    текст вопроса
         * @param type    тип вопроса
         * @param options варианты ответа
         */
        public QuestionWithOptionsDto(Long id, String text,
                                       com.studying.platform.entity.QuizQuestionType type,
                                       List<AnswerOptionDto> options) {
            this.id = id;
            this.text = text;
            this.type = type;
            this.options = options;
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
        public com.studying.platform.entity.QuizQuestionType getType() {
            return type;
        }

        /**
         * Устанавливает тип вопроса.
         *
         * @param type тип вопроса
         */
        public void setType(com.studying.platform.entity.QuizQuestionType type) {
            this.type = type;
        }

        /**
         * Возвращает список вариантов ответа.
         *
         * @return варианты ответа
         */
        public List<AnswerOptionDto> getOptions() {
            return options;
        }

        /**
         * Устанавливает список вариантов ответа.
         *
         * @param options варианты ответа
         */
        public void setOptions(List<AnswerOptionDto> options) {
            this.options = options;
        }
    }

    /**
     * Вариант ответа для прохождения: только идентификатор и текст без признака правильности.
     */
    public static class AnswerOptionDto {

        /**
         * Идентификатор варианта ответа.
         */
        private Long id;

        /**
         * Текст варианта ответа.
         */
        private String text;

        /**
         * Публичный конструктор по умолчанию.
         */
        public AnswerOptionDto() {
        }

        /**
         * Создаёт DTO варианта ответа для прохождения без признака правильности.
         *
         * @param id        идентификатор варианта
         * @param text      текст варианта
         */
        public AnswerOptionDto(Long id, String text) {
            this.id = id;
            this.text = text;
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
         * Возвращает текст варианта ответа.
         *
         * @return текст варианта
         */
        public String getText() {
            return text;
        }

        /**
         * Устанавливает текст варианта ответа.
         *
         * @param text текст варианта
         */
        public void setText(String text) {
            this.text = text;
        }

    }
}
