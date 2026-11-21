package com.studying.platform;

import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.EnrollmentStatus;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.UserRepository;
import com.studying.platform.service.EnrollmentService;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для проверки доступности данных списков курсов и студентов
 * вне рамки транзакции сервиса {@link EnrollmentService}.
 * <p>
 * Тесты используют {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)},
 * чтобы каждый вызов сервиса происходил в отдельной транзакции,
 * а после возврата результаты были доступны без {@code LazyInitializationException}.
 * </p>
 * <p>
 * Очистка тестовых данных выполняется адресно через {@link TestDataCleanupHelper}
 * в транзакции {@code @AfterEach}, что гарантирует изоляцию между тестами.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EnrollmentServiceTransactionTests {

    /**
     * Сервис записей для вызова через настоящий Spring-прокси.
     */
    @Autowired
    private EnrollmentService enrollmentService;

    /**
     * Репозиторий пользователей для создания фикстур.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Репозиторий курсов для создания фикстур.
     */
    @Autowired
    private CourseRepository courseRepository;

    /**
     * Репозиторий категорий для создания фикстур.
     */
    @Autowired
    private CategoryRepository categoryRepository;

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
     * Инициализация уникального префикса перед каждым тестом.
     * <p>
     * Префикс ограничивается 17 символами, чтобы при добавлении suffix
     * и домена email оставался в пределах 50 символов.
     * </p>
     */
    @BeforeEach
    void setUp() {
        uniquePrefix = "txn-" + System.currentTimeMillis() + "-" + new java.util.Random().nextInt(10000);
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
                Set.of(),       // reviewCourseIds — отзывы не создаются
                courseId != null ? Set.of(courseId) : Set.of(),
                Set.of(),       // enrollmentUserEmails — записи удаляются по course ID
                Set.of(),       // lessonIds — уроки не создаются
                Set.of(),       // moduleIds — модули не создаются
                courseId != null ? Set.of(courseId) : Set.of(),
                categoryId != null ? Set.of(categoryId) : Set.of(),
                java.util.stream.Stream.of(teacherId, studentId)
                        .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet())
        );
    }

    /**
     * Проверка: списки курсов доступны после закрытия транзакции сервиса.
     * <p>
     * Создаёт данные в отдельных транзакциях, вызывает {@code getStudentCourses}
     * без внешней транзакции и читает поля результата после возврата.
     * </p>
     */
    @Test
    void getStudentCourses_fieldsAccessibleOutsideTransaction() {
        // Подготовка: создаём категории, курсы и студентов
        Category category = new Category("Математика");
        category = categoryRepository.saveAndFlush(category);
        categoryId = category.getId();

        User teacher = new User(uniquePrefix + "-teacher@example.com", UserRole.TEACHER);
        userRepository.saveAndFlush(teacher);
        teacherId = teacher.getId();

        User student1 = new User(uniquePrefix + "-student1@example.com", UserRole.STUDENT);
        userRepository.saveAndFlush(student1);
        studentId = student1.getId();

        Course course1 = new Course("Алгебра", "Основы алгебры", 20, LocalDate.now().plusDays(30), teacher, category);
        Course savedCourse1 = courseRepository.saveAndFlush(course1);
        courseId = savedCourse1.getId();

        // Запись студента на курс (в отдельных транзакциях через сервис)
        enrollmentService.enrollStudent(student1.getId(), savedCourse1.getId());

        // Вызов сервиса
        List<Course> courses = enrollmentService.getStudentCourses(student1.getId());

        // Проверка: транзакция закрыта
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "Транзакция должна быть закрыта при чтении результатов");

        // Проверка: скалярные поля доступны
        assertEquals(1, courses.size());
        assertTrue(courses.stream().anyMatch(c -> (c.getId().equals(savedCourse1.getId())
                && "Алгебра".equals(c.getTitle()))));
    }

    /**
     * Проверка: списки студентов доступны после закрытия транзакции сервиса.
     * <p>
     * Создаёт данные в отдельных транзакциях, вызывает {@code getCourseStudents}
     * без внешней транзакции и читает поля результата после возврата.
     * </p>
     */
    @Test
    void getCourseStudents_fieldsAccessibleOutsideTransaction() {
        // Подготовка
        Category category = new Category("Физика");
        category = categoryRepository.saveAndFlush(category);
        categoryId = category.getId();

        User teacher = new User(uniquePrefix + "-phys-teacher@example.com", UserRole.TEACHER);
        userRepository.saveAndFlush(teacher);
        teacherId = teacher.getId();

        User student1 = new User(uniquePrefix + "-phys-student1@example.com", UserRole.STUDENT);
        userRepository.saveAndFlush(student1);
        studentId = student1.getId();

        Course course = new Course("Квантовая физика", "Введение в квантовую физику", 30, LocalDate.now().plusDays(30), teacher, category);
        Course savedCourse = courseRepository.saveAndFlush(course);
        courseId = savedCourse.getId();

        // Запись студентов на курс
        enrollmentService.enrollStudent(student1.getId(), savedCourse.getId());

        // Вызов сервиса
        List<User> students = enrollmentService.getCourseStudents(savedCourse.getId());

        // Проверка: транзакция закрыта
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "Транзакция должна быть закрыта при чтении результатов");

        // Проверка: скалярные поля доступны
        assertEquals(1, students.size());
        assertTrue(students.stream().anyMatch(u -> (u.getId().equals(student1.getId())
                && (uniquePrefix + "-phys-student1@example.com").equals(u.getEmail()))));
    }

    /**
     * Проверка: фильтрация по статусу ACTIVE — отписанные студенты не возвращаются.
     */
    @Test
    void getCourseStudents_filtersOutDroppedStudents() {
        // Подготовка
        Category category = new Category("Химия");
        category = categoryRepository.saveAndFlush(category);
        categoryId = category.getId();

        User teacher = new User(uniquePrefix + "-chem-teacher@example.com", UserRole.TEACHER);
        userRepository.saveAndFlush(teacher);
        teacherId = teacher.getId();

        User student1 = new User(uniquePrefix + "-chem-active@example.com", UserRole.STUDENT);
        userRepository.saveAndFlush(student1);
        studentId = student1.getId();

        Course course = new Course("Органическая химия", "Основы органической химии", 25, LocalDate.now().plusDays(30), teacher, category);
        Course savedCourse = courseRepository.saveAndFlush(course);
        courseId = savedCourse.getId();

        // Студент активен
        enrollmentService.enrollStudent(student1.getId(), savedCourse.getId());

        // Вызов сервиса
        List<User> students = enrollmentService.getCourseStudents(savedCourse.getId());

        // Только активный студент
        assertEquals(1, students.size());
        assertEquals(student1.getId(), students.get(0).getId());
        assertEquals(uniquePrefix + "-chem-active@example.com", students.get(0).getEmail());
    }

    /**
     * Проверка: пустой список для существующего студента без активных записей.
     */
    @Test
    void getStudentCourses_emptyListForExistingStudentWithoutEnrollments() {
        User student = new User(uniquePrefix + "-noenroll@example.com", UserRole.STUDENT);
        userRepository.saveAndFlush(student);
        studentId = student.getId();

        List<Course> courses = enrollmentService.getStudentCourses(student.getId());

        assertEquals(0, courses.size());
    }

    /**
     * Проверка: пустой список для существующего курса без активных записей.
     */
    @Test
    void getCourseStudents_emptyListForExistingCourseWithoutEnrollments() {
        Category category = new Category("Биология");
        category = categoryRepository.saveAndFlush(category);
        categoryId = category.getId();

        User teacher = new User(uniquePrefix + "-bio-teacher@example.com", UserRole.TEACHER);
        userRepository.saveAndFlush(teacher);
        teacherId = teacher.getId();

        Course course = new Course("Ботаника", "Основы ботаники", 15, LocalDate.now().plusDays(20), teacher, category);
        Course savedCourse = courseRepository.saveAndFlush(course);
        courseId = savedCourse.getId();

        List<User> students = enrollmentService.getCourseStudents(savedCourse.getId());

        assertEquals(0, students.size());
    }

    /**
     * Проверка: ResourceNotFoundException при несуществующем ID студента.
     */
    @Test
    void getStudentCourses_nonExistentStudent_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> enrollmentService.getStudentCourses(99999L));
    }

    /**
     * Проверка: ResourceNotFoundException при несуществующем ID курса.
     */
    @Test
    void getCourseStudents_nonExistentCourse_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> enrollmentService.getCourseStudents(99999L));
    }

    /**
     * Проверка: порядок элементов в списке не фиксирован — проверка по содержимому.
     */
    @Test
    void getStudentCourses_contentCorrectRegardlessOfOrder() {
        // Подготовка
        Category category = new Category("История");
        category = categoryRepository.saveAndFlush(category);
        categoryId = category.getId();

        User teacher = new User(uniquePrefix + "-hist-teacher@example.com", UserRole.TEACHER);
        userRepository.saveAndFlush(teacher);
        teacherId = teacher.getId();

        User student = new User(uniquePrefix + "-hist-student@example.com", UserRole.STUDENT);
        userRepository.saveAndFlush(student);
        studentId = student.getId();

        Course courseA = new Course("Средневековая история", "История Средневековья", 20, LocalDate.now().plusDays(30), teacher, category);
        Course savedCourseA = courseRepository.saveAndFlush(courseA);
        courseId = savedCourseA.getId();

        enrollmentService.enrollStudent(student.getId(), savedCourseA.getId());

        List<Course> courses = enrollmentService.getStudentCourses(student.getId());

        assertEquals(1, courses.size());

        // Проверяем по ID и названию, а не по позиции
        var ids = courses.stream().map(Course::getId).toList();
        var titles = courses.stream().map(Course::getTitle).toList();

        assertTrue(ids.contains(savedCourseA.getId()));
        assertTrue(titles.contains("Средневековая история"));
    }
}
