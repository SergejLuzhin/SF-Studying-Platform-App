package com.studying.platform;

import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Lesson;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.LessonRepository;
import com.studying.platform.repository.ModuleRepository;
import com.studying.platform.repository.UserRepository;
import com.studying.platform.service.CourseService;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для проверки работы {@link CourseService} вне тестовой транзакции.
 * <p>
 * Тесты используют {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)},
 * чтобы каждый вызов сервисных методов происходил в отдельной транзакции,
 * а данные, возвращаемые Spring-прокси, были доступны после завершения вызова.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CourseServiceTransactionTests {

    /**
     * Сервис управления курсами (Spring-бин).
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
     * Репозиторий курсов для создания фикстур.
     */
    @Autowired
    private CourseRepository courseRepository;

    /**
     * Репозиторий модулей для проверки.
     */
    @Autowired
    private ModuleRepository moduleRepository;

    /**
     * Репозиторий уроков для проверки.
     */
    @Autowired
    private LessonRepository lessonRepository;

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
        uniquePrefix = "cs-txn-" + System.currentTimeMillis() + "-" + new java.util.Random().nextInt(10000);
    }

    /**
     * Очистка тестовых данных после каждого теста в порядке зависимостей.
     */
    @AfterEach
    void tearDown() {
        transactionTemplate.execute(status -> {
            try {
                entityManager.createQuery("DELETE FROM CourseReview").executeUpdate();
            } catch (Exception ignored) {
            }
            try {
                entityManager.createQuery("DELETE FROM Enrollment").executeUpdate();
            } catch (Exception ignored) {
            }
            try {
                entityManager.createQuery("DELETE FROM Lesson").executeUpdate();
            } catch (Exception ignored) {
            }
            try {
                entityManager.createQuery("DELETE FROM Module").executeUpdate();
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
     * Проверка: структура курса доступна после завершения транзакции сервиса.
     * <p>
     * Создаёт данные в отдельных транзакциях (преподаватель, категория, курс, модуль, урок),
     * вызывает {@code getCourseStructure}, проверяет, что транзакция закрыта,
     * затем читает названия модулей и уроков.
     * </p>
     */
    @Test
    void getCourseStructure_fieldsAccessibleOutsideTransaction() {
        // Создание преподавателя
        User teacher = createTeacher("teacher", UserRole.TEACHER);

        // Создание категории
        Category category = createCategory("Информатика");

        // Создание курса через сервис
        Course course = courseService.createCourse(
                "Java-путь", "Изучение Java с нуля", 60,
                LocalDate.now().plusDays(30),
                teacher.getId(), category.getId());

        // Добавление модуля и урока через сервис
        Module module = courseService.addModule(
                course.getId(),
                "Основы Java",
                "Переменные, типы данных, операторы",
                1);

        courseService.addLesson(
                module.getId(),
                "Типы данных",
                "int, double, boolean, String",
                "https://example.com/types");

        // Вызов getCourseStructure
        Course structure = courseService.getCourseStructure(course.getId());

        // Проверка: транзакция закрыта
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "Транзакция должна быть закрыта при чтении результатов");

        // Проверка: скалярные поля структуры доступны
        assertEquals("Java-путь", structure.getTitle());
        assertEquals(1, structure.getModules().size());

        Module loadedModule = structure.getModules().iterator().next();
        assertEquals("Основы Java", loadedModule.getTitle());
        assertEquals("Переменные, типы данных, операторы", loadedModule.getDescription());
        assertEquals(1, loadedModule.getLessons().size());

        Lesson loadedLesson = loadedModule.getLessons().iterator().next();
        assertEquals("Типы данных", loadedLesson.getTitle());
        assertEquals("int, double, boolean, String", loadedLesson.getContent());
    }

    /**
     * Проверка: курс с несколькими модулями и уроками загружается полностью.
     */
    @Test
    void getCourseStructure_multipleModulesAndLessons() {
        // Фикстуры
        String teacherEmail = uniquePrefix + "-multi-teacher@example.com";
        User teacher = createTeacher("multi-teacher", UserRole.TEACHER);
        Category category = createCategory("Математика");

        Course course = courseService.createCourse(
                "Высшая математика", "Алгебра и анализ", 45,
                LocalDate.now().plusDays(20),
                teacher.getId(), category.getId());

        Module mod1 = courseService.addModule(course.getId(), "Линейная алгебра", "Матрицы и определители", 1);
        courseService.addLesson(mod1.getId(), "Определители", "Свойства и вычисление", null);
        courseService.addLesson(mod1.getId(), "Матрицы", "Операции над матрицами", null);

        Module mod2 = courseService.addModule(course.getId(), "Матанализ", "Пределы и производные", 2);
        courseService.addLesson(mod2.getId(), "Пределы", "Определение и свойства", null);

        Course structure = courseService.getCourseStructure(course.getId());

        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());

        assertEquals(2, structure.getModules().size());

        var modules = List.copyOf(structure.getModules());
        assertEquals(2, modules.size());

        int totalLessons = modules.stream()
                .mapToInt(m -> m.getLessons().size())
                .sum();
        assertEquals(3, totalLessons, "Общее количество уроков должно быть 3");
    }

    /**
     * Проверка: ResourceNotFoundException при несуществующем ID курса.
     */
    @Test
    void getCourseStructure_nonExistentCourse_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> courseService.getCourseStructure(99999L));
    }

    /**
     * Проверка: пустая структура курса (без модулей и уроков).
     */
    @Test
    void getCourseStructure_emptyStructure() {
        User teacher = createTeacher("empty-teacher", UserRole.TEACHER);
        Category category = createCategory("Пустой курс");

        Course course = courseService.createCourse(
                "Без содержания", "Курс без модулей", 10,
                LocalDate.now().plusDays(15),
                teacher.getId(), category.getId());

        Course structure = courseService.getCourseStructure(course.getId());

        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());

        assertEquals("Без содержания", structure.getTitle());
        assertTrue(structure.getModules().isEmpty(),
                "Модули должны быть пустыми");
    }

    /**
     * Создает и сохраняет пользователя в транзакции.
     *
     * @param localPart локальная часть email (до @)
     * @param role      роль пользователя
     * @return сохранённый пользователь
     */
    private User createTeacher(String localPart, UserRole role) {
        return transactionTemplate.execute(status -> {
            String email = uniquePrefix.substring(0, Math.min(20, uniquePrefix.length())) + "-" + localPart + "@t.com";
            User user = new User(email, role);
            entityManager.persist(user);
            entityManager.flush();
            return user;
        });
    }

    /**
     * Создает и сохраняет категорию в транзакции.
     *
     * @param name название категории
     * @return сохранённая категория
     */
    private Category createCategory(String name) {
        return transactionTemplate.execute(status -> {
            Category category = new Category(name);
            entityManager.persist(category);
            entityManager.flush();
            return category;
        });
    }
}
