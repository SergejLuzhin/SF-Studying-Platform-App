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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для сервисного слоя управления курсами {@link CourseService}.
 * <p>
 * Проверяют создание, обновление и получение структуры курса,
 * а также добавление модулей и уроков через сервисный слой.
 * Включают позитивные и негативные сценарии с проверкой бизнес-правил.
 * </p>
 * <p>
 * Тесты выполняются с PostgreSQL через Testcontainers в профиле «test».
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CourseManagementTests {

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
                entityManager
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
     * Тест: успешное создание курса с преподавателем и категорией.
     * <p>
     * Создаёт преподавателя и категорию, затем вызывает сервис для создания курса.
     * Проверяет, что курс сохранён и все поля установлены корректно.
     * </p>
     */
    @Test
    void createCourse_shouldSucceedWithValidData() {
        User teacher = createTeacher("createteacher@example.com");
        Category category = createCategory("Программирование");

        Course course = courseService.createCourse(
                "Введение в Java",
                "Основы программирования на Java",
                40,
                LocalDate.of(2026, 9, 1),
                teacher.getId(),
                category.getId()
        );

        assertNotNull(course.getId(), "Идентификатор курса не должен быть null");
        assertEquals("Введение в Java", course.getTitle());
        assertEquals("Основы программирования на Java", course.getDescription());
        assertEquals(40, course.getDuration());
        assertEquals(LocalDate.of(2026, 9, 1), course.getStartDate());
        assertEquals(teacher.getId(), course.getTeacher().getId());
        assertEquals(category.getId(), course.getCategory().getId());
    }

    /**
     * Тест: создание курса с несуществующим преподавателем.
     * <p>
     * Проверяет, что при вызове createCourse с отсутствующим teacherId
     * выбрасывается ResourceNotFoundException.
     * </p>
     */
    @Test
    void createCourse_withNonExistentTeacher_shouldThrow() {
        Category category = createCategory("Тестовая категория");

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> courseService.createCourse(
                        "Курс", "Описание", 20,
                        LocalDate.of(2026, 9, 1),
                        99999L,
                        category.getId()
                ),
                "Должно быть выброшено ResourceNotFoundException при отсутствии преподавателя"
        );
        assertTrue(exception.getMessage().contains("не найден"),
                "Сообщение исключения должно содержать информацию об отсутствии преподавателя");
    }

    /**
     * Тест: создание курса пользователем с ролью STUDENT вместо TEACHER.
     * <p>
     * Проверяет, что сервис не позволяет создать курс студентом.
     * </p>
     */
    @Test
    void createCourse_withWrongRole_shouldThrow() {
        User student = createStudent("student@example.com");
        Category category = createCategory("Тестовая категория");

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> courseService.createCourse(
                        "Курс", "Описание", 20,
                        LocalDate.of(2026, 9, 1),
                        student.getId(),
                        category.getId()
                ),
                "Должно быть выброшено ResourceNotFoundException при неверной роли преподавателя"
        );
        assertTrue(exception.getMessage().contains("не является преподавателем"),
                "Сообщение исключения должно содержать информацию о неверной роли");
    }

    /**
     * Тест: создание курса с несуществующей категорией.
     * <p>
     * Проверяет, что при вызове createCourse с отсутствующим categoryId
     * выбрасывается ResourceNotFoundException.
     * </p>
     */
    @Test
    void createCourse_withNonExistentCategory_shouldThrow() {
        User teacher = createTeacher("createteacher2@example.com");

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> courseService.createCourse(
                        "Курс", "Описание", 20,
                        LocalDate.of(2026, 9, 1),
                        teacher.getId(),
                        99999L
                ),
                "Должно быть выброшено ResourceNotFoundException при отсутствии категории"
        );
        assertTrue(exception.getMessage().contains("не найдена"),
                "Сообщение исключения должно содержать информацию об отсутствии категории");
    }

    /**
     * Тест: успешное обновление данных курса.
     * <p>
     * Создаёт курс через сервис, затем обновляет его данные через updateCourse.
     * Проверяет, что все поля корректно изменены.
     * </p>
     */
    @Test
    void updateCourse_shouldSucceedWithValidData() {
        User teacher = createTeacher("updateteacher@example.com");
        Category category = createCategory("Маркетинг");

        Course course = courseService.createCourse(
                "Основы маркетинга", "Начальный курс", 15,
                LocalDate.of(2026, 9, 1),
                teacher.getId(),
                category.getId()
        );

        Long courseId = course.getId();

        // Обновление данных через сервис
        Course updated = courseService.updateCourse(
                courseId,
                "Маркетинг: продвинутый курс",
                "Расширенный курс по digital-маркетингу.",
                30,
                LocalDate.of(2026, 10, 1)
        );

        assertEquals("Маркетинг: продвинутый курс", updated.getTitle());
        assertEquals("Расширенный курс по digital-маркетингу.", updated.getDescription());
        assertEquals(30, updated.getDuration());
        assertEquals(LocalDate.of(2026, 10, 1), updated.getStartDate());

        // Проверка через репозиторий
        Course fromRepo = courseRepository.findById(courseId).orElseThrow();
        assertEquals("Маркетинг: продвинутый курс", fromRepo.getTitle());
        assertEquals(30, fromRepo.getDuration());
    }

    /**
     * Тест: обновление несуществующего курса.
     * <p>
     * Проверяет, что при вызове updateCourse с отсутствующим courseId
     * выбрасывается ResourceNotFoundException.
     * </p>
     */
    @Test
    void updateCourse_withNonExistentCourse_shouldThrow() {
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> courseService.updateCourse(
                        99999L,
                        "Новый заголовок",
                        "Новое описание",
                        25,
                        LocalDate.of(2026, 11, 1)
                ),
                "Должно быть выброшено ResourceNotFoundException при отсутствии курса"
        );
        assertTrue(exception.getMessage().contains("не найден"),
                "Сообщение исключения должно содержать информацию об отсутствии курса");
    }

    /**
     * Тест: успешное добавление модуля к курсу.
     * <p>
     * Создаёт курс через сервис, затем добавляет модуль через addModule.
     * Проверяет, что модуль сохранён и связан с курсом.
     * </p>
     */
    @Test
    void addModule_shouldSucceedWithValidCourse() {
        User teacher = createTeacher("addmoduleteacher@example.com");
        Category category = createCategory("Дизайн");

        Course course = courseService.createCourse(
                "UI/UX Дизайн", "Курс по дизайну", 20,
                LocalDate.of(2026, 10, 15),
                teacher.getId(),
                category.getId()
        );

        Module module = courseService.addModule(
                course.getId(),
                "Основы дизайна",
                "Принципы визуального дизайна",
                1
        );

        assertNotNull(module.getId(), "Идентификатор модуля не должен быть null");
        assertEquals("Основы дизайна", module.getTitle());
        assertEquals("Принципы визуального дизайна", module.getDescription());
        assertEquals(1, module.getOrderIndex());
        assertEquals(course.getId(), module.getCourse().getId());

        // Проверка через репозиторий
        Course fromRepo = courseRepository.findById(course.getId()).orElseThrow();
        assertFalse(fromRepo.getModules().isEmpty(), "Модули курса не должны быть пустыми");
        assertEquals(1, fromRepo.getModules().size());
    }

    /**
     * Тест: успешное добавление урока к модулю.
     * <p>
     * Создаёт курс и модуль через сервис, затем добавляет урок через addLesson.
     * Проверяет, что урок сохранён и связан с модулем.
     * </p>
     */
    @Test
    void addLesson_shouldSucceedWithValidModule() {
        User teacher = createTeacher("addlessonteacher@example.com");
        Category category = createCategory("Программирование");

        Course course = courseService.createCourse(
                "Введение в Java", "Основы Java", 40,
                LocalDate.of(2026, 9, 1),
                teacher.getId(),
                category.getId()
        );

        Module module = courseService.addModule(
                course.getId(),
                "Переменные и типы",
                "Основные типы данных Java",
                1
        );

        Lesson lesson = courseService.addLesson(
                module.getId(),
                "Типы данных",
                "int, str, float, boolean",
                "https://example.com/video1"
        );

        assertNotNull(lesson.getId(), "Идентификатор урока не должен быть null");
        assertEquals("Типы данных", lesson.getTitle());
        assertEquals("int, str, float, boolean", lesson.getContent());
        assertEquals("https://example.com/video1", lesson.getVideoUrl());
        assertEquals(module.getId(), lesson.getModule().getId());

        // Проверка через репозиторий
        Module fromRepo = moduleRepository.findById(module.getId()).orElseThrow();
        assertFalse(fromRepo.getLessons().isEmpty(), "Уроки модуля не должны быть пустыми");
        assertEquals(1, fromRepo.getLessons().size());
    }

    /**
     * Тест: получение полной структуры курса с модулями и уроками.
     * <p>
     * Создаёт курс, модуль и урок через сервис, затем вызывает getCourseStructure.
     * Проверяет, что все ленивые коллекции инициализированы и данные доступны.
     * </p>
     */
    @Test
    void getCourseStructure_shouldReturnFullStructure() {
        User teacher = createTeacher("structureteacher@example.com");
        Category category = createCategory("Data Science");

        Course course = courseService.createCourse(
                "Введение в Data Science", "Курс по науке о данных", 50,
                LocalDate.of(2026, 9, 1),
                teacher.getId(),
                category.getId()
        );

        Module module = courseService.addModule(
                course.getId(),
                "Машинное обучение",
                "Основы ML",
                1
        );

        courseService.addLesson(
                module.getId(),
                "Линейная регрессия",
                "Основы линейной регрессии",
                "https://example.com/regression"
        );

        // Загрузка структуры через сервис (внутри транзакции)
        Course structure = courseService.getCourseStructure(course.getId());

        assertNotNull(structure, "Структура курса не должна быть null");
        assertEquals("Введение в Data Science", structure.getTitle());
        assertFalse(structure.getModules().isEmpty(), "Модули не должны быть пустыми");
        assertEquals(1, structure.getModules().size());

        Module loadedModule = structure.getModules().iterator().next();
        assertEquals("Машинное обучение", loadedModule.getTitle());
        assertFalse(loadedModule.getLessons().isEmpty(), "Уроки не должны быть пустыми");
        assertEquals(1, loadedModule.getLessons().size());

        Lesson loadedLesson = loadedModule.getLessons().iterator().next();
        assertEquals("Линейная регрессия", loadedLesson.getTitle());
    }

    /**
     * Тест: получение структуры несуществующего курса.
     * <p>
     * Проверяет, что при вызове getCourseStructure с отсутствующим courseId
     * выбрасывается ResourceNotFoundException.
     * </p>
     */
    @Test
    void getCourseStructure_withNonExistentCourse_shouldThrow() {
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> courseService.getCourseStructure(99999L),
                "Должно быть выброшено ResourceNotFoundException при отсутствии курса"
        );
        assertTrue(exception.getMessage().contains("не найден"),
                "Сообщение исключения должно содержать информацию об отсутствии курса");
    }

    /**
     * Тест: создание курса с нулевой продолжительностью.
     * <p>
     * Проверяет, что при вызове createCourse с duration = 0
     * выбрасывается IllegalArgumentException.
     * </p>
     */
    @Test
    void createCourse_withZeroDuration_shouldThrow() {
        User teacher = createTeacher("createteacherzeroduration@example.com");
        Category category = createCategory("Тестовая категория");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> courseService.createCourse(
                        "Курс", "Описание", 0,
                        LocalDate.of(2026, 9, 1),
                        teacher.getId(),
                        category.getId()
                ),
                "Должно быть выброшено IllegalArgumentException при нулевой продолжительности"
        );
        assertTrue(exception.getMessage().contains("не менее 1 часа"),
                "Сообщение исключения должно содержать информацию о минимальной продолжительности");
    }

    /**
     * Тест: создание курса с отрицательной продолжительностью.
     * <p>
     * Проверяет, что при вызове createCourse с отрицательным duration
     * выбрасывается IllegalArgumentException.
     * </p>
     */
    @Test
    void createCourse_withNegativeDuration_shouldThrow() {
        User teacher = createTeacher("createteacherneg@example.com");
        Category category = createCategory("Тестовая категория");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> courseService.createCourse(
                        "Курс", "Описание", -5,
                        LocalDate.of(2026, 9, 1),
                        teacher.getId(),
                        category.getId()
                ),
                "Должно быть выброшено IllegalArgumentException при отрицательной продолжительности"
        );
        assertTrue(exception.getMessage().contains("не менее 1 часа"),
                "Сообщение исключения должно содержать информацию о минимальной продолжительности");
    }

    /**
     * Тест: создание курса с null продолжительностью.
     * <p>
     * Проверяет, что при вызове createCourse с duration = null
     * выбрасывается IllegalArgumentException и курс не создаётся.
     * </p>
     */
    @Test
    void createCourse_withNullDuration_shouldThrow() {
        User teacher = createTeacher("createteachernullduration@example.com");
        Category category = createCategory("Тестовая категория");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> courseService.createCourse(
                        "Курс", "Описание", null,
                        LocalDate.of(2026, 9, 1),
                        teacher.getId(),
                        category.getId()
                ),
                "Должно быть выброшено IllegalArgumentException при null продолжительности"
        );
        assertTrue(exception.getMessage().contains("не менее 1 часа"),
                "Сообщение исключения должно содержать информацию о минимальной продолжительности");

        // Проверка: курс не создан
        assertEquals(0L, courseRepository.count(),
                "В базе не должно быть создано курсов после ошибочного создания");
    }

    /**
     * Тест: создание курса с минимальной допустимой продолжительностью 1 час.
     * <p>
     * Проверяет, что при duration = 1 курс создаётся успешно.
     * </p>
     */
    @Test
    void createCourse_withMinimumDuration_shouldSucceed() {
        User teacher = createTeacher("createteachermin@example.com");
        Category category = createCategory("Тестовая категория");

        Course course = courseService.createCourse(
                "Минимальный курс", "Курс на 1 час", 1,
                LocalDate.of(2026, 9, 1),
                teacher.getId(),
                category.getId()
        );

        assertNotNull(course.getId(), "Идентификатор курса не должен быть null");
        assertEquals(1, course.getDuration(), "Продолжительность должна быть равна 1");
    }

    /**
     * Тест: обновление курса с нулевой продолжительностью.
     * <p>
     * Проверяет, что при вызове updateCourse с duration = 0
     * выбрасывается IllegalArgumentException и данные не меняются.
     * </p>
     */
    @Test
    void updateCourse_withZeroDuration_shouldThrow() {
        User teacher = createTeacher("updateteacherzeroduration@example.com");
        Category category = createCategory("Тестовая категория");

        Course course = courseService.createCourse(
                "Исходный курс", "Описание", 20,
                LocalDate.of(2026, 9, 1),
                teacher.getId(),
                category.getId()
        );

        Long courseId = course.getId();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> courseService.updateCourse(
                        courseId,
                        "Новый заголовок",
                        "Новое описание",
                        0,
                        LocalDate.of(2026, 10, 1)
                ),
                "Должно быть выброшено IllegalArgumentException при нулевой продолжительности"
        );
        assertTrue(exception.getMessage().contains("не менее 1 часа"),
                "Сообщение исключения должно содержать информацию о минимальной продолжительности");

        // Проверка: данные не изменились
        Course unchanged = courseRepository.findById(courseId).orElseThrow();
        assertEquals("Исходный курс", unchanged.getTitle(),
                "Название не должно измениться после ошибочного обновления");
        assertEquals(20, unchanged.getDuration(),
                "Продолжительность не должна измениться после ошибочного обновления");
    }

    /**
     * Тест: обновление курса с отрицательной продолжительностью.
     * <p>
     * Проверяет, что при вызове updateCourse с отрицательным duration
     * выбрасывается IllegalArgumentException и данные не меняются.
     * </p>
     */
    @Test
    void updateCourse_withNegativeDuration_shouldThrow() {
        User teacher = createTeacher("updateteacherneg@example.com");
        Category category = createCategory("Тестовая категория");

        Course course = courseService.createCourse(
                "Исходный курс", "Описание", 20,
                LocalDate.of(2026, 9, 1),
                teacher.getId(),
                category.getId()
        );

        Long courseId = course.getId();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> courseService.updateCourse(
                        courseId,
                        "Новый заголовок",
                        "Новое описание",
                        -10,
                        LocalDate.of(2026, 10, 1)
                ),
                "Должно быть выброшено IllegalArgumentException при отрицательной продолжительности"
        );
        assertTrue(exception.getMessage().contains("не менее 1 часа"),
                "Сообщение исключения должно содержать информацию о минимальной продолжительности");

        // Проверка: данные не изменились
        Course unchanged = courseRepository.findById(courseId).orElseThrow();
        assertEquals("Исходный курс", unchanged.getTitle(),
                "Название не должно измениться после ошибочного обновления");
        assertEquals(20, unchanged.getDuration(),
                "Продолжительность не должна измениться после ошибочного обновления");
    }

    /**
     * Тест: обновление курса с минимальной допустимой продолжительностью 1 час.
     * <p>
     * Проверяет, что при duration = 1 обновление проходит успешно.
     * </p>
     */
    @Test
    void updateCourse_withMinimumDuration_shouldSucceed() {
        User teacher = createTeacher("updateteachermin@example.com");
        Category category = createCategory("Тестовая категория");

        Course course = courseService.createCourse(
                "Курс", "Описание", 30,
                LocalDate.of(2026, 9, 1),
                teacher.getId(),
                category.getId()
        );

        Long courseId = course.getId();

        Course updated = courseService.updateCourse(
                courseId,
                "Обновлённый курс",
                "Новое описание",
                1,
                LocalDate.of(2026, 10, 1)
        );

        assertEquals(1, updated.getDuration(), "Продолжительность должна быть 1 час");
        assertEquals("Обновлённый курс", updated.getTitle(), "Название должно обновиться");

        Course fromRepo = courseRepository.findById(courseId).orElseThrow();
        assertEquals(1, fromRepo.getDuration(), "Продолжительность сохранена в БД");
    }

    /**
     * Тест: обновление курса с null продолжительностью сохраняет прежнее значение.
     * <p>
     * Проверяет, что передача null в duration не меняет поле —
     * это означает «не изменять продолжительность».
     * </p>
     */
    @Test
    void updateCourse_withNullDuration_shouldPreserveValue() {
        User teacher = createTeacher("updateteachernull@example.com");
        Category category = createCategory("Тестовая категория");

        Course course = courseService.createCourse(
                "Курс", "Описание", 25,
                LocalDate.of(2026, 9, 1),
                teacher.getId(),
                category.getId()
        );

        Long courseId = course.getId();

        Course updated = courseService.updateCourse(
                courseId,
                "Новое название",
                "Новое описание",
                null,
                LocalDate.of(2026, 10, 1)
        );

        assertEquals(25, updated.getDuration(),
                "Продолжительность должна остаться прежней при null");
        assertEquals("Новое название", updated.getTitle(),
                "Другие поля должны обновиться");
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
