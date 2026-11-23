package com.studying.platform;

import com.studying.platform.entity.Assignment;
import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Enrollment;
import com.studying.platform.entity.EnrollmentStatus;
import com.studying.platform.entity.Lesson;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.Submission;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.AssignmentRepository;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.EnrollmentRepository;
import com.studying.platform.repository.LessonRepository;
import com.studying.platform.repository.ModuleRepository;
import com.studying.platform.repository.SubmissionRepository;
import com.studying.platform.repository.UserRepository;
import com.studying.platform.service.SubmissionService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для сущности «Решение» учебной платформы.
 * <p>
 * Проверяют бизнес-логику отправки решений студентами через {@link SubmissionService},
 * а также корректность хранения и выборки через {@link SubmissionRepository}.
 * Тесты выполняются с PostgreSQL через Testcontainers в профиле «test».
 * Автоматический откат транзакции после каждого теста обеспечивает изоляцию.
 * </p>
 *
 * @see Submission
 * @see SubmissionService
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Import(SubmissionService.class)
class SubmissionTests {

    /**
     * Менеджер сущностей для ручного управления транзакциями.
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * Тестовый менеджер сущностей.
     */
    @Autowired
    private TestEntityManager testEntityManager;

    /**
     * Репозиторий для сущности Submission.
     */
    @Autowired
    private SubmissionRepository submissionRepository;

    /**
     * Репозиторий для сущности Assignment.
     */
    @Autowired
    private AssignmentRepository assignmentRepository;

    /**
     * Репозиторий для сущности Lesson.
     */
    @Autowired
    private LessonRepository lessonRepository;

    /**
     * Репозиторий для сущности Module.
     */
    @Autowired
    private ModuleRepository moduleRepository;

    /**
     * Репозиторий для сущности Course.
     */
    @Autowired
    private CourseRepository courseRepository;

    /**
     * Репозиторий для сущности Category.
     */
    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Репозиторий для сущности User.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Репозиторий для сущности Enrollment.
     */
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    /**
     * Сервис отправки решений.
     */
    @Autowired
    private SubmissionService submissionService;

    /**
     * Источник данных для проверки продукта базы данных.
     */
    @Autowired
    private DataSource dataSource;

    /**
     * JdbcTemplate для выполнения прямых SQL-запросов.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Тест: успешная отправка решения студентом.
     * <p>
     * Создаёт полный контекст: категорию, преподавателя, курс, модуль, урок, задание,
     * студента и запись на курс. Вызывает submitSolution, проверяет все поля.
     * </p>
     */
    @Test
    void submitSolution_shouldSaveSolution() {
        // Подготовка контекста
        Category category = createCategory("Программирование");
        User teacher = createTeacher("teacher.solution@example.com");
        Course course = createCourse("Основы Java", teacher, category);
        Module module = createModule("Модуль 1", course, 1);
        Lesson lesson = createLesson("Введение", "Синтаксис", module);
        Assignment assignment = createAssignment(lesson, "ДЗ 1", "Написать Hello World",
                LocalDate.of(2026, 10, 1), 100);

        // Создание студента и запись на курс
        User student = createStudent("student.solution@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        // Отправка решения
        Submission submission = submissionService.submitSolution(
                assignment.getId(), student.getId(), "public class Hello { public static void main(String[] args) { System.out.println(\"Hello\"); } }");

        // Проверки
        assertNotNull(submission.getId(), "Идентификатор решения не должен быть null");
        assertNotNull(submission.getSubmittedAt(), "Время отправки не должно быть null");
        assertEquals("public class Hello { public static void main(String[] args) { System.out.println(\"Hello\"); } }",
                submission.getContent(), "Текст ответа должен совпадать");
        assertNull(submission.getScore(), "Оценка должна быть null до проверки");
        assertNull(submission.getFeedback(), "Комментарий должен быть null до проверки");
        assertEquals(assignment.getId(), submission.getAssignment().getId(),
                "Ссылка на задание должна совпадать");
        assertEquals(student.getId(), submission.getStudent().getId(),
                "Ссылка на студента должна совпадать");

        // Проверка сохранения через репозиторий
        Submission loaded = submissionRepository.findById(submission.getId()).orElseThrow();
        assertEquals(submission.getId(), loaded.getId());
        assertEquals(submission.getContent(), loaded.getContent());
    }

    /**
     * Тест: хранение длинного текста в поле content через JPA и прямой SQL.
     * <p>
     * Аналогично {@code AssignmentServiceTests.testTextStorageViaJpaAndSql}.
     * </p>
     */
    @Test
    void submitSolution_textStorageViaJpaAndSql() {
        // Подготовка контекста
        Category category = createCategory("Литература");
        User teacher = createTeacher("teacher.solution.text@example.com");
        Course course = createCourse("История литературы", teacher, category);
        Module module = createModule("Золотой век", course, 1);
        Lesson lesson = createLesson("Пушкин", null, module);
        Assignment assignment = createAssignment(lesson, "Сочинение", "Написать сочинение",
                LocalDate.of(2026, 11, 1), 100);

        User student = createStudent("student.text.storage@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        // Длинный текст
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longContent.append("Строка текста номер ").append(i).append(
                    ": Проверка хранения TEXT-поля в сущности Submission. Текст должен быть ").append(
                    "сохранён без потерь и корректно прочитан через JPA и прямой SQL. ").append(
                    "Это имитирует развёрнутый ответ студента на учебное задание. ");
        }

        Submission submission = submissionService.submitSolution(
                assignment.getId(), student.getId(), longContent.toString());

        String savedText = longContent.toString();

        // Чтение через JPA
        Submission loadedViaJpa = submissionRepository.findById(submission.getId()).orElseThrow();
        assertEquals(savedText, loadedViaJpa.getContent(),
                "Текст, сохранённый через JPA, должен быть прочитан без изменений");

        // Чтение через JdbcTemplate
        String sqlText = jdbcTemplate.queryForObject(
                "SELECT content FROM submissions WHERE id = ?",
                String.class, submission.getId());
        assertEquals(savedText, sqlText,
                "Текст из прямого SQL должен совпадать с исходным");
    }

    /**
     * Тест: репозиторий находит решения по assignmentId.
     */
    @Test
    void submissionRepositoryFindByAssignmentId() {
        Category category = createCategory("Математика");
        User teacher = createTeacher("teacher.find.assign@example.com");
        Course course = createCourse("Линейная алгебра", teacher, category);
        Module module = createModule("Матрицы", course, 1);
        Lesson lesson = createLesson("Операции", null, module);
        Assignment assignment = createAssignment(lesson, "Задачи", "Решить задачи",
                LocalDate.of(2026, 10, 1), 80);

        User student1 = createStudent("find.student1@example.com");
        User student2 = createStudent("find.student2@example.com");
        User student3 = createStudent("find.student3@example.com");

        createEnrollment(student1, course, EnrollmentStatus.ACTIVE);
        createEnrollment(student2, course, EnrollmentStatus.ACTIVE);
        createEnrollment(student3, course, EnrollmentStatus.ACTIVE);

        submissionService.submitSolution(assignment.getId(), student1.getId(), "Ответ студента 1");
        submissionService.submitSolution(assignment.getId(), student2.getId(), "Ответ студента 2");
        submissionService.submitSolution(assignment.getId(), student3.getId(), "Ответ студента 3");

        var submissions = submissionRepository.findByAssignmentId(assignment.getId());
        assertEquals(3, submissions.size(),
                "Должно быть найдено 3 решения для данного задания");
    }

    /**
     * Тест: репозиторий находит решения по studentId.
     */
    @Test
    void submissionRepositoryFindByStudentId() {
        Category category = createCategory("Физика");
        User teacher = createTeacher("teacher.find.student@example.com");
        Course course = createCourse("Квантовая физика", teacher, category);
        Module module = createModule("Частицы", course, 1);
        Lesson lesson1 = createLesson("Фотон", null, module);
        Lesson lesson2 = createLesson("Электрон", null, module);
        Assignment assignment1 = createAssignment(lesson1, "ДЗ 1", "Вопросы о фотоне",
                LocalDate.of(2026, 10, 1), 60);
        Assignment assignment2 = createAssignment(lesson2, "ДЗ 2", "Вопросы об электроне",
                LocalDate.of(2026, 10, 15), 70);

        User student = createStudent("find.mysolutions@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        submissionService.submitSolution(assignment1.getId(), student.getId(), "Ответ 1");
        submissionService.submitSolution(assignment2.getId(), student.getId(), "Ответ 2");

        var submissions = submissionRepository.findByStudentId(student.getId());
        assertEquals(2, submissions.size(),
                "Должно быть найдено 2 решения данного студента");
    }

    /**
     * Тест: репозиторий находит существующее решение по паре assignmentId + studentId.
     */
    @Test
    void submissionRepositoryFindByAssignmentIdAndStudentId_existing() {
        Category category = createCategory("Химия");
        User teacher = createTeacher("teacher.exist.sub@example.com");
        Course course = createCourse("Органическая химия", teacher, category);
        Module module = createModule("Углеводороды", course, 1);
        Lesson lesson = createLesson("Алканы", null, module);
        Assignment assignment = createAssignment(lesson, "ДЗ", "Строение алканов",
                LocalDate.of(2026, 11, 1), 90);

        User student = createStudent("exist.sub@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        submissionService.submitSolution(assignment.getId(), student.getId(), "CH3-CH3");

        Optional<Submission> found = submissionRepository
                .findByAssignmentIdAndStudentId(assignment.getId(), student.getId());
        assertTrue(found.isPresent(), "Решение должно быть найдено по паре ID");
        assertEquals(assignment.getId(), found.get().getAssignment().getId());
        assertEquals(student.getId(), found.get().getStudent().getId());
    }

    /**
     * Тест: репозиторий не находит решение по паре, когда решения нет.
     */
    @Test
    void submissionRepositoryFindByAssignmentIdAndStudentId_notExisting() {
        Category category = createCategory("Биология");
        User teacher = createTeacher("teacher.noexist.sub@example.com");
        Course course = createCourse("Генетика", teacher, category);
        Module module = createModule("Законы Менделя", course, 1);
        Lesson lesson = createLesson("Первый закон", null, module);
        Assignment assignment = createAssignment(lesson, "ДЗ", "Гибридизация",
                LocalDate.of(2026, 11, 1), 85);

        User student = createStudent("noexist.sub@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        Optional<Submission> found = submissionRepository
                .findByAssignmentIdAndStudentId(assignment.getId(), student.getId());
        assertTrue(found.isEmpty(), "Решение не должно быть найдено");
    }

    /**
     * Тест: коллекция submissions в Assignment загружается корректно.
     */
    @Test
    void assignment_submissionsCollection() {
        Category category = createCategory("Информатика");
        User teacher = createTeacher("teacher.subcoll3@example.com");
        Course course = createCourse("Базы данных", teacher, category);
        Module module = createModule("SQL", course, 1);
        Lesson lesson = createLesson("Запросы", null, module);
        Assignment assignment = createAssignment(lesson, "SQL-запрос", "Написать запрос",
                LocalDate.of(2026, 10, 1), 100);

        User student1 = createStudent("subcoll.student1c@example.com");
        User student2 = createStudent("subcoll.student2c@example.com");
        createEnrollment(student1, course, EnrollmentStatus.ACTIVE);
        createEnrollment(student2, course, EnrollmentStatus.ACTIVE);

        submissionService.submitSolution(assignment.getId(), student1.getId(), "SELECT * FROM users");
        submissionService.submitSolution(assignment.getId(), student2.getId(), "SELECT name FROM courses");

        // Проверяем, что решения действительно сохранены в БД
        long countInDb = submissionRepository.count();
        assertEquals(2, countInDb, "В базе должно быть 2 решения");

        // Проверяем по assignmentId
        List<Submission> submissionsByAssignment = submissionRepository.findByAssignmentId(assignment.getId());
        assertEquals(2, submissionsByAssignment.size(),
                "По assignmentId должно быть найдено 2 решения");

        // Проверяем, что в Submission правильно установлен assignment (обратная связь)
        Submission loadedSub = submissionsByAssignment.get(0);
        assertNotNull(loadedSub.getAssignment(), "В решении должно быть задание");
        assertEquals(assignment.getId(), loadedSub.getAssignment().getId(),
                "Задание в решении должно совпадать с созданным");
    }

    // ==================== Негативные тесты ====================

    /**
     * Тест: отправка решения на несуществующее задание вызывает исключение.
     */
    @Test
    void submitSolution_nonExistentAssignment_shouldThrow() {
        User student = createStudent("nonexist.assignment@example.com");

        assertThrows(ResourceNotFoundException.class, () ->
                submissionService.submitSolution(99999L, student.getId(), "Текст ответа"),
                "Должно быть выброшено исключение при отправке на несуществующее задание");
    }

    /**
     * Тест: отправка решения студентом, не записанным на курс.
     */
    @Test
    void submitSolution_studentNotEnrolled_shouldThrow() {
        Category category = createCategory("Астрономия");
        User teacher = createTeacher("teacher.no.enroll@example.com");
        Course course = createCourse("Введение в астрономию", teacher, category);
        Module module = createModule("Планеты", course, 1);
        Lesson lesson = createLesson("Марс", null, module);
        Assignment assignment = createAssignment(lesson, "ДЗ", "Описание Марса",
                LocalDate.of(2026, 11, 1), 75);

        User student = createStudent("no.enroll@example.com");
        // Студент НЕ записан на курс

        assertThrows(ResourceNotFoundException.class, () ->
                submissionService.submitSolution(assignment.getId(), student.getId(),
                        "Марс — четвёртая планета"),
                "Должно быть выброшено исключение при отправке незаписанным студентом");
    }

    /**
     * Тест: преподаватель не может отправить решение.
     */
    @Test
    void submitSolution_teacherCannotSubmit() {
        Category category = createCategory("Философия");
        User teacher = createTeacher("teacher.cannot.submit@example.com");
        Course course = createCourse("Логика", teacher, category);
        Module module = createModule("Силлогизмы", course, 1);
        Lesson lesson = createLesson("Типы", null, module);
        Assignment assignment = createAssignment(lesson, "ДЗ", "Анализ",
                LocalDate.of(2026, 11, 1), 60);

        // Преподаватель не может быть записан на свой курс (нет метода записи для преподавателей)
        assertThrows(ResourceNotFoundException.class, () ->
                submissionService.submitSolution(assignment.getId(), teacher.getId(),
                        "Анализ силлогизма"),
                "Преподаватель не должен иметь возможность отправить решение");
    }

    /**
     * Тест: повторная отправка решения вызывает исключение.
     */
    @Test
    void submitSolution_duplicateSolution_shouldThrow() {
        Category category = createCategory("Геометрия");
        User teacher = createTeacher("teacher.duplicate@example.com");
        Course course = createCourse("Планиметрия", teacher, category);
        Module module = createModule("Треугольники", course, 1);
        Lesson lesson = createLesson("Теорема Пифагора", null, module);
        Assignment assignment = createAssignment(lesson, "ДЗ", "Доказать теорему",
                LocalDate.of(2026, 11, 1), 100);

        User student = createStudent("duplicate@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        submissionService.submitSolution(assignment.getId(), student.getId(),
                "a² + b² = c²");

        assertThrows(IllegalStateException.class, () ->
                submissionService.submitSolution(assignment.getId(), student.getId(),
                        "a^2 + b^2 = c^2"),
                "Должно быть выброшено исключение при повторной отправке решения");
    }

    /**
     * Тест: отправка решения после отписки студента запрещена.
     */
    @Test
    void submitSolution_afterDropEnrollment_shouldThrow() {
        Category category = createCategory("Экономика");
        User teacher = createTeacher("teacher.drop.enroll@example.com");
        Course course = createCourse("Микроэкономика", teacher, category);
        Module module = createModule("Спрос", course, 1);
        Lesson lesson = createLesson("Кривая спроса", null, module);
        Assignment assignment = createAssignment(lesson, "ДЗ", "Построить график",
                LocalDate.of(2026, 11, 1), 80);

        User student = createStudent("drop.enroll@example.com");
        Enrollment enrollment = createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        // Сначала студент отправляет решение успешно
        Submission first = submissionService.submitSolution(
                assignment.getId(), student.getId(), "Кривая убывает");

        assertNotNull(first.getId(), "Первое решение должно быть сохранено");

        // Отписываем студента
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        entityManager.merge(enrollment);
        entityManager.flush();

        // Пытаемся отправить ещё одно решение — должно быть запрещено
        assertThrows(ResourceNotFoundException.class, () ->
                submissionService.submitSolution(assignment.getId(), student.getId(),
                        "Кривая предложения"),
                "Отправка после отписки должна быть запрещена");
    }

    /**
     * Тест: null assignment вызывает нарушение целостности данных.
     */
    @Test
    void submission_nullAssignment_shouldFail() {
        User student = createStudent("student.null.assign@example.com");
        Submission submission = new Submission();
        submission.setStudent(student);
        submission.setContent("Ответ");
        submission.setSubmittedAt(java.time.LocalDateTime.now());

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(submission);
            entityManager.flush();
        }, "Сохранение решения без задания должно вызвать нарушение целостности");
    }

    /**
     * Тест: null student вызывает нарушение целостности данных.
     */
    @Test
    void submission_nullStudent_shouldFail() {
        Category category = createCategory("Искусство");
        User teacher = createTeacher("teacher.null.student@example.com");
        Course course = createCourse("История искусств", teacher, category);
        Module module = createModule("Ренессанс", course, 1);
        Lesson lesson = createLesson("Да Винчи", null, module);
        Assignment assignment = createAssignment(lesson, "ДЗ", "Анализ работ",
                LocalDate.of(2026, 11, 1), 70);

        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setContent("Анализ");
        submission.setSubmittedAt(java.time.LocalDateTime.now());

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(submission);
            entityManager.flush();
        }, "Сохранение решения без студента должно вызвать нарушение целостности");
    }

    /**
     * Тест: null content вызывает нарушение целостности данных.
     */
    @Test
    void submission_nullContent_shouldFail() {
        Category category = createCategory("Музыка");
        User teacher = createTeacher("teacher.null.content@example.com");
        Course course = createCourse("Теория музыки", teacher, category);
        Module module = createModule("Лады", course, 1);
        Lesson lesson = createLesson("Мажор и минор", null, module);
        Assignment assignment = createAssignment(lesson, "ДЗ", "Определить лад",
                LocalDate.of(2026, 11, 1), 50);

        User student = createStudent("student.null.content@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setSubmittedAt(java.time.LocalDateTime.now());

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(submission);
            entityManager.flush();
        }, "Сохранение решения без текста должно вызвать нарушение целостности");
    }

    /**
     * Тест: составное уникальное ограничение (assignment_id, student_id) предотвращает дублирование.
     */
    @Test
    void uniqueConstraint_assignmentIdStudentId_shouldPreventDuplicate() {
        Category category = createCategory("Психология");
        User teacher = createTeacher("teacher.unique.constraint@example.com");
        Course course = createCourse("Общая психология", teacher, category);
        Module module = createModule("Память", course, 1);
        Lesson lesson = createLesson("Виды памяти", null, module);
        Assignment assignment = createAssignment(lesson, "ДЗ", "Классификация",
                LocalDate.of(2026, 11, 1), 80);

        User student = createStudent("unique.constraint@example.com");
        createEnrollment(student, course, EnrollmentStatus.ACTIVE);

        // Сохраняем первое решение напрямую
        Submission first = new Submission();
        first.setAssignment(assignment);
        first.setStudent(student);
        first.setContent("Первое решение");
        first.setSubmittedAt(java.time.LocalDateTime.now());
        entityManager.persist(first);
        entityManager.flush();

        // Попытка создать дубликат
        assertThrows(PersistenceException.class, () -> {
            Submission duplicate = new Submission();
            duplicate.setAssignment(assignment);
            duplicate.setStudent(student);
            duplicate.setContent("Дубликат");
            duplicate.setSubmittedAt(java.time.LocalDateTime.now());
            entityManager.persist(duplicate);
            entityManager.flush();
        }, "Составное уникальное ограничение должно предотвратить дублирование решения");
    }

    /**
     * Проверка, что в тестах используется PostgreSQL через Testcontainers.
     */
    @Test
    void databaseIsPostgreSQL() throws Exception {
        try (var connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            assertEquals("PostgreSQL", productName,
                    "Тестовая база должна быть PostgreSQL, получено: " + productName);
        }
    }

    // ==================== Вспомогательные методы ====================

    /**
     * Создаёт и сохраняет тестовую категорию.
     *
     * @param name название категории
     * @return сохранённая категория
     */
    private Category createCategory(String name) {
        Category category = new Category(name);
        entityManager.persist(category);
        entityManager.flush();
        return category;
    }

    /**
     * Создаёт и сохраняет тестового преподавателя.
     *
     * @param email адрес электронной почты
     * @return сохранённый пользователь
     */
    private User createTeacher(String email) {
        User user = new User(email, UserRole.TEACHER);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    /**
     * Создаёт и сохраняет тестового студента.
     *
     * @param email адрес электронной почты
     * @return сохранённый пользователь
     */
    private User createStudent(String email) {
        User user = new User(email, UserRole.STUDENT);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    /**
     * Создаёт и сохраняет тестовый курс с преподавателем и категорией.
     *
     * @param title    название курса
     * @param teacher  преподаватель
     * @param category категория
     * @return сохранённый курс
     */
    private Course createCourse(String title, User teacher, Category category) {
        Course course = new Course(title, "Описание курса", 40,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();
        return course;
    }

    /**
     * Создаёт и сохраняет тестовый модуль.
     *
     * @param title    название модуля
     * @param course   курс
     * @param orderIndex порядковый номер
     * @return сохранённый модуль
     */
    private Module createModule(String title, Course course, Integer orderIndex) {
        Module module = new Module();
        module.setTitle(title);
        module.setOrderIndex(orderIndex);
        module.setCourse(course);
        entityManager.persist(module);
        entityManager.flush();
        return module;
    }

    /**
     * Создаёт и сохраняет тестовый урок.
     *
     * @param title   название урока
     * @param content содержание урока
     * @param module  модуль
     * @return сохранённый урок
     */
    private Lesson createLesson(String title, String content, Module module) {
        Lesson lesson = new Lesson();
        lesson.setTitle(title);
        lesson.setContent(content);
        lesson.setModule(module);
        entityManager.persist(lesson);
        entityManager.flush();
        return lesson;
    }

    /**
     * Создаёт и сохраняет тестовое задание.
     *
     * @param lesson      урок
     * @param title       название задания
     * @param description описание
     * @param dueDate     срок выполнения
     * @param maxScore    максимальный балл
     * @return сохранённое задание
     */
    private Assignment createAssignment(Lesson lesson, String title, String description,
                                        LocalDate dueDate, Integer maxScore) {
        Assignment assignment = new Assignment(lesson, title, description, dueDate, maxScore);
        entityManager.persist(assignment);
        entityManager.flush();
        return assignment;
    }

    /**
     * Создаёт и сохраняет тестовую запись на курс.
     *
     * @param user   студент
     * @param course курс
     * @param status статус записи
     * @return сохранённая запись
     */
    private Enrollment createEnrollment(User user, Course course, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment(user, course,
                java.time.LocalDateTime.now(), status);
        entityManager.persist(enrollment);
        entityManager.flush();
        return enrollment;
    }
}
