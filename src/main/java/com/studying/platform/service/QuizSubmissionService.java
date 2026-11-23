package com.studying.platform.service;

import com.studying.platform.entity.AnswerOption;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Enrollment;
import com.studying.platform.entity.EnrollmentStatus;
import com.studying.platform.entity.Quiz;
import com.studying.platform.entity.QuizSubmission;
import com.studying.platform.entity.Question;
import com.studying.platform.entity.User;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.AnswerOptionRepository;
import com.studying.platform.repository.EnrollmentRepository;
import com.studying.platform.repository.QuizRepository;
import com.studying.platform.repository.QuizSubmissionRepository;
import com.studying.platform.repository.QuestionRepository;
import com.studying.platform.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Сервис для управления прохождением викторин учебной платформы.
 * <p>
 * Обеспечивает логику проверки ответов студента, вычисления итогового балла
 * и сохранения результатов. Включает проверку права студента на прохождение теста
 * (факт активной записи на курс).
 * </p>
 *
 * @see QuizSubmission
 * @see Quiz
 */
@Service
@Transactional
public class QuizSubmissionService {

    private final QuizSubmissionRepository quizSubmissionRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    /**
     * Конструктор для внедрения зависимостей.
     *
     * @param quizSubmissionRepository    репозиторий результатов тестов
     * @param quizRepository              репозиторий викторин
     * @param questionRepository          репозиторий вопросов
     * @param enrollmentRepository        репозиторий записей на курс
     * @param userRepository              репозиторий пользователей
     * @param entityManager               JPA-менеджер для сохранения сущностей
     */
    public QuizSubmissionService(QuizSubmissionRepository quizSubmissionRepository,
                                 QuizRepository quizRepository,
                                 QuestionRepository questionRepository,
                                 EnrollmentRepository enrollmentRepository,
                                 UserRepository userRepository,
                                 EntityManager entityManager) {
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    /**
     * Возвращает викторину для прохождения, проверяя её наличие в базе.
     * <p>
     * До окончания транзакции загружаются вопросы и варианты ответов. После возврата
     * доступны скалярные поля викторины и загруженная структура; доступность других
     * ленивых связей не гарантируется. Сущность содержит признаки правильных ответов
     * и не предназначена для непосредственной выдачи студенту через публичный API.
     * </p>
     *
     * @param quizId идентификатор викторины
     * @return найденная викторина с вопросами и вариантами ответов
     * @throws ResourceNotFoundException если викторина с заданным идентификатором не найдена
     */
    @Transactional(readOnly = true)
    public Quiz startQuiz(Long quizId) {
        return quizRepository.findByIdWithQuestions(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Викторина с идентификатором " + quizId + " не найдена"));
    }

    /**
     * Проверяет ответы студента, вычисляет балл и сохраняет результат.
     * <p>
     * Логика работы:
     * 1. Проверяет существование викторины и студента.
     * 2. Проверяет активную запись студента на курс, к которому привязана викторина.
     * 3. Загружает все вопросы викторины и правильные варианты ответов.
     * 4. Вычисляет балл на основе точного совпадения выбранных студентом вариантов
     *    с правильными (для вопросов с одним и несколькими вариантами ответа).
     * 5. Сохраняет новый результат {@link QuizSubmission}.
     * </p>
     *
     * @param quizId  идентификатор викторины
     * @param studentId идентификатор студента
     * @param answers карта соответствия: ключ — идентификатор вопроса, значение —
     *                множество идентификаторов выбранных вариантов ответа
     * @return сохранённый результат прохождения теста
     * @throws ResourceNotFoundException если викторина не найдена, студент не найден
     *                                   или студент не записан на курс викторины
     * @throws IllegalArgumentException  если параметр answers равен null
     */
    @Transactional
    public QuizSubmission submitAnswers(Long quizId, Long studentId, Map<Long, Set<Long>> answers) {
        if (answers == null) {
            throw new IllegalArgumentException("Карта ответов не может быть null");
        }

        // 1. Валидация викторины
        Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Викторина с идентификатором " + quizId + " не найдена"));

        // 2. Проверка записи на курс
        Course course = quiz.getModule().getCourse(); // безопасно внутри транзакции
        Long courseId = course.getId();

        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseIdAndStatus(studentId, courseId, EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Студент с идентификатором " + studentId + " не записан на курс с идентификатором " + courseId));

        // 3. Валидация студента
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Студент с идентификатором " + studentId + " не найден"));

        // 4. Загрузка структуры теста
        List<Question> questions = quiz.getQuestions().stream().toList();
        int totalQuestions = questions.size();

        // Сбор правильных ответов: questionId -> Set<optionId>
        Map<Long, Set<Long>> correctOptionsMap = new HashMap<>();
        for (Question question : questions) {
            Set<Long> correctOptionIds = new HashSet<>();
            for (AnswerOption option : question.getOptions()) {
                if (Boolean.TRUE.equals(option.getIsCorrect())) {
                    correctOptionIds.add(option.getId());
                }
            }
            correctOptionsMap.put(question.getId(), correctOptionIds);
        }

        // 5. Вычисление результата
        int correctCount = 0;
        for (Question question : questions) {
            Long questionId = question.getId();
            Set<Long> correctSet = correctOptionsMap.get(questionId);
            Set<Long> selectedSet = answers.get(questionId);

            if (selectedSet == null || selectedSet.isEmpty()) {
                continue; // Студент не ответил на вопрос
            }

            if (selectedSet.equals(correctSet)) {
                correctCount++;
            }
        }

        // Целочисленное деление: (правильных / всего) * 100
        int score = totalQuestions > 0 ? (correctCount * 100) / totalQuestions : 0;

        // 6. Сохранение результата
        QuizSubmission submission = new QuizSubmission(quiz, student, score, LocalDateTime.now());
        entityManager.persist(submission);
        entityManager.flush();

        return submission;
    }
}
