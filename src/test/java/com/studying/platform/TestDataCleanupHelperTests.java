package com.studying.platform;

import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Lesson;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.LessonRepository;
import com.studying.platform.repository.ModuleRepository;
import com.studying.platform.repository.UserRepository;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для {@link TestDataCleanupHelper}.
 * <p>
 * Проверяют, что помощник очистки корректно удаляет отслеживаемые сущности
 * и не затрагивает посторонние данные. Тесты выполняются через полный
 * Spring-контекст с PostgreSQL через Testcontainers.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TestDataCleanupHelperTests {

    /**
     * Общий помощник для безопасной очистки тестовых данных.
     */
    @Autowired
    private TestDataCleanupHelper cleanupHelper;

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
     * Репозиторий пользователей для проверки удаления.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Репозиторий категорий для проверки удаления.
     */
    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Репозиторий курсов для проверки удаления.
     */
    @Autowired
    private CourseRepository courseRepository;

    /**
     * Репозиторий модулей для проверки удаления.
     */
    @Autowired
    private ModuleRepository moduleRepository;

    /**
     * Репозиторий уроков для проверки удаления.
     */
    @Autowired
    private LessonRepository lessonRepository;

    /**
     * Уникальный префикс для тестовых данных.
     */
    private String uniquePrefix;

    /**
     * Инициализация уникального префикса перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        uniquePrefix = "cleanup-test-" + System.currentTimeMillis();
    }

    /**
     * Проверка: помощник удаляет отслеживаемые категории.
     * <p>
     * Создаёт категорию, вызывает очистку с её ID, затем проверяет,
     * что категория удалена.
     * </p>
     */
    @Test
    void cleanup_removesTrackedCategories() {
        Category trackedCategory = new Category("CleanupTrackedCategory");
        entityManager.persist(trackedCategory);
        entityManager.flush();
        Long trackedId = trackedCategory.getId();

        cleanupHelper.clearTestEntities(
                entityManager,
                transactionTemplate,
                Set.of(), // submissionIds
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                Set.of(trackedId),
                Set.of()
        );

        entityManager.clear();
        assertFalse(categoryRepository.findById(trackedId).isPresent(),
                "Отслеживаемая категория должна быть удалена");
    }

    /**
     * Проверка: помощник удаляет отслеживаемых пользователей.
     * <p>
     * Создаёт пользователя, вызывает очистку с его идентификатором,
     * затем проверяет, что пользователь удалён.
     * </p>
     */
    @Test
    void cleanup_removesTrackedUsers() {
        User trackedUser = new User(uniquePrefix + "-tracked-user@t.com", UserRole.STUDENT);
        entityManager.persist(trackedUser);
        entityManager.flush();

        cleanupHelper.clearTestEntities(
                entityManager,
                transactionTemplate,
                Set.of(), // submissionIds
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                Set.of(trackedUser.getId())
        );

        entityManager.clear();
        assertFalse(userRepository.findByEmail(uniquePrefix + "-tracked-user@t.com").isPresent(),
                "Отслеживаемый пользователь должен быть удалён");
    }

    /**
     * Проверка: помощник не удаляет посторонние категории.
     * <p>
     * Создаёт постороннюю категорию, вызывает очистку с другим ID,
     * затем проверяет, что посторонняя категория осталась.
     * </p>
     */
    @Test
    void cleanup_doesNotRemoveUntrackedCategories() {
        Category untrackedCategory = new Category("CleanupUntrackedCategory-" + System.nanoTime());
        entityManager.persist(untrackedCategory);
        entityManager.flush();
        Long untrackedId = untrackedCategory.getId();

        // Очистка с другим ID
        cleanupHelper.clearTestEntities(
                entityManager,
                transactionTemplate,
                Set.of(), // submissionIds
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                Set.of(99999L), // другой ID
                Set.of()
        );

        entityManager.clear();
        assertTrue(categoryRepository.findById(untrackedId).isPresent(),
                "Посторонняя категория не должна быть удалена");
    }

    /**
     * Проверка: помощник не удаляет посторонних пользователей.
     * <p>
     * Создаёт постороннего пользователя с уникальным email,
     * вызывает очистку с другим идентификатором, затем проверяет,
     * что посторонний пользователь остался.
     * </p>
     */
    @Test
    void cleanup_doesNotRemoveUntrackedUsers() {
        String otherEmail = "cleanup-other-" + System.nanoTime() + "@t.com";
        User untrackedUser = new User(otherEmail, UserRole.TEACHER);
        entityManager.persist(untrackedUser);
        entityManager.flush();

        // Очистка с другим идентификатором
        cleanupHelper.clearTestEntities(
                entityManager,
                transactionTemplate,
                Set.of(), // submissionIds
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                Set.of(99999L)
        );

        entityManager.clear();
        assertTrue(userRepository.findByEmail(otherEmail).isPresent(),
                "Посторонний пользователь не должен быть удалён");
    }

    /**
     * Проверяет, что очистка по ID сохраняет другого пользователя с тем же префиксом email.
     */
    @Test
    void cleanup_preservesUserWithSameEmailPrefix() {
        User tracked = new User(uniquePrefix + "-tracked@t.com", UserRole.STUDENT);
        User other = new User(uniquePrefix + "-other@t.com", UserRole.STUDENT);
        entityManager.persist(tracked);
        entityManager.persist(other);
        entityManager.flush();
        Long trackedId = tracked.getId();
        Long otherId = other.getId();

        cleanupHelper.clearTestEntities(entityManager, transactionTemplate,
                Set.of(), // submissionIds
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(trackedId));
        entityManager.clear();

        assertFalse(userRepository.findById(trackedId).isPresent());
        assertTrue(userRepository.findById(otherId).isPresent());
    }

    /**
     * Проверка: помощник корректно обрабатывает пустые коллекции ID.
     * <p>
     * Вызывает очистку со всеми пустыми коллекциями — не должно быть ошибок.
     * </p>
     */
    @Test
    void cleanup_withEmptyIdSets_doesNotThrow() {
        assertDoesNotThrow(() ->
                cleanupHelper.clearTestEntities(
                        entityManager,
                        transactionTemplate,
                        Set.of(), // submissionIds
                        Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                        Set.of()
                ),
                "Очистка с пустыми коллекциями не должна выбрасывать исключение"
        );
    }

    /**
     * Проверка: помощник удаляет курсы и все зависимые сущности в правильном порядке.
     * <p>
     * Создаёт курс с модулем и уроком, вызывает очистку, затем проверяет,
     * что все связанные сущности удалены.
     * </p>
     */
    @Test
    void cleanup_removesCourseAndDependencies() {
        // Создание пользователя (преподаватель)
        User teacher = new User(uniquePrefix + "-cleanup-teacher@t.com", UserRole.TEACHER);
        entityManager.persist(teacher);
        entityManager.flush();

        // Создание категории
        Category category = new Category("CleanupCategory");
        entityManager.persist(category);
        entityManager.flush();

        // Создание курса
        Course course = new Course("CleanupCourse", "Описание", 10, LocalDate.now().plusDays(7), teacher, category);
        entityManager.persist(course);
        entityManager.flush();
        Long courseId = course.getId();

        // Создание модуля
        Module module = new Module("CleanupModule", "Описание", 1, course);
        entityManager.persist(module);
        entityManager.flush();
        Long moduleId = module.getId();

        // Создание урока
        Lesson lesson = new Lesson("CleanupLesson", "Содержание", null, module);
        entityManager.persist(lesson);
        entityManager.flush();
        Long lessonId = lesson.getId();

        // Очистка
        cleanupHelper.clearTestEntities(
                entityManager,
                transactionTemplate,
                Set.of(),       // submissionIds
                Set.of(),       // reviewCourseIds
                Set.of(),       // enrollmentCourseIds — записей нет
                Set.of(),       // enrollmentUserEmails
                Set.of(lessonId),
                Set.of(moduleId),
                Set.of(courseId),
                Set.of(),       // categoryIds — категория не отслеживается (общая)
                Set.of(teacher.getId())
        );

        entityManager.clear();

        // Проверка удаления
        assertFalse(courseRepository.findById(courseId).isPresent(), "Курс должен быть удалён");
        assertFalse(moduleRepository.findById(moduleId).isPresent(), "Модуль должен быть удалён");
        assertFalse(lessonRepository.findById(lessonId).isPresent(), "Урок должен быть удалён");
    }
}
