package com.studying.platform.dto;

import java.util.Map;
import java.util.Set;

/**
 * DTO для отправки ответов студента на викторину.
 * <p>
 * Содержит идентификатор студента и карту ответов:
 * ключ — идентификатор вопроса, значение — множество
 * идентификаторов выбранных вариантов ответа.
 * </p>
 *
 * @see com.studying.platform.entity.QuizSubmission
 */
public class SubmitAnswersRequestDto {

    /**
     * Идентификатор студента, отправляющего ответы.
     */
    private Long studentId;

    /**
     * Карта ответов: вопрос → выбранные варианты.
     */
    private Map<Long, Set<Long>> answers;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public SubmitAnswersRequestDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param studentId идентификатор студента
     * @param answers   карта ответов
     */
    public SubmitAnswersRequestDto(Long studentId, Map<Long, Set<Long>> answers) {
        this.studentId = studentId;
        this.answers = answers;
    }

    /**
     * Возвращает идентификатор студента.
     *
     * @return идентификатор студента
     */
    public Long getStudentId() {
        return studentId;
    }

    /**
     * Устанавливает идентификатор студента.
     *
     * @param studentId идентификатор студента
     */
    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    /**
     * Возвращает карту ответов.
     *
     * @return карта ответов
     */
    public Map<Long, Set<Long>> getAnswers() {
        return answers;
    }

    /**
     * Устанавливает карту ответов.
     *
     * @param answers карта ответов
     */
    public void setAnswers(Map<Long, Set<Long>> answers) {
        this.answers = answers;
    }
}
