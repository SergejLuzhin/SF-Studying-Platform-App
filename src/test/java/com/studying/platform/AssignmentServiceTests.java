package com.studying.platform;

import com.studying.platform.entity.Assignment;
import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Lesson;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.repository.AssignmentRepository;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.LessonRepository;
import com.studying.platform.repository.ModuleRepository;
import com.studying.platform.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для сущности «Задание» учебной платформы.
 * <p>
 * Проверяют сохранение, обновление, удаление задания, связь с уроком,
 * ограничения целостности данных (ссылка на урок обязательна),
 * а также корректное хранение текста поля description
 * через JPA и прямой SQL на PostgreSQL.
 * </p>
 * <p>
 * Тесты выполняются с PostgreSQL через Testcontainers в профиле «test».
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AssignmentServiceTests {

    /**
     * Менеджер сущностей для ручного управления транзакциями.
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * Репозиторий для сущности Assignment.
     */
    @Autowired
    private AssignmentRepository assignmentRepository;

    /**
     * Репозиторий для сущности Lesson.
     */
    @Autowired
    private LessonRepository lessonRepository;

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
     * JdbcTemplate для выполнения прямых SQL-запросов в транзакции.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

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
     * @param email адрес электронной почты преподавателя
     * @return сохранённый пользователь
     */
    private User createTeacher(String email) {
        User user = new User(email, UserRole.TEACHER);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    /**
     * Создаёт и сохраняет тестовый курс с преподавателем и категорией.
     *
     * @param title    название курса
     * @param teacher  преподаватель
     * @param category категория
     * @return сохранённый курс
     */
    private Course createCourse(String title, User teacher, Category category) {
        Course course = new Course(title, "Описание тестового курса", 40,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();
        return course;
    }

    /**
     * Создаёт и сохраняет тестовый модуль в указанном курсе.
     *
     * @param title    название модуля
     * @param course   курс
     * @param orderIndex порядковый номер
     * @return сохранённый модуль
     */
    private Module createModule(String title, Course course, Integer orderIndex) {
        Module module = new Module();
        module.setTitle(title);
        module.setOrderIndex(orderIndex);
        module.setCourse(course);
        entityManager.persist(module);
        entityManager.flush();
        return module;
    }

    /**
     * Создаёт и сохраняет тестовый урок в указанном модуле.
     *
     * @param title   название урока
     * @param content содержание урока
     * @param module  модуль
     * @return сохранённый урок
     */
    private Lesson createLesson(String title, String content, Module module) {
        Lesson lesson = new Lesson();
        lesson.setTitle(title);
        lesson.setContent(content);
        lesson.setModule(module);
        entityManager.persist(lesson);
        entityManager.flush();
        return lesson;
    }

    /**
     * Тест: создание задания с привязкой к уроку.
     * <p>
     * Создаёт полный контект: категория, преподаватель, курс, модуль, урок.
     * Сохраняет задание, проверяет идентификатор, поля и связь с уроком.
     * </p>
     */
    @Test
    void testCreateAssignmentWithLesson() {
        Category category = createCategory("Программирование");
        User teacher = createTeacher("teacher_assignment@example.com");
        Course course = createCourse("Основы Java", teacher, category);

        Module module = createModule("Учебный модуль", course, 1);
        Lesson lesson = createLesson("Введение в Java", "Основы синтаксиса", module);

        Assignment assignment = new Assignment();
        assignment.setTitle("Домашнее задание 1");
        assignment.setDescription("Написать программу приветствия");
        assignment.setDueDate(LocalDate.of(2026, 10, 1));
        assignment.setMaxScore(100);
        assignment.setLesson(lesson);

        entityManager.persist(assignment);
        entityManager.flush();

        Long assignmentId = assignment.getId();
        assertNotNull(assignmentId, "Идентификатор задания не должен быть null");
        assertEquals("Домашнее задание 1", assignment.getTitle());
        assertEquals("Написать программу приветствия", assignment.getDescription());
        assertEquals(LocalDate.of(2026, 10, 1), assignment.getDueDate());
        assertEquals(100, assignment.getMaxScore());
        assertEquals(lesson.getId(), assignment.getLesson().getId(),
                "Внешний ключ задания должен ссылаться на урок");
    }

    /**
     * Тест: обновление всех полей задания.
     * <p>
     * Создаёт задание, затем обновляет title, description, dueDate и maxScore.
     * Проверяет, что все изменения сохранены.
     * </p>
     */
    @Test
    void testUpdateAssignmentFields() {
        Category category = createCategory("Математика");
        User teacher = createTeacher("teacher_update_assign@example.com");
        Course course = createCourse("Высшая математика", teacher, category);

        Module module = createModule("Алгебра", course, 1);
        Lesson lesson = createLesson("Линейная алгебра", "Матрицы и определители", module);

        Assignment assignment = new Assignment(lesson, "Первое задание",
                "Решить задачи 1-10", LocalDate.of(2026, 9, 15), 50);
        entityManager.persist(assignment);
        entityManager.flush();

        Long assignmentId = assignment.getId();

        // Обновляем задание
        Assignment loaded = assignmentRepository.findById(assignmentId).orElseThrow();
        loaded.setTitle("Обновлённое задание");
        loaded.setDescription("Решить задачи 11-20 и подготовить доклад");
        loaded.setDueDate(LocalDate.of(2026, 10, 1));
        loaded.setMaxScore(150);
        entityManager.merge(loaded);
        entityManager.flush();

        Assignment updated = assignmentRepository.findById(assignmentId).orElseThrow();
        assertEquals("Обновлённое задание", updated.getTitle());
        assertEquals("Решить задачи 11-20 и подготовить доклад", updated.getDescription());
        assertEquals(LocalDate.of(2026, 10, 1), updated.getDueDate());
        assertEquals(150, updated.getMaxScore());
        assertEquals(lesson.getId(), updated.getLesson().getId(),
                "Связь с уроком не должна измениться при обновлении");
    }

    /**
     * Тест: удаление задания.
     * <p>
     * Создаёт задание, сохраняет, затем удаляет по идентификатору.
     * Проверяет отсутствие в репозитории.
     * </p>
     */
    @Test
    void testDeleteAssignment() {
        Category category = createCategory("Физика");
        User teacher = createTeacher("teacher_delete_assign@example.com");
        Course course = createCourse("Квантовая физика", teacher, category);

        Module module = createModule("Волновая механика", course, 1);
        Lesson lesson = createLesson("Уравнение Шрёдингера", null, module);

        Assignment assignment = new Assignment(lesson, "Расчёт волновой функции",
                "Вычислить волновую функцию частицы в ящике",
                LocalDate.of(2026, 11, 1), 80);
        entityManager.persist(assignment);
        entityManager.flush();

        Long assignmentId = assignment.getId();
        assertTrue(assignmentRepository.findById(assignmentId).isPresent(),
                "Задание должно существовать перед удалением");

        assignmentRepository.deleteById(assignmentId);
        entityManager.flush();
        entityManager.clear();

        assertFalse(assignmentRepository.findById(assignmentId).isPresent(),
                "Удалённое задание не должно быть найдено");
    }

    /**
     * Тест: null lesson для задания вызывает нарушение целостности данных.
     * <p>
     * Попытка сохранить задание без урока должна привести к
     * {@code ConstraintViolationException} из-за ограничения NOT NULL
     * на колонке {@code lesson_id}.
     * </p>
     */
    @Test
    void testNullLessonForAssignment() {
        Assignment assignment = new Assignment();
        assignment.setTitle("Задание без урока");
        assignment.setDescription("Некорректное задание");

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(assignment);
            entityManager.flush();
        }, "Сохранение задания без урока должно вызвать нарушение целостности данных");
    }

    /**
     * Тест: хранение текста в поле description через JPA и прямое SQL-чтение.
     * <p>
     * Сохраняет задание с длинным текстом в description, затем читает
     * через JPA и через JdbcTemplate для верификации TEXT-поля.
     * </p>
     */
    @Test
    void testTextStorageViaJpaAndSql() {
        // Подготовка контекста
        Category category = createCategory("Литература");
        User teacher = createTeacher("teacher_text@example.com");
        Course course = createCourse("История русской литературы", teacher, category);

        Module module = createModule("Золотой век", course, 1);
        Lesson lesson = createLesson("Пушкин и его время", null, module);

        // Длинный текст для проверки TEXT-поля
        StringBuilder longDescription = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longDescription.append("Строка текста номер ").append(i).append(
                    ": Это проверочный текст для хранения в поле description ").append(
                    "сущности Assignment. Текст должен быть сохранён без потерь ").append(
                    "и корректно прочитан как через JPA, так и через прямой SQL. ").append(
                    "Данный текст имитирует реальное содержание описания задания, ").append(
                    "которое может быть достаточно объёмным. ");
        }

        Assignment assignment = new Assignment(lesson, "Аналитическое сочинение",
                longDescription.toString(), LocalDate.of(2026, 12, 1), 100);
        entityManager.persist(assignment);
        entityManager.flush();

        Long assignmentId = assignment.getId();
        String savedText = longDescription.toString();

        // Чтение через JPA
        Assignment loadedViaJpa = assignmentRepository.findById(assignmentId).orElseThrow();
        assertEquals(savedText, loadedViaJpa.getDescription(),
                "Текст, сохранённый через JPA, должен быть прочитан без изменений");

        // Чтение через JdbcTemplate (в той же транзакции)
        String sqlText = jdbcTemplate.queryForObject(
                "SELECT \"description\" FROM assignments WHERE id = ?",
                String.class, assignmentId);
        assertEquals(savedText, sqlText,
                "Текст из прямого SQL должен совпадать с исходным");
    }

    /**
     * Тест: репозиторий Assignment находит задания по lessonId.
     */
    @Test
    void testAssignmentRepositoryFindByLessonId() {
        Category category = createCategory("Информатика");
        User teacher = createTeacher("teacher_findassign@example.com");
        Course course = createCourse("Основы алгоритмов", teacher, category);

        Module module = createModule("Сортировки", course, 1);
        Lesson lesson = createLesson("Алгоритмы сортировки", null, module);

        Assignment assignment1 = new Assignment(lesson, "Bubble Sort",
                "Реализовать пузырьковую сортировку",
                LocalDate.of(2026, 10, 1), 60);
        entityManager.persist(assignment1);
        entityManager.flush();

        Assignment assignment2 = new Assignment(lesson, "Quick Sort",
                "Реализовать быструю сортировку",
                LocalDate.of(2026, 10, 15), 80);
        entityManager.persist(assignment2);
        entityManager.flush();

        Assignment assignment3 = new Assignment(lesson, "Merge Sort",
                "Реализовать сортировку слиянием",
                LocalDate.of(2026, 10, 30), 80);
        entityManager.persist(assignment3);
        entityManager.flush();

        var assignments = assignmentRepository.findByLessonId(lesson.getId());
        assertEquals(3, assignments.size(),
                "Должно быть найдено 3 задания для данного урока");
    }

    /**
     * Тест: репозиторий Assignment возвращает пустой список при отсутствии заданий.
     */
    @Test
    void testAssignmentRepositoryReturnsEmptyListWhenNoAssignments() {
        Category category = createCategory("История");
        User teacher = createTeacher("teacher_emptyassign@example.com");
        Course course = createCourse("Вторая мировая война", teacher, category);

        Module module = createModule("Причины войны", course, 1);
        Lesson lesson = createLesson("Предпосылки конфликта", null, module);

        var assignments = assignmentRepository.findByLessonId(lesson.getId());
        assertTrue(assignments.isEmpty(),
                "Список заданий должен быть пустым");
    }

    /**
     * Тест: задание с null значениями необязательных полей сохраняется.
     * <p>
     * dueDate и maxScore могут быть null — это допустимые значения.
     * </p>
     */
    @Test
    void testAssignmentWithNullOptionalFields() {
        Category category = createCategory("Художественный");
        User teacher = createTeacher("teacher_nullfields@example.com");
        Course course = createCourse("Живопись", teacher, category);

        Module module = createModule("Масляные краски", course, 1);
        Lesson lesson = createLesson("Техники масляной живописи", null, module);

        Assignment assignment = new Assignment(lesson, "Свободное задание",
                "Нарисовать пейзаж", null, null);
        entityManager.persist(assignment);
        entityManager.flush();

        Long assignmentId = assignment.getId();
        assertNotNull(assignmentId, "Идентификатор не должен быть null");

        Assignment loaded = assignmentRepository.findById(assignmentId).orElseThrow();
        assertNull(loaded.getDueDate(), "Срок выполнения может быть null");
        assertNull(loaded.getMaxScore(), "Максимальный балл может быть null");
        assertEquals("Свободное задание", loaded.getTitle());
    }

    /**
     * Тест: задание не удаляет связанный урок.
     * <p>
     * При удалении задания урок, категория, преподаватель и курс должны остаться.
     * </p>
     */
    @Test
    void testDeleteAssignmentDoesNotAffectRelatedObjects() {
        Category category = createCategory("Биология");
        User teacher = createTeacher("teacher_assignnotdelete@example.com");
        Course course = createCourse("Молекулярная биология", teacher, category);

        Module module = createModule("ДНК и РНК", course, 1);
        Lesson lesson = createLesson("Структура ДНК", null, module);

        Assignment assignment = new Assignment(lesson, "Моделирование двойной спирали",
                "Создать 3D-модель ДНК", LocalDate.of(2026, 11, 15), 70);
        entityManager.persist(assignment);
        entityManager.flush();

        Long assignmentId = assignment.getId();
        Long lessonId = lesson.getId();
        Long moduleId = module.getId();
        Long courseId = course.getId();
        Long teacherId = teacher.getId();
        Long categoryId = category.getId();

        assignmentRepository.deleteById(assignmentId);
        entityManager.flush();
        entityManager.clear();

        assertFalse(assignmentRepository.findById(assignmentId).isPresent(),
                "Задание должно быть удалено");
        assertTrue(lessonRepository.findById(lessonId).isPresent(),
                "Урок должен остаться");
        assertTrue(moduleRepository.findById(moduleId).isPresent(),
                "Модуль должен остаться");
        assertTrue(courseRepository.findById(courseId).isPresent(),
                "Курс должен остаться");
        assertTrue(userRepository.findById(teacherId).isPresent(),
                "Преподаватель должен остаться");
        assertTrue(categoryRepository.findById(categoryId).isPresent(),
                "Категория должна остаться");
    }

    /**
     * Тест: обновление только части полей задания.
     * <p>
     * При обновлении только названия и срока выполнения остальные поля
     * (описание и maxScore) не должны измениться.
     * </p>
     */
    @Test
    void testPartialUpdateAssignment() {
        Category category = createCategory("Экономика");
        User teacher = createTeacher("teacher_partialupdate@example.com");
        Course course = createCourse("Микроэкономика", teacher, category);

        Module module = createModule("Спрос и предложение", course, 1);
        Lesson lesson = createLesson("Равновесная цена", null, module);

        Assignment assignment = new Assignment(lesson, "Кривые спроса",
                "Построить графики", LocalDate.of(2026, 9, 20), 90);
        entityManager.persist(assignment);
        entityManager.flush();

        Long assignmentId = assignment.getId();
        String originalDescription = "Построить графики";
        Integer originalMaxScore = 90;

        // Обновляем только title и dueDate
        Assignment loaded = assignmentRepository.findById(assignmentId).orElseThrow();
        loaded.setTitle("Обновлённые кривые");
        loaded.setDueDate(LocalDate.of(2026, 10, 5));
        entityManager.merge(loaded);
        entityManager.flush();

        Assignment updated = assignmentRepository.findById(assignmentId).orElseThrow();
        assertEquals("Обновлённые кривые", updated.getTitle());
        assertEquals(LocalDate.of(2026, 10, 5), updated.getDueDate());
        assertEquals(originalDescription, updated.getDescription(),
                "Описание не должно измениться при частичном обновлении");
        assertEquals(originalMaxScore, updated.getMaxScore(),
                "Максимальный балл не должен измениться при частичном обновлении");
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
