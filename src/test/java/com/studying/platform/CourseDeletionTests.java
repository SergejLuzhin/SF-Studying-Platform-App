package com.studying.platform;

import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Enrollment;
import com.studying.platform.entity.EnrollmentStatus;
import com.studying.platform.entity.Lesson;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.Quiz;
import com.studying.platform.entity.QuizSubmission;
import com.studying.platform.entity.Tag;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.AssignmentRepository;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.CourseReviewRepository;
import com.studying.platform.repository.EnrollmentRepository;
import com.studying.platform.repository.LessonRepository;
import com.studying.platform.repository.ModuleRepository;
import com.studying.platform.repository.QuizRepository;
import com.studying.platform.repository.QuizSubmissionRepository;
import com.studying.platform.repository.SubmissionRepository;
import com.studying.platform.repository.TagRepository;
import com.studying.platform.repository.UserRepository;
import com.studying.platform.service.CourseService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для метода удаления курса {@link CourseService#deleteCourse(Long)}.
 * <p>
 * Проверяют согласованные правила удаления курса: пустой курс удаляется,
 * курс с учебной историей (записи, решения, результаты тестов, отзывы) не удаляется.
 * Общие объекты (преподаватель, категория, теги) сохраняются.
 * </p>
 * <p>
 * Тесты выполняются с PostgreSQL через Testcontainers в профиле «test».
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CourseDeletionTests {

    /**
     * Менеджер сущностей для ручного управления транзакциями.
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * Репозиторий для сущности Course.
     */
    @Autowired
    private CourseRepository courseRepository;

    /**
     * Репозиторий для сущности Module.
     */
    @Autowired
    private ModuleRepository moduleRepository;

    /**
     * Репозиторий для сущности Lesson.
     */
    @Autowired
    private LessonRepository lessonRepository;

    /**
     * Репозиторий для сущности User.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Репозиторий для сущности Category.
     */
    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Репозиторий для сущности Assignment.
     */
    @Autowired
    private AssignmentRepository assignmentRepository;

    /**
     * Репозиторий для сущности Tag.
     */
    @Autowired
    private TagRepository tagRepository;

    /**
     * Репозиторий для сущности Submission.
     */
    @Autowired
    private SubmissionRepository submissionRepository;

    /**
     * Репозиторий для сущности QuizSubmission.
     */
    @Autowired
    private QuizSubmissionRepository quizSubmissionRepository;

    /**
     * Репозиторий для сущности Quiz.
     */
    @Autowired
    private QuizRepository quizRepository;

    /**
     * Репозиторий для сущности Enrollment.
     */
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    /**
     * Репозиторий для сущности CourseReview.
     */
    @Autowired
    private CourseReviewRepository courseReviewRepository;

    /**
     * Источник данных для проверки продукта базы данных.
     */
    @Autowired
    private DataSource dataSource;

    /**
     * Сервис управления курсами. Создаётся вручную для обхода проблем
     * с автоматическим сканированием в {@code @DataJpaTest}.
     */
    private CourseService courseService;

    /**
     * Инициализирует сервисный слой перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        courseService = new CourseService(
                courseRepository,
                moduleRepository,
                lessonRepository,
                userRepository,
                categoryRepository,
                entityManager,
                assignmentRepository,
                tagRepository,
                submissionRepository,
                quizSubmissionRepository,
                quizRepository,
                enrollmentRepository,
                courseReviewRepository
        );
    }

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
     * @param email email преподавателя
     * @return сохранённый пользователь с ролью TEACHER
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
     * @param email email студента
     * @return сохранённый пользователь с ролью STUDENT
     */
    private User createStudent(String email) {
        User user = new User(email, UserRole.STUDENT);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    /**
     * Создаёт и сохраняет запись студента на курс, корректно связывая обе стороны.
     *
     * @param student студент
     * @param course  курс
     * @return созданная запись
     */
    private Enrollment createEnrollment(User student, Course course) {
        Enrollment enrollment = new Enrollment(student, course, LocalDateTime.now(), EnrollmentStatus.ACTIVE);
        entityManager.persist(enrollment);
        entityManager.flush();
        return enrollment;
    }

    /**
     * Позитивный тест: удаление пустого курса.
     * <p>
     * Создаёт курс без модулей, уроков и записей. Проверяет успешное удаление
     * и отсутствие курса в репозитории.
     * </p>
     */
    @Test
    void deleteEmptyCourse_shouldSucceed() {
        // Подготовка
        User teacher = createTeacher("deleteemptyteacher@example.com");
        Category category = createCategory("Физика");
        Course course = new Course(
                "Квантовая механика", "Основы квантовой физики",
                30, LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        // Выполнение
        courseService.deleteCourse(course.getId());

        // Проверка
        assertFalse(courseRepository.findById(course.getId()).isPresent(),
                "Удалённый курс не должен найден в репозитории");
    }

    /**
     * Позитивный тест: удаление курса с модулями и уроками (без учебной истории).
     * <p>
     * Создаёт курс с модулем, уроком и заданием (но без решений).
     * Проверяет, что курс и всё содержимое (модули, уроки, задания)
     * удаляются каскадом.
     * </p>
     */
    @Test
    void deleteCourseWithModulesAndLessons_shouldCascadeRemoveContent() {
        // Подготовка
        User teacher = createTeacher("cascadeteacher@example.com");
        Category category = createCategory("Математика");
        Course course = new Course(
                "Алгебра", "Линейная алгебра",
                40, LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        // Модуль корректно связывается с курсом в обе стороны
        Module module = new Module("Матрицы", "Операции с матрицами", 1, course);
        course.getModules().add(module);
        entityManager.persist(module);
        entityManager.flush();

        // Урок корректно связывается с модулем в обе стороны
        Lesson lesson = new Lesson("Умножение матриц", "Правило умножения",
                "https://example.com/video", module);
        module.getLessons().add(lesson);
        entityManager.persist(lesson);
        entityManager.flush();

        // Задание привязано к уроку
        var assignment = new com.studying.platform.entity.Assignment(
                lesson, "Эссе", "Напишите эссе о матрицах",
                LocalDate.of(2026, 10, 1), 100);
        entityManager.persist(assignment);
        entityManager.flush();

        // Удаление курса
        courseService.deleteCourse(course.getId());

        // Проверка: курс удалён
        assertFalse(courseRepository.findById(course.getId()).isPresent(),
                "Курс должен быть удалён");

        // Проверка: модуль удалён каскадом
        assertFalse(moduleRepository.findById(module.getId()).isPresent(),
                "Модуль должен быть удалён каскадом");

        // Проверка: урок удалён каскадом
        assertFalse(lessonRepository.findById(lesson.getId()).isPresent(),
                "Урок должен быть удалён каскадом");
    }

    /**
     * Негативный тест: удаление курса с активными записями.
     * <p>
     * Создаёт курс с активной записью студента. Проверяет, что удаление
     * вызывает {@link IllegalStateException} и курс не удалён.
     * </p>
     */
    @Test
    void deleteCourseWithActiveEnrollments_shouldFail() {
        // Подготовка
        User teacher = createTeacher("activeenrollmentteacher@example.com");
        Category category = createCategory("Биология");
        Course course = new Course(
                "Общая биология", "Основы биологии",
                20, LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        User student = createStudent("activeenrollmentstudent@example.com");
        createEnrollment(student, course);

        // Выполнение и проверка
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> courseService.deleteCourse(course.getId()),
                "Курс с активными записями не должен удаляться");

        assertTrue(exception.getMessage().contains("записи"),
                "Сообщение должно содержать информацию о записях");

        // Проверка: курс не удалён
        assertTrue(courseRepository.findById(course.getId()).isPresent(),
                "Курс с активными записями не должен быть удалён");
    }

    /**
     * Негативный тест: удаление курса с отписавшимися студентами (DROPPED).
     * <p>
     * Создаёт курс, запись, отписывает студента. Проверяет, что удаление
     * с записью статуса DROPPED запрещено (запись остаётся в базе).
     * </p>
     */
    @Test
    void deleteCourseWithDroppedEnrollments_shouldFail() {
        // Подготовка
        User teacher = createTeacher("droppedenrollmentteacher@example.com");
        Category category = createCategory("География");
        Course course = new Course(
                "География России", "Регионы России",
                25, LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        User student = createStudent("droppedenrollmentstudent@example.com");
        Enrollment enrollment = createEnrollment(student, course);
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        entityManager.flush();

        // Выполнение и проверка
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> courseService.deleteCourse(course.getId()),
                "Курс с отписавшимися студентами не должен удаляться");

        assertTrue(exception.getMessage().contains("записи"),
                "Сообщение должно содержать информацию о записях");

        // Проверка: курс не удалён
        assertTrue(courseRepository.findById(course.getId()).isPresent(),
                "Курс с записями DROPPED не должен быть удалён");
    }

    /**
     * Негативный тест: удаление курса с решениями.
     * <p>
     * Создаёт курс, модуль, урок, задание, студента, активную запись и решение.
     * Проверяет, что удаление с наличием решений запрещено.
     * </p>
     */
    @Test
    void deleteCourseWithSubmissions_shouldFail() {
        // Подготовка
        User teacher = createTeacher("submissionteacher@example.com");
        Category category = createCategory("Философия");
        Course course = new Course(
                "Логика", "Основы логики",
                20, LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        // Модуль с корректной двусторонней связью
        Module module = new Module("Силлогизмы", "Типы силлогизмов", 1, course);
        course.getModules().add(module);
        entityManager.persist(module);
        entityManager.flush();

        // Урок с корректной двусторонней связью
        Lesson lesson = new Lesson("Центрический силлогизм", "Правила построения",
                null, module);
        module.getLessons().add(lesson);
        entityManager.persist(lesson);
        entityManager.flush();

        var assignment = new com.studying.platform.entity.Assignment(
                lesson, "Эссе", "Напишите эссе о силлогизмах",
                LocalDate.of(2026, 10, 1), 100);
        entityManager.persist(assignment);
        entityManager.flush();

        User student = createStudent("submissionstudent@example.com");
        createEnrollment(student, course);

        var submission = new com.studying.platform.entity.Submission(
                assignment, student, "Силлогизм — это вид рассуждения");
        entityManager.persist(submission);
        entityManager.flush();

        // Выполнение и проверка
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> courseService.deleteCourse(course.getId()),
                "Курс с решениями не должен удаляться");

        assertTrue(exception.getMessage().contains("решения"),
                "Сообщение должно содержать информацию о решениях");

        // Проверка: курс не удалён, решение сохранено
        assertTrue(courseRepository.findById(course.getId()).isPresent(),
                "Курс не должен быть удалён");
        assertTrue(submissionRepository.findById(submission.getId()).isPresent(),
                "Решение должно быть сохранено");
    }

    /**
     * Негативный тест: удаление курса с результатами тестов.
     * <p>
     * Создаёт курс, модуль, викторину, студента, активную запись и результат теста.
     * Проверяет, что удаление с наличием результатов тестов запрещено.
     * </p>
     */
    @Test
    void deleteCourseWithQuizSubmissions_shouldFail() {
        // Подготовка
        User teacher = createTeacher("quizsubteacher@example.com");
        Category category = createCategory("Экономика");
        Course course = new Course(
                "Макроэкономика", "Основы макроэкономики",
                30, LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        // Модуль с корректной двусторонней связью
        Module module = new Module("ВВП", "Расчёт ВВП", 1, course);
        course.getModules().add(module);
        entityManager.persist(module);
        entityManager.flush();

        Quiz quiz = new Quiz(module, "Тест по ВВП", 15);
        entityManager.persist(quiz);
        entityManager.flush();

        User student = createStudent("quizsubstudent@example.com");
        createEnrollment(student, course);

        var quizSubmission = new QuizSubmission(
                quiz, student, 80, LocalDateTime.now());
        entityManager.persist(quizSubmission);
        entityManager.flush();

        // Выполнение и проверка
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> courseService.deleteCourse(course.getId()),
                "Курс с результатами тестов не должен удаляться");

        assertTrue(exception.getMessage().contains("результаты"),
                "Сообщение должно содержать информацию о результатах тестов");

        // Проверка: курс не удалён, результат сохранён
        assertTrue(courseRepository.findById(course.getId()).isPresent(),
                "Курс не должен быть удалён");
        assertTrue(quizSubmissionRepository.findById(quizSubmission.getId()).isPresent(),
                "Результат теста должен быть сохранён");
    }

    /**
     * Негативный тест: удаление курса с отзывами.
     * <p>
     * Создаёт курс и отзыв студента (без записей на курс для изоляции проверки).
     * Проверяет, что удаление с наличием отзывов запрещено.
     * </p>
     */
    @Test
    void deleteCourseWithReviews_shouldFail() {
        // Подготовка
        User teacher = createTeacher("reviewteacher@example.com");
        Category category = createCategory("Искусство");
        Course course = new Course(
                "История живописи", "От Возрождения до импрессионизма",
                25, LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        User student = createStudent("reviewstudent@example.com");
        // Студент записан на курс, но запись помечена как DROPPED,
        // однако запись всё ещё существует в базе и блокирует удаление курса.
        // Для изоляции проверки отзывов создаём отзыв напрямую (без сервиса).
        var review = new com.studying.platform.entity.CourseReview(
                5, "Отличный курс!", LocalDateTime.now(), course, student);
        entityManager.persist(review);
        entityManager.flush();

        // Выполнение и проверка
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> courseService.deleteCourse(course.getId()),
                "Курс с отзывами не должен удаляться");

        assertTrue(exception.getMessage().contains("отзывы"),
                "Сообщение должно содержать информацию об отзывах");

        // Проверка: курс не удалён, отзыв сохранён
        assertTrue(courseRepository.findById(course.getId()).isPresent(),
                "Курс не должен быть удалён");
        assertTrue(courseReviewRepository.findById(review.getId()).isPresent(),
                "Отзыв должен быть сохранён");
    }

    /**
     * Негативный тест: удаление несуществующего курса.
     * <p>
     * Проверяет, что при удалении несуществующего курса
     * выбрасывается {@link ResourceNotFoundException}.
     * </p>
     */
    @Test
    void deleteNonExistentCourse_shouldThrow() {
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> courseService.deleteCourse(99999L),
                "Удаление несуществующего курса должно вызвать ResourceNotFoundException");

        assertTrue(exception.getMessage().contains("не найден"),
                "Сообщение должно содержать информацию об отсутствии курса");
    }

    /**
     * Тест на защиту преподавателя при удалении курса.
     * <p>
     * Создаёт курс с преподавателем, удаляет курс.
     * Проверяет, что преподаватель существует после удаления.
     * </p>
     */
    @Test
    void deleteCourse_shouldNotAffectTeacher() {
        // Подготовка
        User teacher = createTeacher("protectedteacher@example.com");
        Category category = createCategory("Астрономия");
        Course course = new Course(
                "Введение в астрономию", "Основы астрономии",
                20, LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        Long teacherId = teacher.getId();

        // Удаление
        courseService.deleteCourse(course.getId());

        // Проверка
        assertTrue(userRepository.findById(teacherId).isPresent(),
                "Преподаватель должен существовать после удаления курса");
    }

    /**
     * Тест на защиту категории при удалении курса.
     * <p>
     * Создаёт курс с категорией, удаляет курс.
     * Проверяет, что категория существует после удаления.
     * </p>
     */
    @Test
    void deleteCourse_shouldNotAffectCategory() {
        // Подготовка
        User teacher = createTeacher("categoryprotectedteacher@example.com");
        Category category = createCategory("Программирование");
        Course course = new Course(
                "Python для начинающих", "Основы Python",
                30, LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        Long categoryId = category.getId();

        // Удаление
        courseService.deleteCourse(course.getId());

        // Проверка
        assertTrue(categoryRepository.findById(categoryId).isPresent(),
                "Категория должна существовать после удаления курса");
        assertEquals("Программирование",
                categoryRepository.findById(categoryId).orElseThrow().getName(),
                "Название категории должно остаться прежним");
    }

    /**
     * Тест на защиту тегов при удалении курса.
     * <p>
     * Создаёт курс с тегами, удаляет курс.
     * Проверяет, что теги существуют после удаления.
     * </p>
     */
    @Test
    void deleteCourse_shouldNotAffectTags() {
        // Подготовка
        User teacher = createTeacher("tagprotectedteacher@example.com");
        Category category = createCategory("Дизайн");
        Course course = new Course(
                "Веб-дизайн", "Основы веб-дизайна",
                25, LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        Tag tag1 = new Tag("HTML");
        entityManager.persist(tag1);
        entityManager.flush();

        Tag tag2 = new Tag("CSS");
        entityManager.persist(tag2);
        entityManager.flush();

        Set<Tag> tags = Set.of(tag1, tag2);
        course.getTags().addAll(tags);
        entityManager.flush();

        Long tag1Id = tag1.getId();
        Long tag2Id = tag2.getId();

        // Удаление
        courseService.deleteCourse(course.getId());

        // Проверка
        assertTrue(tagRepository.findById(tag1Id).isPresent(),
                "Тег 1 должен существовать после удаления курса");
        assertTrue(tagRepository.findById(tag2Id).isPresent(),
                "Тег 2 должен существовать после удаления курса");
        assertEquals("HTML", tagRepository.findById(tag1Id).orElseThrow().getName());
        assertEquals("CSS", tagRepository.findById(tag2Id).orElseThrow().getName());
    }

    /**
     * Проверка, что в тестах используется PostgreSQL через Testcontainers.
     * <p>
     * Гарантирует, что {@code @DataJpaTest} с {@code @AutoConfigureTestDatabase(replace = NONE)}
     * использует контейнерную PostgreSQL, а не встроенную H2-базу.
     * </p>
     */
    @Test
    void databaseIsPostgreSQL() throws Exception {
        try (var connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            assertEquals("PostgreSQL", productName,
                    "Тестовая база должна быть PostgreSQL, получено: " + productName);
        }
    }
}
