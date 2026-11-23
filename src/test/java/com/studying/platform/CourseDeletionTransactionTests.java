package com.studying.platform;

import com.studying.platform.entity.*;
import com.studying.platform.entity.Module;
import com.studying.platform.service.CourseService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Проверяет удаление курса через транзакционный Spring-сервис после реального завершения транзакции. */
@SpringBootTest
@ActiveProfiles("test")
class CourseDeletionTransactionTests {
    @Autowired private CourseService service;
    @Autowired private EntityManager em;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;

    private Long teacherId, studentId, categoryId, courseId, otherCourseId, moduleId, lessonId;
    private Long assignmentId, quizId, questionId, optionId, tagId, historyId;
    private History historyType;

    /** Виды учебной истории для независимой проверки каждого запрета. */
    enum History { ACTIVE, DROPPED, SUBMISSION, QUIZ_RESULT, REVIEW }

    /** Создаёт подтверждённую фикстуру с выбранным содержимым и уникальными данными. */
    private void createFixture(boolean assignment, boolean quiz, boolean sharedTag) {
        transactions.executeWithoutResult(status -> {
            String unique = UUID.randomUUID().toString();
            User teacher = new User("t" + unique + "@test.io", UserRole.TEACHER);
            User student = new User("s" + unique + "@test.io", UserRole.STUDENT);
            Category category = new Category(unique);
            em.persist(teacher); em.persist(student); em.persist(category);
            teacherId = teacher.getId(); studentId = student.getId(); categoryId = category.getId();
            Course course = new Course(unique, "Описание", 10, LocalDate.now(), teacher, category);
            em.persist(course); courseId = course.getId();
            Module module = new Module("Модуль", "Описание", 1, course);
            em.persist(module); moduleId = module.getId();
            if (assignment) {
                Lesson lesson = new Lesson("Урок", "Текст", null, module);
                em.persist(lesson); lessonId = lesson.getId();
                Assignment task = new Assignment(lesson, "Задание", "Описание", LocalDate.now(), 100);
                em.persist(task); assignmentId = task.getId();
            }
            if (quiz) {
                Quiz test = new Quiz(module, "Тест", 10);
                em.persist(test); quizId = test.getId();
                Question question = new Question(test, "Вопрос", QuizQuestionType.SINGLE_CHOICE);
                em.persist(question); questionId = question.getId();
                AnswerOption option = new AnswerOption(question, "Ответ", true);
                em.persist(option); optionId = option.getId();
            }
            if (sharedTag) {
                Tag tag = new Tag(unique);
                em.persist(tag); tagId = tag.getId();
                Course other = new Course("other-" + unique, "Описание", 10, LocalDate.now(), teacher, category);
                em.persist(other); otherCourseId = other.getId();
                course.getTags().add(tag);
                other.getTags().add(tag);
            }
            em.flush();
        });
    }

    /** Удаляет только созданные тестом строки в порядке зависимостей, не скрывая ошибки очистки. */
    @AfterEach
    void cleanUp() {
        transactions.executeWithoutResult(status -> {
            if (historyType != null) {
                String table = switch (historyType) {
                    case ACTIVE, DROPPED -> "enrollments";
                    case SUBMISSION -> "submissions";
                    case QUIZ_RESULT -> "quizsubmission";
                    case REVIEW -> "course_reviews";
                };
                delete(table, historyId);
            }
            delete("answeroption", optionId);
            delete("question", questionId);
            delete("quiz", quizId);
            delete("assignments", assignmentId);
            delete("lessons", lessonId);
            delete("modules", moduleId);
            if (courseId != null) jdbc.update("delete from course_tag where course_id = ?", courseId);
            if (otherCourseId != null) jdbc.update("delete from course_tag where course_id = ?", otherCourseId);
            delete("courses", courseId);
            delete("courses", otherCourseId);
            delete("tags", tagId);
            delete("users", studentId);
            delete("users", teacherId);
            delete("categories", categoryId);
        });
    }

    /** Удаляет строку заданной таблицы по её идентификатору, если он был создан.
     * @param table имя таблицы фикстуры
     * @param id идентификатор строки
     */
    private void delete(String table, Long id) {
        if (id != null) jdbc.update("delete from " + table + " where id = ?", id);
    }

    /** Проверяет отсутствие внешней транзакции и вызов сервиса через Spring-прокси. */
    private void deleteThroughProxy() {
        assertTrue(AopUtils.isAopProxy(service));
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        service.deleteCourse(courseId);
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    }

    /** Проверяет удаление задания вместе с курсом после подтверждения транзакции. */
    @Test
    void deletesAssignmentStructure() {
        createFixture(true, false, false);
        deleteThroughProxy();
        assertDeleted();
    }

    /** Проверяет удаление теста, вопроса и варианта вместе с курсом. */
    @Test
    void deletesQuizStructure() {
        createFixture(false, true, false);
        deleteThroughProxy();
        assertDeleted();
    }

    /** Проверяет удаление обоих видов содержимого и сохранение общего тега другого курса. */
    @Test
    void deletesMixedContentButPreservesSharedTag() {
        createFixture(true, true, true);
        deleteThroughProxy();
        assertDeleted();
        transactions.executeWithoutResult(status -> {
            assertNotNull(em.find(Tag.class, tagId));
            Course other = em.find(Course.class, otherCourseId);
            assertNotNull(other);
            assertTrue(other.getTags().stream().anyMatch(tag -> tag.getId().equals(tagId)));
            assertEquals(0, jdbc.queryForObject("select count(*) from course_tag where course_id = ?", Integer.class, courseId));
        });
    }

    /** Проверяет, что удаление задания с решением откатывается без потери данных. */
    @Test
    void deletionOfAssignmentWithSubmissionRollsBack() {
        createFixture(true, false, false);
        historyType = History.SUBMISSION;
        transactions.executeWithoutResult(status -> {
            Submission submission = new Submission(em.find(Assignment.class, assignmentId),
                    em.find(User.class, studentId), "Решение");
            em.persist(submission);
            historyId = submission.getId();
        });
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertThrows(org.hibernate.exception.ConstraintViolationException.class,
                () -> service.deleteAssignment(assignmentId));
        transactions.executeWithoutResult(status -> {
            assertNotNull(em.find(Assignment.class, assignmentId));
            assertNotNull(em.find(Submission.class, historyId));
            assertNotNull(em.find(Course.class, courseId));
        });
    }

    /** Проверяет защиту решения при наличии отписанной записи на курс. */
    @Test
    void preservesHistoryAfterDrop() {
        createFixture(true, false, false);
        historyType = History.SUBMISSION;
        Long enrollmentId = transactions.execute(status -> {
            User student = em.find(User.class, studentId);
            Enrollment enrollment = new Enrollment(student, em.find(Course.class, courseId),
                    LocalDateTime.now(), EnrollmentStatus.DROPPED);
            em.persist(enrollment);
            Submission submission = new Submission(em.find(Assignment.class, assignmentId), student, "Решение");
            em.persist(submission);
            historyId = submission.getId();
            return enrollment.getId();
        });
        try {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            assertThrows(IllegalStateException.class, () -> service.deleteCourse(courseId));
            transactions.executeWithoutResult(status -> {
                assertNotNull(em.find(Course.class, courseId));
                assertNotNull(em.find(Enrollment.class, enrollmentId));
                assertNotNull(em.find(Submission.class, historyId));
            });
        } finally {
            transactions.executeWithoutResult(status -> delete("enrollments", enrollmentId));
        }
    }

    /** Проверяет отсутствие созданной структуры и сохранность преподавателя и категории в новой транзакции. */
    private void assertDeleted() {
        transactions.executeWithoutResult(status -> {
            assertNull(em.find(Course.class, courseId));
            assertNull(em.find(Module.class, moduleId));
            if (lessonId != null) assertNull(em.find(Lesson.class, lessonId));
            if (assignmentId != null) assertNull(em.find(Assignment.class, assignmentId));
            if (quizId != null) assertNull(em.find(Quiz.class, quizId));
            if (questionId != null) assertNull(em.find(Question.class, questionId));
            if (optionId != null) assertNull(em.find(AnswerOption.class, optionId));
            assertNotNull(em.find(User.class, teacherId));
            assertNotNull(em.find(Category.class, categoryId));
        });
    }

    /** Проверяет сохранность курса и истории каждого вида после отказа в удалении.
     * @param history вид учебной истории
     */
    @ParameterizedTest
    @EnumSource(History.class)
    void rejectsDeletionAndPreservesHistory(History history) {
        createFixture(true, true, false);
        historyType = history;
        transactions.executeWithoutResult(status -> {
            Course course = em.find(Course.class, courseId);
            User student = em.find(User.class, studentId);
            Object row = switch (history) {
                case ACTIVE, DROPPED -> new Enrollment(student, course, LocalDateTime.now(),
                        history == History.ACTIVE ? EnrollmentStatus.ACTIVE : EnrollmentStatus.DROPPED);
                case SUBMISSION -> new Submission(em.find(Assignment.class, assignmentId), student, "Решение");
                case QUIZ_RESULT -> new QuizSubmission(em.find(Quiz.class, quizId), student, 80, LocalDateTime.now());
                case REVIEW -> new CourseReview(5, "Отзыв", LocalDateTime.now(), course, student);
            };
            em.persist(row);
            em.flush();
            historyId = (Long) em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(row);
        });
        assertTrue(AopUtils.isAopProxy(service));
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.deleteCourse(courseId));
        String expected = switch (history) {
            case ACTIVE, DROPPED -> "записи";
            case SUBMISSION -> "решения";
            case QUIZ_RESULT -> "результаты";
            case REVIEW -> "отзывы";
        };
        assertTrue(error.getMessage().contains(expected));
        transactions.executeWithoutResult(status -> {
            assertNotNull(em.find(Course.class, courseId));
            assertNotNull(em.find(Module.class, moduleId));
            assertNotNull(em.find(Lesson.class, lessonId));
            assertNotNull(em.find(Assignment.class, assignmentId));
            assertNotNull(em.find(Quiz.class, quizId));
            assertNotNull(em.find(Question.class, questionId));
            assertNotNull(em.find(AnswerOption.class, optionId));
            Class<?> type = switch (history) {
                case ACTIVE, DROPPED -> Enrollment.class;
                case SUBMISSION -> Submission.class;
                case QUIZ_RESULT -> QuizSubmission.class;
                case REVIEW -> CourseReview.class;
            };
            assertNotNull(em.find(type, historyId));
        });
    }
}
