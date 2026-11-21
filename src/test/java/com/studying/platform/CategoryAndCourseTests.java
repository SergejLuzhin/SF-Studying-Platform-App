package com.studying.platform;

import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.TransientPropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для сущностей «Категория» и «Курс» учебной платформы.
 * <p>
 * Проверяют корректное создание, сохранение, чтение, обновление и удаление категорий и курсов,
 * а также соблюдение ограничений базы данных: уникальность имени категории, обязательность полей,
 * корректность внешних ключей на преподавателя и категорию.
 * </p>
 * <p>
 * Тесты выполняются с PostgreSQL через Testcontainers в профиле «test».
 * Исключения проверяются на уровне Hibernate (без Spring-обёртки).
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryAndCourseTests {

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
     * Репозиторий для сущности Category.
     */
    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Репозиторий для сущности Course.
     */
    @Autowired
    private CourseRepository courseRepository;

    /**
     * Репозиторий для сущности User.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Проверка: сохранение категории и курса с преподавателем.
     * <p>
     * Создаёт категорию и преподавателя, затем курс с привязкой к обоим.
     * Проверяет, что все объекты корректно сохраняются и связи установлены.
     * </p>
     */
    @Test
    void saveCategoryAndCourse_shouldPersistAndRetrieve() {
        // Создание преподавателя
        User teacher = new User("teacher@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        // Создание категории
        Category category = new Category("Программирование");
        entityManager.persist(category);
        entityManager.flush();

        // Создание курса
        Course course = new Course("Введение в Java", "Основы Java для начинающих", 40,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        // Проверка: оба объекта сохранены
        Long courseId = course.getId();
        assertNotNull(courseId, "Идентификатор курса не должен быть null после сохранения");

        Long categoryId = category.getId();
        assertNotNull(categoryId, "Идентификатор категории не должен быть null после сохранения");

        // Чтение курса
        Course savedCourse = courseRepository.findById(courseId).orElseThrow();
        assertEquals("Введение в Java", savedCourse.getTitle());
        assertEquals("Основы Java для начинающих", savedCourse.getDescription());
        assertEquals(40, savedCourse.getDuration());
        assertEquals(LocalDate.of(2026, 9, 1), savedCourse.getStartDate());
        assertEquals(teacher.getId(), savedCourse.getTeacher().getId());
        assertEquals(category.getId(), savedCourse.getCategory().getId());

        // Чтение категории
        Category savedCategory = categoryRepository.findById(categoryId).orElseThrow();
        assertEquals("Программирование", savedCategory.getName());
    }

    /**
     * Проверка: поиск курса по названию.
     * <p>
     * Создаёт категорию и преподавателя, сохраняет курс, затем ищет его по названию.
     * Проверяет, что курс найден с корректными данными.
     * </p>
     */
    @Test
    void findByTitle_shouldReturnCourse() {
        User teacher = new User("findteacher@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Дизайн");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("UI/UX Дизайн", "Курс по дизайну интерфейсов", 20,
                LocalDate.of(2026, 10, 15), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        // Поиск по названию
        var found = courseRepository.findByTitle("UI/UX Дизайн");
        assertTrue(found.isPresent(), "Курс с названием UI/UX Дизайн должен быть найден");
        assertEquals("UI/UX Дизайн", found.get().getTitle());
        assertEquals("Курс по дизайну интерфейсов", found.get().getDescription());
    }

    /**
     * Проверка: поиск курса по несуществующему названию.
     * <p>
     * Проверяет, что метод {@code findByTitle} возвращает пустое значение
     * для отсутствующего курса.
     * </p>
     */
    @Test
    void findByTitle_shouldReturnEmptyForNonExistent() {
        var found = courseRepository.findByTitle("Отсутствующий курс");
        assertTrue(found.isEmpty(), "Курс с несуществующим названием не должен быть найден");
    }

    /**
     * Проверка: обновление данных курса.
     * <p>
     * Создаёт курс, загружает его, обновляет название и описание,
     * затем проверяет, что изменения корректно сохранены.
     * </p>
     */
    @Test
    void updateCourse_shouldChangeData() {
        User teacher = new User("updateteacher@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Маркетинг");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("Основы маркетинга", "Начальный курс", 15,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        // Обновление
        Course loaded = courseRepository.findById(course.getId()).orElseThrow();
        loaded.setTitle("Маркетинг: продвинутый курс");
        loaded.setDescription("Расширенный курс по digital-маркетингу.");
        loaded.setDuration(30);
        entityManager.merge(loaded);
        entityManager.flush();

        // Проверка обновлённых данных
        Course updated = courseRepository.findById(course.getId()).orElseThrow();
        assertEquals("Маркетинг: продвинутый курс", updated.getTitle());
        assertEquals("Расширенный курс по digital-маркетингу.", updated.getDescription());
        assertEquals(30, updated.getDuration());
    }

    /**
     * Проверка: удаление курса из базы данных.
     * <p>
     * Создаёт курс без связанных модулей, записей и отзывов, затем удаляет его.
     * Проверяет, что курс корректно удалён.
     * </p>
     */
    @Test
    void deleteCourse_shouldRemoveFromDatabase() {
        User teacher = new User("deleteteacher@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Философия");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("Введение в философию", "Краткий обзор философских идей", 10,
                LocalDate.of(2026, 11, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        Long courseId = course.getId();

        // Удаление курса
        courseRepository.deleteById(courseId);
        entityManager.flush();

        // Проверка: курс удалён
        assertTrue(courseRepository.findById(courseId).isEmpty(),
                "Курс должен быть удалён из базы данных");
    }

    /**
     * Проверка: создание курса с null названием.
     * <p>
     * Пытается создать курс с {@code null} названием
     * и проверяет, что Hibernate отвергает такую операцию.
     * </p>
     */
    @Test
    void saveCourse_nullTitle_shouldThrow() {
        User teacher = new User("nulltitle@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Тестовая категория");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course(null, "Описание", 20,
                LocalDate.of(2026, 9, 1), teacher, category);

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(course);
            entityManager.flush();
        }, "Сохранение курса с null названием должно вызвать нарушение целостности данных");
    }

    /**
     * Проверка: создание курса с null датой начала.
     * <p>
     * Пытается создать курс с {@code null} датой начала
     * и проверяет, что Hibernate отвергает такую операцию.
     * </p>
     */
    @Test
    void saveCourse_nullStartDate_shouldThrow() {
        User teacher = new User("nulldate@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Тестовая категория");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("Курс", "Описание", 20,
                null, teacher, category);

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(course);
            entityManager.flush();
        }, "Сохранение курса с null датой начала должно вызвать нарушение целостности данных");
    }

    /**
     * Проверка: создание курса без преподавателя.
     * <p>
     * Пытается создать курс с {@code null} преподавателем
     * и проверяет, что Hibernate отвергает такую операцию.
     * </p>
     */
    @Test
    void saveCourse_nullTeacher_shouldThrow() {
        Category category = new Category("Тестовая категория");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("Курс", "Описание", 20,
                LocalDate.of(2026, 9, 1), null, category);

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(course);
            entityManager.flush();
        }, "Сохранение курса без преподавателя должно вызвать нарушение целостности данных");
    }

    /**
     * Проверка: создание курса без категории.
     * <p>
     * Пытается создать курс с {@code null} категорией
     * и проверяет, что Hibernate отвергает такую операцию.
     * </p>
     */
    @Test
    void saveCourse_nullCategory_shouldThrow() {
        User teacher = new User("nullcategory@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Course course = new Course("Курс", "Описание", 20,
                LocalDate.of(2026, 9, 1), teacher, null);

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(course);
            entityManager.flush();
        }, "Сохранение курса без категории должно вызвать нарушение целостности данных");
    }

    /**
     * Проверка: уникальность имени категории.
     * <p>
     * Создаёт две категории с одинаковым именем
     * и проверяет, что база данных выбрасывает исключение о нарушении целостности данных.
     * </p>
     */
    @Test
    void saveCategory_duplicateName_shouldThrow() {
        Category category1 = new Category("Программирование");
        entityManager.persist(category1);
        entityManager.flush();

        Category category2 = new Category("Программирование");

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(category2);
            entityManager.flush();
        }, "Сохранение категории с дублирующим именем должно вызвать нарушение целостности данных");
    }

    /**
     * Проверка: курс со ссылкой на несуществующего преподавателя.
     * <p>
     * Пытается создать курс с непривязанным объектом преподавателя
     * и проверяет, что Hibernate отвергает операцию.
     * </p>
     */
    @Test
    void saveCourse_nonExistentTeacher_shouldThrow() {
        Category category = new Category("Тестовая категория");
        entityManager.persist(category);
        entityManager.flush();

        User detachedTeacher = new User("ghost@example.com", UserRole.TEACHER);
        // Преподаватель не сохраняется — остаётся «призрачным»

        Course course = new Course("Курс с призраком", "Описание", 20,
                LocalDate.of(2026, 9, 1), detachedTeacher, category);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            entityManager.persist(course);
            entityManager.flush();
        }, "Сохранение курса с несуществующим преподавателем должно вызвать ошибку transient-значения");
        assertTrue(exception.getCause() instanceof TransientPropertyValueException,
                "Причина исключения должна быть TransientPropertyValueException");
    }

    /**
     * Проверка: курс со ссылкой на несуществующую категорию.
     * <p>
     * Пытается создать курс с непривязанной категорией
     * и проверяет, что Hibernate отвергает операцию.
     * </p>
     */
    @Test
    void saveCourse_nonExistentCategory_shouldThrow() {
        User teacher = new User("ghostteacher@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category detachedCategory = new Category("Призрачная категория");
        // Категория не сохраняется — остаётся «призрачной»

        Course course = new Course("Курс с призраком", "Описание", 20,
                LocalDate.of(2026, 9, 1), teacher, detachedCategory);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            entityManager.persist(course);
            entityManager.flush();
        }, "Сохранение курса с несуществующей категорией должно вызвать ошибку transient-значения");
        assertTrue(exception.getCause() instanceof TransientPropertyValueException,
                "Причина исключения должна быть TransientPropertyValueException");
    }

    /**
     * Проверка: загрузка курса — категория доступна сразу (EAGER).
     * <p>
     * Создаёт и сохраняет курс, затем загружает его и проверяет,
     * что объект категории загружен корректно (EAGER-загрузка).
     * </p>
     */
    @Test
    void loadCourse_shouldFetchCategory() {
        User teacher = new User("catfetch@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Математика");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("Линейная алгебра", "Основы линейной алгебры", 35,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        // Загрузка курса
        Course loaded = courseRepository.findById(course.getId()).orElseThrow();
        assertNotNull(loaded.getCategory(), "Категория курса должна быть загружена (EAGER)");
        assertEquals("Математика", loaded.getCategory().getName());
    }

    /**
     * Проверка: загрузка курса — ссылка на преподавателя доступна.
     * <p>
     * Создаёт и сохраняет курс, затем загружает его и проверяет,
     * что ссылка на преподавателя доступна.
     * </p>
     */
    @Test
    void loadCourse_teacherReference() {
        User teacher = new User("teacherref@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Физика");
        entityManager.persist(category);
        entityManager.flush();

        Course course = new Course("Квантовая механика", "Основы квантовой физики", 45,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        // Загрузка курса
        Course loaded = courseRepository.findById(course.getId()).orElseThrow();
        assertNotNull(loaded.getTeacher(), "Преподаватель курса должен быть доступен");
        assertEquals("teacherref@example.com", loaded.getTeacher().getEmail());
        assertEquals(UserRole.TEACHER, loaded.getTeacher().getRole());
    }

    /**
     * Проверка: сохранение категории с курсами.
     * <p>
     * Создаёт категорию, добавляет в неё курс через коллекцию {@code courses},
     * сохраняет категорию и проверяет, что курс корректно связан.
     * </p>
     */
    @Test
    void saveCategoryWithCourses_shouldPersistCourse() {
        User teacher = new User("categoryteacher@example.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        Category category = new Category("Химия");
        entityManager.persist(category);
        entityManager.flush();

        // Создание курса с привязкой к категории
        Course course = new Course("Органическая химия", "Курс по органической химии", 30,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();

        // Добавление курса в коллекцию категории
        category.getCourses().add(course);
        entityManager.merge(category);
        entityManager.flush();

        // Проверка: категория загружена с курсами
        Category loaded = categoryRepository.findById(category.getId()).orElseThrow();
        assertFalse(loaded.getCourses().isEmpty(),
                "Коллекция курсов категории не должна быть пустой");
        assertEquals(1, loaded.getCourses().size(),
                "В категории должен быть ровно один курс");
        assertEquals("Органическая химия", loaded.getCourses().iterator().next().getTitle());
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
