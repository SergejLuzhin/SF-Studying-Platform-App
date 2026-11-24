package com.studying.platform;

import com.studying.platform.entity.Enrollment;
import com.studying.platform.entity.EnrollmentStatus;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.Question;
import com.studying.platform.entity.Quiz;
import com.studying.platform.entity.QuizQuestionType;
import com.studying.platform.entity.Tag;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.repository.AnswerOptionRepository;
import com.studying.platform.repository.AssignmentRepository;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.EnrollmentRepository;
import com.studying.platform.repository.LessonRepository;
import com.studying.platform.repository.ModuleRepository;
import com.studying.platform.repository.QuestionRepository;
import com.studying.platform.repository.QuizRepository;
import com.studying.platform.repository.TagRepository;
import com.studying.platform.repository.UserRepository;
import com.studying.platform.seeder.DemoDataSeeder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для сидера демонстрационных данных {@link DemoDataSeeder}.
 * <p>
 * Проверяют создание полного набора демонстрационных данных (преподаватель, студенты,
 * курс со структурой, записи, викторина) и корректную обработку повторного запуска
 * без создания дубликатов. Тесты выполняются через Testcontainers + PostgreSQL
 * с активацией профиля «test,demo».
 * </p>
 * <p>
 * Используется профиль «test» с {@code ddl-auto: create-drop} для полной изоляции
 * между запусками. Профиль «demo» активирует {@link DemoDataSeeder}.
 * </p>
 *
 * @see DemoDataSeeder
 */
@SpringBootTest
@ActiveProfiles({"test", "demo"})
class DemoDataSeederTest {

    /**
     * Репозиторий пользователей для проверки созданных пользователей.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Репозиторий категорий для проверки созданных категорий.
     */
    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Репозиторий тегов для проверки созданных тегов.
     */
    @Autowired
    private TagRepository tagRepository;

    /**
     * Репозиторий курсов для проверки созданных курсов.
     */
    @Autowired
    private com.studying.platform.repository.CourseRepository courseRepository;

    /**
     * Репозиторий модулей для проверки созданных модулей.
     */
    @Autowired
    private ModuleRepository moduleRepository;

    /**
     * Репозиторий уроков для проверки созданных уроков.
     */
    @Autowired
    private LessonRepository lessonRepository;

    /**
     * Репозиторий заданий для проверки созданных заданий.
     */
    @Autowired
    private AssignmentRepository assignmentRepository;

    /**
     * Репозиторий записей для проверки созданных записей.
     */
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    /**
     * Репозиторий викторин для проверки созданных викторин.
     */
    @Autowired
    private QuizRepository quizRepository;

    /**
     * Репозиторий вопросов для проверки созданных вопросов.
     */
    @Autowired
    private QuestionRepository questionRepository;

    /**
     * Репозиторий вариантов ответа для проверки созданных вариантов.
     */
    @Autowired
    private AnswerOptionRepository answerOptionRepository;

    /**
     * Менеджер сущностей для прямой работы с базой данных.
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * Источник данных для прямого SQL-подключения.
     */
    @Autowired
    private DataSource dataSource;

    /**
     * Проверка: сидер создаёт преподавателя при первом запуске.
     * <p>
     * Проверяет, что после старта приложения с профилем «demo» в базе данных
     * присутствует демонстрационный преподаватель с email {@code demo-teacher@platform.test}
     * и ролью {@code TEACHER}.
     * </p>
     */
    @Test
    void seeder_createsDemoTeacherOnFirstRun() {
        Optional<User> teacher = userRepository.findByEmail("demo-teacher@platform.test");

        assertTrue(teacher.isPresent(), "Демонстрационный преподаватель должен быть создан");
        assertEquals("demo-teacher@platform.test", teacher.get().getEmail());
        assertEquals(UserRole.TEACHER, teacher.get().getRole());
    }

    /**
     * Проверка: сидер создаёт двух студентов.
     * <p>
     * Проверяет наличие двух демонстрационных студентов с ролями {@code STUDENT}.
     * </p>
     */
    @Test
    void seeder_createsDemoStudents() {
        Optional<User> studentOne = userRepository.findByEmail("demo-student-one@platform.test");
        Optional<User> studentTwo = userRepository.findByEmail("demo-student-two@platform.test");

        assertTrue(studentOne.isPresent(), "Студент One должен быть создан");
        assertTrue(studentTwo.isPresent(), "Студент Two должен быть создан");
        assertEquals(UserRole.STUDENT, studentOne.get().getRole());
        assertEquals(UserRole.STUDENT, studentTwo.get().getRole());
    }

    /**
     * Проверка: сидер создаёт категорию и теги.
     * <p>
     * Проверяет создание категории «Programming» и трёх тегов:
     * «Java», «Hibernate», «SpringBoot».
     * </p>
     */
    @Test
    void seeder_createsCategoryAndTags() {
        Optional<Tag> javaTag = tagRepository.findByName("Java");
        Optional<Tag> hibernateTag = tagRepository.findByName("Hibernate");
        Optional<Tag> springBootTag = tagRepository.findByName("SpringBoot");

        assertTrue(javaTag.isPresent(), "Тег Java должен быть создан");
        assertTrue(hibernateTag.isPresent(), "Тег Hibernate должен быть создан");
        assertTrue(springBootTag.isPresent(), "Тег SpringBoot должен быть создан");

        Optional<com.studying.platform.entity.Category> category =
                categoryRepository.findByName("Programming");
        assertTrue(category.isPresent(), "Категория Programming должна быть создана");
    }

    /**
     * Проверка: сидер создаёт курс со структурой.
     * <p>
     * Проверяет создание курса «Intro to Java» с привязкой к преподавателю
     * и категории, а также корректное добавление тегов.
     * Данные читаются через JPQL для обхода ленивой загрузки.
     * </p>
     */
    @Test
    void seeder_createsCourseWithStructure() {
        var result = entityManager.createQuery(
                        "SELECT c.title, c.duration, t.email, cat.name FROM Course c "
                                + "JOIN c.teacher t JOIN c.category cat WHERE c.title = :title",
                        Object[].class)
                .setParameter("title", "Intro to Java")
                .getSingleResult();

        assertNotNull(result);
        assertEquals("Intro to Java", result[0]);
        assertNotNull(result[1]); // duration
        assertEquals("demo-teacher@platform.test", result[2]); // teacher email
        assertEquals("Programming", result[3]); // category name

        // Проверка тегов курса через JPQL
        long tagCount = entityManager.createQuery(
                        "SELECT COUNT(t) FROM Course c JOIN c.tags t WHERE c.title = :title", Long.class)
                .setParameter("title", "Intro to Java")
                .getSingleResult();
        assertEquals(3, tagCount);
    }

    /**
     * Проверка: сидер создаёт модули и уроки.
     * <p>
     * Проверяет создание двух модулей и четырёх уроков в правильном порядке.
     * </p>
     */
    @Test
    void seeder_createsModulesAndLessons() {
        List<Module> modules = moduleRepository.findAll();
        List<com.studying.platform.entity.Lesson> lessons = lessonRepository.findAll();

        assertEquals(2, modules.size(), "Должно быть 2 модуля");
        assertEquals(4, lessons.size(), "Должно быть 4 урока");

        Module moduleOne = modules.stream()
                .filter(m -> m.getTitle().equals("Java Basics"))
                .findFirst().orElse(null);
        assertNotNull(moduleOne);
        assertEquals(1, moduleOne.getOrderIndex());
        assertEquals(2, lessonRepository.findByModuleId(moduleOne.getId()).size());
    }

    /**
     * Проверка: сидер создаёт задания для уроков.
     * <p>
     * Проверяет создание четырёх заданий, по одному на каждый урок.
     * </p>
     */
    @Test
    void seeder_createsAssignments() {
        List<com.studying.platform.entity.Assignment> assignments = assignmentRepository.findAll();

        assertEquals(4, assignments.size(), "Должно быть 4 задания");
    }

    /**
     * Проверка: сидер создаёт записи студентов на курс.
     * <p>
     * Проверяет создание двух активных записей для демонстрационных студентов.
     * Данные читаются через JPQL для обхода ленивой загрузки.
     * </p>
     */
    @Test
    void seeder_createsEnrollments() {
        long activeCount = entityManager.createQuery(
                        "SELECT COUNT(e) FROM Enrollment e WHERE e.status = :status", Long.class)
                .setParameter("status", EnrollmentStatus.ACTIVE)
                .getSingleResult();

        assertEquals(2, activeCount, "Должно быть 2 активные записи");

        // Проверка ролей пользователей через записи — JPQL
        long studentEnrollments = entityManager.createQuery(
                        "SELECT COUNT(e) FROM Enrollment e JOIN e.user u WHERE u.role = :role", Long.class)
                .setParameter("role", UserRole.STUDENT)
                .getSingleResult();

        assertEquals(2, studentEnrollments, "Записи должны быть у студентов");
    }

    /**
     * Проверка: сидер создаёт викторину с вопросами и вариантами.
     * <p>
     * Проверяет создание викторины для второго модуля, двух вопросов
     * (один SINGLE_CHOICE, один MULTIPLE_CHOICE) и шести вариантов ответа.
     * Варианты ответа читаются через JPQL для обхода ленивой загрузки.
     * </p>
     */
    @Test
    void seeder_createsQuizWithQuestions() {
        Optional<Quiz> quizOpt = quizRepository.findAll().stream()
                .filter(q -> q.getTitle().equals("Module 2 Quiz"))
                .findFirst();

        assertTrue(quizOpt.isPresent(), "Викторина Module 2 Quiz должна быть создана");
        Quiz quiz = quizOpt.get();

        // Подсчёт вопросов по типам через JPQL
        long singleCount = entityManager.createQuery(
                        "SELECT COUNT(q) FROM Question q WHERE q.quiz.id = :quizId AND q.type = :type", Long.class)
                .setParameter("quizId", quiz.getId())
                .setParameter("type", QuizQuestionType.SINGLE_CHOICE)
                .getSingleResult();

        long multiCount = entityManager.createQuery(
                        "SELECT COUNT(q) FROM Question q WHERE q.quiz.id = :quizId AND q.type = :type", Long.class)
                .setParameter("quizId", quiz.getId())
                .setParameter("type", QuizQuestionType.MULTIPLE_CHOICE)
                .getSingleResult();

        assertEquals(1, singleCount, "Должен быть 1 вопрос с одиночным выбором");
        assertEquals(1, multiCount, "Должен быть 1 вопрос с множественным выбором");

        // Подсчёт вариантов ответа через JPQL
        long totalOptions = entityManager.createQuery(
                        "SELECT COUNT(a) FROM Question q JOIN q.options a WHERE q.quiz.id = :quizId", Long.class)
                .setParameter("quizId", quiz.getId())
                .getSingleResult();
        assertEquals(6, totalOptions, "Должно быть 6 вариантов ответа");
    }

    /**
     * Проверка: повторный запуск сидера не создаёт дубликатов преподавателя.
     * <p>
     * Вызывает метод {@code seedDemoData} повторно и проверяет,
     * что количество преподавателей с демо-адресом электронной почты остаётся равным 1.
     * </p>
     */
    @Test
    void seeder_noDuplicatesOnSecondRun() {
        // Сидер уже выполнился при старте контекста
        long initialCount = userRepository.findByEmail("demo-teacher@platform.test").map(t -> 1L).orElse(0L);
        assertEquals(1L, initialCount, "Демонстрационный преподаватель должен быть ровно один");

        // Повторный вызов сидера — метод проверяет existsByEmail и не создаёт дубликатов
        entityManager.clear();

        DemoDataSeeder seeder = new DemoDataSeeder(
                userRepository, categoryRepository, tagRepository,
                courseRepository, moduleRepository, lessonRepository,
                assignmentRepository, enrollmentRepository, quizRepository,
                questionRepository, answerOptionRepository);

        // seedDemoData проверяет existsByEmail и не создаёт дубликатов
        seeder.seedDemoData();

        long afterCount = userRepository.findByEmail("demo-teacher@platform.test").map(t -> 1L).orElse(0L);
        assertEquals(initialCount, afterCount,
                "Повторный запуск не должен создавать дубликатов преподавателя");
    }

    /**
     * Проверка: данные доступны через прямое SQL-соединение с PostgreSQL.
     * <p>
     * Подтверждает, что сидер записал данные в настоящую PostgreSQL-базу
     * (через Testcontainers), а не в память.
     * </p>
     */
    @Test
    void dataPersistedInPostgres() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            ResultSet resultSet = statement.executeQuery(
                    "SELECT COUNT(*) FROM users WHERE login = 'demo-teacher@platform.test'");
            assertTrue(resultSet.next());
            assertEquals(1, resultSet.getInt(1),
                    "SQL: преподаватель должен быть найден в таблице users");
        }
    }
}
