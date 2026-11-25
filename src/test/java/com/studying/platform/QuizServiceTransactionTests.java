package com.studying.platform;

import com.studying.platform.entity.AnswerOption;
import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.Question;
import com.studying.platform.entity.Quiz;
import com.studying.platform.entity.QuizQuestionType;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.service.QuizSubmissionService;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Проверяет загрузку структуры теста через сервис и её доступность вне транзакции. */
@SpringBootTest
@ActiveProfiles("test")
class QuizServiceTransactionTests {
    /** Создаёт экземпляр тестов загрузки структуры для JUnit. */
    QuizServiceTransactionTests() {
    }

    @Autowired private QuizSubmissionService service;
    @Autowired private EntityManager em;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;
    private Long teacherId, categoryId, courseId, moduleId, quizId;
    private final List<Long> questionIds = new ArrayList<>();
    private final List<Long> optionIds = new ArrayList<>();

    /** Создаёт подтверждённую фикстуру теста с вопросами либо без них.
     * @param withQuestions нужно ли создать два вопроса с вариантами ответов
     */
    private void createFixture(boolean withQuestions) {
        transactions.executeWithoutResult(status -> {
            String unique = UUID.randomUUID().toString();
            User teacher = new User("t" + unique + "@test.io", UserRole.TEACHER);
            em.persist(teacher); teacherId = teacher.getId();
            Category category = new Category(unique);
            em.persist(category); categoryId = category.getId();
            Course course = new Course(unique, "Описание", 10, LocalDate.now(), teacher, category);
            em.persist(course); courseId = course.getId();
            Module module = new Module("Модуль", "Описание", 1, course);
            em.persist(module); moduleId = module.getId();
            Quiz quiz = new Quiz(module, "Проверка знаний", 15);
            em.persist(quiz); quizId = quiz.getId();
            if (withQuestions) {
                for (int i = 1; i <= 2; i++) {
                    Question question = new Question(quiz, "Вопрос " + i, QuizQuestionType.SINGLE_CHOICE);
                    em.persist(question); questionIds.add(question.getId());
                    for (int j = 1; j <= 2; j++) {
                        AnswerOption option = new AnswerOption(question, "Ответ " + i + "." + j, j == 1);
                        em.persist(option); optionIds.add(option.getId());
                    }
                }
            }
        });
    }

    /** Удаляет только созданные текущим тестом строки в порядке зависимостей. */
    @AfterEach
    void cleanUp() {
        transactions.executeWithoutResult(status -> {
            optionIds.forEach(id -> delete("answeroption", id));
            questionIds.forEach(id -> delete("question", id));
            delete("quiz", quizId);
            delete("modules", moduleId);
            delete("courses", courseId);
            delete("categories", categoryId);
            delete("users", teacherId);
        });
    }

    /** Удаляет созданную строку по идентификатору.
     * @param table таблица фикстуры
     * @param id идентификатор строки
     */
    private void delete(String table, Long id) {
        if (id != null) jdbc.update("delete from " + table + " where id = ?", id);
    }

    /** Проверяет чтение скалярных полей, нескольких вопросов и всех вариантов вне транзакции. */
    @Test
    void startQuizReturnsCompleteStructureOutsideTransaction() {
        createFixture(true);
        assertTrue(AopUtils.isAopProxy(service));
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        Quiz quiz = service.startQuiz(quizId);
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertEquals("Проверка знаний", quiz.getTitle());
        assertEquals(15, quiz.getTimeLimit());
        assertEquals(Set.of("Вопрос 1", "Вопрос 2"), quiz.getQuestions().stream()
                .map(Question::getText).collect(Collectors.toSet()));
        assertEquals(Set.of("Ответ 1.1", "Ответ 1.2", "Ответ 2.1", "Ответ 2.2"),
                quiz.getQuestions().stream().flatMap(question -> question.getOptions().stream())
                        .map(AnswerOption::getText).collect(Collectors.toSet()));
        assertTrue(quiz.getQuestions().stream().allMatch(question -> question.getOptions().size() == 2));
    }

    /** Проверяет, что викторина без вопросов отклоняется при запуске. */
    @Test
    void startQuizWithoutQuestionsThrowsException() {
        createFixture(false);
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertThrows(IllegalStateException.class, () -> service.startQuiz(quizId));
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    }

    /** Проверяет ожидаемое исключение для отсутствующего теста. */
    @Test
    void startQuizRejectsMissingId() {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertThrows(ResourceNotFoundException.class, () -> service.startQuiz(-1L));
    }

    /** Проверяет, что обычная загрузка JPA сохраняет ленивость обеих коллекций. */
    @Test
    void collectionsRemainLazyByDefault() {
        createFixture(true);
        transactions.executeWithoutResult(status -> {
            Quiz quiz = em.find(Quiz.class, quizId);
            assertFalse(Hibernate.isInitialized(quiz.getQuestions()));
            Question question = em.find(Question.class, questionIds.getFirst());
            assertFalse(Hibernate.isInitialized(question.getOptions()));
        });
    }
}
