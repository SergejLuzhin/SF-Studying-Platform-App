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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для проверки доступности данных списков курсов и студентов
 * вне рамки транзакции сервиса {@link EnrollmentService}.
 * <p>
 * Тесты используют {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)},
 * чтобы каждый вызов сервиса происходил в отдельной транзакции,
 * а после возврата результаты были доступны без {@code LazyInitializationException}.
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
     * Уникальный префикс для тестовых данных.
     */
    private String uniquePrefix;

    /**
     * Инициализация уникального префикса перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        uniquePrefix = "txn-" + System.currentTimeMillis() + "-" + new java.util.Random().nextInt(10000);
    }

    /**
     * Очистка тестовых данных после каждого теста в порядке зависимостей.
     */
    @AfterEach
    void tearDown() {
        transactionTemplate.execute(status -> {
            try {
                entityManager.createQuery("DELETE FROM Enrollment").executeUpdate();
            } catch (Exception ignored) {
            }
            try {
                entityManager.createQuery("DELETE FROM Course").executeUpdate();
            } catch (Exception ignored) {
            }
            try {
                entityManager.createQuery("DELETE FROM Category").executeUpdate();
            } catch (Exception ignored) {
            }
            try {
                entityManager.createQuery("DELETE FROM User WHERE email LIKE :prefix")
                        .setParameter("prefix", uniquePrefix + "%")
                        .executeUpdate();
            } catch (Exception ignored) {
            }
            return null;
        });
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

        User teacher = new User(uniquePrefix + "-teacher@example.com", UserRole.TEACHER);
        userRepository.saveAndFlush(teacher);

        User student1 = new User(uniquePrefix + "-student1@example.com", UserRole.STUDENT);
        userRepository.saveAndFlush(student1);

        User student2 = new User(uniquePrefix + "-student2@example.com", UserRole.STUDENT);
        userRepository.saveAndFlush(student2);

        Course course1 = new Course("Алгебра", "Основы алгебры", 20, LocalDate.now().plusDays(30), teacher, category);
        Course savedCourse1 = courseRepository.saveAndFlush(course1);

        Course course2 = new Course("Геометрия", "Основы геометрии", 20, LocalDate.now().plusDays(30), teacher, category);
        Course savedCourse2 = courseRepository.saveAndFlush(course2);

        // Запись студента на курсы (в отдельных транзакциях через сервис)
        enrollmentService.enrollStudent(student1.getId(), savedCourse1.getId());
        enrollmentService.enrollStudent(student1.getId(), savedCourse2.getId());

        // Вызов сервиса
        List<Course> courses = enrollmentService.getStudentCourses(student1.getId());

        // Проверка: транзакция закрыта
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "Транзакция должна быть закрыта при чтении результатов");

        // Проверка: скалярные поля доступны
        assertEquals(2, courses.size());
        assertTrue(courses.stream().anyMatch(c -> (c.getId().equals(savedCourse1.getId())
                && "Алгебра".equals(c.getTitle()))));
        assertTrue(courses.stream().anyMatch(c -> (c.getId().equals(savedCourse2.getId())
                && "Геометрия".equals(c.getTitle()))));
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

        User teacher = new User(uniquePrefix + "-phys-teacher@example.com", UserRole.TEACHER);
        userRepository.saveAndFlush(teacher);

        User student1 = new User(uniquePrefix + "-phys-student1@example.com", UserRole.STUDENT);
        userRepository.saveAndFlush(student1);

        User student2 = new User(uniquePrefix + "-phys-student2@example.com", UserRole.STUDENT);
        userRepository.saveAndFlush(student2);

        Course course = new Course("Квантовая физика", "Введение в квантовую физику", 30, LocalDate.now().plusDays(30), teacher, category);
        Course savedCourse = courseRepository.saveAndFlush(course);

        // Запись студентов на курс
        enrollmentService.enrollStudent(student1.getId(), savedCourse.getId());
        enrollmentService.enrollStudent(student2.getId(), savedCourse.getId());

        // Вызов сервиса
        List<User> students = enrollmentService.getCourseStudents(savedCourse.getId());

        // Проверка: транзакция закрыта
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "Транзакция должна быть закрыта при чтении результатов");

        // Проверка: скалярные поля доступны
        assertEquals(2, students.size());
        assertTrue(students.stream().anyMatch(u -> (u.getId().equals(student1.getId())
                && (uniquePrefix + "-phys-student1@example.com").equals(u.getEmail()))));
        assertTrue(students.stream().anyMatch(u -> (u.getId().equals(student2.getId())
                && (uniquePrefix + "-phys-student2@example.com").equals(u.getEmail()))));
    }

    /**
     * Проверка: фильтрация по статусу ACTIVE — отписанные студенты не возвращаются.
     */
    @Test
    void getCourseStudents_filtersOutDroppedStudents() {
        // Подготовка
        Category category = new Category("Химия");
        category = categoryRepository.saveAndFlush(category);

        User teacher = new User(uniquePrefix + "-chem-teacher@example.com", UserRole.TEACHER);
        userRepository.saveAndFlush(teacher);

        User student1 = new User(uniquePrefix + "-chem-active@example.com", UserRole.STUDENT);
        userRepository.saveAndFlush(student1);

        User student2 = new User(uniquePrefix + "-chem-dropped@example.com", UserRole.STUDENT);
        userRepository.saveAndFlush(student2);

        Course course = new Course("Органическая химия", "Основы органической химии", 25, LocalDate.now().plusDays(30), teacher, category);
        Course savedCourse = courseRepository.saveAndFlush(course);

        // Студент 1 активен, студент 2 отписан
        enrollmentService.enrollStudent(student1.getId(), savedCourse.getId());
        enrollmentService.enrollStudent(student2.getId(), savedCourse.getId());
        enrollmentService.dropEnrollment(student2.getId(), savedCourse.getId());

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

        User teacher = new User(uniquePrefix + "-bio-teacher@example.com", UserRole.TEACHER);
        userRepository.saveAndFlush(teacher);

        Course course = new Course("Ботаника", "Основы ботаники", 15, LocalDate.now().plusDays(20), teacher, category);
        Course savedCourse = courseRepository.saveAndFlush(course);

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

        User teacher = new User(uniquePrefix + "-hist-teacher@example.com", UserRole.TEACHER);
        userRepository.saveAndFlush(teacher);

        User student = new User(uniquePrefix + "-hist-student@example.com", UserRole.STUDENT);
        userRepository.saveAndFlush(student);

        Course courseA = new Course("Средневековая история", "История Средневековья", 20, LocalDate.now().plusDays(30), teacher, category);
        Course savedCourseA = courseRepository.saveAndFlush(courseA);

        Course courseB = new Course("Новая история", "История Нового времени", 20, LocalDate.now().plusDays(30), teacher, category);
        Course savedCourseB = courseRepository.saveAndFlush(courseB);

        enrollmentService.enrollStudent(student.getId(), savedCourseA.getId());
        enrollmentService.enrollStudent(student.getId(), savedCourseB.getId());

        List<Course> courses = enrollmentService.getStudentCourses(student.getId());

        assertEquals(2, courses.size());

        // Проверяем по ID и названию, а не по позиции
        var ids = courses.stream().map(Course::getId).toList();
        var titles = courses.stream().map(Course::getTitle).toList();

        assertTrue(ids.contains(savedCourseA.getId()));
        assertTrue(ids.contains(savedCourseB.getId()));
        assertTrue(titles.contains("Средневековая история"));
        assertTrue(titles.contains("Новая история"));
    }
}
