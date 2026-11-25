package com.studying.platform;

import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.CourseReview;
import com.studying.platform.entity.Tag;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.EnrollmentRepository;
import com.studying.platform.repository.TagRepository;
import com.studying.platform.repository.UserRepository;
import com.studying.platform.service.CourseReviewService;
import com.studying.platform.service.CourseService;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для проверки работы {@link CourseReviewService}
 * и методов управления тегами в {@link CourseService}.
 * <p>
 * Тесты используют {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)},
 * чтобы каждый вызов сервисных методов происходил в отдельной транзакции,
 * а данные, возвращаемые Spring-прокси, были доступны после завершения вызова.
 * Создание фикстур выполняется через {@code TransactionTemplate},
 * чтобы обеспечить транзакцию для {@code EntityManager.persist()}.
 * </p>
 * <p>
 * Очистка тестовых данных выполняется адресно через {@link TestDataCleanupHelper}
 * в транзакции {@code @AfterEach}, что гарантирует изоляцию между тестами.
 * </p>
 *
 * @see CourseReviewService
 * @see CourseService
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CourseReviewAndTagTests {

    /**
     * Сервис отзывов (Spring-бин).
     */
    @Autowired
    private CourseReviewService courseReviewService;

    /**
     * Сервис курсов (Spring-бин).
     */
    @Autowired
    private CourseService courseService;

    /**
     * Репозиторий пользователей для создания фикстур.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Репозиторий категорий для создания фикстур.
     */
    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Репозиторий курсов для проверки.
     */
    @Autowired
    private CourseRepository courseRepository;

    /**
     * Репозиторий тегов для создания и проверки.
     */
    @Autowired
    private TagRepository tagRepository;

    /**
     * Репозиторий отзывов для проверки.
     */
    @Autowired
    private com.studying.platform.repository.CourseReviewRepository courseReviewRepository;

    /**
     * Репозиторий записей для создания фикстур.
     */
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    /**
     * Менеджер сущностей для создания тестовых данных.
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * Шаблон транзакции для безопасной очистки данных.
     */
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Общий помощник для безопасной очистки тестовых данных.
     */
    @Autowired
    private TestDataCleanupHelper cleanupHelper;

    /**
     * Уникальный префикс для тестовых данных.
     */
    private String uniquePrefix;

    /**
     * Идентификатор созданного преподавателя (отслеживается для адресной очистки).
     */
    private Long teacherId;

    /**
     * Идентификатор созданного студента (отслеживается для адресной очистки).
     */
    private Long studentId;

    /**
     * Идентификатор созданной категории (отслеживается для адресной очистки).
     */
    private Long categoryId;

    /**
     * Идентификатор созданного курса (отслеживается для адресной очистки).
     */
    private Long courseId;

    /**
     * Идентификаторы всех созданных курсов (для поддержки нескольких курсов).
     */
    private Set<Long> allCourseIds = new java.util.HashSet<>();

    /**
     * Идентификатор созданного студента-преподавателя (отслеживается для адресной очистки).
     */
    private Long teacherReviewId;

    /**
     * Идентификаторы созданных тегов (для отслеживания очистки).
     */
    private Set<Long> tagIds = new java.util.HashSet<>();

    /**
     * Идентификаторы курсов, на которых оставлены отзывы (для отслеживания очистки).
     */
    private Set<Long> reviewCourseIds = new java.util.HashSet<>();

    /**
     * Идентификаторы курсов, на которых есть записи студентов (для отслеживания очистки).
     */
    private Set<Long> enrollmentCourseIds = new java.util.HashSet<>();

    /**
     * Email студентов, у которых есть записи (для отслеживания очистки).
     */
    private Set<String> enrollmentUserEmails = new java.util.HashSet<>();

    /**
     * Инициализация уникального префикса перед каждым тестом.
     * <p>
     * Префикс ограничивается 20 символами, чтобы при добавлении suffix
     * и домена email оставался в пределах 50 символов.
     * Также очищает tracking-наборы, чтобы каждый тест начинал с чистого листа.
     * </p>
     */
    @BeforeEach
    void setUp() {
        uniquePrefix = "rev-tag-" + System.currentTimeMillis() + "-" + new java.util.Random().nextInt(10000);
        reviewCourseIds.clear();
        enrollmentCourseIds.clear();
        enrollmentUserEmails.clear();
        tagIds.clear();
        allCourseIds.clear();
        categoryId = null;
        courseId = null;
        teacherId = null;
        studentId = null;
        teacherReviewId = null;
    }

    /**
     * Очистка тестовых данных после каждого теста в порядке зависимостей.
     * <p>
     * Удаляет созданные данные через {@link TestDataCleanupHelper} в транзакции.
     * Пропускаются только те сущности, чьи ID не отслеживались (null).
     * </p>
     */
    @AfterEach
    void tearDown() {
        cleanupHelper.clearTestEntities(
                entityManager,
                transactionTemplate,
                Set.of(),              // submissionIds
                !reviewCourseIds.isEmpty() ? reviewCourseIds : Set.of(),
                !enrollmentCourseIds.isEmpty() ? enrollmentCourseIds : Set.of(),
                !enrollmentUserEmails.isEmpty() ? enrollmentUserEmails : Set.of(),
                Set.of(),              // lessonIds
                Set.of(),              // moduleIds
                !allCourseIds.isEmpty() ? allCourseIds : Set.of(),
                categoryId != null ? Set.of(categoryId) : Set.of(),
                buildUserIds()
        );
    }

    /**
     * Собирает все идентификаторы пользователей, созданных в текущем тесте.
     * <p>
     * Включает преподавателя (teacherId), студента (studentId)
     * и преподавателя для теста отзывов (teacherReviewId).
     * </p>
     *
     * @return набор ID пользователей
     */
    private Set<Long> buildUserIds() {
        Set<Long> ids = new java.util.HashSet<>();
        if (teacherId != null) ids.add(teacherId);
        if (studentId != null) ids.add(studentId);
        if (teacherReviewId != null) ids.add(teacherReviewId);
        return ids;
    }

    // ========================================================================
    // Позитивные тесты для CourseReviewService
    // ========================================================================

    /**
     * Проверка: студент, записанный на курс, успешно оставляет отзыв.
     * <p>
     * Создаёт преподавателя, категорию, курс, студента и запись.
     * Студент оставляет отзыв с рейтингом 4 и комментарием.
     * Проверяет создание отзыва и сохранение в базе данных.
     * </p>
     */
    @Test
    void createReview_shouldSucceed_forEnrolledStudent() {
        // Подготовка: преподаватель и категория
        User teacher = createTeacherInTransaction("revteacher1");
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Программирование");
        categoryId = category.getId();

        // Создание курса
        Course course = courseService.createCourse(
                "Java для начинающих", "Основы Java", 40,
                LocalDate.now().plusDays(30),
                teacher.getId(), category.getId());
        courseId = course.getId();
        allCourseIds.add(courseId);

        // Создание студента и записи на курс
        User student = createStudentInTransaction("revstudent1");
        studentId = student.getId();
        createEnrollmentInTransaction(student.getId(), course.getId());

        // Выполнение: отзыв
        CourseReview review = courseReviewService.createReview(
                student.getId(), course.getId(), 4, "Отличный курс!");

        // Проверка: отзыв создан
        assertNotNull(review.getId(), "ID отзыва не должен быть null");
        assertEquals(4, review.getRating(), "Рейтинг должен быть 4");
        assertEquals("Отличный курс!", review.getComment(), "Комментарий должен совпадать");

        // Проверка: отзыв сохранён в БД через прямой запрос к репозиторию
        // (объекты review.getCourse() и review.getStudent() могут быть detached)
        CourseReview saved = courseReviewRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new AssertionError("Отзыв не найден в базе данных"));
        assertNotNull(saved.getId(), "Отзыв должен быть сохранён в базе данных");
        assertEquals(student.getId(), saved.getStudent().getId(), "ID студента должен совпадать");
        assertEquals(course.getId(), saved.getCourse().getId(), "ID курса должен совпадать");

        reviewCourseIds.add(course.getId());
    }

    /**
     * Проверка: отзыв с минимальным рейтингом (1) и пустым комментарием сохраняется.
     * <p>
     * Создаёт все фикстуры и оставляет отзыв с рейтингом 1 и пустым комментарием.
     * Проверяет, что рейтинг 1 сохранён корректно.
     * </p>
     */
    @Test
    void createReview_withMinimumRating_shouldSucceed() {
        User teacher = createTeacherInTransaction("revteacher2");
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Математика");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Алгебра", "Линейная алгебра", 30,
                LocalDate.now().plusDays(20),
                teacher.getId(), category.getId());
        courseId = course.getId();
        allCourseIds.add(courseId);

        User student = createStudentInTransaction("revstudent2");
        studentId = student.getId();
        createEnrollmentInTransaction(student.getId(), course.getId());

        // Выполнение: отзыв с рейтингом 1
        CourseReview review = courseReviewService.createReview(
                student.getId(), course.getId(), 1, "");

        // Проверка
        assertEquals(1, review.getRating(), "Рейтинг должен быть 1");
        assertEquals("", review.getComment(), "Комментарий должен быть пустым");

        reviewCourseIds.add(course.getId());
    }

    /**
     * Проверка: отзыв с максимальным рейтингом (5) и длинным комментарием сохраняется.
     * <p>
     * Создаёт все фикстуры и оставляет отзыв с рейтингом 5 и многострочным комментарием.
     * </p>
     */
    @Test
    void createReview_withMaximumRating_shouldSucceed() {
        User teacher = createTeacherInTransaction("revteacher3");
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Дизайн");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "UI/UX Дизайн", "Создание интерфейсов", 25,
                LocalDate.now().plusDays(15),
                teacher.getId(), category.getId());
        courseId = course.getId();
        allCourseIds.add(courseId);

        User student = createStudentInTransaction("revstudent3");
        studentId = student.getId();
        createEnrollmentInTransaction(student.getId(), course.getId());

        String longComment = "Этот курс превзошёл все мои ожидания. Материалы структурированы отлично, "
                + "практические задания полезны, а преподаватель отвечает на вопросы быстро. Рекомендую!";

        CourseReview review = courseReviewService.createReview(
                student.getId(), course.getId(), 5, longComment);

        // Проверка
        assertEquals(5, review.getRating(), "Рейтинг должен быть 5");
        assertEquals(longComment, review.getComment(), "Длинный комментарий должен сохраниться");

        reviewCourseIds.add(course.getId());
    }

    /**
     * Проверка: несколько отзывов на один курс возвращаются списком.
     * <p>
     * Создаёт два студента, записывает их на один курс, каждый оставляет отзыв.
     * Проверяет, что выборка по курсу возвращает оба отзыва.
     * </p>
     */
    @Test
    void getReviewsByCourse_shouldReturnList() {
        // Подготовка: курс
        User teacher = createTeacherInTransaction("revteacher4");
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Физика");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Физика", "Основы механики", 35,
                LocalDate.now().plusDays(25),
                teacher.getId(), category.getId());
        courseId = course.getId();
        allCourseIds.add(courseId);

        // Студент 1
        User student1 = createStudentInTransaction("revstudent4a");
        studentId = student1.getId();
        createEnrollmentInTransaction(student1.getId(), course.getId());
        courseReviewService.createReview(student1.getId(), course.getId(), 5, "Превосходно");

        // Студент 2
        User student2 = createStudentInTransaction("revstudent4b");
        createEnrollmentInTransaction(student2.getId(), course.getId());
        courseReviewService.createReview(student2.getId(), course.getId(), 3, "Нормально");

        // Проверка
        List<CourseReview> reviews = courseReviewService.getReviewsByCourse(course.getId());
        assertEquals(2, reviews.size(), "Должно быть 2 отзыва");

        reviewCourseIds.add(course.getId());
    }

    /**
     * Проверка: отзывы студента возвращаются списком.
     * <p>
     * Создаёт один курс с двумя разными категориями, студент записывается на оба
     * и оставляет отзывы. Проверяет выборку по студенту.
     * </p>
     */
    @Test
    void getReviewsByStudent_shouldReturnList() {
        // Подготовка: два курса
        User teacher = createTeacherInTransaction("revteacher5");
        teacherId = teacher.getId();
        Category cat1 = createCategoryInTransaction("Химия");
        categoryId = cat1.getId();

        Course course1 = courseService.createCourse(
                "Органическая химия", "Молекулы и реакции", 30,
                LocalDate.now().plusDays(20),
                teacher.getId(), cat1.getId());
        courseId = course1.getId();
        allCourseIds.add(course1.getId());

        Category cat2 = createCategoryInTransaction("Биология");
        Course course2 = courseService.createCourse(
                "Биология", "Клетки и организмы", 28,
                LocalDate.now().plusDays(22),
                teacher.getId(), cat2.getId());
        allCourseIds.add(course2.getId());

        // Студент
        User student = createStudentInTransaction("revstudent5");
        studentId = student.getId();
        createEnrollmentInTransaction(student.getId(), course1.getId());
        createEnrollmentInTransaction(student.getId(), course2.getId());

        courseReviewService.createReview(student.getId(), course1.getId(), 4, "Хорошая химия");
        courseReviewService.createReview(student.getId(), course2.getId(), 5, "Отличная биология");

        // Проверка: отзывы студента
        List<CourseReview> reviews = courseReviewService.getReviewsByStudent(student.getId());
        assertEquals(2, reviews.size(), "Должно быть 2 отзыва студента");

        reviewCourseIds.add(course1.getId());
        reviewCourseIds.add(course2.getId());
    }

    // ========================================================================
    // Тесты для управления тегами
    // ========================================================================

    /**
     * Проверка: теги успешно привязываются к курсу.
     * <p>
     * Создаёт курс и два тега, привязывает теги к курсу через {@code addTagsToCourse}.
     * Проверяет через {@code countTagsByCourseId}, что теги связаны с курсом.
     * </p>
     */
    @Test
    void addTagsToCourse_shouldLinkTags() {
        // Подготовка: курс
        User teacher = createTeacherInTransaction("tagteacher");
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Информатика");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Веб-разработка", "HTML, CSS, JavaScript", 50,
                LocalDate.now().plusDays(40),
                teacher.getId(), category.getId());
        courseId = course.getId();
        allCourseIds.add(courseId);

        // Создание тегов в транзакции
        Tag tag1 = createTagInTransaction("Java");
        Tag tag2 = createTagInTransaction("Spring");
        tagIds.add(tag1.getId());
        tagIds.add(tag2.getId());

        // Выполнение: привязка тегов к курсу
        courseService.addTagsToCourse(course.getId(), Set.of(tag1.getId(), tag2.getId()));

        // Проверка: теги привязаны (через count без загрузки lazy коллекции)
        assertEquals(2, courseRepository.countTagsByCourseId(course.getId()),
                "Курс должен содержать 2 тега");
    }

    /**
     * Проверка: удаление тега из курса не удаляет сам тег и не влияет на другие курсы.
     * <p>
     * Создаёт два курса с общим тегом, удаляет тег из первого курса.
     * Проверяет, что тег остался у второго курса и сам тег не удалён.
     * </p>
     */
    @Test
    void removeTagsFromCourse_shouldUnlinkOnly() {
        // Подготовка: курс 1
        User teacher = createTeacherInTransaction("tagteacher2");
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Математика");
        categoryId = category.getId();

        Course course1 = courseService.createCourse(
                "Алгебра", "Линейная алгебра", 30,
                LocalDate.now().plusDays(20),
                teacher.getId(), category.getId());
        courseId = course1.getId();
        allCourseIds.add(course1.getId());

        // Создание общего тега
        Tag sharedTag = createTagInTransaction("Алгебра");
        tagIds.add(sharedTag.getId());

        // Курс 2
        Course course2 = courseService.createCourse(
                "Геометрия", "Начала геометрии", 25,
                LocalDate.now().plusDays(22),
                teacher.getId(), category.getId());
        allCourseIds.add(course2.getId());

        // Привязка общего тега к обоим курсам
        courseService.addTagsToCourse(course1.getId(), Set.of(sharedTag.getId()));
        courseService.addTagsToCourse(course2.getId(), Set.of(sharedTag.getId()));

        // Выполнение: удаление тега из первого курса
        courseService.removeTagsFromCourse(course1.getId(), Set.of(sharedTag.getId()));

        // Проверка: тег удалён из первого курса (через подсчёт, без загрузки lazy коллекции)
        long tagsInCourse1 = courseRepository.countTagsByCourseId(course1.getId());
        assertEquals(0, tagsInCourse1, "Первый курс не должен содержать тегов");

        // Проверка: тег остался у второго курса
        long tagsInCourse2 = courseRepository.countTagsByCourseId(course2.getId());
        assertEquals(1, tagsInCourse2, "Второй курс должен содержать 1 тег");

        // Проверка: тег существует в БД
        assertTrue(tagRepository.existsById(sharedTag.getId()),
                "Тег должен остаться в базе данных");
    }

    // ========================================================================
    // Негативные тесты для CourseReviewService
    // ========================================================================

    /**
     * Проверка: создание отзыва с несуществующим ID студента вызывает исключение.
     */
    @Test
    void createReview_nonExistentStudent_shouldThrow() {
        // Подготовка: курс
        User teacher = createTeacherInTransaction("revteacher6");
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("История");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "История", "Мировая история", 35,
                LocalDate.now().plusDays(25),
                teacher.getId(), category.getId());
        courseId = course.getId();
        allCourseIds.add(courseId);

        // Выполнение и проверка
        assertThrows(ResourceNotFoundException.class,
                () -> courseReviewService.createReview(99999L, course.getId(), 4, "Комментарий"),
                "Должно быть выброшено ResourceNotFoundException для несуществующего студента");
    }

    /**
     * Проверка: преподаватель не может оставить отзыв.
     * <p>
     * Создаёт курс, преподаватель (другой) пытается оставить отзыв.
     * </p>
     */
    @Test
    void createReview_teacherCannotReview_shouldThrow() {
        // Подготовка: курс с преподавателем 1
        User teacher1 = createTeacherInTransaction("revteacher7a");
        teacherId = teacher1.getId();
        Category category = createCategoryInTransaction("Философия");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Философия", "Введение в философию", 20,
                LocalDate.now().plusDays(10),
                teacher1.getId(), category.getId());
        courseId = course.getId();
        allCourseIds.add(courseId);

        // Создание другого преподавателя
        User teacher2 = createTeacherInTransaction("revteacher7b");
        teacherReviewId = teacher2.getId();

        // Выполнение и проверка
        assertThrows(ResourceNotFoundException.class,
                () -> courseReviewService.createReview(
                        teacher2.getId(), course.getId(), 4, "Сложный курс"),
                "Преподаватель не должен иметь возможность оставить отзыв");
    }

    /**
     * Проверка: администратор не может оставить отзыв.
     * <p>
     * Создаёт курс и администратора, который пытается оставить отзыв.
     * </p>
     */
    @Test
    void createReview_adminCannotReview_shouldThrow() {
        // Подготовка: курс
        User teacher = createTeacherInTransaction("revteacher8");
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Экономика");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Экономика", "Основы макроэкономики", 30,
                LocalDate.now().plusDays(20),
                teacher.getId(), category.getId());
        courseId = course.getId();
        allCourseIds.add(courseId);

        // Создание администратора
        User admin = createAdminInTransaction("revadmin8");
        studentId = admin.getId();

        // Выполнение и проверка
        assertThrows(ResourceNotFoundException.class,
                () -> courseReviewService.createReview(
                        admin.getId(), course.getId(), 3, "Непонятно"),
                "Администратор не должен иметь возможность оставить отзыв");
    }

    /**
     * Проверка: студент, не записанный на курс, не может оставить отзыв.
     * <p>
     * Создаёт курс, студента (без записи на курс) и проверяет, что отзыв не создаётся.
     * </p>
     */
    @Test
    void createReview_nonEnrolledStudent_shouldThrow() {
        // Подготовка: курс
        User teacher = createTeacherInTransaction("revteacher9");
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Музыка");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Музыка", "Теория музыки", 25,
                LocalDate.now().plusDays(15),
                teacher.getId(), category.getId());
        courseId = course.getId();
        allCourseIds.add(courseId);

        // Студент без записи
        User student = createStudentInTransaction("revstudent9");
        studentId = student.getId();

        // Выполнение и проверка
        assertThrows(ResourceNotFoundException.class,
                () -> courseReviewService.createReview(
                        student.getId(), course.getId(), 4, "Интересно"),
                "Незаписанный студент не должен иметь возможность оставить отзыв");
    }

    /**
     * Проверка: повторное создание отзыва одним студентом на один курс вызывает исключение.
     * <p>
     * Создаёт курс, студента, запись и первый отзыв. Затем пытается создать второй отзыв.
     * </p>
     */
    @Test
    void createReview_duplicate_shouldThrow() {
        // Подготовка
        User teacher = createTeacherInTransaction("revteacher10");
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Литература");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Литература", "Мировая литература", 35,
                LocalDate.now().plusDays(25),
                teacher.getId(), category.getId());
        courseId = course.getId();
        allCourseIds.add(courseId);

        User student = createStudentInTransaction("revstudent10");
        studentId = student.getId();
        createEnrollmentInTransaction(student.getId(), course.getId());

        // Первый отзыв
        courseReviewService.createReview(student.getId(), course.getId(), 4, "Хорошо");
        reviewCourseIds.add(course.getId());

        // Второй отзыв (дубликат)
        assertThrows(IllegalStateException.class,
                () -> courseReviewService.createReview(
                        student.getId(), course.getId(), 3, "Плохо"),
                "Дублирующий отзыв должен вызвать IllegalStateException");
    }

    /**
     * Проверка: рейтинг вне диапазона (0) вызывает исключение.
     */
    @Test
    void createReview_ratingTooLow_shouldThrow() {
        // Подготовка
        User teacher = createTeacherInTransaction("revteacher11");
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Геология");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Геология", "Строение Земли", 20,
                LocalDate.now().plusDays(10),
                teacher.getId(), category.getId());
        courseId = course.getId();
        allCourseIds.add(courseId);

        User student = createStudentInTransaction("revstudent11");
        studentId = student.getId();
        createEnrollmentInTransaction(student.getId(), course.getId());

        // Выполнение и проверка
        assertThrows(IllegalArgumentException.class,
                () -> courseReviewService.createReview(
                        student.getId(), course.getId(), 0, "Невозможно"),
                "Рейтинг 0 должен вызвать IllegalArgumentException");
    }

    /**
     * Проверка: рейтинг вне диапазона (6) вызывает исключение.
     */
    @Test
    void createReview_ratingTooHigh_shouldThrow() {
        // Подготовка
        User teacher = createTeacherInTransaction("revteacher12");
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Астрономия");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Астрономия", "Вселенная", 30,
                LocalDate.now().plusDays(20),
                teacher.getId(), category.getId());
        courseId = course.getId();
        allCourseIds.add(courseId);

        User student = createStudentInTransaction("revstudent12");
        studentId = student.getId();
        createEnrollmentInTransaction(student.getId(), course.getId());

        // Выполнение и проверка
        assertThrows(IllegalArgumentException.class,
                () -> courseReviewService.createReview(
                        student.getId(), course.getId(), 6, "Невозможно"),
                "Рейтинг 6 должен вызвать IllegalArgumentException");
    }

    /**
     * Проверка: создание отзыва с несуществующим ID курса вызывает исключение.
     */
    @Test
    void createReview_nonExistentCourse_shouldThrow() {
        // Подготовка: студент
        User teacher = createTeacherInTransaction("revteacher13");
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Психология");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Психология", "Введение", 25,
                LocalDate.now().plusDays(15),
                teacher.getId(), category.getId());
        courseId = course.getId();
        allCourseIds.add(courseId);

        User student = createStudentInTransaction("revstudent13");
        studentId = student.getId();
        createEnrollmentInTransaction(student.getId(), course.getId());

        // Выполнение и проверка
        assertThrows(ResourceNotFoundException.class,
                () -> courseReviewService.createReview(
                        student.getId(), 99999L, 4, "Комментарий"),
                "Несуществующий курс должен вызвать ResourceNotFoundException");
    }

    /**
     * Проверка: getReviewsByCourse для несуществующего курса вызывает исключение.
     */
    @Test
    void getReviewsByCourse_nonExistent_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> courseReviewService.getReviewsByCourse(99999L));
    }

    /**
     * Проверка: getReviewsByStudent для несуществующего студента вызывает исключение.
     */
    @Test
    void getReviewsByStudent_nonExistent_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> courseReviewService.getReviewsByStudent(99999L));
    }

    /**
     * Проверка: добавление тега с несуществующим ID вызывает исключение.
     */
    @Test
    void addTagsToCourse_nonExistentTag_shouldThrow() {
        // Подготовка: курс
        User teacher = createTeacherInTransaction("tagteacher3");
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Технологии");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Технологии", "IT-технологии", 40,
                LocalDate.now().plusDays(30),
                teacher.getId(), category.getId());
        courseId = course.getId();
        allCourseIds.add(courseId);

        // Выполнение и проверка
        assertThrows(ResourceNotFoundException.class,
                () -> courseService.addTagsToCourse(course.getId(), Set.of(99999L)),
                "Несуществующий тег должен вызвать ResourceNotFoundException");
    }

    /**
     * Проверка: удаление тега с несуществующим ID из курса вызывает исключение.
     */
    @Test
    void removeTagsFromCourse_nonExistentTag_shouldThrow() {
        // Подготовка: курс
        User teacher = createTeacherInTransaction("tagteacher4");
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Искусство");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Искусство", "История искусства", 30,
                LocalDate.now().plusDays(20),
                teacher.getId(), category.getId());
        courseId = course.getId();
        allCourseIds.add(courseId);

        // Выполнение и проверка
        assertThrows(ResourceNotFoundException.class,
                () -> courseService.removeTagsFromCourse(course.getId(), Set.of(99999L)),
                "Несуществующий тег при удалении должен вызвать ResourceNotFoundException");
    }

    // ========================================================================
    // Вспомогательные методы создания фикстур
    // ========================================================================

    /**
     * Создаёт и сохраняет пользователя с ролью TEACHER в транзакции.
     *
     * @param localPart локальная часть email
     * @return сохранённый пользователь
     */
    private User createTeacherInTransaction(String localPart) {
        return transactionTemplate.execute(status -> {
            String email = uniquePrefix.substring(0, Math.min(20, uniquePrefix.length())) + "-" + localPart + "@t.com";
            User user = new User(email, UserRole.TEACHER);
            entityManager.persist(user);
            entityManager.flush();
            return user;
        });
    }

    /**
     * Создаёт и сохраняет пользователя с ролью STUDENT в транзакции.
     *
     * @param localPart локальная часть email
     * @return сохранённый пользователь
     */
    private User createStudentInTransaction(String localPart) {
        return transactionTemplate.execute(status -> {
            String email = uniquePrefix.substring(0, Math.min(20, uniquePrefix.length())) + "-" + localPart + "@s.com";
            User user = new User(email, UserRole.STUDENT);
            entityManager.persist(user);
            entityManager.flush();
            return user;
        });
    }

    /**
     * Создаёт и сохраняет пользователя с ролью ADMIN в транзакции.
     *
     * @param localPart локальная часть email
     * @return сохранённый пользователь
     */
    private User createAdminInTransaction(String localPart) {
        return transactionTemplate.execute(status -> {
            String email = uniquePrefix.substring(0, Math.min(20, uniquePrefix.length())) + "-" + localPart + "@a.com";
            User user = new User(email, UserRole.ADMIN);
            entityManager.persist(user);
            entityManager.flush();
            return user;
        });
    }

    /**
     * Создаёт и сохраняет категорию в транзакции.
     *
     * @param name название категории
     * @return сохранённая категория
     */
    private Category createCategoryInTransaction(String name) {
        return transactionTemplate.execute(status -> {
            String uniqueName = uniquePrefix.substring(0, Math.min(20, uniquePrefix.length())) + "-" + name;
            Category category = new Category(uniqueName);
            entityManager.persist(category);
            entityManager.flush();
            return category;
        });
    }

    /**
     * Создаёт и сохраняет тег в транзакции.
     *
     * @param name название тега
     * @return сохранённый тег
     */
    private Tag createTagInTransaction(String name) {
        return transactionTemplate.execute(status -> {
            Tag tag = new Tag(name);
            entityManager.persist(tag);
            entityManager.flush();
            return tag;
        });
    }

    /**
     * Создаёт и сохраняет запись на курс в транзакции.
     *
     * @param studentId идентификатор студента
     * @param courseId  идентификатор курса
     */
    private void createEnrollmentInTransaction(Long studentId, Long courseId) {
        transactionTemplate.execute(status -> {
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Студент не найден: " + studentId));
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Курс не найден: " + courseId));
            com.studying.platform.entity.Enrollment enrollment =
                    new com.studying.platform.entity.Enrollment(
                            student, course,
                            java.time.LocalDateTime.now(),
                            com.studying.platform.entity.EnrollmentStatus.ACTIVE);
            entityManager.persist(enrollment);
            entityManager.flush();
            enrollmentCourseIds.add(courseId);
            enrollmentUserEmails.add(student.getEmail());
            return null;
        });
    }
}
