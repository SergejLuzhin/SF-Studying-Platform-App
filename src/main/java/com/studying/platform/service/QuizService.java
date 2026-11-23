package com.studying.platform.service;

import com.studying.platform.entity.AnswerOption;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.Question;
import com.studying.platform.entity.Quiz;
import com.studying.platform.entity.QuizQuestionType;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.AnswerOptionRepository;
import com.studying.platform.repository.ModuleRepository;
import com.studying.platform.repository.QuestionRepository;
import com.studying.platform.repository.QuizRepository;
import com.studying.platform.repository.QuizSubmissionRepository;

import jakarta.persistence.EntityManager;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для управления викторинами, вопросами и вариантами ответов учебной платформы.
 * <p>
 * Обеспечивает CRUD-операции для сущностей Quiz, Question и AnswerOption.
 * Проверяет существование связанных объектов, соблюдение бизнес-правил
 * (один модуль — один тест, защита результатов при удалении)
 * и сохраняет изменения через EntityManager.
 * </p>
 *
 * @see Quiz
 * @see Question
 * @see AnswerOption
 */
@Service
@Transactional
public class QuizService {

    /**
     * Репозиторий для работы с викторинами.
     */
    private final QuizRepository quizRepository;

    /**
     * Репозиторий для работы с вопросами.
     */
    private final QuestionRepository questionRepository;

    /**
     * Репозиторий для работы с вариантами ответов.
     */
    private final AnswerOptionRepository answerOptionRepository;

    /**
     * Репозиторий для работы с модулями.
     */
    private final ModuleRepository moduleRepository;

    /**
     * Репозиторий для проверки наличия результатов тестов.
     */
    private final QuizSubmissionRepository quizSubmissionRepository;

    /**
     * Менеджер сущностей для управления транзакциями и сброса контекста.
     */
    private final EntityManager entityManager;

    /**
     * Конструктор сервиса с внедрением зависимостей через конструктор.
     *
     * @param quizRepository          репозиторий викторин
     * @param questionRepository      репозиторий вопросов
     * @param answerOptionRepository  репозиторий вариантов ответов
     * @param moduleRepository        репозиторий модулей
     * @param quizSubmissionRepository репозиторий результатов тестов
     * @param entityManager           менеджер сущностей JPA
     */
    public QuizService(QuizRepository quizRepository,
                       QuestionRepository questionRepository,
                       AnswerOptionRepository answerOptionRepository,
                       ModuleRepository moduleRepository,
                       QuizSubmissionRepository quizSubmissionRepository,
                       EntityManager entityManager) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.answerOptionRepository = answerOptionRepository;
        this.moduleRepository = moduleRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.entityManager = entityManager;
    }

    /**
     * Создаёт викторину в указанном модуле.
     * <p>
     * Проверяет существование модуля и отсутствие другого теста в том же модуле.
     * У модуля не может быть более одного теста.
     * </p>
     *
     * @param moduleId  идентификатор модуля
     * @param title     название викторины
     * @param timeLimit лимит времени на прохождение в минутах (может быть null)
     * @return сохранённая викторина
     * @throws ResourceNotFoundException если модуль с заданным идентификатором не найден
     * @throws IllegalStateException     если в модуле уже существует викторина
     */
    public Quiz createQuiz(Long moduleId, String title, Integer timeLimit) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Модуль с идентификатором " + moduleId + " не найден"));

        quizRepository.findByModuleId(moduleId)
                .ifPresent(existingQuiz -> {
                    throw new IllegalStateException(
                            "В модуле с идентификатором " + moduleId + " уже существует викторина: " + existingQuiz.getTitle());
                });

        Quiz quiz = new Quiz(module, title, timeLimit);
        entityManager.persist(quiz);
        entityManager.flush();
        return quiz;
    }

    /**
     * Возвращает викторину по идентификатору.
     * <p>
     * Если викторина не найдена — выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param quizId идентификатор викторины
     * @return найденная викторина
     * @throws ResourceNotFoundException если викторина не найдена
     */
    @Transactional(readOnly = true)
    public Quiz findQuiz(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Викторина с идентификатором " + quizId + " не найдена"));
    }

    /**
     * Обновляет название и лимит времени существующей викторины.
     * <p>
     * Проверяет существование викторины. Если викторина не найдена —
     * выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param quizId    идентификатор викторины
     * @param title     новое название викторины
     * @param timeLimit новый лимит времени в минутах (может быть null)
     * @return обновлённая викторина
     * @throws ResourceNotFoundException если викторина не найдена
     */
    public Quiz updateQuiz(Long quizId, String title, Integer timeLimit) {
        Quiz quiz = findQuiz(quizId);
        if (title != null) {
            quiz.setTitle(title);
        }
        if (timeLimit != null) {
            quiz.setTimeLimit(timeLimit);
        }
        entityManager.merge(quiz);
        entityManager.flush();
        return quiz;
    }

    /**
     * Удаляет викторину по идентификатору.
     * <p>
     * Проверяет отсутствие результатов прохождения теста. Если результаты существуют —
     * выбрасывает {@link IllegalStateException}. Связанные вопросы и варианты ответов
     * удаляются автоматически благодаря каскаду PERSIST на вопросах
     * (но не при удалении — необходимо удалять вопросы вручную).
     * </p>
     *
     * @param quizId идентификатор викторины для удаления
     * @throws ResourceNotFoundException если викторина не найдена
     * @throws IllegalStateException     если викторина имеет сохранённые результаты
     */
    public void deleteQuiz(Long quizId) {
        Quiz quiz = findQuiz(quizId);

        long submissionCount = quizSubmissionRepository.countByQuizIdIn(List.of(quizId));
        if (submissionCount > 0) {
            throw new IllegalStateException(
                    "Нельзя удалить викторину с идентификатором " + quizId
                            + ": существуют результаты прохождения теста (" + submissionCount + ")");
        }

        // Явно загружаем вопросы и варианты перед удалением
        List<Question> questions = questionRepository.findByQuizId(quizId);
        for (Question question : questions) {
            List<AnswerOption> options = answerOptionRepository.findByQuestionId(question.getId());
            for (AnswerOption option : options) {
                entityManager.remove(option);
            }
            entityManager.remove(question);
        }

        entityManager.remove(quiz);
        entityManager.flush();
    }

    /**
     * Создаёт вопрос в указанной викторине.
     * <p>
     * Проверяет существование викторины, поддерживает двунаправленную связь
     * между викториной и вопросом, сохраняет новый вопрос.
     * </p>
     *
     * @param quizId идентификатор викторины
     * @param text   текст вопроса
     * @param type   тип вопроса (SINGLE_CHOICE или MULTIPLE_CHOICE)
     * @return сохранённый вопрос
     * @throws ResourceNotFoundException если викторина не найдена
     */
    public Question createQuestion(Long quizId, String text, QuizQuestionType type) {
        Quiz quiz = findQuiz(quizId);

        Question question = new Question(quiz, text, type);
        // Поддержка двунаправленной связи: добавляем вопрос в коллекцию викторины
        quiz.getQuestions().add(question);
        question.setQuiz(quiz);
        entityManager.persist(question);
        entityManager.flush();
        return question;
    }

    /**
     * Возвращает вопрос по идентификатору.
     * <p>
     * Если вопрос не найден — выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param questionId идентификатор вопроса
     * @return найденный вопрос
     * @throws ResourceNotFoundException если вопрос не найден
     */
    @Transactional(readOnly = true)
    public Question findQuestion(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Вопрос с идентификатором " + questionId + " не найден"));
    }

    /**
     * Обновляет текст и тип существующего вопроса.
     * <p>
     * Проверяет существование вопроса. Если вопрос не найден —
     * выбрасывает {@link ResourceNotFoundException}.
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
     * Проверяет существование вопроса. Если вопрос не найден —
     * выбрасывает {@link ResourceNotFoundException}.
     * Связанные варианты ответов удаляются через каскад PERSIST
     * при сохранении, но при удалении требуется явное удаление.
     * </p>
     *
     * @param questionId идентификатор вопроса для удаления
     * @throws ResourceNotFoundException если вопрос не найден
     */
    public void deleteQuestion(Long questionId) {
        Question question = findQuestion(questionId);
        entityManager.remove(question);
        entityManager.flush();
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
        Question question = findQuestion(questionId);

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

    /**
     * Возвращает список всех вопросов указанной викторины.
     * <p>
     * Проверяет существование викторины. Если викторина не найдена —
     * выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param quizId идентификатор викторины
     * @return список вопросов викторины
     * @throws ResourceNotFoundException если викторина не найдена
     */
    @Transactional(readOnly = true)
    public List<Question> getQuestionsByQuiz(Long quizId) {
        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException(
                    "Викторина с идентификатором " + quizId + " не найдена");
        }
        return questionRepository.findByQuizId(quizId);
    }

    /**
     * Возвращает список всех вариантов ответа указанного вопроса.
     * <p>
     * Проверяет существование вопроса. Если вопрос не найден —
     * выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param questionId идентификатор вопроса
     * @return список вариантов ответа
     * @throws ResourceNotFoundException если вопрос не найден
     */
    @Transactional(readOnly = true)
    public List<AnswerOption> getOptionsByQuestion(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new ResourceNotFoundException(
                    "Вопрос с идентификатором " + questionId + " не найден");
        }
        return answerOptionRepository.findByQuestionId(questionId);
    }
}
