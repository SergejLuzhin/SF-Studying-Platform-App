package com.studying.platform;

import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.CourseReview;
import com.studying.platform.entity.Lesson;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.Profile;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.CourseReviewRepository;
import com.studying.platform.repository.LessonRepository;
import com.studying.platform.repository.ModuleRepository;
import com.studying.platform.repository.ProfileRepository;
import com.studying.platform.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для проверки корректности хранения текстовых полей в PostgreSQL.
 * <p>
 * Проверяют, что текстовые поля {@code Course.description}, {@code Module.description},
 * {@code Lesson.content}, {@code Profile.bio} и {@code CourseReview.comment}
 * хранятся в колонках типа TEXT как исходные строки, а не как OID Large Object.
 * Используются тексты с кириллицей, переводами строк и длиной больше 255 символов.
 * </p>
 *
 * @see Course
 * @see Module
 * @see Lesson
 * @see Profile
 * @see CourseReview
 */
@SpringBootTest
@ActiveProfiles("test")
class PostgresTextStorageTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private CourseReviewRepository courseReviewRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private final Set<Long> userIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<Long> categoryIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<Long> courseIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<Long> moduleIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<Long> lessonIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<Long> profileIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<Long> reviewIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Выполняет сохранение сущности в транзакции.
     */
    @SuppressWarnings("unchecked")
    private <T> T saveInTransaction(T entity) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        return tt.execute(status -> {
            if (entity instanceof User u) return (T) userRepository.save(u);
            if (entity instanceof Category c) return (T) categoryRepository.save(c);
            if (entity instanceof Course c) return (T) courseRepository.save(c);
            if (entity instanceof Module m) return (T) moduleRepository.save(m);
            if (entity instanceof Lesson l) return (T) lessonRepository.save(l);
            if (entity instanceof Profile p) return (T) profileRepository.save(p);
            if (entity instanceof CourseReview r) return (T) courseReviewRepository.save(r);
            throw new IllegalArgumentException("Unknown entity type: " + entity.getClass());
        });
    }

    /**
     * Создает привязанный к текущему потоку EntityManager.
     */
    private EntityManager createBoundEntityManager() {
        return SharedEntityManagerCreator.createSharedEntityManager(
                entityManager.getEntityManagerFactory());
    }

    @AfterEach
    void cleanup() {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.executeWithoutResult(s -> {
            reviewIds.forEach(id -> { try { courseReviewRepository.deleteById(id); } catch (Exception ignored) {} });
            lessonIds.forEach(id -> { try { lessonRepository.deleteById(id); } catch (Exception ignored) {} });
            moduleIds.forEach(id -> { try { moduleRepository.deleteById(id); } catch (Exception ignored) {} });
            courseIds.forEach(id -> { try { courseRepository.deleteById(id); } catch (Exception ignored) {} });
            categoryIds.forEach(id -> { try { categoryRepository.deleteById(id); } catch (Exception ignored) {} });
            profileIds.forEach(id -> { try { profileRepository.deleteById(id); } catch (Exception ignored) {} });
            userIds.forEach(id -> { try { userRepository.deleteById(id); } catch (Exception ignored) {} });
        });
        userIds.clear();
        categoryIds.clear();
        courseIds.clear();
        moduleIds.clear();
        lessonIds.clear();
        profileIds.clear();
        reviewIds.clear();
    }

    /**
     * Проверяет, что все пять текстовых полей сохраняются и читаются
     * через JPA и прямой SQL как исходные строки.
     */
    @Test
    void textFieldsPersistAndReadCorrectly() {
        // Тестовые тексты: кириллица, перевод строки, > 255 символов
        String longCourseDesc = "Длинное описание курса. " +
                "Это текст для проверки хранения в PostgreSQL через TEXT-колонку. " +
                repeat("Строка для наполнения длины. ", 20) +
                "Кириллица: привет, мир, тест!";
        String longModuleDesc = "Описание модуля. " +
                repeat("Тема модуля: ", 15) +
                "Финал описания модуля.";
        String longLessonContent = "Текстовое содержание урока.\n" +
                "Строка с переводом строки.\n" +
                repeat("Урок содержит: ", 20) +
                "Конец содержимого урока.";
        String longBio = "Биография пользователя: " +
                repeat("Опыт работы и навыки разработчика. ", 15) +
                "Кириллица: разработчик.";
        String longComment = "Длинный комментарий к отзыву: " +
                repeat("Отличный курс, рекомендую всем. ", 15) +
                "Кириллица: спасибо автору!";

        // Создаём сущности в транзакции через TransactionTemplate
        User teacher = saveInTransaction(new User("teacher_text_" + System.currentTimeMillis() + "@test.com", UserRole.TEACHER));
        userIds.add(teacher.getId());

        User student = saveInTransaction(new User("student_text_" + System.currentTimeMillis() + "@test.com", UserRole.STUDENT));
        userIds.add(student.getId());

        Category category = saveInTransaction(new Category("Категория текстовых тестов " + System.currentTimeMillis()));
        categoryIds.add(category.getId());

        Course course = new Course("Курс текстовых полей", longCourseDesc, 40, LocalDate.of(2026, 1, 1), teacher, category);
        course = saveInTransaction(course);
        courseIds.add(course.getId());

        Module module = new Module("Модуль текстов", longModuleDesc, 1, course);
        module.setCourse(course);
        course.getModules().add(module);
        module = saveInTransaction(module);
        moduleIds.add(module.getId());

        Lesson lesson = new Lesson("Урок текстов", longLessonContent, "https://example.com/video", module);
        lesson.setModule(module);
        module.getLessons().add(lesson);
        lesson = saveInTransaction(lesson);
        lessonIds.add(lesson.getId());

        Profile profile = new Profile(student, "Тестовый профиль");
        profile.setBio(longBio);
        profile = saveInTransaction(profile);
        profileIds.add(profile.getId());

        CourseReview review = new CourseReview(5, longComment, LocalDateTime.now(), course, student);
        review = saveInTransaction(review);
        reviewIds.add(review.getId());

        // Flush и clear через транзакционный EntityManager
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.execute(status -> {
            entityManager.flush();
            entityManager.clear();
            return null;
        });

        // Создаём новый EntityManager вне транзакции для чтения
        EntityManager readEm = createBoundEntityManager();

        Course reloadedCourse = readEm.find(Course.class, course.getId());
        assertNotNull(reloadedCourse);
        assertEquals(longCourseDesc, reloadedCourse.getDescription(),
                "Описание курса должно совпадать с исходным текстом через JPA");

        Module reloadedModule = readEm.find(Module.class, module.getId());
        assertNotNull(reloadedModule);
        assertEquals(longModuleDesc, reloadedModule.getDescription(),
                "Описание модуля должно совпадать с исходным текстом через JPA");

        Lesson reloadedLesson = readEm.find(Lesson.class, lesson.getId());
        assertNotNull(reloadedLesson);
        assertEquals(longLessonContent, reloadedLesson.getContent(),
                "Содержание урока должно совпадать с исходным текстом через JPA");

        Profile reloadedProfile = readEm.find(Profile.class, profile.getId());
        assertNotNull(reloadedProfile);
        assertEquals(longBio, reloadedProfile.getBio(),
                "Биография профиля должна совпадать с исходным текстом через JPA");

        CourseReview reloadedReview = readEm.find(CourseReview.class, review.getId());
        assertNotNull(reloadedReview);
        assertEquals(longComment, reloadedReview.getComment(),
                "Комментарий отзыва должен совпадать с исходным текстом через JPA");

        // Через прямой SQL в той же транзакции проверяем, что в колонках именно строки, а не OID
        String sqlCourseDesc = jdbcTemplate.queryForObject(
                "SELECT description FROM courses WHERE id = ?", String.class, course.getId());
        assertEquals(longCourseDesc, sqlCourseDesc,
                "Описание курса должно совпадать с исходным текстом при прямом SQL-чтении");

        String sqlModuleDesc = jdbcTemplate.queryForObject(
                "SELECT description FROM modules WHERE id = ?", String.class, module.getId());
        assertEquals(longModuleDesc, sqlModuleDesc,
                "Описание модуля должно совпадать с исходным текстом при прямом SQL-чтении");

        String sqlLessonContent = jdbcTemplate.queryForObject(
                "SELECT content FROM lessons WHERE id = ?", String.class, lesson.getId());
        assertEquals(longLessonContent, sqlLessonContent,
                "Содержание урока должно совпадать с исходным текстом при прямом SQL-чтении");

        String sqlProfileBio = jdbcTemplate.queryForObject(
                "SELECT bio FROM profiles WHERE id = ?", String.class, profile.getId());
        assertEquals(longBio, sqlProfileBio,
                "Биография профиля должна совпадать с исходным текстом при прямом SQL-чтении");

        String sqlReviewComment = jdbcTemplate.queryForObject(
                "SELECT comment FROM course_reviews WHERE id = ?", String.class, review.getId());
        assertEquals(longComment, sqlReviewComment,
                "Комментарий отзыва должен совпадать с исходным текстом при прямом SQL-чтении");
    }

    /**
     * Проверяет обновление и повторное чтение одного из текстовых полей.
     */
    @Test
    void textFieldsUpdateAndReread() {
        String initialDesc = "Начальное описание.";
        String updatedDesc = "Обновлённое описание курса с кириллицей: обновляем текст.";

        User teacher = saveInTransaction(new User("teacher_upd_" + System.currentTimeMillis() + "@test.com", UserRole.TEACHER));
        userIds.add(teacher.getId());

        Category category = saveInTransaction(new Category("Категория обновления " + System.currentTimeMillis()));
        categoryIds.add(category.getId());

        Course course = new Course("Курс обновления", initialDesc, 20, LocalDate.of(2026, 1, 1), teacher, category);
        final Course savedCourse = saveInTransaction(course);
        courseIds.add(savedCourse.getId());

        // Обновляем описание через транзакцию
        ttExecute(s -> {
            savedCourse.setDescription(updatedDesc);
            courseRepository.saveAndFlush(savedCourse);
            return null;
        });

        EntityManager readEm = createBoundEntityManager();
        Course reloaded = readEm.find(Course.class, savedCourse.getId());
        assertNotNull(reloaded);
        assertEquals(updatedDesc, reloaded.getDescription(),
                "Обновлённое описание курса должно совпадать при повторном чтении через JPA");

        // Проверяем через SQL
        String sqlDesc = jdbcTemplate.queryForObject(
                "SELECT description FROM courses WHERE id = ?", String.class, savedCourse.getId());
        assertEquals(updatedDesc, sqlDesc,
                "Обновлённое описание курса должно совпадать при прямом SQL-чтении");
    }

    /**
     * Проверяет сохранение и чтение null и пустой строки для текстовых полей.
     */
    @Test
    void nullAndEmptyTextFields() {
        String emptyString = "";

        User teacher = saveInTransaction(new User("teacher_null_" + System.currentTimeMillis() + "@test.com", UserRole.TEACHER));
        userIds.add(teacher.getId());

        Category category = saveInTransaction(new Category("Категория null-тестов " + System.currentTimeMillis()));
        categoryIds.add(category.getId());

        // Курс с null-описанием
        Course courseWithNull = new Course("Курс без описания", null, 10, LocalDate.of(2026, 6, 1), teacher, category);
        courseWithNull = saveInTransaction(courseWithNull);
        courseIds.add(courseWithNull.getId());

        // Курс с пустым описанием
        Course courseWithEmpty = new Course("Курс с пустым описанием", emptyString, 10, LocalDate.of(2026, 6, 1), teacher, category);
        courseWithEmpty = saveInTransaction(courseWithEmpty);
        courseIds.add(courseWithEmpty.getId());

        // Модуль с null-описанием
        Module moduleWithNull = new Module("Модуль без описания", null, 1, courseWithNull);
        moduleWithNull.setCourse(courseWithNull);
        courseWithNull.getModules().add(moduleWithNull);
        moduleWithNull = saveInTransaction(moduleWithNull);
        moduleIds.add(moduleWithNull.getId());

        // Урок с null-содержанием
        Lesson lessonWithNull = new Lesson("Урок без содержания", null, null, moduleWithNull);
        lessonWithNull.setModule(moduleWithNull);
        moduleWithNull.getLessons().add(lessonWithNull);
        lessonWithNull = saveInTransaction(lessonWithNull);
        lessonIds.add(lessonWithNull.getId());

        // Профиль с null-биографией
        Profile profileWithNull = new Profile(teacher, "Профиль с null био");
        profileWithNull.setBio(null);
        profileWithNull = saveInTransaction(profileWithNull);
        profileIds.add(profileWithNull.getId());

        // Отзыв с null-комментарием
        CourseReview reviewWithNull = new CourseReview(3, null, LocalDateTime.now(), courseWithNull, teacher);
        reviewWithNull = saveInTransaction(reviewWithNull);
        reviewIds.add(reviewWithNull.getId());

        // Flush и clear
        ttExecute(s -> {
            entityManager.flush();
            entityManager.clear();
            return null;
        });

        EntityManager readEm = createBoundEntityManager();

        // Проверяем null
        Course nullCourse = readEm.find(Course.class, courseWithNull.getId());
        assertNotNull(nullCourse);
        assertNull(nullCourse.getDescription(), "Описание курса с null должно остаться null через JPA");

        String sqlNullDesc = jdbcTemplate.queryForObject(
                "SELECT description FROM courses WHERE id = ?", String.class, courseWithNull.getId());
        assertNull(sqlNullDesc, "Описание курса с null должно быть null при прямом SQL-чтении");

        // Проверяем empty string
        Course emptyCourse = readEm.find(Course.class, courseWithEmpty.getId());
        assertNotNull(emptyCourse);
        assertEquals(emptyString, emptyCourse.getDescription(),
                "Пустое описание курса должно сохраниться через JPA");

        String sqlEmptyDesc = jdbcTemplate.queryForObject(
                "SELECT description FROM courses WHERE id = ?", String.class, courseWithEmpty.getId());
        assertEquals(emptyString, sqlEmptyDesc,
                "Пустое описание курса должно сохраниться при прямом SQL-чтении");

        // Проверяем null для модуля
        Module nullModule = readEm.find(Module.class, moduleWithNull.getId());
        assertNotNull(nullModule);
        assertNull(nullModule.getDescription(), "Описание модуля с null должно остаться null через JPA");

        // Проверяем null для урока
        Lesson nullLesson = readEm.find(Lesson.class, lessonWithNull.getId());
        assertNotNull(nullLesson);
        assertNull(nullLesson.getContent(), "Содержание урока с null должно остаться null через JPA");

        // Проверяем null для профиля
        Profile nullProfile = readEm.find(Profile.class, profileWithNull.getId());
        assertNotNull(nullProfile);
        assertNull(nullProfile.getBio(), "Биография профиля с null должна остаться null через JPA");

        // Проверяем null для отзыва
        CourseReview nullReview = readEm.find(CourseReview.class, reviewWithNull.getId());
        assertNotNull(nullReview);
        assertNull(nullReview.getComment(), "Комментарий отзыва с null должен остаться null через JPA");
    }

    /**
     * Выполняет код в транзакции.
     */
    @SuppressWarnings("unchecked")
    private <T> T ttExecute(TransactionCallback<T> callback) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        return tt.execute(status -> callback.execute(status));
    }

    /**
     * Интерфейс для кода, выполняемого в транзакции.
     */
    @FunctionalInterface
    private interface TransactionCallback<T> {
        T execute(org.springframework.transaction.TransactionStatus status);
    }

    /**
     * Повторяет строку указанное количество раз.
     */
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
