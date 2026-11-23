package com.studying.platform;

import com.studying.platform.entity.Assignment;
import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Lesson;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.Submission;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.service.SubmissionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Проверяет оценивание и хранение комментариев через Spring-сервис после завершения транзакции. */
@SpringBootTest
@ActiveProfiles("test")
class SubmissionGradingTransactionTests {
    @Autowired private SubmissionService service;
    @Autowired private EntityManager em;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;
    private Long teacherId, studentId, adminId, categoryId, courseId, moduleId, lessonId, assignmentId, submissionId;

    /** Варианты ошибочных данных для проверки сохранности оценки. */
    enum InvalidInput { STUDENT, ADMIN, MISSING_TEACHER, NULL_SCORE, NEGATIVE_SCORE, EXCESS_SCORE }

    /** Создаёт и подтверждает изолированную фикстуру с ранее выставленной оценкой. */
    @BeforeEach
    void setUp() {
        transactions.executeWithoutResult(status -> {
            String unique = UUID.randomUUID().toString();
            User teacher = new User("t" + unique + "@test.io", UserRole.TEACHER);
            User student = new User("s" + unique + "@test.io", UserRole.STUDENT);
            User admin = new User("a" + unique + "@test.io", UserRole.ADMIN);
            Category category = new Category(unique);
            em.persist(teacher); teacherId = teacher.getId();
            em.persist(student); studentId = student.getId();
            em.persist(admin); adminId = admin.getId();
            em.persist(category); categoryId = category.getId();
            Course course = new Course(unique, "Описание", 10, LocalDate.now(), teacher, category);
            em.persist(course); courseId = course.getId();
            Module module = new Module("Модуль", "Описание", 1, course);
            em.persist(module); moduleId = module.getId();
            Lesson lesson = new Lesson("Урок", "Текст", null, module);
            em.persist(lesson); lessonId = lesson.getId();
            Assignment assignment = new Assignment(lesson, "Задание", "Описание", LocalDate.now(), 100);
            em.persist(assignment); assignmentId = assignment.getId();
            Submission submission = new Submission(assignment, student, "Решение", 42, "Прежний комментарий");
            em.persist(submission); submissionId = submission.getId();
        });
        assertTrue(AopUtils.isAopProxy(service));
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    }

    /** Удаляет созданные фикстуры по идентификаторам в порядке зависимостей. */
    @AfterEach
    void cleanUp() {
        transactions.executeWithoutResult(status -> {
            delete("submissions", submissionId);
            delete("assignments", assignmentId);
            delete("lessons", lessonId);
            delete("modules", moduleId);
            delete("courses", courseId);
            delete("categories", categoryId);
            delete("users", adminId);
            delete("users", studentId);
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

    /** Проверяет неизменность оценки и комментария при ошибочных входных данных.
     * @param input тип некорректного ввода
     */
    @ParameterizedTest
    @EnumSource(InvalidInput.class)
    void rejectsInvalidInputWithoutChangingPersistedGrade(InvalidInput input) {
        Long grader = switch (input) {
            case STUDENT -> studentId;
            case ADMIN -> adminId;
            case MISSING_TEACHER -> -1L;
            default -> teacherId;
        };
        Integer score = switch (input) {
            case NULL_SCORE -> null;
            case NEGATIVE_SCORE -> -1;
            case EXCESS_SCORE -> 101;
            default -> 50;
        };
        Class<? extends RuntimeException> expected = input == InvalidInput.MISSING_TEACHER
                ? ResourceNotFoundException.class : IllegalArgumentException.class;
        assertThrows(expected, () -> service.gradeSubmission(submissionId, score, "Новый комментарий", grader));
        assertGrade(42, "Прежний комментарий");
    }

    /** Проверяет оценивание преподавателем на границах и повторную запись результата. */
    @Test
    void teacherCanGradeAndRegradeAtBounds() {
        service.gradeSubmission(submissionId, 0, "Ноль", teacherId);
        assertGrade(0, "Ноль");
        service.gradeSubmission(submissionId, 100, "Максимум", teacherId);
        assertGrade(100, "Максимум");
    }

    /** Проверяет отсутствие верхней границы при незаданном максимуме задания. */
    @Test
    void teacherCanGradeWithoutMaximum() {
        transactions.executeWithoutResult(status -> em.find(Assignment.class, assignmentId).setMaxScore(null));
        service.gradeSubmission(submissionId, 1000, "Без ограничения", teacherId);
        assertGrade(1000, "Без ограничения");
    }

    /** Проверяет длинный комментарий, его замену и null через JPA и SQL после подтверждения транзакций. */
    @Test
    void longFeedbackPersistsAndCanBeReplacedOrCleared() {
        String feedback = "Подробный комментарий преподавателя с кириллицей.\n".repeat(12);
        assertTrue(feedback.length() > 255);
        service.gradeSubmission(submissionId, 88, feedback, teacherId);
        assertGrade(88, feedback);
        assertEquals(feedback, jdbc.queryForObject(
                "select feedback from submissions where id = ?", String.class, submissionId));

        String replacement = "Исправленный комментарий\n".repeat(15);
        service.gradeSubmission(submissionId, 95, replacement, teacherId);
        assertGrade(95, replacement);
        assertEquals(replacement, jdbc.queryForObject(
                "select feedback from submissions where id = ?", String.class, submissionId));

        service.gradeSubmission(submissionId, 100, null, teacherId);
        assertGrade(100, null);
        assertNull(jdbc.queryForObject(
                "select feedback from submissions where id = ?", String.class, submissionId));
    }

    /** Проверяет ошибку для отсутствующего решения без изменения существующего. */
    @Test
    void rejectsMissingSubmission() {
        assertThrows(ResourceNotFoundException.class, () -> service.gradeSubmission(-1L, 5, "Комментарий", teacherId));
        assertGrade(42, "Прежний комментарий");
    }

    /** Читает решение в новой транзакции и проверяет оценку с комментарием.
     * @param score ожидаемая оценка
     * @param feedback ожидаемый комментарий
     */
    private void assertGrade(int score, String feedback) {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        transactions.executeWithoutResult(status -> {
            Submission submission = em.find(Submission.class, submissionId);
            assertNotNull(submission);
            assertEquals(score, submission.getScore());
            assertEquals(feedback, submission.getFeedback());
        });
    }
}
