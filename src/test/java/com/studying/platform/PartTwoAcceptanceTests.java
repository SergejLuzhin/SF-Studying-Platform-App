package com.studying.platform;

import com.studying.platform.entity.AnswerOption;
import com.studying.platform.entity.Category;
import com.studying.platform.entity.Enrollment;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.Question;
import com.studying.platform.entity.Quiz;
import com.studying.platform.entity.QuizQuestionType;
import com.studying.platform.entity.QuizSubmission;
import com.studying.platform.entity.Submission;
import com.studying.platform.entity.Tag;
import com.studying.platform.entity.UserRole;
import com.studying.platform.service.CourseService;
import com.studying.platform.service.EnrollmentService;
import com.studying.platform.service.QuizSubmissionService;
import com.studying.platform.service.SubmissionService;
import com.studying.platform.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Сквозная приёмка учебного сценария через реальные Spring-сервисы и PostgreSQL. */
@SpringBootTest
@ActiveProfiles("test")
class PartTwoAcceptanceTests {
    @Autowired private UserService users;
    @Autowired private CourseService courses;
    @Autowired private EnrollmentService enrollments;
    @Autowired private SubmissionService submissions;
    @Autowired private QuizSubmissionService quizzes;
    @Autowired private EntityManager em;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;

    private Long teacherId, studentId, categoryId, tagId, courseId, otherCourseId, enrollmentId, submissionId, resultId;
    private final List<Long> modules = new ArrayList<>();
    private final List<Long> lessons = new ArrayList<>();
    private final List<Long> assignments = new ArrayList<>();
    private final List<Long> quizIds = new ArrayList<>();
    private final List<Long> questionIds = new ArrayList<>();
    private final List<Long> optionIds = new ArrayList<>();

    /** Создаёт экземпляр сквозного теста для JUnit. */
    PartTwoAcceptanceTests() {
    }

    /** Удаляет только созданные тестом строки по идентификаторам в порядке зависимостей. */
    @AfterEach
    void cleanUp() {
        transactions.executeWithoutResult(status -> {
            delete("quizsubmission", resultId);
            delete("submissions", submissionId);
            delete("enrollments", enrollmentId);
            optionIds.forEach(id -> delete("answeroption", id));
            questionIds.forEach(id -> delete("question", id));
            quizIds.forEach(id -> delete("quiz", id));
            assignments.forEach(id -> delete("assignments", id));
            lessons.forEach(id -> delete("lessons", id));
            modules.forEach(id -> delete("modules", id));
            if (courseId != null) jdbc.update("delete from course_tag where course_id = ?", courseId);
            if (otherCourseId != null) jdbc.update("delete from course_tag where course_id = ?", otherCourseId);
            delete("courses", courseId);
            delete("courses", otherCourseId);
            delete("tags", tagId);
            delete("categories", categoryId);
            delete("users", studentId);
            delete("users", teacherId);
        });
    }

    /** Удаляет строку тестовой фикстуры по идентификатору.
     * @param table таблица
     * @param id идентификатор строки
     */
    private void delete(String table, Long id) {
        if (id != null) jdbc.update("delete from " + table + " where id = ?", id);
    }

    /** Создаёт подтверждённую структуру теста в модуле и возвращает её идентификаторы.
     * @param moduleId идентификатор модуля
     * @return идентификаторы теста, вопроса и правильного варианта
     */
    private List<Long> createQuiz(Long moduleId) {
        return transactions.execute(status -> {
            Quiz quiz = new Quiz(em.find(Module.class, moduleId), "Проверка знаний", 15);
            em.persist(quiz); quizIds.add(quiz.getId());
            Question question = new Question(quiz, "Столица Франции?", QuizQuestionType.SINGLE_CHOICE);
            em.persist(question); questionIds.add(question.getId());
            AnswerOption option = new AnswerOption(question, "Париж", true);
            em.persist(option); optionIds.add(option.getId());
            return List.of(quiz.getId(), question.getId(), option.getId());
        });
    }

    /** Проверяет учебную историю и удаление второго курса без истории после реальных commit. */
    @Test
    void fullLearningFlowAndDeletionProtection() {
        String unique = UUID.randomUUID().toString();
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        teacherId = users.createUser("t" + unique + "@test.io", UserRole.TEACHER).getId();
        studentId = users.createUser("s" + unique + "@test.io", UserRole.STUDENT).getId();
        transactions.executeWithoutResult(status -> {
            Category category = new Category(unique);
            em.persist(category); categoryId = category.getId();
            Tag tag = new Tag(unique);
            em.persist(tag); tagId = tag.getId();
        });
        courseId = courses.createCourse(unique, "Описание", 10, LocalDate.now(), teacherId, categoryId).getId();
        Long moduleId = courses.addModule(courseId, "Модуль", "Описание", 1).getId(); modules.add(moduleId);
        Long lessonId = courses.addLesson(moduleId, "Урок", "Текст", null).getId(); lessons.add(lessonId);
        Long assignmentId = courses.createAssignment(lessonId, "Задание", "Описание", LocalDate.now(), 100).getId();
        assignments.add(assignmentId);
        List<Long> quiz = createQuiz(moduleId);
        courses.addTagsToCourse(courseId, Set.of(tagId));
        Enrollment enrollment = enrollments.enrollStudent(studentId, courseId);
        enrollmentId = enrollment.getId();
        Submission submission = submissions.submitSolution(assignmentId, studentId, "Решение");
        submissionId = submission.getId();
        String feedback = "Отзыв преподавателя\n".repeat(20);
        submissions.gradeSubmission(submissionId, 90, feedback, teacherId);
        assertEquals(feedback, jdbc.queryForObject("select feedback from submissions where id = ?", String.class, submissionId));
        Quiz loaded = quizzes.startQuiz(quiz.get(0));
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertEquals("Проверка знаний", loaded.getTitle());
        assertEquals("Париж", loaded.getQuestions().iterator().next().getOptions().iterator().next().getText());
        QuizSubmission result = quizzes.submitAnswers(quiz.get(0), studentId, Map.of(quiz.get(1), Set.of(quiz.get(2))));
        resultId = result.getId();
        assertEquals(100, result.getScore());
        assertThrows(IllegalStateException.class, () -> courses.deleteCourse(courseId));
        transactions.executeWithoutResult(status -> {
            assertNotNull(em.find(Submission.class, submissionId));
            assertNotNull(em.find(QuizSubmission.class, resultId));
            assertNotNull(em.find(Enrollment.class, enrollmentId));
            assertNotNull(em.find(com.studying.platform.entity.Course.class, courseId));
        });

        otherCourseId = courses.createCourse("other-" + unique, "Описание", 10, LocalDate.now(), teacherId, categoryId).getId();
        Long otherModule = courses.addModule(otherCourseId, "Модуль", "Описание", 1).getId(); modules.add(otherModule);
        Long otherLesson = courses.addLesson(otherModule, "Урок", "Текст", null).getId(); lessons.add(otherLesson);
        Long otherAssignment = courses.createAssignment(otherLesson, "Задание", "Описание", LocalDate.now(), 100).getId();
        assignments.add(otherAssignment);
        List<Long> otherQuiz = createQuiz(otherModule);
        courses.addTagsToCourse(otherCourseId, Set.of(tagId));
        courses.deleteCourse(otherCourseId);
        transactions.executeWithoutResult(status -> {
            assertNull(em.find(com.studying.platform.entity.Course.class, otherCourseId));
            assertNull(em.find(Module.class, otherModule));
            assertNull(em.find(com.studying.platform.entity.Lesson.class, otherLesson));
            assertNull(em.find(com.studying.platform.entity.Assignment.class, otherAssignment));
            assertNull(em.find(Quiz.class, otherQuiz.get(0)));
            assertNull(em.find(Question.class, otherQuiz.get(1)));
            assertNull(em.find(AnswerOption.class, otherQuiz.get(2)));
            assertNotNull(em.find(Tag.class, tagId));
            assertNotNull(em.find(com.studying.platform.entity.User.class, teacherId));
            assertEquals(1, jdbc.queryForObject("select count(*) from course_tag where course_id = ? and tag_id = ?", Integer.class, courseId, tagId));
        });
    }
}
