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
 * Интеграционные тесты для проверки работы {@link CourseService} вне тестовой транзакции.
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
     * Идентификатор созданной категории (отслеживается для адресной очистки).
     */
    private Long categoryId;

    /**
     * Идентификатор созданного курса (отслеживается для адресной очистки).
     */
    private Long courseId;

    /**
     * Идентификатор созданного модуля (отслеживается для адресной очистки).
     */
    private Long moduleId;

    /**
     * Идентификаторы созданных модулей (для поддержки нескольких модулей).
     */
    private Set<Long> moduleIds = new java.util.HashSet<>();

    /**
     * Идентификатор созданного урока (отслеживается для адресной очистки).
     */
    private Long lessonId;

    /**
     * Идентификаторы созданных уроков (для поддержки нескольких уроков).
     */
    private Set<Long> lessonIds = new java.util.HashSet<>();

    /**
     * Инициализация уникального префикса перед каждым тестом.
     * <p>
     * Префикс ограничивается 20 символами, чтобы при добавлении suffix
     * и домена email оставался в пределах 50 символов.
     * </p>
     */
    @BeforeEach
    void setUp() {
        uniquePrefix = "cs-txn-" + System.currentTimeMillis() + "-" + new java.util.Random().nextInt(10000);
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
        // Собираем все IDs
        Set<Long> allModuleIds = new java.util.HashSet<>(moduleIds);
        if (moduleId != null) {
            allModuleIds.add(moduleId);
        }
        Set<Long> allLessonIds = new java.util.HashSet<>(lessonIds);
        if (lessonId != null) {
            allLessonIds.add(lessonId);
        }

        cleanupHelper.clearTestEntities(
                entityManager,
                transactionTemplate,
                Set.of(),       // reviewCourseIds — отзывы не создаются
                courseId != null ? Set.of(courseId) : Set.of(),
                Set.of(),       // enrollmentUserEmails — записи не создаются
                !allLessonIds.isEmpty() ? allLessonIds : Set.of(),
                !allModuleIds.isEmpty() ? allModuleIds : Set.of(),
                courseId != null ? Set.of(courseId) : Set.of(),
                categoryId != null ? Set.of(categoryId) : Set.of(),
                teacherId != null ? Set.of(teacherId) : Set.of()
        );
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
        // Создание преподавателя в транзакции
        User teacher = createTeacherInTransaction("teacher", UserRole.TEACHER);
        teacherId = teacher.getId();

        // Создание категории в транзакции
        Category category = createCategoryInTransaction("Информатика");
        categoryId = category.getId();

        // Создание курса через сервис
        Course course = courseService.createCourse(
                "Java-путь", "Изучение Java с нуля", 60,
                LocalDate.now().plusDays(30),
                teacher.getId(), category.getId());
        courseId = course.getId();

        // Добавление модуля и урока через сервис
        Module module = courseService.addModule(
                course.getId(),
                "Основы Java",
                "Переменные, типы данных, операторы",
                1);
        moduleId = module.getId();
        moduleIds.add(moduleId);

        Lesson lesson = courseService.addLesson(
                module.getId(),
                "Типы данных",
                "int, double, boolean, String",
                "https://example.com/types");
        lessonId = lesson.getId();
        lessonIds.add(lessonId);

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
        // Фикстуры в транзакциях
        User teacher = createTeacherInTransaction("multi-teacher", UserRole.TEACHER);
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Математика");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Высшая математика", "Алгебра и анализ", 45,
                LocalDate.now().plusDays(20),
                teacher.getId(), category.getId());
        courseId = course.getId();

        Module mod1 = courseService.addModule(course.getId(), "Линейная алгебра", "Матрицы и определители", 1);
        moduleIds.add(mod1.getId());
        Lesson l1 = courseService.addLesson(mod1.getId(), "Определители", "Свойства и вычисление", null);
        lessonIds.add(l1.getId());
        Lesson l2 = courseService.addLesson(mod1.getId(), "Матрицы", "Операции над матрицами", null);
        lessonIds.add(l2.getId());

        Module mod2 = courseService.addModule(course.getId(), "Матанализ", "Пределы и производные", 2);
        moduleIds.add(mod2.getId());
        Lesson l3 = courseService.addLesson(mod2.getId(), "Пределы", "Определение и свойства", null);
        lessonIds.add(l3.getId());

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
        User teacher = createTeacherInTransaction("empty-teacher", UserRole.TEACHER);
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Пустой курс");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Без содержания", "Курс без модулей", 10,
                LocalDate.now().plusDays(15),
                teacher.getId(), category.getId());
        courseId = course.getId();

        Course structure = courseService.getCourseStructure(course.getId());

        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());

        assertEquals("Без содержания", structure.getTitle());
        assertTrue(structure.getModules().isEmpty(),
                "Модули должны быть пустыми");
    }

    /**
     * Проверка: updateCourse с нулевой длительностью выбрасывает исключение,
     * данные курса остаются неизменными.
     * <p>
     * Создаёт курс через сервис; вызывает updateCourse с duration = 0;
     * ожидает IllegalArgumentException; затем в новой транзакции
     * через репозиторий проверяет, что название и продолжительность прежние.
     * </p>
     */
    @Test
    void updateCourse_withZeroDuration_shouldThrow() {
        // Создание фикстур в транзакциях
        User teacher = createTeacherInTransaction("updatezero-teacher", UserRole.TEACHER);
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Тест нулевой длительности");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Исходный курс", "Описание", 20,
                LocalDate.now().plusDays(10),
                teacher.getId(), category.getId());
        courseId = course.getId();

        // Попытка обновления с duration = 0
        assertThrows(IllegalArgumentException.class,
                () -> courseService.updateCourse(
                        course.getId(),
                        "Новый заголовок",
                        "Новое описание",
                        0,
                        LocalDate.now().plusDays(20)
                ),
                "Должно быть выброшено IllegalArgumentException при нулевой продолжительности");

        // Проверка в новой транзакции: данные не изменились
        Course unchanged = courseRepository.findById(courseId).orElseThrow();
        assertEquals(20, unchanged.getDuration(),
                "Продолжительность не должна измениться после ошибочного обновления");
        assertEquals("Исходный курс", unchanged.getTitle(),
                "Название не должно измениться после ошибочного обновления");
    }

    /**
     * Проверка: updateCourse с отрицательной длительностью выбрасывает исключение,
     * данные курса остаются неизменными.
     */
    @Test
    void updateCourse_withNegativeDuration_shouldThrow() {
        User teacher = createTeacherInTransaction("updateneg-teacher", UserRole.TEACHER);
        teacherId = teacher.getId();
        Category category = createCategoryInTransaction("Тест отрицательной длительности");
        categoryId = category.getId();

        Course course = courseService.createCourse(
                "Исходный курс", "Описание", 25,
                LocalDate.now().plusDays(10),
                teacher.getId(), category.getId());
        courseId = course.getId();

        // Попытка обновления с duration = -10
        assertThrows(IllegalArgumentException.class,
                () -> courseService.updateCourse(
                        course.getId(),
                        "Новый заголовок",
                        "Новое описание",
                        -10,
                        LocalDate.now().plusDays(20)
                ),
                "Должно быть выброшено IllegalArgumentException при отрицательной продолжительности");

        // Проверка в новой транзакции: данные не изменились
        Course unchanged = courseRepository.findById(courseId).orElseThrow();
        assertEquals(25, unchanged.getDuration(),
                "Продолжительность не должна измениться после ошибочного обновления");
        assertEquals("Исходный курс", unchanged.getTitle(),
                "Название не должно измениться после ошибочного обновления");
    }

    /**
     * Создает и сохраняет пользователя в транзакции через TransactionTemplate.
     *
     * @param localPart локальная часть email (до @)
     * @param role      роль пользователя
     * @return сохранённый пользователь
     */
    private User createTeacherInTransaction(String localPart, UserRole role) {
        return transactionTemplate.execute(status -> {
            String email = uniquePrefix.substring(0, Math.min(20, uniquePrefix.length())) + "-" + localPart + "@t.com";
            User user = new User(email, role);
            entityManager.persist(user);
            entityManager.flush();
            return user;
        });
    }

    /**
     * Создает и сохраняет категорию в транзакции через TransactionTemplate.
     *
     * @param name название категории
     * @return сохранённая категория
     */
    private Category createCategoryInTransaction(String name) {
        return transactionTemplate.execute(status -> {
            Category category = new Category(name);
            entityManager.persist(category);
            entityManager.flush();
            return category;
        });
    }
}
