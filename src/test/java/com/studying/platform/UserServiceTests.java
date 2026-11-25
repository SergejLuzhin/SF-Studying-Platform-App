package com.studying.platform;

import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Enrollment;
import com.studying.platform.entity.EnrollmentStatus;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.EnrollmentRepository;
import com.studying.platform.repository.UserRepository;
import com.studying.platform.service.EnrollmentService;
import com.studying.platform.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для сервисного слоя управления пользователями {@link UserService}.
 * <p>
 * Проверяют создание, чтение, валидацию и обработку дубликатов пользователей
 * через настоящего Spring-бин сервиса в профиле «test» с PostgreSQL через Testcontainers.
 * Также подтверждают, что пользователи, созданные сервисом, корректно используются
 * в существующих сервисах {@link com.studying.platform.service.CourseService}
 * и {@link EnrollmentService}.
 * </p>
 * <p>
 * Тесты выполняются без внешней транзакции теста, чтобы проверить поведение
 * реальных сервисных транзакций.
 * </p>
 * <p>
 * Очистка тестовых данных выполняется адресно через {@link TestDataCleanupHelper}
 * в транзакции {@code @AfterEach}, что гарантирует изоляцию между тестами.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
class UserServiceTests {

    /**
     * Сервис управления пользователями.
     */
    @Autowired
    private UserService userService;

    /**
     * Менеджер сущностей для создания фикстур и очистки данных.
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * Репозиторий пользователей для создания фикстур.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Репозиторий категорий для создания тестовых данных.
     */
    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Репозиторий курсов для интеграционных проверок.
     */
    @Autowired
    private CourseRepository courseRepository;

    /**
     * Репозиторий записей для интеграционных проверок.
     */
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    /**
     * Сервис записей для интеграционных проверок.
     */
    @Autowired
    private EnrollmentService enrollmentService;

    /**
     * Источник данных для проверки продукта базы данных.
     */
    @Autowired
    private DataSource dataSource;

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
     * Уникальный префикс для тестовых данных, обеспечивающий изоляцию между тестами.
     */
    private String uniquePrefix;

    /** Идентификаторы пользователей, созданных текущим тестом. */
    private final Set<Long> userIds = new java.util.HashSet<>();

    /**
     * Идентификатор созданного пользователя (отслеживается для адресной очистки).
     */
    private Long userId;

    /**
     * Идентификатор созданной категории (отслеживается для адресной очистки).
     */
    private Long categoryId;

    /**
     * Идентификатор созданного курса (отслеживается для адресной очистки).
     */
    private Long courseId;

    /**
     * Идентификатор созданной записи (отслеживается для адресной очистки).
     */
    private Long enrollmentId;

    /**
     * Инициализация уникального префикса перед каждым тестом.
     * <p>
     * Префикс ограничивается 14 символами, чтобы при добавлении suffix
     * и домена email оставался в пределах 50 символов.
     * </p>
     */
    @BeforeEach
    void setUp() {
        uniquePrefix = "test-" + System.currentTimeMillis() + "-" + new java.util.Random().nextInt(10000);
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
                Set.of(),       // submissionIds — решения не создаются
                Set.of(),       // reviewCourseIds — отзывы не создаются
                courseId != null ? Set.of(courseId) : Set.of(),
                Set.of(),       // enrollmentUserEmails — записи удаляются по course ID
                Set.of(),       // lessonIds — уроки не создаются
                Set.of(),       // moduleIds — модули не создаются
                courseId != null ? Set.of(courseId) : Set.of(),
                categoryId != null ? Set.of(categoryId) : Set.of(),
                userIds
        );
    }

    // --- Позитивные сценарии ---

    /**
     * Проверка: создание пользователя с ролью STUDENT.
     * <p>
     * Создаёт студента через сервис, затем проверяет, что пользователь
     * сохранён в базе данных и может быть прочитан через репозиторий.
     * </p>
     */
    @Test
    void createUser_withStudentRole_shouldPersistAndRetrieve() {
        String email = uniquePrefix + "-student@example.com";

        User student = userService.createUser(email, UserRole.STUDENT);
        userIds.add(student.getId());

        assertNotNull(student.getId());
        assertEquals(email, student.getEmail());
        assertEquals(UserRole.STUDENT, student.getRole());
        assertNull(student.getProfile());

        // Проверка сохранения в БД
        Optional<User> found = userRepository.findById(student.getId());
        assertTrue(found.isPresent());
        assertEquals(email, found.get().getEmail());
        assertEquals(UserRole.STUDENT, found.get().getRole());
    }

    /**
     * Проверка: создание пользователя с ролью TEACHER.
     * <p>
     * Создаёт преподавателя через сервис и проверяет корректность полей.
     * </p>
     */
    @Test
    void createUser_withTeacherRole_shouldPersistAndRetrieve() {
        String email = uniquePrefix + "-teacher@example.com";

        User teacher = userService.createUser(email, UserRole.TEACHER);
        userIds.add(teacher.getId());

        assertNotNull(teacher.getId());
        assertEquals(email, teacher.getEmail());
        assertEquals(UserRole.TEACHER, teacher.getRole());

        Optional<User> found = userRepository.findById(teacher.getId());
        assertTrue(found.isPresent());
        assertEquals(email, found.get().getEmail());
    }

    /**
     * Проверка: чтение существующего пользователя.
     * <p>
     * Создаёт пользователя, затем читает его через {@code getUser}.
     * </p>
     */
    @Test
    void getUser_shouldReturnExistingUser() {
        String email = uniquePrefix + "-read@example.com";
        User created = userService.createUser(email, UserRole.STUDENT);
        userIds.add(created.getId());
        userId = created.getId();

        User found = userService.getUser(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals(email, found.getEmail());
        assertEquals(UserRole.STUDENT, found.getRole());
    }

    /**
     * Проверка: чтение пользователя в новой транзакции.
     * <p>
     * Созданный пользователь должен быть доступен после завершения транзакции создания.
     * </p>
     */
    @Test
    void getUser_shouldBeAvailableOutsideTransaction() {
        String email = uniquePrefix + "-trans@example.com";
        User created = userService.createUser(email, UserRole.TEACHER);
        userIds.add(created.getId());
        userId = created.getId();

        // entityManager.clear() не вызывается — тест работает без внешней транзакции
        User found = userService.getUser(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals(email, found.getEmail());
    }

    // --- Негативные сценарии валидации ---

    /**
     * Проверка: null в качестве адреса электронной почты.
     */
    @Test
    void createUser_withNullEmail_shouldThrowConstraintViolationException() {
        assertThrows(ConstraintViolationException.class,
                () -> userService.createUser(null, UserRole.STUDENT));
    }

    /**
     * Проверка: пустая строка в качестве адреса электронной почты.
     */
    @Test
    void createUser_withEmptyEmail_shouldThrowConstraintViolationException() {
        assertThrows(ConstraintViolationException.class,
                () -> userService.createUser("", UserRole.STUDENT));
    }

    /**
     * Проверка: строка из пробелов в качестве адреса электронной почты.
     */
    @Test
    void createUser_withWhitespaceEmail_shouldThrowConstraintViolationException() {
        assertThrows(ConstraintViolationException.class,
                () -> userService.createUser("   ", UserRole.STUDENT));
    }

    /**
     * Проверка: некорректный формат адреса электронной почты.
     */
    @Test
    void createUser_withInvalidEmailFormat_shouldThrowConstraintViolationException() {
        assertThrows(ConstraintViolationException.class,
                () -> userService.createUser("not-an-email", UserRole.STUDENT));
    }

    /**
     * Проверка: адрес электронной почты длиннее 50 символов.
     */
    @Test
    void createUser_withTooLongEmail_shouldThrowConstraintViolationException() {
        String longEmail = "a".repeat(46) + "@ex.com"; // 51 символ
        assertThrows(ConstraintViolationException.class,
                () -> userService.createUser(longEmail, UserRole.STUDENT));
    }

    /**
     * Проверка: null в качестве роли пользователя.
     */
    @Test
    void createUser_withNullRole_shouldThrowConstraintViolationException() {
        String email = uniquePrefix + "-norole@example.com";
        assertThrows(ConstraintViolationException.class,
                () -> userService.createUser(email, null));
    }

    /**
     * Проверка: корректный адрес электронной почты с максимальной длиной (50 символов).
     */
    @Test
    void createUser_withMaxLengthEmail_shouldSucceed() {
        // 43 символа "a" + "@ex.com" = 50 символов
        String email = "a".repeat(43) + "@ex.com";
        assertEquals(50, email.length());
        User user = userService.createUser(email, UserRole.ADMIN);
        userIds.add(user.getId());
        assertNotNull(user.getId());
        assertEquals(email, user.getEmail());
    }

    // --- Сценарии дубликата ---

    /**
     * Проверка: повторная регистрация с тем же адресом электронной почты.
     */
    @Test
    void createUser_withDuplicateEmail_shouldThrowIllegalStateException() {
        String email = uniquePrefix + "-dup@example.com";

        userIds.add(userService.createUser(email, UserRole.STUDENT).getId());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> userService.createUser(email, UserRole.TEACHER));
        assertTrue(exception.getMessage().contains("уже существует"));
    }

    // --- Сценарий отсутствия ---

    /**
     * Проверка: чтение пользователя с несуществующим идентификатором.
     */
    @Test
    void getUser_withNonExistentId_shouldThrowResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUser(99999L));
    }

    // --- Интеграция с CourseService ---

    /**
     * Проверка: преподаватель, созданный UserService, может быть использован для создания курса.
     * <p>
     * Создаёт категорию через репозиторий, преподавателя через UserService,
     * затем сохраняет курс через CourseRepository. Проверяет, что все объекты
     * связаны корректно, а ID преподавателя совпадают.
     * </p>
     */
    @Test
    void createdTeacher_canBeUsedForCourseCreation() {
        // Создание категории через репозиторий
        Category category = new Category("Информатика");
        category = categoryRepository.saveAndFlush(category);
        categoryId = category.getId();

        // Создание преподавателя через UserService
        String teacherEmail = uniquePrefix + "-course-teacher@example.com";
        User teacher = userService.createUser(teacherEmail, UserRole.TEACHER);
        userIds.add(teacher.getId());
        userId = teacher.getId();

        // Создание курса через CourseRepository напрямую (проверка связи)
        Course course = new Course("Java-курс", "Изучение Java", 40, LocalDate.now().plusDays(30), teacher, category);
        Course saved = courseRepository.saveAndFlush(course);
        courseId = saved.getId();

        assertEquals(teacher.getId(), saved.getTeacher().getId());
        assertEquals(category.getId(), saved.getCategory().getId());
        assertEquals("Java-курс", saved.getTitle());
    }

    // --- Интеграция с EnrollmentService ---

    /**
     * Проверка: студент, созданный UserService, может быть записан через EnrollmentService.
     * <p>
     * Создаёт преподавателя, категорию, курс и студента (все через сервисы/репозитории),
     * затем записывает студента на курс через Spring-бин EnrollmentService.
     * </p>
     */
    @Test
    void createdStudent_canBeEnrolledViaEnrollmentService() {
        // Создание преподавателя через UserService
        String teacherEmail = uniquePrefix + "-enroll-teacher@example.com";
        User teacher = userService.createUser(teacherEmail, UserRole.TEACHER);
        userIds.add(teacher.getId());
        userId = teacher.getId();

        // Создание категории и курса через репозитории
        Category category = new Category("Математика");
        category = categoryRepository.saveAndFlush(category);
        categoryId = category.getId();

        Course course = new Course("Алгебра", "Основы алгебры", 20, LocalDate.now().plusDays(14), teacher, category);
        course = courseRepository.saveAndFlush(course);
        courseId = course.getId();

        // Создание студента через UserService
        String studentEmail = uniquePrefix + "-enroll-student@example.com";
        User student = userService.createUser(studentEmail, UserRole.STUDENT);
        userIds.add(student.getId());
        userId = student.getId();

        // Запись студента через Spring-бин EnrollmentService
        Enrollment enrollment = enrollmentService.enrollStudent(student.getId(), course.getId());
        enrollmentId = enrollment.getId();

        assertNotNull(enrollment.getId());
        assertEquals(EnrollmentStatus.ACTIVE, enrollment.getStatus());
        assertEquals(student.getId(), enrollment.getUser().getId());
        assertEquals(course.getId(), enrollment.getCourse().getId());
    }
}
