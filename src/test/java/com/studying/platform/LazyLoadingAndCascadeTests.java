package com.studying.platform;

import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Lesson;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.repository.AssignmentRepository;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.CourseRepository;
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
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для демонстрации поведения ленивой загрузки и проверки
 * сквозных CRUD-операций и каскадных связей на PostgreSQL через Testcontainers.
 * <p>
 * Тесты используют {@code @SpringBootTest} для полного запуска контекста Spring,
 * что позволяет управлять транзакциями вручную через {@code TransactionTemplate}.
 * Это необходимо для корректной демонстрации {@code LazyInitializationException}
 * при обращении к ленивым коллекциям вне транзакции.
 * </p>
 * <p>
 * Все тесты выполняются с PostgreSQL через Testcontainers в профиле «test».
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
class LazyLoadingAndCascadeTests {

    /** Менеджер сущностей для ручного управления сессией Hibernate. */
    @Autowired
    private EntityManager entityManager;

    /** Шаблон для управления Spring-транзакциями. */
    @Autowired
    private TransactionTemplate transactionTemplate;

    /** Репозиторий для сущности Course. */
    @Autowired
    private CourseRepository courseRepository;

    /** Репозиторий для сущности Module. */
    @Autowired
    private ModuleRepository moduleRepository;

    /** Репозиторий для сущности Lesson. */
    @Autowired
    private LessonRepository lessonRepository;

    /** Репозиторий для сущности User. */
    @Autowired
    private UserRepository userRepository;

    /** Репозиторий для сущности Category. */
    @Autowired
    private CategoryRepository categoryRepository;

    /** Репозиторий для сущности Assignment. */
    @Autowired
    private AssignmentRepository assignmentRepository;

    /** Репозиторий для сущности Tag. */
    @Autowired
    private TagRepository tagRepository;

    /** Репозиторий для сущности Submission. */
    @Autowired
    private SubmissionRepository submissionRepository;

    /** Репозиторий для сущности QuizSubmission. */
    @Autowired
    private QuizSubmissionRepository quizSubmissionRepository;

    /** Репозиторий для сущности Quiz. */
    @Autowired
    private QuizRepository quizRepository;

    /** Репозиторий для сущности Enrollment. */
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    /** Репозиторий для сущности CourseReview. */
    @Autowired
    private com.studying.platform.repository.CourseReviewRepository courseReviewRepository;

    /** Сервис управления курсами (autowired — для работы @Transactional прокси). */
    @Autowired
    private CourseService courseService;

    /** JDBC-шаблон для прямых SQL-запросов к базе данных. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Источник данных для проверки продукта базы данных. */
    @Autowired
    private DataSource dataSource;

    /** Идентификатор тестовой категории для очистки после каждого теста. */
    private Long testCategoryId;

    /**
     * Очищает тестовые данные после каждого теста для обеспечения изоляции.
     */
    @AfterEach
    void tearDown() {
        if (testCategoryId != null) {
            // Простое удаление: курс по category_id, категория по ID
            // Каскады JPA обрабатывают зависимые записи; SQL-каскады с ON DELETE CASCADE
            // или удаление вручную в нужном порядке
            jdbcTemplate.update("DELETE FROM course_tag WHERE course_id IN (SELECT id FROM courses WHERE category_id = ?)",
                    testCategoryId);
            jdbcTemplate.update("DELETE FROM course_reviews WHERE course_id IN (SELECT id FROM courses WHERE category_id = ?)",
                    testCategoryId);
            jdbcTemplate.update("DELETE FROM enrollments WHERE course_id IN (SELECT id FROM courses WHERE category_id = ?)",
                    testCategoryId);
            jdbcTemplate.update("DELETE FROM lessons WHERE module_id IN "
                    + "(SELECT m.id FROM modules m JOIN courses c ON m.course_id = c.id WHERE c.category_id = ?)",
                    testCategoryId);
            jdbcTemplate.update("DELETE FROM assignments WHERE lesson_id IN "
                    + "(SELECT l.id FROM lessons l JOIN modules m ON l.module_id = m.id JOIN courses c ON m.course_id = c.id WHERE c.category_id = ?)",
                    testCategoryId);
            jdbcTemplate.update("DELETE FROM submissions WHERE assignment_id IN "
                    + "(SELECT a.id FROM assignments a JOIN lessons l ON a.lesson_id = l.id JOIN modules m ON l.module_id = m.id JOIN courses c ON m.course_id = c.id WHERE c.category_id = ?)",
                    testCategoryId);
            jdbcTemplate.update("DELETE FROM modules WHERE course_id IN (SELECT id FROM courses WHERE category_id = ?)",
                    testCategoryId);
            jdbcTemplate.update("DELETE FROM courses WHERE category_id = ?", testCategoryId);
            jdbcTemplate.update("DELETE FROM categories WHERE id = ?", testCategoryId);
            entityManager.clear();
        }
    }

    /**
     * Создаёт и сохраняет тестовую категорию в транзакции.
     *
     * @param name название категории
     * @return сохранённая категория
     */
    private Category createTestCategory(String name) {
        final Category[] category = new Category[1];
        transactionTemplate.execute(status -> {
            category[0] = new Category(name);
            entityManager.persist(category[0]);
            entityManager.flush();
            return null;
        });
        testCategoryId = category[0].getId();
        return category[0];
    }

    /**
     * Создаёт и сохраняет тестового преподавателя в транзакции.
     *
     * @param email email преподавателя
     * @return сохранённый пользователь с ролью TEACHER
     */
    private User createTeacher(String email) {
        final User[] user = new User[1];
        transactionTemplate.execute(status -> {
            user[0] = new User(email, UserRole.TEACHER);
            entityManager.persist(user[0]);
            entityManager.flush();
            return null;
        });
        return user[0];
    }

    /**
     * Тест: демонстрация исключения LazyInitializationException при доступе к ленивой коллекции
     * {@code modules} после завершения транзакции.
     * <p>
     * Созданный курс сохраняется в транзакции, после чего транзакция завершается.
     * Затем происходит попытка обращения к ленивой коллекции, что должно привести
     * к выбросу {@code LazyInitializationException}.
     * </p>
     */
    @Test
    void lazyInitializationException_collections_shouldOccurAfterTransactionCommit() {
        // Шаг 1: создаём категорию и курс в транзакции
        Category category = createTestCategory("Демонстрация LIE");
        User teacher = createTeacher("lie@example.com");

        final Long[] courseId = new Long[1];
        transactionTemplate.execute(status -> {
            Course course = new Course("Курс с ленивыми модулями", "Описание курса",
                    20, LocalDate.of(2026, 9, 1), teacher, category);
            entityManager.persist(course);
            entityManager.flush();
            courseId[0] = course.getId();
            return null;
        });

        // Шаг 2: после завершения транзакции пытаемся получить ленивую коллекцию modules
        Course detachedCourse = courseRepository.findById(courseId[0]).orElseThrow();

        // Поле modules — @OneToMany(fetch = LAZY). При обращении вне транзакции
        // Hibernate пытается загрузить данные через закрытую сессию.
        LazyInitializationException exception = assertThrows(
                LazyInitializationException.class,
                () -> detachedCourse.getModules().size(),
                "Обращение к ленивой коллекции после завершения транзакции должно вызвать LazyInitializationException"
        );

        assertTrue(exception.getMessage().toLowerCase().contains("session"),
                "Сообщение исключения должно содержать информацию о закрытой сессии: "
                        + exception.getMessage());
    }

    /**
     * Тест: демонстрация исключения LazyInitializationException при доступе к ленивой связи
     * {@code teacher} (ManyToOne LAZY) после завершения транзакции.
     * <p>
     * Проверяет, что ManyToOne-связь, настроенная на ленивую загрузку,
     * вызывает исключение при обращении вне транзакции.
     * </p>
     */
    @Test
    void lazyInitializationException_manyToOne_shouldOccurAfterTransactionCommit() {
        // Шаг 1: создаём курс с LAZY-связью на преподавателя
        Category category = createTestCategory("Демонстрация LIE ManyToOne");
        User teacher = createTeacher("liemanymany@example.com");

        final Long[] courseId = new Long[1];
        transactionTemplate.execute(status -> {
            Course course = new Course("Курс с ленивым преподавателем", "Описание",
                    10, LocalDate.of(2026, 10, 1), teacher, category);
            entityManager.persist(course);
            entityManager.flush();
            courseId[0] = course.getId();
            return null;
        });

        // Шаг 2: после завершения транзакции пытаемся получить ленивую связь teacher
        Course detachedCourse = courseRepository.findById(courseId[0]).orElseThrow();

        LazyInitializationException exception = assertThrows(
                LazyInitializationException.class,
                () -> detachedCourse.getTeacher().getEmail(),
                "Обращение к ленивой ManyToOne-связи после завершения транзакции должно вызвать LazyInitializationException"
        );

        assertTrue(exception.getMessage().toLowerCase().contains("session"),
                "Сообщение исключения должно содержать информацию о закрытой сессии");
    }

    /**
     * Тест: сервисный метод {@code getCourseStructure} корректно возвращает
     * полную структуру курса (модули и уроки) внутри транзакции.
     * <p>
     * Созданные курс, модуль и урок загружаются через autowired сервис,
     * который выполняется в транзакции через Spring AOP-прокси.
     * Все ленивые коллекции инициализируются и доступны.
     * </p>
     */
    @Test
    void serviceCourseStructure_shouldReturnFullHierarchy() {
        Category category = createTestCategory("Сервисная структура");
        User teacher = createTeacher("servicestruct@example.com");

        // Создаём иерархию курс-модуль-урок в транзакции
        final Long[] courseId = new Long[1];
        transactionTemplate.execute(status -> {
            Course course = new Course("Курс по структуре", "Полная структура курса",
                    30, LocalDate.of(2026, 9, 1), teacher, category);
            entityManager.persist(course);
            entityManager.flush();
            courseId[0] = course.getId();

            Module module = new Module("Введение", "Основы курса", 1, course);
            entityManager.persist(module);
            entityManager.flush();

            Lesson lesson = new Lesson("Первый урок", "Текст урока",
                    "https://example.com/video", module);
            entityManager.persist(lesson);
            entityManager.flush();
            return null;
        });

        // Загрузка через autowired сервисный метод (внутри @Transactional прокси)
        Course loadedCourse = courseService.getCourseStructure(courseId[0]);

        assertNotNull(loadedCourse, "Курс не должен быть null");
        assertEquals("Курс по структуре", loadedCourse.getTitle());

        // Модули должны быть загружены
        assertFalse(loadedCourse.getModules().isEmpty(),
                "Модули должны быть загружены в рамках транзакции");
        assertEquals(1, loadedCourse.getModules().size());

        Module loadedModule = loadedCourse.getModules().iterator().next();
        assertEquals("Введение", loadedModule.getTitle());

        // Уроки должны быть загружены (вложенная инициализация)
        assertFalse(loadedModule.getLessons().isEmpty(),
                "Уроки должны быть загружены в рамках транзакции");
        assertEquals(1, loadedModule.getLessons().size());

        Lesson loadedLesson = loadedModule.getLessons().iterator().next();
        assertEquals("Первый урок", loadedLesson.getTitle());
        assertEquals("Текст урока", loadedLesson.getContent());
    }

    /**
     * Тест: каскадное сохранение иерархии курс-модуль-урок.
     * <p>
     * Создаётся иерархия через сервисный метод, после чего проверяется,
     * что все три сущности сохранены в базе данных PostgreSQL и внешние ключи
     * установлены корректно.
     * </p>
     */
    @Test
    void cascadeSave_shouldPersistFullHierarchy() {
        Category category = createTestCategory("Каскадное сохранение");
        User teacher = createTeacher("cascadesave@example.com");

        // Создаём курс через сервис (внутри транзакции)
        final Long[] courseId = new Long[1];
        transactionTemplate.execute(status -> {
            Course course = new Course("Курс для каскада", "Каскадное сохранение модулей",
                    25, LocalDate.of(2026, 11, 1), teacher, category);
            entityManager.persist(course);
            entityManager.flush();
            courseId[0] = course.getId();
            return null;
        });

        // Добавляем модуль и урок через autowired сервис
        Long moduleId = courseService.addModule(
                courseId[0], "Модуль 1", "Первый модуль курса", 1).getId();
        Long lessonId = courseService.addLesson(
                moduleId, "Урок 1.1", "Содержание урока",
                "https://example.com/lesson1").getId();

        // Проверка: все три сущности существуют в базе данных
        assertTrue(courseRepository.existsById(courseId[0]),
                "Курс должен быть сохранён в базе данных");
        assertTrue(moduleRepository.existsById(moduleId),
                "Модуль должен быть сохранён в базе данных");
        assertTrue(lessonRepository.existsById(lessonId),
                "Урок должен быть сохранён в базе данных");

        // Проверка через прямой SQL: внешние ключи
        Integer moduleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM modules WHERE course_id = ?", Integer.class, courseId[0]);
        assertEquals(1, moduleCount, "Модуль должен быть привязан к курсу через внешний ключ");

        Integer lessonCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lessons WHERE module_id = ?", Integer.class, moduleId);
        assertEquals(1, lessonCount, "Урок должен быть привязан к модулю через внешний ключ");
    }

    /**
     * Тест: каскадное удаление — при удалении курса удаляются все модули и уроки.
     * <p>
     * Создаётся иерархия, затем курс удаляется через autowired сервис.
     * Проверяется, что записи модулей и уроков удалены благодаря каскадным операциям.
     * </p>
     */
    @Test
    void cascadeDelete_shouldRemoveAssociatedEntities() {
        Category category = createTestCategory("Каскадное удаление");
        User teacher = createTeacher("cascadedelete@example.com");

        final Long[] courseId = new Long[1];
        transactionTemplate.execute(status -> {
            Course course = new Course("Курс для каскада удаления", "Удаление с каскадом",
                    15, LocalDate.of(2026, 12, 1), teacher, category);
            entityManager.persist(course);
            entityManager.flush();
            courseId[0] = course.getId();

            Module module = new Module("Модуль для удаления", "Удаление модуля", 1, course);
            entityManager.persist(module);
            entityManager.flush();

            Lesson lesson = new Lesson("Урок для удаления", "Текст", null, module);
            entityManager.persist(lesson);
            entityManager.flush();
            return null;
        });

        // Удаляем курс через autowired сервис (@Transactional метод)
        courseService.deleteCourse(courseId[0]);

        // Проверка: все связанные сущности удалены
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM modules WHERE course_id = ?", Integer.class, courseId[0]),
                "Модули курса должны быть удалены каскадно");
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lessons WHERE module_id IN "
                        + "(SELECT id FROM modules WHERE course_id = ?)", Integer.class, courseId[0]),
                "Уроки модулей должны быть удалены каскадно");

        // Сам курс тоже удалён
        assertFalse(courseRepository.existsById(courseId[0]),
                "Курс должен быть удалён");
    }

    /**
     * Тест: каскадное удаление — общие теги и категории не удаляются.
     * <p>
     * При удалении курса теги и категория остаются в базе данных.
     * Проверяется, что после удаления курса запись категории не исчезает.
     * </p>
     */
    @Test
    void cascadeDelete_shouldPreserveSharedEntities() {
        Category category = createTestCategory("Сохранение общих сущностей");
        User teacher = createTeacher("sharepreservation@example.com");

        final Long[] courseId = new Long[1];
        transactionTemplate.execute(status -> {
            Course course = new Course("Курс с общими сущностями", "Теги и категория",
                    10, LocalDate.of(2026, 9, 15), teacher, category);
            entityManager.persist(course);
            entityManager.flush();
            courseId[0] = course.getId();
            return null;
        });

        // Удаляем курс через autowired сервис
        courseService.deleteCourse(courseId[0]);

        // Проверка: категория осталась
        assertTrue(categoryRepository.existsById(category.getId()),
                "Категория должна остаться в базе данных после удаления курса");

        // Проверка: преподаватель остался
        assertTrue(userRepository.existsById(teacher.getId()),
                "Преподаватель должен остаться в базе данных после удаления курса");
    }

    /**
     * Тест: операция обновления курса.
     * <p>
     * Создаётся курс с исходным названием, затем обновляется через autowired сервис.
     * Проверяется, что в базе данных сохранены обновлённые значения.
     * </p>
     */
    @Test
    void courseUpdate_shouldChangePersistedData() {
        Category category = createTestCategory("Обновление курса");
        User teacher = createTeacher("updatecourse@example.com");

        // Создание курса
        final Long[] courseId = new Long[1];
        transactionTemplate.execute(status -> {
            Course course = new Course("Исходное название", "Первоначальное описание",
                    20, LocalDate.of(2026, 9, 1), teacher, category);
            entityManager.persist(course);
            entityManager.flush();
            courseId[0] = course.getId();
            return null;
        });

        // Обновление через autowired сервисный метод
        Course updated = courseService.updateCourse(
                courseId[0],
                "Обновлённое название",
                "Новое описание курса",
                40,
                LocalDate.of(2027, 1, 1)
        );

        // Проверка: сервис вернул обновлённые данные
        assertEquals("Обновлённое название", updated.getTitle(),
                "Название должно быть обновлено");
        assertEquals("Новое описание курса", updated.getDescription(),
                "Описание должно быть обновлено");
        assertEquals(40, updated.getDuration(),
                "Продолжительность должна быть обновлена");

        // Проверка через прямой SQL: данные сохранены в базе PostgreSQL
        List<List<String>> rows = jdbcTemplate.query(
                "SELECT title, description, duration, start_date FROM courses WHERE id = ?",
                (rs, rowNum) -> List.of(
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("duration"),
                        rs.getString("start_date")
                ),
                courseId[0]
        );
        assertEquals(1, rows.size(), "Курс должен быть найден по SQL");
        List<String> row = rows.get(0);

        assertEquals("Обновлённое название", row.get(0),
                "Название должно быть сохранено в базе данных");
        assertEquals("Новое описание курса", row.get(1),
                "Описание должно быть сохранено в базе данных");
        assertEquals("40", row.get(2),
                "Продолжительность должна быть сохранена в базе данных");
    }

    /**
     * Тест: создание сущности через репозиторий и её чтение.
     * <p>
     * Проверяет базовый CRUD-сценарий: сохранение курса через репозиторий
     * и последующее чтение того же курса.
     * </p>
     */
    @Test
    void crudCreateAndRead_shouldWork() {
        Category category = createTestCategory("CRUD создание и чтение");
        User teacher = createTeacher("crudcreate@example.com");

        final Long[] courseId = new Long[1];
        transactionTemplate.execute(status -> {
            Course course = new Course("CRUD Курс", "Описание для CRUD",
                    12, LocalDate.of(2026, 10, 1), teacher, category);
            entityManager.persist(course);
            entityManager.flush();
            courseId[0] = course.getId();
            return null;
        });

        // Чтение из базы через репозиторий
        Course found = courseRepository.findById(courseId[0]).orElseThrow();
        assertEquals("CRUD Курс", found.getTitle());
        assertEquals("Описание для CRUD", found.getDescription());
        assertEquals(12, found.getDuration());

        // Проверка через SQL
        List<String> row = jdbcTemplate.query(
                "SELECT title, duration FROM courses WHERE id = ?",
                (rs, rowNum) -> List.of(rs.getString("title"), rs.getString("duration")),
                courseId[0]
        ).get(0);

        assertEquals("CRUD Курс", row.get(0));
        assertEquals("12", row.get(1));
    }

    /**
     * Тест: удаление сущности из базы данных.
     * <p>
     * Создаёт курс, удаляет его, затем проверяет, что запись отсутствует в базе.
     * </p>
     */
    @Test
    void crudDelete_shouldRemoveFromDatabase() {
        Category category = createTestCategory("CRUD удаление");
        User teacher = createTeacher("cruddelete@example.com");

        final Long[] courseId = new Long[1];
        transactionTemplate.execute(status -> {
            Course course = new Course("Курс для удаления CRUD", "Будет удалён",
                    5, LocalDate.of(2026, 11, 1), teacher, category);
            entityManager.persist(course);
            entityManager.flush();
            courseId[0] = course.getId();
            return null;
        });

        // Проверка: курс существует
        assertTrue(courseRepository.existsById(courseId[0]));

        // Удаление
        courseService.deleteCourse(courseId[0]);

        // Проверка: курс удалён
        assertFalse(courseRepository.existsById(courseId[0]),
                "Курс должен быть удалён из базы данных");

        // Проверка через SQL: курс удалён
        List<List<String>> rows = jdbcTemplate.query(
                "SELECT title FROM courses WHERE id = ?",
                (rs, rowNum) -> List.of(rs.getString("title")),
                courseId[0]
        );
        assertTrue(rows.isEmpty(),
                "SQL-запрос не должен вернуть курс после удаления");
    }

    /**
     * Проверка: в тестах используется PostgreSQL через Testcontainers.
     * <p>
     * Гарантирует, что {@code @SpringBootTest} с профилем «test»
     * подключается к тестовой PostgreSQL через Testcontainers,
     * а не к другой базе данных.
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

    /**
     * Тест: ленивая загрузка коллекции тегов вызывает исключение вне транзакции.
     * <p>
     * Проверяет, что связь ManyToMany с флагом LAZY также защищает данные
     * от незапланированной загрузки.
     * </p>
     */
    @Test
    void lazyInitializationException_manyToMany_shouldOccurAfterTransactionCommit() {
        Category category = createTestCategory("Демонстрация LIE ManyToMany");
        User teacher = createTeacher("liemanymany2@example.com");

        final Long[] courseId = new Long[1];
        transactionTemplate.execute(status -> {
            Course course = new Course("Курс с ленивыми тегами", "Демонстрация ManyToMany LAZY",
                    20, LocalDate.of(2026, 9, 1), teacher, category);
            entityManager.persist(course);
            entityManager.flush();
            courseId[0] = course.getId();
            return null;
        });

        // Теги не привязаны, но при обращении к пустой ленивой коллекции
        // Hibernate всё равно должен попытаться выполнить запрос для инициализации.
        Course detachedCourse = courseRepository.findById(courseId[0]).orElseThrow();

        LazyInitializationException exception = assertThrows(
                LazyInitializationException.class,
                () -> detachedCourse.getTags().size(),
                "Обращение к ленивой ManyToMany-коллекции после завершения транзакции должно вызвать LazyInitializationException"
        );

        assertTrue(exception.getMessage().toLowerCase().contains("session"),
                "Сообщение исключения должно содержать информацию о закрытой сессии");
    }
}
