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
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.AnswerOptionRepository;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.EnrollmentRepository;
import com.studying.platform.repository.ModuleRepository;
import com.studying.platform.repository.QuizRepository;
import com.studying.platform.repository.QuizSubmissionRepository;
import com.studying.platform.repository.QuestionRepository;
import com.studying.platform.repository.UserRepository;
import com.studying.platform.service.QuizSubmissionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для сущности «Результат прохождения теста» учебной платформы.
 * <p>
 * Проверяют бизнес-логику прохождения викторин, вычисление баллов,
 * ограничения записи на курс и множественные попытки.
 * Тесты выполняются с PostgreSQL через Testcontainers в профиле «test».
 * </p>
 *
 * @see QuizSubmission
 * @see QuizSubmissionService
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Import(QuizSubmissionService.class)
class QuizSubmissionTests {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private QuizSubmissionRepository quizSubmissionRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerOptionRepository answerOptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private QuizSubmissionService quizSubmissionService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ==================== Позитивные сценарии ====================

    /**
     * T1: Викторина с SINGLE_CHOICE, правильный ответ → score = 100.
     */
    @Test
    void submitAnswers_singleChoice_correct() {
        Category category = createCategory("Математика");
        User teacher = createTeacher("teacher.sc1@example.com");
        Course course = createCourse("Алгебра", teacher, category);
        Module module = createModule("Основы", course, 1);
        Quiz quiz = createQuiz(module, "Тест по основам", 30);
        Question question = createQuestion(quiz, "Сколько будет 2+2?", QuizQuestionType.SINGLE_CHOICE);
        AnswerOption correct = createAnswerOption(question, "4", true);

        User student = createStudent("student.sc1@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        QuizSubmission submission = quizSubmissionService.submitAnswers(
                quiz.getId(), student.getId(),
                Map.of(question.getId(), Set.of(correct.getId())));

        assertEquals(100, submission.getScore(), "Балл должен быть 100 при правильном ответе");
    }

    /**
     * T2: Викторина с MULTIPLE_CHOICE (2 правильных из 4), точное совпадение → score = 100.
     */
    @Test
    void submitAnswers_multipleChoice_exactMatch() {
        Category category = createCategory("География");
        User teacher = createTeacher("teacher.mc1@example.com");
        Course course = createCourse("Страны мира", teacher, category);
        Module module = createModule("Столицы", course, 1);
        Quiz quiz = createQuiz(module, "Столицы", 20);
        Question question = createQuestion(quiz, "Столицы России и Франции?", QuizQuestionType.MULTIPLE_CHOICE);
        AnswerOption correct1 = createAnswerOption(question, "Москва", true);
        AnswerOption correct2 = createAnswerOption(question, "Париж", true);
        createAnswerOption(question, "Лондон", false);
        createAnswerOption(question, "Берлин", false);

        User student = createStudent("student.mc1@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        QuizSubmission submission = quizSubmissionService.submitAnswers(
                quiz.getId(), student.getId(),
                Map.of(question.getId(), Set.of(correct1.getId(), correct2.getId())));

        assertEquals(100, submission.getScore(), "Точное совпадение множеств должно дать 100 баллов");
    }

    /**
     * T3: Смешанные вопросы, 2 правильных из 3 → score = 66 (целочисленное деление).
     */
    @Test
    void submitAnswers_mixedQuestions_partialScore() {
        Category category = createCategory("Физика");
        User teacher = createTeacher("teacher.mix1@example.com");
        Course course = createCourse("Механика", teacher, category);
        Module module = createModule("Законы", course, 1);
        Quiz quiz = createQuiz(module, "Законы Ньютона", 45);

        Question q1 = createQuestion(quiz, "Первый закон?", QuizQuestionType.SINGLE_CHOICE);
        createAnswerOption(q1, "Инерция", true);
        Question q2 = createQuestion(quiz, "Второй закон?", QuizQuestionType.SINGLE_CHOICE);
        createAnswerOption(q2, "F=ma", true);
        Question q3 = createQuestion(quiz, "Третий закон?", QuizQuestionType.MULTIPLE_CHOICE);
        createAnswerOption(q3, "Действие", true);
        createAnswerOption(q3, "Противодействие", true);

        User student = createStudent("student.mix1@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        Long correctQ1Id = getAnswerOptionByText(q1, "Инерция").getId();
        QuizSubmission submission = quizSubmissionService.submitAnswers(
                quiz.getId(), student.getId(),
                Map.of(
                        q1.getId(), Set.of(correctQ1Id),
                        q2.getId(), Set.of(getAnswerOptionByText(q2, "F=ma").getId()),
                        q3.getId(), Set.of()
                ));
        
        // 2 correct out of 3 = 66
        assertEquals(66, submission.getScore(), "2 правильных из 3 вопросов должны дать 66 баллов");
    }

    private AnswerOption getAnswerOptionByText(Question question, String text) {
        return answerOptionRepository.findByQuestionId(question.getId()).stream()
                .filter(o -> o.getText().equals(text))
                .findFirst()
                .orElseThrow();
    }

    /**
     * T4: Все ответы правильные → score = 100.
     */
    @Test
    void submitAnswers_allCorrectFullScore() {
        Category category = createCategory("Химия");
        User teacher = createTeacher("teacher.allcorrect@example.com");
        Course course = createCourse("Органика", teacher, category);
        Module module = createModule("Алканы", course, 1);
        Quiz quiz = createQuiz(module, "Тест по алканам", 15);

        Question q1 = createQuestion(quiz, "Формула метана?", QuizQuestionType.SINGLE_CHOICE);
        AnswerOption a1 = createAnswerOption(q1, "CH4", true);
        Question q2 = createQuestion(quiz, "Тип связи?", QuizQuestionType.SINGLE_CHOICE);
        AnswerOption a2 = createAnswerOption(q2, "Ковалентная", true);
        Question q3 = createQuestion(quiz, "Гибридизация?", QuizQuestionType.MULTIPLE_CHOICE);
        AnswerOption a3 = createAnswerOption(q3, "sp3", true);

        User student = createStudent("student.allcorrect@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        QuizSubmission submission = quizSubmissionService.submitAnswers(
                quiz.getId(), student.getId(),
                Map.of(
                        q1.getId(), Set.of(a1.getId()),
                        q2.getId(), Set.of(a2.getId()),
                        q3.getId(), Set.of(a3.getId())
                ));

        assertEquals(100, submission.getScore(), "Все ответы правильные → 100 баллов");
    }

    /**
     * T5: Множественные попытки → создаются новые записи.
     */
    @Test
    void submitAnswers_multipleAttempts_newRecords() {
        Category category = createCategory("История");
        User teacher = createTeacher("teacher.multi@example.com");
        Course course = createCourse("Древний Рим", teacher, category);
        Module module = createModule("Республика", course, 1);
        Quiz quiz = createQuiz(module, "Тест по Республике", 20);
        Question question = createQuestion(quiz, "Когда основан Рим?", QuizQuestionType.SINGLE_CHOICE);
        createAnswerOption(question, "753 до н.э.", true);

        User student = createStudent("student.multi@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        Map<Long, Set<Long>> answers = Map.of(question.getId(), Set.of(getAnswerOptionByText(question, "753 до н.э.").getId()));

        quizSubmissionService.submitAnswers(quiz.getId(), student.getId(), answers);
        quizSubmissionService.submitAnswers(quiz.getId(), student.getId(), answers);

        List<QuizSubmission> submissions = quizSubmissionRepository.findByStudentId(student.getId());
        assertEquals(2, submissions.size(), "Должно быть создано 2 отдельные записи результатов");
    }

    /**
     * T6: findByStudentId возвращает результаты.
     */
    @Test
    void findByStudentId_returnsResults() {
        Category category = createCategory("Биология");
        User teacher = createTeacher("teacher.findstud@example.com");
        Course course = createCourse("Ботаника", teacher, category);
        Module module = createModule("Растения", course, 1);
        Quiz quiz = createQuiz(module, "Тест по растениям", 30);
        Question question = createQuestion(quiz, "Фотосинтез?", QuizQuestionType.SINGLE_CHOICE);
        createAnswerOption(question, "Процесс", true);

        User student = createStudent("student.findstud@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        quizSubmissionService.submitAnswers(quiz.getId(), student.getId(),
                Map.of(question.getId(), Set.of(getAnswerOptionByText(question, "Процесс").getId())));
        quizSubmissionService.submitAnswers(quiz.getId(), student.getId(), Map.of());

        List<QuizSubmission> results = quizSubmissionRepository.findByStudentId(student.getId());
        assertEquals(2, results.size(), "findByStudentId должен вернуть все результаты студента");
    }

    /**
     * T7: findByQuizId возвращает результаты.
     */
    @Test
    void findByQuizId_returnsResults() {
        Category category = createCategory("Философия");
        User teacher = createTeacher("teacher.findquiz@example.com");
        Course course = createCourse("Этика", teacher, category);
        Module module = createModule("Аристотель", course, 1);
        Quiz quiz = createQuiz(module, "Никомахова этика", 60);
        Question question = createQuestion(quiz, "Цель жизни?", QuizQuestionType.SINGLE_CHOICE);
        createAnswerOption(question, "Эвдемония", true);

        User student1 = createStudent("s1.findquiz@example.com");
        User student2 = createStudent("s2.findquiz@example.com");
        createEnrollment(student1, course, EnrollmentStatus.ACTIVE);
        createEnrollment(student2, course, EnrollmentStatus.ACTIVE);

        Map<Long, Set<Long>> answers = Map.of(question.getId(), Set.of(getAnswerOptionByText(question, "Эвдемония").getId()));
        quizSubmissionService.submitAnswers(quiz.getId(), student1.getId(), answers);
        quizSubmissionService.submitAnswers(quiz.getId(), student1.getId(), answers);
        quizSubmissionService.submitAnswers(quiz.getId(), student2.getId(), answers);

        List<QuizSubmission> results = quizSubmissionRepository.findByQuizId(quiz.getId());
        assertEquals(3, results.size(), "findByQuizId должен вернуть все результаты по викторине");
    }

    /**
     * T8: Проверка сохранения score через JPA и прямой SQL.
     */
    @Test
    void submitAnswers_textStorageViaJpaAndSql() {
        Category category = createCategory("Информатика");
        User teacher = createTeacher("teacher.sqlcheck@example.com");
        Course course = createCourse("Java Core", teacher, category);
        Module module = createModule("ООП", course, 1);
        Quiz quiz = createQuiz(module, "Вопросы по ООП", 10);
        Question question = createQuestion(quiz, "Полиморфизм?", QuizQuestionType.SINGLE_CHOICE);
        createAnswerOption(question, "Множественность форм", true);

        User student = createStudent("student.sqlcheck@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        QuizSubmission submission = quizSubmissionService.submitAnswers(
                quiz.getId(), student.getId(),
                Map.of(question.getId(), Set.of(getAnswerOptionByText(question, "Множественность форм").getId())));

        Integer jpaScore = submission.getScore();

        Integer sqlScore = jdbcTemplate.queryForObject(
                "SELECT score FROM quizsubmission WHERE id = ?",
                Integer.class, submission.getId());

        assertEquals(jpaScore, sqlScore, "Балл, прочитанный через JPA и SQL, должен совпадать");
    }

    // ==================== Негативные сценарии ====================

    /**
     * N1: Несуществующая викторина → ResourceNotFoundException.
     */
    @Test
    void submitAnswers_nonExistentQuiz_throws() {
        assertThrows(ResourceNotFoundException.class, () ->
                quizSubmissionService.submitAnswers(99999L, 1L, Map.of()));
    }

    /**
     * N2: Студент не записан на курс → ResourceNotFoundException.
     */
    @Test
    void submitAnswers_studentNotEnrolled_throws() {
        Category category = createCategory("Психология");
        User teacher = createTeacher("teacher.noenroll@example.com");
        Course course = createCourse("Общая психология", teacher, category);
        Module module = createModule("Память", course, 1);
        Quiz quiz = createQuiz(module, "Тест по памяти", 20);
        Question question = createQuestion(quiz, "Виды памяти?", QuizQuestionType.SINGLE_CHOICE);
        createAnswerOption(question, "Разные виды", true);

        User student = createStudent("noenroll@example.com");
        // Студент НЕ записан на курс

        assertThrows(ResourceNotFoundException.class, () ->
                quizSubmissionService.submitAnswers(quiz.getId(), student.getId(),
                        Map.of(question.getId(), Set.of(getAnswerOptionByText(question, "Разные виды").getId()))));
    }

    /**
     * N3: Студент отписался (DROPPED) → ResourceNotFoundException.
     */
    @Test
    void submitAnswers_afterDropEnrollment_throws() {
        Category category = createCategory("Социология");
        User teacher = createTeacher("teacher.dropped@example.com");
        Course course = createCourse("Соц. структуры", teacher, category);
        Module module = createModule("Группы", course, 1);
        Quiz quiz = createQuiz(module, "Тест по группам", 15);
        Question question = createQuestion(quiz, "Типы групп?", QuizQuestionType.SINGLE_CHOICE);
        createAnswerOption(question, "Первичные и вторичные", true);

        User student = createStudent("dropped@example.com");
        Enrollment enrollment = createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        // Отписка
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        entityManager.merge(enrollment);
        entityManager.flush();

        assertThrows(ResourceNotFoundException.class, () ->
                quizSubmissionService.submitAnswers(quiz.getId(), student.getId(),
                        Map.of(question.getId(), Set.of(getAnswerOptionByText(question, "Первичные и вторичные").getId()))));
    }

    /**
     * N4: SINGLE_CHOICE, неправильный ответ → score = 0 для этого вопроса.
     */
    @Test
    void submitAnswers_singleChoice_wrongAnswer() {
        Category category = createCategory("Астрономия");
        User teacher = createTeacher("teacher.wrong.sc@example.com");
        Course course = createCourse("Космос", teacher, category);
        Module module = createModule("Планеты", course, 1);
        Quiz quiz = createQuiz(module, "Планеты Солнечной системы", 10);
        Question question = createQuestion(quiz, "Самая большая планета?", QuizQuestionType.SINGLE_CHOICE);
        createAnswerOption(question, "Юпитер", true);
        AnswerOption wrong = createAnswerOption(question, "Сатурн", false);

        User student = createStudent("wrong.sc@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        QuizSubmission submission = quizSubmissionService.submitAnswers(
                quiz.getId(), student.getId(),
                Map.of(question.getId(), Set.of(wrong.getId())));

        assertEquals(0, submission.getScore(), "Неправильный ответ должен дать 0 баллов");
    }

    /**
     * N5: MULTIPLE_CHOICE, выбран 1 из 2 правильных → score = 0.
     */
    @Test
    void submitAnswers_multipleChoice_missingCorrect() {
        Category category = createCategory("Биология");
        User teacher = createTeacher("teacher.mc.missing@example.com");
        Course course = createCourse("Генетика", teacher, category);
        Module module = createModule("Законы", course, 1);
        Quiz quiz = createQuiz(module, "Законы Менделя", 20);
        Question question = createQuestion(quiz, "Законы Менделя?", QuizQuestionType.MULTIPLE_CHOICE);
        createAnswerOption(question, "Единообразие", true);
        createAnswerOption(question, "Расщепление", true);
        createAnswerOption(question, "Независимость", false);

        User student = createStudent("mc.missing@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        QuizSubmission submission = quizSubmissionService.submitAnswers(
                quiz.getId(), student.getId(),
                Map.of(question.getId(), Set.of(getAnswerOptionByText(question, "Единообразие").getId())));

        assertEquals(0, submission.getScore(), "Частичное совпадение не должно засчитываться");
    }

    /**
     * N6: MULTIPLE_CHOICE, выбраны все правильные + один неправильный → score = 0.
     */
    @Test
    void submitAnswers_multipleChoice_extraWrong() {
        Category category = createCategory("Физика");
        User teacher = createTeacher("teacher.mc.extra@example.com");
        Course course = createCourse("Кванты", teacher, category);
        Module module = createModule("Фотон", course, 1);
        Quiz quiz = createQuiz(module, "Свойства фотона", 15);
        Question question = createQuestion(quiz, "Свойства фотона?", QuizQuestionType.MULTIPLE_CHOICE);
        createAnswerOption(question, "Нет массы", true);
        createAnswerOption(question, "Скорость света", true);
        createAnswerOption(question, "Имеет заряд", false);

        User student = createStudent("mc.extra@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        Long correct1Id = getAnswerOptionByText(question, "Нет массы").getId();
        Long correct2Id = getAnswerOptionByText(question, "Скорость света").getId();
        Long wrongId = getAnswerOptionByText(question, "Имеет заряд").getId();

        QuizSubmission submission = quizSubmissionService.submitAnswers(
                quiz.getId(), student.getId(),
                Map.of(question.getId(), Set.of(correct1Id, correct2Id, wrongId)));

        assertEquals(0, submission.getScore(), "Лишний неправильный вариант должен обнулить балл");
    }

    /**
     * N7: Указан ID варианта, не принадлежащего викторине → вопрос не засчитан.
     */
    @Test
    void submitAnswers_foreignOptionId() {
        Category category = createCategory("Химия");
        User teacher = createTeacher("teacher.foreign@example.com");
        Course course = createCourse("Неорганика", teacher, category);
        Module module = createModule("Металлы", course, 1);
        Quiz quiz = createQuiz(module, "Металлы", 10);
        Question question = createQuestion(quiz, "Самый реактивный металл?", QuizQuestionType.SINGLE_CHOICE);
        createAnswerOption(question, "Цезий", true);

        User student = createStudent("foreign@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        // Используем заведомо несуществующий ID варианта
        QuizSubmission submission = quizSubmissionService.submitAnswers(
                quiz.getId(), student.getId(),
                Map.of(question.getId(), Set.of(99999L)));

        assertEquals(0, submission.getScore(), "Чужой ID варианта не должен засчитаться");
    }

    /**
     * N8: answers = null → IllegalArgumentException.
     */
    @Test
    void submitAnswers_nullAnswersMap_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                quizSubmissionService.submitAnswers(1L, 1L, null));
    }

    /**
     * N9: Студент не найден → ResourceNotFoundException.
     */
    @Test
    void submitAnswers_nonExistentStudent_throws() {
        Category category = createCategory("Литература");
        User teacher = createTeacher("teacher.nostudent@example.com");
        Course course = createCourse("Русская литература", teacher, category);
        Module module = createModule("Пушкин", course, 1);
        Quiz quiz = createQuiz(module, "Евгений Онегин", 30);
        Question question = createQuestion(quiz, "Кто автор?", QuizQuestionType.SINGLE_CHOICE);
        createAnswerOption(question, "Пушкин", true);

        assertThrows(ResourceNotFoundException.class, () ->
                quizSubmissionService.submitAnswers(quiz.getId(), 99999L,
                        Map.of(question.getId(), Set.of(getAnswerOptionByText(question, "Пушкин").getId()))));
    }

    /**
     * N10: Прямое сохранение с quiz = null → ConstraintViolationException.
     */
    @Test
    void quizSubmission_nullQuiz_throws() {
        User student = createStudent("student.nq@example.com");
        QuizSubmission submission = new QuizSubmission();
        submission.setStudent(student);
        submission.setScore(100);
        submission.setTakenAt(LocalDateTime.now());

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(submission);
            entityManager.flush();
        });
    }

    /**
     * N11: Прямое сохранение с student = null → ConstraintViolationException.
     */
    @Test
    void quizSubmission_nullStudent_throws() {
        Quiz quiz = createQuiz(createModule("Модуль для теста", createCourse("Курс", createTeacher("t.nulls@example.com"), createCategory("Категория")), 1), "Тест", 10);
        Question question = createQuestion(quiz, "Вопрос?", QuizQuestionType.SINGLE_CHOICE);
        createAnswerOption(question, "Ответ", true);

        QuizSubmission submission = new QuizSubmission();
        submission.setQuiz(quiz);
        submission.setScore(100);
        submission.setTakenAt(LocalDateTime.now());

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(submission);
            entityManager.flush();
        });
    }

    /**
     * N12: Прямое сохранение с score = null → ConstraintViolationException.
     */
    @Test
    void quizSubmission_nullScore_throws() {
        User student = createStudent("student.nscore@example.com");
        Quiz quiz = createQuiz(createModule("Модуль", createCourse("Курс", createTeacher("t.nsc@example.com"), createCategory("Кат")), 1), "Тест", 10);

        QuizSubmission submission = new QuizSubmission();
        submission.setQuiz(quiz);
        submission.setStudent(student);
        submission.setTakenAt(LocalDateTime.now());

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(submission);
            entityManager.flush();
        });
    }

    /**
     * N13: Прямое сохранение с takenAt = null → ConstraintViolationException.
     */
    @Test
    void quizSubmission_nullTakenAt_throws() {
        User student = createStudent("student.nta@example.com");
        Quiz quiz = createQuiz(createModule("Модуль", createCourse("Курс", createTeacher("t.nta@example.com"), createCategory("Кат")), 1), "Тест", 10);

        QuizSubmission submission = new QuizSubmission();
        submission.setQuiz(quiz);
        submission.setStudent(student);
        submission.setScore(100);

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(submission);
            entityManager.flush();
        });
    }

    // ==================== Проверка сохранения ====================

    /**
     * S1: Результат сохраняется в БД и доступен через репозиторий.
     */
    @Test
    void submitAnswers_savesResult() {
        Category category = createCategory("Математика");
        User teacher = createTeacher("teacher.save@example.com");
        Course course = createCourse("Геометрия", teacher, category);
        Module module = createModule("Планиметрия", course, 1);
        Quiz quiz = createQuiz(module, "Треугольники", 20);
        Question question = createQuestion(quiz, "Сумма углов?", QuizQuestionType.SINGLE_CHOICE);
        createAnswerOption(question, "180", true);

        User student = createStudent("student.save@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        QuizSubmission submission = quizSubmissionService.submitAnswers(
                quiz.getId(), student.getId(),
                Map.of(question.getId(), Set.of(getAnswerOptionByText(question, "180").getId())));

        QuizSubmission loaded = quizSubmissionRepository.findById(submission.getId()).orElseThrow();
        assertNotNull(loaded.getId(), "Идентификатор должен быть присвоен");
        assertEquals(100, loaded.getScore(), "Балл должен совпадать");
    }

    /**
     * S2: takenAt не null и в пределах текущей минуты.
     */
    @Test
    void submitAnswers_takenAtIsCurrent() {
        Category category = createCategory("История");
        User teacher = createTeacher("teacher.time@example.com");
        Course course = createCourse("Вторая мировая", teacher, category);
        Module module = createModule("1945 год", course, 1);
        Quiz quiz = createQuiz(module, "Итоги войны", 30);
        Question question = createQuestion(quiz, "Дата окончания?", QuizQuestionType.SINGLE_CHOICE);
        createAnswerOption(question, "1945", true);

        User student = createStudent("student.time@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        LocalDateTime before = LocalDateTime.now();
        QuizSubmission submission = quizSubmissionService.submitAnswers(
                quiz.getId(), student.getId(),
                Map.of(question.getId(), Set.of(getAnswerOptionByText(question, "1945").getId())));
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(submission.getTakenAt(), "takenAt не должен быть null");
        assertTrue(submission.getTakenAt().isAfter(before) || submission.getTakenAt().isEqual(before),
                "takenAt должен быть не раньше начала теста");
        assertTrue(submission.getTakenAt().isBefore(after) || submission.getTakenAt().isEqual(after),
                "takenAt должен быть не позже конца теста");
    }

    // ==================== Вспомогательные методы ====================

    private Category createCategory(String name) {
        Category category = new Category(name);
        entityManager.persist(category);
        entityManager.flush();
        return category;
    }

    private User createTeacher(String email) {
        User user = new User(email, UserRole.TEACHER);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private User createStudent(String email) {
        User user = new User(email, UserRole.STUDENT);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private Course createCourse(String title, User teacher, Category category) {
        Course course = new Course(title, "Описание", 40, java.time.LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();
        return course;
    }

    private Module createModule(String title, Course course, Integer orderIndex) {
        Module module = new Module();
        module.setTitle(title);
        module.setOrderIndex(orderIndex);
        module.setCourse(course);
        entityManager.persist(module);
        entityManager.flush();
        return module;
    }

    private Quiz createQuiz(Module module, String title, Integer timeLimit) {
        Quiz quiz = new Quiz(module, title, timeLimit);
        entityManager.persist(quiz);
        entityManager.flush();
        return quiz;
    }

    private Question createQuestion(Quiz quiz, String text, QuizQuestionType type) {
        Question question = new Question(quiz, text, type);
        quiz.getQuestions().add(question);
        entityManager.persist(question);
        entityManager.flush();
        return question;
    }

    private AnswerOption createAnswerOption(Question question, String text, Boolean isCorrect) {
        AnswerOption option = new AnswerOption(question, text, isCorrect);
        question.getOptions().add(option);
        entityManager.persist(option);
        entityManager.flush();
        return option;
    }

    private Enrollment createEnrollment(User user, Course course, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setEnrollDate(LocalDateTime.now());
        enrollment.setStatus(status);
        entityManager.persist(enrollment);
        entityManager.flush();
        return enrollment;
    }
}
