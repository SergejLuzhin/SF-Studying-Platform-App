package com.studying.platform.service;

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
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.Iterator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для управления курсами, модулями и уроками учебной платформы.
 * <p>
 * Обеспечивает бизнес-логику создания, изменения и получения структуры курса,
 * а также добавления модулей и уроков. Все публичные методы транзакционны.
 * </p>
 *
 * @see Course
 * @see Module
 * @see Lesson
 */
@Service
@Transactional
public class CourseService {

    /**
     * Репозиторий для работы с сущностью «Курс».
     */
    private final CourseRepository courseRepository;

    /**
     * Репозиторий для работы с сущностью «Модуль».
     */
    private final ModuleRepository moduleRepository;

    /**
     * Репозиторий для работы с сущностью «Урок».
     */
    private final LessonRepository lessonRepository;

    /**
     * Репозиторий для работы с сущностью «Пользователь».
     */
    private final UserRepository userRepository;

    /**
     * Репозиторий для работы с сущностью «Категория».
     */
    private final CategoryRepository categoryRepository;

    /**
     * Менеджер сущностей для управления транзакциями и сброса контекста.
     */
    private final EntityManager entityManager;

    /**
     * Конструктор сервиса с внедрением зависимостей через конструктор.
     *
     * @param courseRepository   репозиторий курсов
     * @param moduleRepository   репозиторий модулей
     * @param lessonRepository   репозиторий уроков
     * @param userRepository     репозиторий пользователей
     * @param categoryRepository репозиторий категорий
     * @param entityManager      менеджер сущностей JPA
     */
    public CourseService(CourseRepository courseRepository,
                         ModuleRepository moduleRepository,
                         LessonRepository lessonRepository,
                         UserRepository userRepository,
                         CategoryRepository categoryRepository,
                         EntityManager entityManager) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.entityManager = entityManager;
    }

    /**
     * Создаёт новый курс с заданными параметрами.
     * <p>
     * Проверяет существование преподавателя и его роль TEACHER,
     * а также существование категории. Если проверка не пройдена —
     * выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param title       название курса
     * @param description описание курса
     * @param duration    продолжительность в часах
     * @param startDate   дата начала курса
     * @param teacherId   идентификатор преподавателя
     * @param categoryId  идентификатор категории
     * @return сохранённый курс
     * @throws ResourceNotFoundException если преподаватель или категория не найдены,
     *                                   или роль преподавателя не TEACHER
     */
    public Course createCourse(String title, String description, Integer duration,
                               LocalDate startDate, Long teacherId, Long categoryId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Преподаватель с идентификатором " + teacherId + " не найден"));

        if (teacher.getRole() != UserRole.TEACHER) {
            throw new ResourceNotFoundException(
                    "Пользователь с идентификатором " + teacherId + " не является преподавателем");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Категория с идентификатором " + categoryId + " не найдена"));

        Course course = new Course(title, description, duration, startDate, teacher, category);
        Course saved = courseRepository.save(course);
        entityManager.flush();
        return saved;
    }

    /**
     * Обновляет данные существующего курса.
     * <p>
     * Проверяет существование курса по идентификатору.
     * Обновляет доступные поля: название, описание, продолжительность, дата начала.
     * </p>
     *
     * @param courseId    идентификатор курса для обновления
     * @param title       новое название курса
     * @param description новое описание курса
     * @param duration    новая продолжительность в часах
     * @param startDate   новая дата начала курса
     * @return обновлённый курс
     * @throws ResourceNotFoundException если курс с заданным идентификатором не найден
     */
    public Course updateCourse(Long courseId, String title, String description,
                               Integer duration, LocalDate startDate) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Курс с идентификатором " + courseId + " не найден"));

        if (title != null) {
            course.setTitle(title);
        }
        if (description != null) {
            course.setDescription(description);
        }
        if (duration != null) {
            course.setDuration(duration);
        }
        if (startDate != null) {
            course.setStartDate(startDate);
        }

        return courseRepository.save(course);
    }

    /**
     * Добавляет модуль к существующему курсу.
     * <p>
     * Проверяет существование курса. Создает модуль с заданным названием,
     * описанием и порядковым номером, привязывает к курсу и сохраняет.
     * Каскадное сохранение обеспечивает запись модуля вместе с курсом.
     * После сохранения производится сброс менеджера сущностей
     * для получения сгенерированного идентификатора модуля.
     * </p>
     *
     * @param courseId    идентификатор курса
     * @param title       название модуля
     * @param description описание модуля
     * @param orderIndex  порядковый номер модуля в курсе
     * @return созданный модуль
     * @throws ResourceNotFoundException если курс с заданным идентификатором не найден
     */
    public Module addModule(Long courseId, String title, String description, Integer orderIndex) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Курс с идентификатором " + courseId + " не найден"));

        Module module = new Module(title, description, orderIndex, course);
        // Явное сохранение модуля, затем привязка к курсу
        entityManager.persist(module);
        course.getModules().add(module);
        entityManager.flush();
        return module;
    }

    /**
     * Добавляет урок к существующему модулю.
     * <p>
     * Проверяет существование модуля. Создает урок с заданным названием,
     * содержанием и ссылкой на видео, привязывает к модулю и сохраняет.
     * Каскадное сохранение обеспечивает запись урока вместе с модулем.
     * После сохранения производится сброс менеджера сущностей
     * для получения сгенерированного идентификатора урока.
     * </p>
     *
     * @param moduleId идентификатор модуля
     * @param title    название урока
     * @param content  текстовое содержание урока
     * @param videoUrl ссылка на видео-материал
     * @return созданный урок
     * @throws ResourceNotFoundException если модуль с заданным идентификатором не найден
     */
    public Lesson addLesson(Long moduleId, String title, String content, String videoUrl) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Модуль с идентификатором " + moduleId + " не найден"));

        Lesson lesson = new Lesson(title, content, videoUrl, module);
        // Явное сохранение урока, затем привязка к модулю
        entityManager.persist(lesson);
        module.getLessons().add(lesson);
        entityManager.flush();
        return lesson;
    }

    /**
     * Получает полную структуру курса с модулями и уроками.
     * <p>
     * Метод выполняется внутри транзакции, поэтому доступ к ленивым
     * коллекциям (модули, уроки) безопасен и не вызывает {@code LazyInitializationException}.
     * Производит принудительную инициализацию всех ленивых коллекций
     * для гарантии полной загрузки структуры.
     * </p>
     *
     * @param courseId идентификатор курса
     * @return курс с полностью загруженными модулями и уроками
     * @throws ResourceNotFoundException если курс с заданным идентификатором не найден
     */
    public Course getCourseStructure(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Курс с идентификатором " + courseId + " не найден"));

        // Принудительная инициализация ленивой коллекции модулей
        var modules = course.getModules();
        int size = modules.size();
        if (size > 0) {
            // Итерация для инициализации каждого модуля и его уроков
            Iterator<Module> iterator = modules.iterator();
            while (iterator.hasNext()) {
                Module module = iterator.next();
                var lessons = module.getLessons();
                lessons.size();
            }
        }

        return course;
    }
}
