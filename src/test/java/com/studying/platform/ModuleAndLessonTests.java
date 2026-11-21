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
import org.hibernate.TransientPropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для сущностей «Модуль» и «Урок» учебной платформы.
 * <p>
 * Проверяют сохранение, каскадные операции (сохранение и удаление),
 * чтение структуры «курс → модули → уроки», негативные сценарии
 * и специализированные методы репозиториев.
 * </p>
 * <p>
 * Тесты выполняются с PostgreSQL через Testcontainers в профиле «test».
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ModuleAndLessonTests {

    /**
     * Менеджер сущностей для ручного управления транзакциями.
     */
    @Autowired
    private EntityManager entityManager;

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
     * Создаёт и сохраняет тестовую категорию.
     */
    private Category createCategory(String name) {
        Category category = new Category(name);
        entityManager.persist(category);
        entityManager.flush();
        return category;
    }

    /**
     * Создаёт и сохраняет тестового преподавателя.
     */
    private User createTeacher(String email) {
        User user = new User(email, UserRole.TEACHER);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    /**
     * Создаёт и сохраняет тестовый курс с преподавателем и категорией.
     */
    private Course createCourse(String title, User teacher, Category category) {
        Course course = new Course(title, "Описание тестового курса", 40,
                LocalDate.of(2026, 9, 1), teacher, category);
        entityManager.persist(course);
        entityManager.flush();
        return course;
    }

    /**
     * Тест: сохранение модуля с привязкой к курсу.
     * Модуль должен быть сохранён с ненулевым идентификатором.
     */
    @Test
    void testSaveModuleWithCourse() {
        Category category = createCategory("Программирование");
        User teacher = createTeacher("teacher_module@example.com");
        Course course = createCourse("Основы Java", teacher, category);

        Module module = new Module();
        module.setTitle("Введение в Java");
        module.setDescription("Основные понятия");
        module.setOrderIndex(1);
        module.setCourse(course);

        entityManager.persist(module);
        entityManager.flush();

        Long moduleId = module.getId();
        assertNotNull(moduleId, "Идентификатор модуля не должен быть null");
        assertEquals("Введение в Java", module.getTitle());
        assertEquals(1, module.getOrderIndex());
        assertEquals(course.getId(), module.getCourse().getId());
    }

    /**
     * Тест: сохранение урока с привязкой к модулю.
     * Урок должен корректно связываться с модулем.
     */
    @Test
    void testSaveLessonWithModule() {
        Category category = createCategory("Python");
        User teacher = createTeacher("teacher_lesson@example.com");
        Course course = createCourse("Основы Python", teacher, category);

        Module module = new Module();
        module.setTitle("Переменные");
        module.setOrderIndex(1);
        module.setCourse(course);
        entityManager.persist(module);
        entityManager.flush();

        Lesson lesson = new Lesson();
        lesson.setTitle("Типы данных");
        lesson.setContent("int, str, float, boolean");
        lesson.setVideoUrl("https://example.com/video1");
        lesson.setModule(module);

        entityManager.persist(lesson);
        entityManager.flush();

        Long lessonId = lesson.getId();
        assertNotNull(lessonId, "Идентификатор урока не должен быть null");
        assertEquals("Типы данных", lesson.getTitle());
        assertEquals(module.getId(), lesson.getModule().getId());
        assertEquals("int, str, float, boolean", lesson.getContent());
        assertEquals("https://example.com/video1", lesson.getVideoUrl());
    }

    /**
     * Тест: каскадное сохранение курса с модулями и уроками.
     * <p>
     * Сохраняет категорию и преподавателя как общие объекты, создаёт новый курс
     * с двумя модулями и тремя уроками, заполняет двусторонние связи.
     * Вызывает {@code persist(course)} один раз — каскады PERSIST на Module
     * и Lesson обеспечивают запись всех дочерних объектов.
     * После {@code flush} и {@code clear} контекста загружает курс заново
     * и проверяет количество, поля и внешние ключи.
     * </p>
     */
    @Test
    void testCascadeSaveCourseWithModulesAndLessons() {
        // Сохраняем общие объекты (категория и преподаватель)
        Category category = createCategory("Дизайн");
        User teacher = createTeacher("teacher_cascade@example.com");

        // Строим новый курс с модулями и уроками
        Course course = new Course("UI/UX Дизайн", "Курс по дизайну", 20,
                LocalDate.of(2026, 10, 15), teacher, category);

        Module module1 = new Module("Основы дизайна", "Основные понятия", 1, course);
        course.getModules().add(module1);

        Module module2 = new Module("Прототипирование", "Инструменты", 2, course);
        course.getModules().add(module2);

        Lesson lesson1 = new Lesson("Принципы дизайна", "Контраст, выравнивание", null, module1);
        module1.getLessons().add(lesson1);

        Lesson lesson2 = new Lesson("Цветовая палитра", "Теория цвета", null, module1);
        module1.getLessons().add(lesson2);

        Lesson lesson3 = new Lesson("Инструменты", "Figma, Sketch", "https://example.com/prototype", module2);
        module2.getLessons().add(lesson3);

        // Сохраняем только курс — каскады сохранят модули и уроки
        entityManager.persist(course);
        entityManager.flush();

        // Запоминаем ID
        Long courseId = course.getId();
        Long module1Id = module1.getId();
        Long module2Id = module2.getId();
        Long lesson1Id = lesson1.getId();
        Long lesson3Id = lesson3.getId();

        // Очищаем контекст — данные должны быть в БД
        entityManager.clear();

        // Загружаем курс заново из БД
        Course loadedCourse = courseRepository.findById(courseId).orElseThrow();
        assertEquals(2, loadedCourse.getModules().size(),
                "Курс должен содержать 2 модуля");
        assertEquals("UI/UX Дизайн", loadedCourse.getTitle());

        // Проверяем модули
        Module loadedModule1 = moduleRepository.findById(module1Id).orElseThrow();
        assertEquals(2, loadedModule1.getLessons().size(),
                "Модуль 1 должен содержать 2 урока");
        assertEquals("Основы дизайна", loadedModule1.getTitle());
        assertEquals(courseId, loadedModule1.getCourse().getId(),
                "Внешний ключ модуля должен ссылаться на курс");

        Module loadedModule2 = moduleRepository.findById(module2Id).orElseThrow();
        assertEquals(1, loadedModule2.getLessons().size(),
                "Модуль 2 должен содержать 1 урок");
        assertEquals("Прототипирование", loadedModule2.getTitle());

        // Проверяем уроки
        Lesson loadedLesson1 = lessonRepository.findById(lesson1Id).orElseThrow();
        assertEquals("Принципы дизайна", loadedLesson1.getTitle());
        assertEquals(module1Id, loadedLesson1.getModule().getId(),
                "Внешний ключ урока должен ссылаться на модуль");

        Lesson loadedLesson3 = lessonRepository.findById(lesson3Id).orElseThrow();
        assertEquals("Инструменты", loadedLesson3.getTitle());
        assertEquals("https://example.com/prototype", loadedLesson3.getVideoUrl());
    }

    /**
     * Тест: каскадное удаление курса удаляет модули и уроки, но не общие объекты.
     * <p>
     * Сохраняет категорию и преподавателя, создаёт курс с модулями и уроками,
     * запоминает ID всех объектов. После удаления курса проверяет отсутствие
     * курса, модулей и уроков, но сохранение категории и преподавателя.
     * </p>
     */
    @Test
    void testCascadeDeleteCourseRemovesModulesAndLessons() {
        // Сохраняем общие объекты
        Category category = createCategory("Маркетинг");
        User teacher = createTeacher("teacher_delete@example.com");

        // Создаём курс с модулями и уроками
        Course course = new Course("Основы маркетинга", "Введение в маркетинг", 25,
                LocalDate.of(2026, 9, 1), teacher, category);

        Module module1 = new Module("Стратегия", "Маркетинговые стратегии", 1, course);
        course.getModules().add(module1);

        Module module2 = new Module("Аналитика", "Метрики и KPI", 2, course);
        course.getModules().add(module2);

        Lesson lesson1 = new Lesson("Виды стратегий", "Описание стратегий", null, module1);
        module1.getLessons().add(lesson1);

        Lesson lesson2 = new Lesson("KPI метрики", "Основные метрики", null, module2);
        module2.getLessons().add(lesson2);

        // Сохраняем только курс — каскады сохранят всё
        entityManager.persist(course);
        entityManager.flush();

        // Запоминаем ID
        Long courseId = course.getId();
        Long module1Id = module1.getId();
        Long module2Id = module2.getId();
        Long lesson1Id = lesson1.getId();
        Long lesson2Id = lesson2.getId();
        Long teacherId = teacher.getId();
        Long categoryId = category.getId();

        // Удаляем курс
        courseRepository.deleteById(courseId);
        entityManager.flush();
        // Очищаем контекст — убедиться, что данные удалены из базы, а не только из кэша
        entityManager.clear();

        // Проверка: курс, модули и уроки удалены (чтение из очищенного контекста)
        assertFalse(courseRepository.findById(courseId).isPresent(),
                "Курс должен быть удалён");
        assertFalse(moduleRepository.findById(module1Id).isPresent(),
                "Модуль 1 должен быть удалён");
        assertFalse(moduleRepository.findById(module2Id).isPresent(),
                "Модуль 2 должен быть удалён");
        assertFalse(lessonRepository.findById(lesson1Id).isPresent(),
                "Урок 1 должен быть удалён");
        assertFalse(lessonRepository.findById(lesson2Id).isPresent(),
                "Урок 2 должен быть удалён");

        // Проверка: общие объекты сохранены
        assertTrue(categoryRepository.findById(categoryId).isPresent(),
                "Категория не должна быть удалена");
        assertTrue(userRepository.findById(teacherId).isPresent(),
                "Преподаватель не должен быть удалён");
    }

    /**
     * Тест: чтение структуры «курс → модули → уроки» в транзакции.
     * Загруженный курс должен содержать модули и уроки.
     */
    @Test
    void testReadCourseStructureInTransaction() {
        Category category = createCategory("Data Science");
        User teacher = createTeacher("teacher_structure@example.com");
        Course course = createCourse("Введение в Data Science", teacher, category);

        Module module = new Module();
        module.setTitle("Машинное обучение");
        module.setOrderIndex(1);
        module.setCourse(course);
        course.getModules().add(module);

        Lesson lesson1 = new Lesson();
        lesson1.setTitle("Линейная регрессия");
        lesson1.setContent("Основы линейной регрессии");
        lesson1.setModule(module);
        module.getLessons().add(lesson1);

        Lesson lesson2 = new Lesson();
        lesson2.setTitle("Логистическая регрессия");
        lesson2.setContent("Основы логистической регрессии");
        lesson2.setModule(module);
        module.getLessons().add(lesson2);

        entityManager.persist(module);
        entityManager.persist(lesson1);
        entityManager.persist(lesson2);
        entityManager.merge(course);
        entityManager.flush();

        Course loadedCourse = courseRepository.findById(course.getId()).orElseThrow();
        assertNotNull(loadedCourse);
        assertFalse(loadedCourse.getModules().isEmpty(), "Модули не должны быть пустыми");

        Module loadedModule = loadedCourse.getModules().iterator().next();
        assertNotNull(loadedModule);
        assertEquals("Машинное обучение", loadedModule.getTitle());
        assertFalse(loadedModule.getLessons().isEmpty(), "Уроки не должны быть пустыми");
        assertEquals(2, loadedModule.getLessons().size());
    }

    /**
     * Тест: удаление урока из коллекции lessons (orphanRemoval).
     * При удалении урока из коллекции модуля он должен быть удалён из базы данных.
     */
    @Test
    void testOrphanRemoval() {
        Category category = createCategory("Тестирование");
        User teacher = createTeacher("teacher_orphan3@example.com");
        Course course = createCourse("Тестирование ПО", teacher, category);

        Module module = new Module();
        module.setTitle("Юнит-тесты");
        module.setOrderIndex(1);
        module.setCourse(course);
        course.getModules().add(module);

        Lesson lesson1 = new Lesson();
        lesson1.setTitle("JUnit основы");
        lesson1.setModule(module);
        module.getLessons().add(lesson1);

        Lesson lesson2 = new Lesson();
        lesson2.setTitle("Mockito");
        lesson2.setModule(module);
        module.getLessons().add(lesson2);

        // Сохраняем через курс — каскады сработают автоматически
        entityManager.merge(course);
        entityManager.flush();

        // После merge каскады возвращают новые managed-экземпляры
        Module savedModule = moduleRepository.findByCourseIdAndOrderIndex(course.getId(), 1).stream()
                .findFirst().orElseThrow();
        Long moduleDeletedId = savedModule.getId();

        // Загружаем модуль и удаляем урок из коллекции
        Module loadedModule = moduleRepository.findById(moduleDeletedId).orElseThrow();
        assertEquals(2, loadedModule.getLessons().size());

        // Загружаем конкретный урок для удаления
        Lesson lessonToRemove = loadedModule.getLessons().iterator().next();
        Long lessonToRemoveId = lessonToRemove.getId();
        loadedModule.getLessons().remove(lessonToRemove);

        // Flush — orphanRemoval на Module.lessons обработает каскадное удаление
        entityManager.flush();

        Module updatedModule = moduleRepository.findById(moduleDeletedId).orElseThrow();
        assertEquals(1, updatedModule.getLessons().size());
        assertFalse(lessonRepository.findById(lessonToRemoveId).isPresent(),
                "Удалённый урок не должен быть найден в базе данных");
    }

    /**
     * Тест: null title для урока вызывает исключение целостности данных.
     */
    @Test
    void testNullTitleForLesson() {
        Category category = createCategory("Физика");
        User teacher = createTeacher("teacher_nulltitle@example.com");
        Course course = createCourse("Физика", teacher, category);

        Module module = new Module();
        module.setTitle("Механика");
        module.setOrderIndex(1);
        module.setCourse(course);
        entityManager.persist(module);
        entityManager.flush();

        Lesson lesson = new Lesson();
        lesson.setTitle(null);
        lesson.setContent("Законы Ньютона");
        lesson.setModule(module);

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(lesson);
            entityManager.flush();
        }, "Сохранение урока с null названием должно вызвать нарушение целостности данных");
    }

    /**
     * Тест: null module для урока вызывает исключение целостности данных.
     */
    @Test
    void testNullModuleForLesson() {
        Lesson lesson = new Lesson();
        lesson.setTitle("Свободный урок");
        lesson.setContent("Без привязки к модулю");

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(lesson);
            entityManager.flush();
        }, "Сохранение урока без модуля должно вызвать нарушение целостности данных");
    }

    /**
     * Тест: создание нескольких модулей с одинаковым orderIndex.
     * Уникальность orderIndex не enforced на уровне БД — оба модуля сохраняются.
     */
    @Test
    void testModuleOrderIndex() {
        Category category = createCategory("Математика");
        User teacher = createTeacher("teacher_order@example.com");
        Course course = createCourse("Высшая математика", teacher, category);

        Module module1 = new Module();
        module1.setTitle("Алгебра");
        module1.setOrderIndex(1);
        module1.setCourse(course);

        Module module2 = new Module();
        module2.setTitle("Геометрия");
        module2.setOrderIndex(1);
        module2.setCourse(course);

        course.getModules().add(module1);
        course.getModules().add(module2);

        entityManager.persist(module1);
        entityManager.persist(module2);
        entityManager.merge(course);
        entityManager.flush();

        Long courseId = course.getId();

        // findByCourseIdAndOrderIndex возвращает List — оба модуля с orderIndex=1
        List<Module> modules = moduleRepository.findByCourseIdAndOrderIndex(courseId, 1);
        assertEquals(2, modules.size(),
                "Должно быть найдено два модуля с одинаковым orderIndex");
    }

    /**
     * Тест: репозиторий Lesson находит уроки по moduleId.
     */
    @Test
    void testLessonRepositoryFindByModuleId() {
        Category category = createCategory("Языки");
        User teacher = createTeacher("teacher_findby@example.com");
        Course course = createCourse("Английский язык", teacher, category);

        Module module = new Module();
        module.setTitle("Грамматика");
        module.setOrderIndex(1);
        module.setCourse(course);
        entityManager.persist(module);
        entityManager.flush();

        Lesson lesson1 = new Lesson();
        lesson1.setTitle("Present Simple");
        lesson1.setModule(module);

        Lesson lesson2 = new Lesson();
        lesson2.setTitle("Past Simple");
        lesson2.setModule(module);

        entityManager.persist(lesson1);
        entityManager.persist(lesson2);
        entityManager.flush();

        List<Lesson> lessons = lessonRepository.findByModuleId(module.getId());
        assertEquals(2, lessons.size());

        List<String> titles = lessons.stream()
                .map(Lesson::getTitle)
                .sorted()
                .toList();
        assertEquals(List.of("Past Simple", "Present Simple"), titles);
    }

    /**
     * Тест: модуль с пустой коллекцией уроков сохраняется без ошибок.
     */
    @Test
    void testModuleWithoutLessons() {
        Category category = createCategory("Философия");
        User teacher = createTeacher("teacher_empty@example.com");
        Course course = createCourse("Введение в философию", teacher, category);

        Module module = new Module();
        module.setTitle("История философии");
        module.setOrderIndex(1);
        module.setCourse(course);

        entityManager.persist(module);
        entityManager.flush();

        Long moduleId = module.getId();
        assertNotNull(moduleId, "Идентификатор модуля не должен быть null");

        Module loaded = moduleRepository.findById(moduleId).orElseThrow();
        assertTrue(loaded.getLessons().isEmpty(), "Коллекция уроков должна быть пустой");
    }

    /**
     * Тест: курс без модулей и уроков (базовый сценарий).
     */
    @Test
    void testCourseWithoutModulesAndLessons() {
        Category category = createCategory("Пустая категория");
        User teacher = createTeacher("teacher_nomodules@example.com");
        Course course = createCourse("Курс без содержания", teacher, category);

        Long courseId = course.getId();
        assertNotNull(courseId, "Идентификатор курса не должен быть null");

        Course loaded = courseRepository.findById(courseId).orElseThrow();
        assertTrue(loaded.getModules().isEmpty(), "Модули должны быть пустыми");
    }

    /**
     * Тест: каскадное удаление модуля удаляет его уроки.
     */
    @Test
    void testCascadeDeleteModuleRemovesLessons() {
        Category category = createCategory("Химия");
        User teacher = createTeacher("teacher_moduledelete@example.com");
        Course course = createCourse("Органическая химия", teacher, category);

        Module module = new Module();
        module.setTitle("Углеводороды");
        module.setOrderIndex(1);
        module.setCourse(course);

        Lesson lesson1 = new Lesson();
        lesson1.setTitle("Алканы");
        lesson1.setModule(module);
        module.getLessons().add(lesson1);

        Lesson lesson2 = new Lesson();
        lesson2.setTitle("Алкены");
        lesson2.setModule(module);
        module.getLessons().add(lesson2);

        // Корректные двусторонние связи
        course.getModules().add(module);
        module.setCourse(course);
        lesson1.setModule(module);
        lesson2.setModule(module);

        entityManager.persist(module);
        entityManager.persist(lesson1);
        entityManager.persist(lesson2);
        entityManager.merge(course);
        entityManager.flush();

        Long moduleDeletedId = module.getId();
        Long lesson1Id = lesson1.getId();

        // Удаляем модуль из коллекции курса (orphanRemoval + cascade REMOVE)
        Course loadedCourse = courseRepository.findById(course.getId()).orElseThrow();
        loadedCourse.getModules().remove(module);
        entityManager.merge(loadedCourse);
        entityManager.flush();

        assertFalse(moduleRepository.findById(moduleDeletedId).isPresent(),
                "Модуль должен быть удалён");
        assertFalse(lessonRepository.findById(lesson1Id).isPresent(),
                "Урок должен быть удалён");
    }

    /**
     * Тест: урок с пустыми content и videoUrl сохраняется без ошибок.
     */
    @Test
    void testLessonWithEmptyContentAndVideoUrl() {
        Category category = createCategory("Искусство");
        User teacher = createTeacher("teacher_emptylesson@example.com");
        Course course = createCourse("История искусства", teacher, category);

        Module module = new Module();
        module.setTitle("Ренессанс");
        module.setOrderIndex(1);
        module.setCourse(course);
        entityManager.persist(module);
        entityManager.flush();

        Lesson lesson = new Lesson();
        lesson.setTitle("Эпоха Возрождения");
        lesson.setContent(null);
        lesson.setVideoUrl(null);
        lesson.setModule(module);

        entityManager.persist(lesson);
        entityManager.flush();

        Long lessonId = lesson.getId();
        assertNotNull(lessonId, "Идентификатор урока не должен быть null");

        Lesson loaded = lessonRepository.findById(lessonId).orElseThrow();
        assertEquals("Эпоха Возрождения", loaded.getTitle());
        assertNull(loaded.getContent());
        assertNull(loaded.getVideoUrl());
    }

    /**
     * Тест: обновление модуля и урока.
     * Проверка, что изменения title и description сохраняются.
     */
    @Test
    void testUpdateModuleAndLesson() {
        Category category = createCategory("Биология");
        User teacher = createTeacher("teacher_update@example.com");
        Course course = createCourse("Биология", teacher, category);

        Module module = new Module();
        module.setTitle("Клетка");
        module.setOrderIndex(1);
        module.setCourse(course);
        entityManager.persist(module);
        entityManager.flush();

        Lesson lesson = new Lesson();
        lesson.setTitle("Структура клетки");
        lesson.setContent("Мембрана, ядро, органеллы");
        lesson.setModule(module);
        entityManager.persist(lesson);
        entityManager.flush();

        // Обновляем модуль
        Module loadedModule = moduleRepository.findById(module.getId()).orElseThrow();
        loadedModule.setTitle("Клеточная биология");
        loadedModule.setDescription("Углублённое изучение");
        entityManager.merge(loadedModule);
        entityManager.flush();

        // Обновляем урок
        Lesson loadedLesson = lessonRepository.findById(lesson.getId()).orElseThrow();
        loadedLesson.setTitle("Органеллы клетки");
        loadedLesson.setContent("Митохондрии, рибосомы, ЭПС");
        loadedLesson.setVideoUrl("https://example.com/cell");
        entityManager.merge(loadedLesson);
        entityManager.flush();

        Module updatedModule = moduleRepository.findById(loadedModule.getId()).orElseThrow();
        assertEquals("Клеточная биология", updatedModule.getTitle());
        assertEquals("Углублённое изучение", updatedModule.getDescription());

        Lesson updatedLesson = lessonRepository.findById(loadedLesson.getId()).orElseThrow();
        assertEquals("Органеллы клетки", updatedLesson.getTitle());
        assertEquals("Митохондрии, рибосомы, ЭПС", updatedLesson.getContent());
        assertEquals("https://example.com/cell", updatedLesson.getVideoUrl());
    }

    /**
     * Тест: LessonRepository возвращает пустой список при отсутствии уроков.
     */
    @Test
    void testLessonRepositoryReturnsEmptyListWhenNoLessons() {
        Category category = createCategory("Пустой тест");
        User teacher = createTeacher("teacher_emptylist@example.com");
        Course course = createCourse("Test", teacher, category);

        Module module = new Module();
        module.setTitle("Без уроков");
        module.setOrderIndex(1);
        module.setCourse(course);
        entityManager.persist(module);
        entityManager.flush();

        List<Lesson> lessons = lessonRepository.findByModuleId(module.getId());
        assertTrue(lessons.isEmpty(), "Список уроков должен быть пустым");
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
