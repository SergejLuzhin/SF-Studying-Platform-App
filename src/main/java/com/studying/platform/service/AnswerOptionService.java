package com.studying.platform.service;

import com.studying.platform.entity.AnswerOption;
import com.studying.platform.entity.Question;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.AnswerOptionRepository;
import com.studying.platform.repository.QuestionRepository;

import jakarta.persistence.EntityManager;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для управления вариантами ответов викторины учебной платформы.
 * <p>
 * Обеспечивает CRUD-операции для сущности AnswerOption.
 * Проверяет существование связанного вопроса и сохраняет изменения
 * через EntityManager.
 * </p>
 *
 * @see AnswerOption
 * @see Question
 */
@Service
@Transactional
public class AnswerOptionService {

    /**
     * Репозиторий для работы с вариантами ответов.
     */
    private final AnswerOptionRepository answerOptionRepository;

    /**
     * Репозиторий для работы с вопросами.
     */
    private final QuestionRepository questionRepository;

    /**
     * Менеджер сущностей для управления транзакциями и сброса контекста.
     */
    private final EntityManager entityManager;

    /**
     * Конструктор сервиса с внедрением зависимостей через конструктор.
     *
     * @param answerOptionRepository  репозиторий вариантов ответов
     * @param questionRepository      репозиторий вопросов
     * @param entityManager           менеджер сущностей JPA
     */
    public AnswerOptionService(AnswerOptionRepository answerOptionRepository,
                               QuestionRepository questionRepository,
                               EntityManager entityManager) {
        this.answerOptionRepository = answerOptionRepository;
        this.questionRepository = questionRepository;
        this.entityManager = entityManager;
    }

    /**
     * Создаёт вариант ответа для указанного вопроса.
     * <p>
     * Проверяет существование вопроса и сохраняет новый вариант.
     * </p>
     *
     * @param questionId идентификатор вопроса
     * @param text       текст варианта ответа
     * @param isCorrect  признак правильности варианта
     * @return сохранённый вариант ответа
     * @throws ResourceNotFoundException если вопрос не найден
     */
    public AnswerOption createAnswerOption(Long questionId, String text, Boolean isCorrect) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Вопрос с идентификатором " + questionId + " не найден"));

        AnswerOption option = new AnswerOption(question, text, isCorrect);
        entityManager.persist(option);
        entityManager.flush();
        return option;
    }

    /**
     * Возвращает вариант ответа по идентификатору.
     * <p>
     * Если вариант не найден — выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param answerOptionId идентификатор варианта ответа
     * @return найденный вариант ответа
     * @throws ResourceNotFoundException если вариант не найден
     */
    @Transactional(readOnly = true)
    public AnswerOption findAnswerOption(Long answerOptionId) {
        return answerOptionRepository.findById(answerOptionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Вариант ответа с идентификатором " + answerOptionId + " не найден"));
    }

    /**
     * Обновляет текст и признак правильности существующего варианта ответа.
     * <p>
     * Проверяет существование варианта. Если вариант не найден —
     * выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param answerOptionId идентификатор варианта ответа
     * @param text           новый текст варианта
     * @param isCorrect      новый признак правильности
     * @return обновлённый вариант ответа
     * @throws ResourceNotFoundException если вариант не найден
     */
    public AnswerOption updateAnswerOption(Long answerOptionId, String text, Boolean isCorrect) {
        AnswerOption option = findAnswerOption(answerOptionId);
        if (text != null) {
            option.setText(text);
        }
        if (isCorrect != null) {
            option.setIsCorrect(isCorrect);
        }
        entityManager.merge(option);
        entityManager.flush();
        return option;
    }

    /**
     * Удаляет вариант ответа по идентификатору.
     * <p>
     * Проверяет существование варианта. Если вариант не найден —
     * выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param answerOptionId идентификатор варианта для удаления
     * @throws ResourceNotFoundException если вариант не найден
     */
    public void deleteAnswerOption(Long answerOptionId) {
        AnswerOption option = findAnswerOption(answerOptionId);
        entityManager.remove(option);
        entityManager.flush();
    }
}
