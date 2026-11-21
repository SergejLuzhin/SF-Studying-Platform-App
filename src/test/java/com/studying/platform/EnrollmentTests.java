package com.studying.platform;

import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Enrollment;
import com.studying.platform.entity.EnrollmentStatus;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.EnrollmentRepository;
import com.studying.platform.repository.UserRepository;
import com.studying.platform.service.EnrollmentService;
import com.studying.platform.exception.ResourceNotFoundException;

import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для сущности «Запись на курс» {@link Enrollment}
 * и сервисного слоя {@link EnrollmentService}.
 * <p>
 * Проверяют корректность записи студента на курс, отписки, выборки
 * активных записей, а также обработку ошибок: неверная роль, повторная запись,
 * отсутствие записи, несуществующие объекты.
 * </p>
 * <p>
 * Тесты выполняются с PostgreSQL через Testcontainers в профиле «test».
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EnrollmentService.class)
class EnrollmentTests {

    /**
     * Тестовый менеджер сущностей для создания тестовых данных.
     */
    @Autowired
    private TestEntityManager testEntityManager;

    /**
     * Репозиторий для сущности «Пользователь».
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Репозиторий для сущности «Курс».
     */
    @Autowired
    private CourseRepository courseRepository;

    /**
     * Репозиторий для сущности «Запись на курс».
     */
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    /**
     * Сервис для управления записью на курс.
     */
    @Autowired
    private EnrollmentService enrollmentService;

    /**
     * Менеджер сущностей для ручного управления транзакциями.
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * Источник данных для проверки продукта базы данных.
     */
    @Autowired
    private DataSource dataSource;

    /**
     * Позитивный тест: запись студента на курс.
     * <p>
     * Создаёт студента, преподавателя и курс, затем записывает
     * студента на курс через сервис. Проверяет создание записи
     * со статусом ACTIVE и корректной датой.
     * </p>
     */
    @Test
    void enrollStudent_shouldCreateActiveRecord() {
        // Подготовка: преподаватель и курс
        User teacher = new User("teachertest@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Математика");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("Высшая математика", "Курс по высшей математике", 40,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        // Подготовка: студент
        User student = new User("studenttest@example.com", UserRole.STUDENT);
        entityManager.persist(student);
        entityManager.flush();

        // Выполнение: запись студента на курс
        Enrollment enrollment = enrollmentService.enrollStudent(student.getId(), course.getId());

        // Проверка
        assertNotNull(enrollment.getId(), "Идентификатор записи не должен быть null");
        assertEquals(EnrollmentStatus.ACTIVE, enrollment.getStatus(),
                "Статус записи должен быть ACTIVE");
        assertNotNull(enrollment.getEnrollDate(), "Дата записи не должна быть null");
        assertEquals(student.getId(), enrollment.getUser().getId(),
                "Запись должна ссылаться на студента");
        assertEquals(course.getId(), enrollment.getCourse().getId(),
                "Запись должна ссылаться на курс");

        // Проверка сохранения в БД
        Enrollment persisted = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertEquals(EnrollmentStatus.ACTIVE, persisted.getStatus(),
                "Запись должна быть сохранена в базе данных со статусом ACTIVE");
    }

    /**
     * Позитивный тест: отписка студента от курса.
     * <p>
     * Записывает студента на курс, затем отписывает его.
     * Проверяет, что статус изменился на DROPPED, запись не удалена.
     * </p>
     */
    @Test
    void dropEnrollment_shouldMarkAsDropped() {
        // Подготовка
        User teacher = new User("dropteacher@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Физика");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("Квантовая физика", "Основы квантовой механики", 35,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        User student = new User("dropstudent@example.com", UserRole.STUDENT);
        entityManager.persist(student);
        entityManager.flush();

        // Запись
        Enrollment enrollment = enrollmentService.enrollStudent(student.getId(), course.getId());
        assertEquals(EnrollmentStatus.ACTIVE, enrollment.getStatus());

        // Отписка
        Enrollment dropped = enrollmentService.dropEnrollment(student.getId(), course.getId());

        // Проверка
        assertEquals(EnrollmentStatus.DROPPED, dropped.getStatus(),
                "Статус записи должен измениться на DROPPED");

        // Проверка сохранения в БД
        Enrollment persisted = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertEquals(EnrollmentStatus.DROPPED, persisted.getStatus(),
                "Запись должна быть сохранена в БД со статусом DROPPED");

        // Проверка: активная запись больше не находится
        var activeFound = enrollmentRepository
                .findByUserIdAndCourseIdAndStatus(student.getId(), course.getId(), EnrollmentStatus.ACTIVE);
        assertTrue(activeFound.isEmpty(),
                "Активная запись после отписки не должна находиться");
    }

    /**
     * Позитивный тест: выборка активных курсов студента.
     * <p>
     * Записывает студента на несколько курсов, проверяет, что
     * метод возвращает корректный список.
     * </p>
     */
    @Test
    void getStudentCourses_shouldReturnActiveCourses() {
        // Подготовка: преподаватель и категории
        User teacher = new User("coursesstudentteacher@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category cat1 = new Category("Программирование");
        entityManager.persist(cat1);
        entityManager.flush();

        Category cat2 = new Category("Дизайн");
        entityManager.persist(cat2);
        entityManager.flush();

        Course course1 = new Course("Java для начинающих", "Основы Java", 20,
                LocalDate.of(2026, 9, 1), teacher, cat1);
        entityManager.persist(course1);
        entityManager.flush();

        Course course2 = new Course("UI/UX Дизайн", "Основы дизайна", 15,
                LocalDate.of(2026, 10, 1), teacher, cat2);
        entityManager.persist(course2);
        entityManager.flush();

        // Студент
        User student = new User("coursesstudent@example.com", UserRole.STUDENT);
        entityManager.persist(student);
        entityManager.flush();

        // Запись на оба курса
        enrollmentService.enrollStudent(student.getId(), course1.getId());
        enrollmentService.enrollStudent(student.getId(), course2.getId());

        // Выполнение: получение курсов
        List<Course> studentCourses = enrollmentService.getStudentCourses(student.getId());

        // Проверка
        assertEquals(2, studentCourses.size(),
                "Студент должен быть записан на 2 активных курса");
        assertTrue(studentCourses.stream().anyMatch(c -> c.getId().equals(course1.getId())),
                "В списке должен быть курс 1");
        assertTrue(studentCourses.stream().anyMatch(c -> c.getId().equals(course2.getId())),
                "В списке должен быть курс 2");
    }

    /**
     * Позитивный тест: выборка активных студентов курса.
     * <p>
     * Записывает нескольких студентов на курс, проверяет, что
     * метод возвращает корректный список студентов.
     * </p>
     */
    @Test
    void getCourseStudents_shouldReturnActiveStudents() {
        // Подготовка: преподаватель и курс
        User teacher = new User("coursesstudentteacher2@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Маркетинг");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("Digital-маркетинг", "Основы digital-маркетинга", 25,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        // Студенты
        User student1 = new User("student1@example.com", UserRole.STUDENT);
        entityManager.persist(student1);
        entityManager.flush();

        User student2 = new User("student2@example.com", UserRole.STUDENT);
        entityManager.persist(student2);
        entityManager.flush();

        User student3 = new User("student3@example.com", UserRole.STUDENT);
        entityManager.persist(student3);
        entityManager.flush();

        // Запись на курс
        enrollmentService.enrollStudent(student1.getId(), course.getId());
        enrollmentService.enrollStudent(student2.getId(), course.getId());
        enrollmentService.enrollStudent(student3.getId(), course.getId());

        // Выполнение: получение студентов
        List<User> courseStudents = enrollmentService.getCourseStudents(course.getId());

        // Проверка
        assertEquals(3, courseStudents.size(),
                "На курс должны быть записаны 3 студента");
        assertTrue(courseStudents.stream().anyMatch(u -> u.getId().equals(student1.getId())),
                "В списке должен быть студент 1");
        assertTrue(courseStudents.stream().anyMatch(u -> u.getId().equals(student2.getId())),
                "В списке должен быть студент 2");
        assertTrue(courseStudents.stream().anyMatch(u -> u.getId().equals(student3.getId())),
                "В списке должен быть студент 3");
    }

    /**
     * Негативный тест: запись преподавателя на курс.
     * <p>
     * Пытается записать пользователя с ролью TEACHER на курс
     * и проверяет, что выбрасывается {@link ResourceNotFoundException}.
     * </p>
     */
    @Test
    void enrollStudent_withTeacher_shouldThrow() {
        // Подготовка: преподаватель
        User teacher = new User("enrollteacher@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Астрономия");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("Астрофизика", "Курс по астрофизике", 30,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        // Выполнение и проверка: преподаватель не может записаться
        assertThrows(ResourceNotFoundException.class, () -> {
            enrollmentService.enrollStudent(teacher.getId(), course.getId());
        }, "Преподаватель не должен иметь возможность записаться на курс");
    }

    /**
     * Негативный тест: повторная запись того же студента на тот же курс.
     * <p>
     * Записывает студента на курс, затем пытается записать повторно.
     * Проверяет, что выбрасывается {@link IllegalStateException}.
     * </p>
     */
    @Test
    void enrollStudent_duplicate_shouldThrow() {
        // Подготовка
        User teacher = new User("dupeteacher@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Биология");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("Общая биология", "Курс по общей биологии", 20,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        User student = new User("dupestudent@example.com", UserRole.STUDENT);
        entityManager.persist(student);
        entityManager.flush();

        // Первая запись
        enrollmentService.enrollStudent(student.getId(), course.getId());

        // Повторная попытка
        assertThrows(IllegalStateException.class, () -> {
            enrollmentService.enrollStudent(student.getId(), course.getId());
        }, "Повторная запись студента на тот же курс должна вызвать исключение");
    }

    /**
     * Негативный тест: отписка при отсутствии записи.
     * <p>
     * Пытается отписать студента от курса, на который он не записан.
     * Проверяет, что выбрасывается {@link ResourceNotFoundException}.
     * </p>
     */
    @Test
    void dropEnrollment_nonExistent_shouldThrow() {
        // Подготовка
        User teacher = new User("dropteacher@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("История");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("Мировая история", "Курс по мировой истории", 40,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        User student = new User("dropstudent@example.com", UserRole.STUDENT);
        entityManager.persist(student);
        entityManager.flush();

        // Отписка без предварительной записи
        assertThrows(ResourceNotFoundException.class, () -> {
            enrollmentService.dropEnrollment(student.getId(), course.getId());
        }, "Отписка при отсутствии записи должна вызвать исключение");
    }

    /**
     * Негативный тест: запись с несуществующим студентом.
     * <p>
     * Пытается записать на курс пользователя с несуществующим ID.
     * Проверяет, что выбрасывается {@link ResourceNotFoundException}.
     * </p>
     */
    @Test
    void enrollStudent_nonExistentStudent_shouldThrow() {
        // Подготовка: курс
        User teacher = new User("nonexistteacher@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Химия");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("Неорганическая химия", "Курс по неорганической химии", 25,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        // Попытка записи с несуществующим ID
        assertThrows(ResourceNotFoundException.class, () -> {
            enrollmentService.enrollStudent(99999L, course.getId());
        }, "Запись с несуществующим ID студента должна вызвать исключение");
    }

    /**
     * Негативный тест: запись с несуществующим курсом.
     * <p>
     * Пытается записать студента на курс с несуществующим ID.
     * Проверяет, что выбрасывается {@link ResourceNotFoundException}.
     * </p>
     */
    @Test
    void enrollStudent_nonExistentCourse_shouldThrow() {
        // Подготовка: студент
        User student = new User("nonexiststudent@example.com", UserRole.STUDENT);
        entityManager.persist(student);
        entityManager.flush();

        // Попытка записи на несуществующий курс
        assertThrows(ResourceNotFoundException.class, () -> {
            enrollmentService.enrollStudent(student.getId(), 99999L);
        }, "Запись на несуществующий курс должна вызвать исключение");
    }

    /**
     * Позитивный тест: проверка статуса записи.
     * <p>
     * Записывает студента на курс, проверяет статус ACTIVE,
     * затем отписывает и проверяет, что статус становится null.
     * </p>
     */
    @Test
    void getEnrollmentStatus_shouldReturnCorrectStatus() {
        // Подготовка
        User teacher = new User("statusteacher@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Геометрия");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("Аналитическая геометрия", "Курс по аналитической геометрии", 30,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        User student = new User("statusstudent@example.com", UserRole.STUDENT);
        entityManager.persist(student);
        entityManager.flush();

        // Проверка: до записи статус null
        assertNull(enrollmentService.getEnrollmentStatus(student.getId(), course.getId()),
                "До записи статус должен быть null");

        // Запись
        enrollmentService.enrollStudent(student.getId(), course.getId());

        // Проверка: после записи статус ACTIVE
        assertEquals(EnrollmentStatus.ACTIVE,
                enrollmentService.getEnrollmentStatus(student.getId(), course.getId()),
                "После записи статус должен быть ACTIVE");

        // Отписка
        enrollmentService.dropEnrollment(student.getId(), course.getId());

        // Проверка: после отписки статус null (активная запись больше не существует)
        assertNull(enrollmentService.getEnrollmentStatus(student.getId(), course.getId()),
                "После отписки активная запись больше не существует, статус должен быть null");
    }

    /**
     * Позитивный тест: студент с ролью ADMIN не может записаться.
     * <p>
     * Пытается записать пользователя с ролью ADMIN на курс
     * и проверяет, что выбрасывается {@link ResourceNotFoundException}.
     * </p>
     */
    @Test
    void enrollStudent_withAdmin_shouldThrow() {
        // Подготовка: администратор и курс
        User admin = new User("admintest@example.com", UserRole.ADMIN);
        entityManager.persist(admin);
        entityManager.flush();

        User teacher = new User("adminteacher2@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Литература");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("Русская литература", "Курс по русской литературе", 20,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        // Попытка записи администратора на курс
        assertThrows(ResourceNotFoundException.class, () -> {
            enrollmentService.enrollStudent(admin.getId(), course.getId());
        }, "Администратор не должен иметь возможность записаться на курс");
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