package com.studying.platform;

import com.studying.platform.entity.AnswerOption;
import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Enrollment;
import com.studying.platform.entity.EnrollmentStatus;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.Question;
import com.studying.platform.entity.Quiz;
import com.studying.platform.entity.QuizQuestionType;
import com.studying.platform.entity.QuizSubmission;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.service.QuizSubmissionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Проверяет готовность теста и сохранение результатов в отдельных транзакциях PostgreSQL. */
@SpringBootTest
@ActiveProfiles("test")
class QuizReadinessTransactionTests {
    /** Создаёт экземпляр тестов готовности для JUnit. */
    QuizReadinessTransactionTests() {
    }

    @Autowired private QuizSubmissionService service;
    @Autowired private EntityManager em;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;
    private Long teacherId, studentId, categoryId, courseId, moduleId, quizId, enrollmentId;
    private Long otherModuleId, otherQuizId;
    private final List<Long> questions = new ArrayList<>();
    private final List<Long> options = new ArrayList<>();
    private final List<Long> results = new ArrayList<>();

    /** Незавершённые структуры, запрещённые для прохождения. */
    enum InvalidStructure { NO_QUESTIONS, NO_OPTIONS, SINGLE_NO_CORRECT, SINGLE_TWO_CORRECT, MULTIPLE_NO_CORRECT }

    /** Создаёт и подтверждает курс, пустой тест и активную запись студента. */
    @BeforeEach
    void setUp() {
        transactions.executeWithoutResult(status -> {
            String unique = UUID.randomUUID().toString();
            User teacher = new User("t" + unique + "@test.io", UserRole.TEACHER);
            User student = new User("s" + unique + "@test.io", UserRole.STUDENT);
            Category category = new Category(unique);
            em.persist(teacher); teacherId = teacher.getId();
            em.persist(student); studentId = student.getId();
            em.persist(category); categoryId = category.getId();
            Course course = new Course(unique, "Описание", 10, LocalDate.now(), teacher, category);
            em.persist(course); courseId = course.getId();
            Module module = new Module("Модуль", "Описание", 1, course);
            em.persist(module); moduleId = module.getId();
            Quiz quiz = new Quiz(module, "Тест", 10);
            em.persist(quiz); quizId = quiz.getId();
            Enrollment enrollment = new Enrollment(student, course, LocalDateTime.now(), EnrollmentStatus.ACTIVE);
            em.persist(enrollment); enrollmentId = enrollment.getId();
        });
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    }

    /** Удаляет созданные тестом строки по идентификаторам в порядке зависимостей. */
    @AfterEach
    void cleanUp() {
        transactions.executeWithoutResult(status -> {
            results.forEach(id -> delete("quizsubmission", id));
            options.forEach(id -> delete("answeroption", id));
            questions.forEach(id -> delete("question", id));
            delete("quiz", otherQuizId);
            delete("quiz", quizId);
            delete("enrollments", enrollmentId);
            delete("modules", otherModuleId);
            delete("modules", moduleId);
            delete("courses", courseId);
            delete("categories", categoryId);
            delete("users", studentId);
            delete("users", teacherId);
        });
    }

    /** Удаляет строку созданной фикстуры по идентификатору.
     * @param table таблица фикстуры
     * @param id идентификатор строки
     */
    private void delete(String table, Long id) {
        if (id != null) jdbc.update("delete from " + table + " where id = ?", id);
    }

    /** Добавляет вопрос в отдельной подтверждённой транзакции.
     * @param type тип вопроса
     * @return идентификатор вопроса
     */
    private Long addQuestion(QuizQuestionType type) {
        return transactions.execute(status -> {
            Question question = new Question(em.find(Quiz.class, quizId), "Вопрос", type);
            em.persist(question);
            questions.add(question.getId());
            return question.getId();
        });
    }

    /** Добавляет вариант в отдельной подтверждённой транзакции.
     * @param questionId идентификатор вопроса
     * @param correct признак правильности
     * @return идентификатор варианта
     */
    private Long addOption(Long questionId, boolean correct) {
        return transactions.execute(status -> {
            AnswerOption option = new AnswerOption(em.find(Question.class, questionId), "Вариант", correct);
            em.persist(option);
            options.add(option.getId());
            return option.getId();
        });
    }

    /** Проверяет каждый вид незавершённой структуры и отсутствие результата после отказа.
     * @param structure вид некорректной структуры
     */
    @ParameterizedTest
    @EnumSource(InvalidStructure.class)
    void rejectsIncompleteStructureWithoutSavingResult(InvalidStructure structure) {
        if (structure != InvalidStructure.NO_QUESTIONS) {
            Long question = addQuestion(structure == InvalidStructure.MULTIPLE_NO_CORRECT
                    ? QuizQuestionType.MULTIPLE_CHOICE : QuizQuestionType.SINGLE_CHOICE);
            if (structure != InvalidStructure.NO_OPTIONS) {
                addOption(question, structure == InvalidStructure.SINGLE_TWO_CORRECT);
            }
            if (structure == InvalidStructure.SINGLE_TWO_CORRECT) addOption(question, true);
        }
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.submitAnswers(quizId, studentId, Map.of()));
        assertNotNull(error.getMessage());
        transactions.executeWithoutResult(status -> assertEquals(0L, countResults()));
    }

    /** Проверяет сохранение неполного вопроса, его дополнение и успешное прохождение обоих типов. */
    @Test
    void incompleteStructureCanBeCompletedAndPassed() {
        Long single = addQuestion(QuizQuestionType.SINGLE_CHOICE);
        assertThrows(IllegalStateException.class, () -> service.submitAnswers(quizId, studentId, Map.of()));
        transactions.executeWithoutResult(status -> {
            assertNotNull(em.find(Question.class, single));
            assertEquals(0L, countResults());
        });
        Long one = addOption(single, true);
        Long multiple = addQuestion(QuizQuestionType.MULTIPLE_CHOICE);
        Long two = addOption(multiple, true);
        Long three = addOption(multiple, true);
        addOption(multiple, false);
        QuizSubmission result = service.submitAnswers(quizId, studentId, Map.of(single, Set.of(one), multiple, Set.of(two, three)));
        results.add(result.getId());
        transactions.executeWithoutResult(status -> {
            assertEquals(100, em.find(QuizSubmission.class, result.getId()).getScore());
            assertEquals(1L, countResults());
        });
    }

    /** Проверяет все вопросы, включая отсутствующий в ответах некорректный вопрос. */
    @Test
    void rejectsInvalidUnansweredQuestion() {
        Long valid = addQuestion(QuizQuestionType.SINGLE_CHOICE);
        Long option = addOption(valid, true);
        addQuestion(QuizQuestionType.MULTIPLE_CHOICE);
        assertThrows(IllegalStateException.class,
                () -> service.submitAnswers(quizId, studentId, Map.of(valid, Set.of(option))));
        transactions.executeWithoutResult(status -> assertEquals(0L, countResults()));
    }

    /** Проверяет игнорирование чужого вопроса и незачёт варианта другого вопроса. */
    @Test
    void ignoresForeignQuestionAndRejectsForeignOptionAsAnswer() {
        Long first = addQuestion(QuizQuestionType.SINGLE_CHOICE);
        Long firstOption = addOption(first, true);
        Long second = addQuestion(QuizQuestionType.SINGLE_CHOICE);
        Long secondOption = addOption(second, true);
        Long foreignQuestion = transactions.execute(status -> {
            Module otherModule = new Module("Другой модуль", "Описание", 2, em.find(Course.class, courseId));
            em.persist(otherModule); otherModuleId = otherModule.getId();
            Quiz otherQuiz = new Quiz(otherModule, "Другой тест", 10);
            em.persist(otherQuiz); otherQuizId = otherQuiz.getId();
            Question question = new Question(otherQuiz, "Чужой вопрос", QuizQuestionType.SINGLE_CHOICE);
            em.persist(question); questions.add(question.getId());
            return question.getId();
        });
        QuizSubmission correct = service.submitAnswers(quizId, studentId,
                Map.of(first, Set.of(firstOption), second, Set.of(secondOption), foreignQuestion, Set.of(firstOption)));
        results.add(correct.getId());
        QuizSubmission wrong = service.submitAnswers(quizId, studentId,
                Map.of(first, Set.of(secondOption), second, Set.of(secondOption)));
        results.add(wrong.getId());
        transactions.executeWithoutResult(status -> {
            assertEquals(100, em.find(QuizSubmission.class, correct.getId()).getScore());
            assertEquals(50, em.find(QuizSubmission.class, wrong.getId()).getScore());
            assertEquals(2L, countResults());
        });
    }

    /** Подсчитывает результаты только текущего теста.
     * @return количество сохранённых результатов
     */
    private long countResults() {
        return jdbc.queryForObject("select count(*) from quizsubmission where quiz_id = ?", Long.class, quizId);
    }
}
