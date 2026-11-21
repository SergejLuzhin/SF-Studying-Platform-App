package com.studying.platform;

import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.EnrollmentStatus;
import com.studying.platform.entity.Lesson;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.service.CourseService;
import com.studying.platform.service.EnrollmentService;
import com.studying.platform.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Сквозной интеграционный тест MVP-сценария через реальные Spring-бины сервисов.
 * <p>
 * Тест выполняет полный жизненный цикл MVP: создание преподавателя и студента через
 * {@link UserService}, создание категории, курса, модуля и урока через {@link CourseService},
 * запись студента через {@link EnrollmentService}, чтение структуры и списков вне транзакции,
 * отписку, проверку статуса {@code DROPPED}, пустых активных списков и запрета повторной записи.
 * </p>
 * <p>
 * Тест использует {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)},
 * чтобы каждый вызов сервисных методов происходил в собственной транзакции,
 * а данные, возвращаемые сервисами, были доступны после завершения вызова.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MvpFlowTests {

    /**
     * Сервис управления пользователями.
     */
    @Autowired
    private UserService userService;

    /**
     * Сервис управления курсами.
     */
    @Autowired
    private CourseService courseService;

    /**
     * Сервис управления записями на курсы.
     */
    @Autowired
    private EnrollmentService enrollmentService;

    /**
     * Репозиторий категорий для создания фикстур.
     */
    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Менеджер сущностей для очистки тестовых данных.
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * Уникальный префикс для тестовых данных.
     */
    private String uniquePrefix;

    /**
     * Инициализация уникального префикса перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        uniquePrefix = "mvp-" + System.currentTimeMillis() + "-" + new java.util.Random().nextInt(10000);
    }

    /**
     * Очистка тестовых данных после каждого теста в порядке зависимостей.
     */
    @AfterEach
    void tearDown() {
        // Порядок удаления: записи → курс → модули → уроки → категория → пользователи
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
    }

    /**
     * Сквозной тест MVP-сценария: от создания пользователей до отписки и проверки запретов.
     * <p>
     * Последовательно выполняет: создание преподавателя и студента, создание категории,
     * курса, модуля и урока, запись студента, чтение структуры курса и списков студентов/курсов,
     * отписку студента, проверку статуса DROPPED, проверку пустых активных списков и запрет
     * повторной записи.
     * </p>
     */
    @Test
    void fullMvpFlow_createEnrollReadDropReenrollmentBlocked() {
        // 1. Создание преподавателя через сервис
        User teacher = userService.createUser(
                uniquePrefix + "-teacher@example.com", UserRole.TEACHER);
        assertNotNull(teacher.getId(), "Преподаватель должен иметь ID после создания");

        // 2. Создание студента через сервис
        User student = userService.createUser(
                uniquePrefix + "-student@example.com", UserRole.STUDENT);
        assertNotNull(student.getId(), "Студент должен иметь ID после создания");

        // 3. Проверка: транзакции закрыты после каждого вызова сервиса
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "После createUser транзакция должна быть закрыта");

        // 4. Создание категории через репозиторий
        Category category = new Category("Математика");
        category = categoryRepository.saveAndFlush(category);

        // 5. Создание курса через сервис
        Course course = courseService.createCourse(
                "Высшая математика", "Курс по высшей математике", 40,
                LocalDate.now().plusDays(30),
                teacher.getId(), category.getId());
        assertNotNull(course.getId(), "Курс должен иметь ID после создания");

        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "После createCourse транзакция должна быть закрыта");

        // 6. Добавление модуля и урока через сервис
        Module module = courseService.addModule(
                course.getId(),
                "Линейная алгебра",
                "Матрицы, определители, системы уравнений",
                1);
        assertNotNull(module.getId(), "Модуль должен иметь ID после создания");

        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "После addModule транзакция должна быть закрыта");

        Lesson lesson = courseService.addLesson(
                module.getId(),
                "Матрицы",
                "Основные операции над матрицами",
                "https://example.com/matrices");
        assertNotNull(lesson.getId(), "Урок должен иметь ID после создания");

        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "После addLesson транзакция должна быть закрыта");

        // 7. Запись студента на курс через сервис
        enrollmentService.enrollStudent(student.getId(), course.getId());

        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "После enrollStudent транзакция должна быть закрыта");

        // 8. Проверка структуры курса вне транзакции
        Course structure = courseService.getCourseStructure(course.getId());
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "После getCourseStructure транзакция должна быть закрыта");

        assertEquals("Высшая математика", structure.getTitle());
        assertEquals(1, structure.getModules().size(), "Должен быть один модуль");

        Module firstModule = structure.getModules().iterator().next();
        assertEquals("Линейная алгебра", firstModule.getTitle());
        assertEquals(1, firstModule.getLessons().size(), "В модуле должен быть один урок");
        Lesson firstLesson = firstModule.getLessons().iterator().next();
        assertEquals("Матрицы", firstLesson.getTitle());

        // 9. Проверка списков студентов курса и курсов студента вне транзакции
        List<User> courseStudents = enrollmentService.getCourseStudents(course.getId());
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "После getCourseStudents транзакция должна быть закрыта");

        assertEquals(1, courseStudents.size(), "Должен быть один активный студент");
        assertEquals(student.getId(), courseStudents.get(0).getId());
        assertEquals(uniquePrefix + "-student@example.com", courseStudents.get(0).getEmail());

        List<Course> studentCourses = enrollmentService.getStudentCourses(student.getId());
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "После getStudentCourses транзакция должна быть закрыта");

        assertEquals(1, studentCourses.size(), "У студента должен быть один активный курс");
        assertEquals(course.getId(), studentCourses.get(0).getId());
        assertEquals("Высшая математика", studentCourses.get(0).getTitle());

        // 10. Отписка студента от курса
        enrollmentService.dropEnrollment(student.getId(), course.getId());

        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "После dropEnrollment транзакция должна быть закрыта");

        // 11. Проверка статуса DROPPED
        EnrollmentStatus status = enrollmentService.getEnrollmentStatus(student.getId(), course.getId());
        assertEquals(EnrollmentStatus.DROPPED, status,
                "Статус записи должен быть DROPPED после отписки");

        // 12. Проверка пустых активных списков
        List<User> activeStudents = enrollmentService.getCourseStudents(course.getId());
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertTrue(activeStudents.isEmpty(),
                "После отписки активных студентов курса не должно быть");

        List<Course> activeCourses = enrollmentService.getStudentCourses(student.getId());
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertTrue(activeCourses.isEmpty(),
                "После отписки активных курсов студента не должно быть");

        // 13. Проверка запрета повторной записи
        IllegalStateException reEnrollException = assertThrows(
                IllegalStateException.class,
                () -> enrollmentService.enrollStudent(student.getId(), course.getId()),
                "Повторная запись после отписки должна быть запрещена");

        assertTrue(reEnrollException.getMessage()
                        .contains("уже был записан"),
                "Сообщение об ошибке должно содержать информацию о повторной записи");

        // 14. Проверка: исходная запись и статус не изменены после попытки
        assertEquals(EnrollmentStatus.DROPPED,
                enrollmentService.getEnrollmentStatus(student.getId(), course.getId()),
                "Статус должен остаться DROPPED");
    }

    /**
     * Проверка: преподаватель и студент, созданные через UserService,
     * корректно используются в CourseService и EnrollmentService.
     */
    @Test
    void userServiceIntegration_courseAndEnrollmentWork() {
        // Создание пользователей через сервис
        User createdTeacher = userService.createUser(
                uniquePrefix + "-int-teacher@example.com", UserRole.TEACHER);
        User createdStudent = userService.createUser(
                uniquePrefix + "-int-student@example.com", UserRole.STUDENT);

        // Создание категории через репозиторий (допустимо для тестовых фикстур)
        Category category = new Category("Физика");
        category = categoryRepository.saveAndFlush(category);

        // Создание курса через сервис с преподавателем из UserService
        Course course = courseService.createCourse(
                "Физика для начинающих", "Основы физики", 25,
                LocalDate.now().plusDays(15),
                createdTeacher.getId(), category.getId());

        // Запись студента из UserService через EnrollmentService
        enrollmentService.enrollStudent(createdStudent.getId(), course.getId());

        // Проверка записи
        List<Course> courses = enrollmentService.getStudentCourses(createdStudent.getId());
        assertEquals(1, courses.size());
        assertEquals(course.getId(), courses.get(0).getId());

        // Проверка: оба пользователя существуют и корректны
        User loadedTeacher = userService.getUser(createdTeacher.getId());
        assertEquals(createdTeacher.getEmail(), loadedTeacher.getEmail());
        assertEquals(UserRole.TEACHER, loadedTeacher.getRole());

        User loadedStudent = userService.getUser(createdStudent.getId());
        assertEquals(createdStudent.getEmail(), loadedStudent.getEmail());
        assertEquals(UserRole.STUDENT, loadedStudent.getRole());
    }

    /**
     * Проверка: повторная запись на активный курс запрещается.
     */
    @Test
    void reenrollmentActiveCourse_shouldThrow() {
        User teacher = userService.createUser(
                uniquePrefix + "-active-teacher@example.com", UserRole.TEACHER);
        User student = userService.createUser(
                uniquePrefix + "-active-student@example.com", UserRole.STUDENT);

        Category category = new Category("Химия");
        category = categoryRepository.saveAndFlush(category);

        Course course = courseService.createCourse(
                "Органическая химия", "Основы органической химии", 30,
                LocalDate.now().plusDays(20),
                teacher.getId(), category.getId());

        // Первая запись успешна
        enrollmentService.enrollStudent(student.getId(), course.getId());

        // Повторная запись бросает исключение
        assertThrows(IllegalStateException.class,
                () -> enrollmentService.enrollStudent(student.getId(), course.getId()),
                "Повторная запись на активный курс должна быть запрещена");

        // Статус остаётся ACTIVE
        assertEquals(EnrollmentStatus.ACTIVE,
                enrollmentService.getEnrollmentStatus(student.getId(), course.getId()));
    }

    /**
     * Проверка: ResourceNotFoundException при записи на несуществующий курс.
     */
    @Test
    void enrollNonExistentCourse_shouldThrow() {
        User student = userService.createUser(
                uniquePrefix + "-nobody@example.com", UserRole.STUDENT);

        assertThrows(ResourceNotFoundException.class,
                () -> enrollmentService.enrollStudent(student.getId(), 99999L));
    }

    /**
     * Проверка: ресурсNotFoundException при чтении структуры несуществующего курса.
     */
    @Test
    void getNonExistentCourseStructure_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> courseService.getCourseStructure(99999L));
    }
}
