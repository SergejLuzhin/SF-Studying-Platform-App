package com.studying.platform.service;

import com.studying.platform.entity.Module;
import com.studying.platform.entity.AnswerOption;
import com.studying.platform.entity.Question;
import com.studying.platform.entity.Quiz;
import com.studying.platform.entity.QuizQuestionType;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.ModuleRepository;
import com.studying.platform.repository.QuestionRepository;
import com.studying.platform.repository.QuizRepository;
import com.studying.platform.repository.QuizSubmissionRepository;
import com.studying.platform.repository.AnswerOptionRepository;

import jakarta.persistence.EntityManager;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для управления вопросами викторины учебной платформы.
 * <p>
 * Обеспечивает CRUD-операции для сущности Question.
 * Проверяет существование связанной викторины и сохраняет изменения
 * через EntityManager.
 * </p>
 *
 * @see Question
 * @see Quiz
 */
@Service
@Transactional
public class QuestionService {

    /**
     * Репозиторий для работы с вопросами.
     */
    private final QuestionRepository questionRepository;

    /**
     * Репозиторий для работы с викторинами.
     */
    private final QuizRepository quizRepository;

    /**
     * Репозиторий для работы с модулями.
     */
    private final ModuleRepository moduleRepository;

    /**
     * Менеджер сущностей для управления транзакциями и сброса контекста.
     */
    private final EntityManager entityManager;

    /** Репозиторий результатов прохождения для защиты учебной истории. */
    private final QuizSubmissionRepository quizSubmissionRepository;

    /** Репозиторий вариантов ответа для адресного удаления. */
    private final AnswerOptionRepository answerOptionRepository;

    /**
     * Конструктор сервиса с внедрением зависимостей через конструктор.
     *
     * @param questionRepository   репозиторий вопросов
     * @param quizRepository       репозиторий викторин
     * @param moduleRepository     репозиторий модулей
     * @param entityManager        менеджер сущностей JPA
     * @param quizSubmissionRepository репозиторий результатов тестов
     * @param answerOptionRepository репозиторий вариантов ответа
     */
    public QuestionService(QuestionRepository questionRepository,
                           QuizRepository quizRepository,
                           ModuleRepository moduleRepository,
                            EntityManager entityManager,
                            QuizSubmissionRepository quizSubmissionRepository,
                            AnswerOptionRepository answerOptionRepository) {
        this.questionRepository = questionRepository;
        this.quizRepository = quizRepository;
        this.moduleRepository = moduleRepository;
        this.entityManager = entityManager;
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.answerOptionRepository = answerOptionRepository;
    }

    /**
     * Создаёт вопрос в указанной викторине.
     * <p>
     * Проверяет существование викторины и сохраняет новый вопрос.
     * </p>
     *
     * @param quizId идентификатор викторины
     * @param text   текст вопроса
     * @param type   тип вопроса (SINGLE_CHOICE или MULTIPLE_CHOICE)
     * @return сохранённый вопрос
     * @throws ResourceNotFoundException если викторина не найдена
     */
    public Question createQuestion(Long quizId, String text, QuizQuestionType type) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Викторина с идентификатором " + quizId + " не найдена"));

        Question question = new Question(quiz, text, type);
        entityManager.persist(question);
        entityManager.flush();
        return question;
    }

    /**
     * Возвращает вопрос по идентификатору.
     * <p>
     * Если вопрос не найден — выбрасывает {@link ResourceNotFoundException}.
     * Варианты загружаются внутри транзакции для формирования DTO вне неё.
     * </p>
     *
     * @param questionId идентификатор вопроса
     * @return найденный вопрос
     * @throws ResourceNotFoundException если вопрос не найден
     */
    @Transactional(readOnly = true)
    public Question findQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Вопрос с идентификатором " + questionId + " не найден"));
        question.getOptions().size();
        return question;
    }

    /**
     * Обновляет текст и тип существующего вопроса.
     * <p>
     * Проверяет существование вопроса и обновляет переданные поля.
     * </p>
     *
     * @param questionId идентификатор вопроса
     * @param text       новый текст вопроса
     * @param type       новый тип вопроса
     * @return обновлённый вопрос
     * @throws ResourceNotFoundException если вопрос не найден
     */
    public Question updateQuestion(Long questionId, String text, QuizQuestionType type) {
        Question question = findQuestion(questionId);
        if (text != null) {
            question.setText(text);
        }
        if (type != null) {
            question.setType(type);
        }
        entityManager.merge(question);
        entityManager.flush();
        return question;
    }

    /**
     * Удаляет вопрос по идентификатору.
     * <p>
     * Если у родительского теста есть результаты, удаление запрещено.
     * Иначе сначала удаляются варианты ответа, затем вопрос, в одной транзакции.
     * </p>
     *
     * @param questionId идентификатор вопроса для удаления
     * @throws ResourceNotFoundException если вопрос не найден
     * @throws IllegalStateException если у теста есть сохранённые результаты
     */
    public void deleteQuestion(Long questionId) {
        Question question = findQuestion(questionId);
        if (quizSubmissionRepository.countByQuizIdIn(List.of(question.getQuiz().getId())) > 0) {
            throw new IllegalStateException("Нельзя удалить вопрос теста с сохранёнными результатами");
        }
        for (AnswerOption option : answerOptionRepository.findByQuestionId(questionId)) {
            question.getOptions().remove(option);
            entityManager.remove(option);
        }
        entityManager.flush();
        question.getQuiz().getQuestions().remove(question);
        entityManager.remove(question);
        entityManager.flush();
    }
}
