package com.studying.platform.service;

import com.studying.platform.entity.Assignment;
import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.EnrollmentStatus;
import com.studying.platform.entity.Lesson;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.Quiz;
import com.studying.platform.entity.Question;
import com.studying.platform.entity.Tag;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.AssignmentRepository;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.CourseReviewRepository;
import com.studying.platform.repository.EnrollmentRepository;
import com.studying.platform.repository.LessonRepository;
import com.studying.platform.repository.ModuleRepository;
import com.studying.platform.repository.QuizRepository;
import com.studying.platform.repository.QuizSubmissionRepository;
import com.studying.platform.repository.SubmissionRepository;
import com.studying.platform.repository.TagRepository;
import com.studying.platform.repository.UserRepository;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для управления курсами, модулями, уроками и тегами учебной платформы.
 * <p>
 * Обеспечивает бизнес-логику создания, изменения и получения структуры курса,
 * а также добавления модулей, уроков и управления тегами. Все публичные методы транзакционны.
 * </p>
 *
 * @see Course
 * @see Module
 * @see Lesson
 * @see Tag
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
     * Репозиторий для работы с сущностью «Задание».
     */
    private final AssignmentRepository assignmentRepository;

    /**
     * Репозиторий для работы с сущностью «Тег».
     */
    private final TagRepository tagRepository;

    /**
     * Репозиторий для работы с сущностью «Решение».
     */
    private final SubmissionRepository submissionRepository;

    /**
     * Репозиторий для работы с сущностью «Результат прохождения теста».
     */
    private final QuizSubmissionRepository quizSubmissionRepository;

    /**
     * Репозиторий для работы с сущностью «Викторина».
     */
    private final QuizRepository quizRepository;

    /**
     * Репозиторий для работы с сущностью «Запись на курс».
     */
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Репозиторий для работы с сущностью «Отзыв о курсе».
     */
    private final CourseReviewRepository courseReviewRepository;

    /**
     * Конструктор сервиса с внедрением зависимостей через конструктор.
     *
     * @param courseRepository   репозиторий курсов
     * @param moduleRepository   репозиторий модулей
     * @param lessonRepository   репозиторий уроков
     * @param userRepository     репозиторий пользователей
     * @param categoryRepository репозиторий категорий
     * @param entityManager      менеджер сущностей JPA
     * @param assignmentRepository репозиторий заданий
     * @param tagRepository       репозиторий тегов
     * @param submissionRepository репозиторий решений
     * @param quizSubmissionRepository репозиторий результатов тестов
     * @param quizRepository      репозиторий викторин
     * @param enrollmentRepository репозиторий записей
     * @param courseReviewRepository репозиторий отзывов
     */
    public CourseService(CourseRepository courseRepository,
                         ModuleRepository moduleRepository,
                         LessonRepository lessonRepository,
                         UserRepository userRepository,
                         CategoryRepository categoryRepository,
                         EntityManager entityManager,
                         AssignmentRepository assignmentRepository,
                         TagRepository tagRepository,
                         SubmissionRepository submissionRepository,
                         QuizSubmissionRepository quizSubmissionRepository,
                         QuizRepository quizRepository,
                         EnrollmentRepository enrollmentRepository,
                         CourseReviewRepository courseReviewRepository) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.entityManager = entityManager;
        this.assignmentRepository = assignmentRepository;
        this.tagRepository = tagRepository;
        this.submissionRepository = submissionRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.quizRepository = quizRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseReviewRepository = courseReviewRepository;
    }

    /**
     * Проверяет корректность продолжительности курса.
     * <p>
     * Если значение равно {@code null} или меньше единицы —
     * выбрасывает {@link IllegalArgumentException} с понятным сообщением об ошибке.
     * </p>
     *
     * @param duration продолжительность в часах
     * @throws IllegalArgumentException если продолжительность null или меньше 1
     */
    private void validateDuration(Integer duration) {
        if (duration == null || duration < 1) {
            throw new IllegalArgumentException("Продолжительность курса должна быть не менее 1 часа");
        }
    }

    /**
     * Создаёт новый курс с заданными параметрами.
     * <p>
     * Проверяет продолжительность, существование преподавателя и его роль TEACHER,
     * а также существование категории. Если проверка не пройдена —
     * выбрасывает {@link IllegalArgumentException} или {@link ResourceNotFoundException}.
     * </p>
     *
     * @param title       название курса
     * @param description описание курса
     * @param duration    продолжительность в часах
     * @param startDate   дата начала курса
     * @param teacherId   идентификатор преподавателя
     * @param categoryId  идентификатор категории
     * @return сохранённый курс
     * @throws IllegalArgumentException если продолжительность меньше 1 часа
     * @throws ResourceNotFoundException если преподаватель или категория не найдены,
     *                                   или роль преподавателя не TEACHER
     */
    public Course createCourse(String title, String description, Integer duration,
                               LocalDate startDate, Long teacherId, Long categoryId) {
        validateDuration(duration);

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
     * Продолжительность проверяется только при не-{@code null} значении;
     * {@code null} означает «не изменять продолжительность».
     * </p>
     *
     * @param courseId    идентификатор курса для обновления
     * @param title       новое название курса
     * @param description новое описание курса
     * @param duration    новая продолжительность в часах
     * @param startDate   новая дата начала курса
     * @return обновлённый курс
     * @throws IllegalArgumentException если передана некорректная продолжительность
     * @throws ResourceNotFoundException если курс с заданным идентификатором не найден
     */
    public Course updateCourse(Long courseId, String title, String description,
                               Integer duration, LocalDate startDate) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Курс с идентификатором " + courseId + " не найден"));

        if (duration != null) {
            validateDuration(duration);
        }

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
     * Явное сохранение через {@code entityManager.persist(module)} гарантирует
     * запись модуля в базу данных; {@code flush} обеспечивает получение
     * сгенерированного идентификатора.
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
     * Явное сохранение через {@code entityManager.persist(lesson)} гарантирует
     * запись урока в базу данных; {@code flush} обеспечивает получение
     * сгенерированного идентификатора.
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

    /**
     * Создаёт новое задание для указанного урока.
     * <p>
     * Проверяет существование урока по идентификатору. Если урок не найден —
     * выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param lessonId    идентификатор урока
     * @param title       название задания
     * @param description описание задания
     * @param dueDate     срок выполнения
     * @param maxScore    максимальный балл
     * @return сохранённое задание
     * @throws ResourceNotFoundException если урок с заданным идентификатором не найден
     */
    public Assignment createAssignment(Long lessonId, String title,
                                       String description, LocalDate dueDate,
                                       Integer maxScore) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Урок с идентификатором " + lessonId + " не найден"));

        Assignment assignment = new Assignment(lesson, title, description, dueDate, maxScore);
        entityManager.persist(assignment);
        entityManager.flush();
        return assignment;
    }

    /**
     * Обновляет поля существующего задания.
     * <p>
     * Проверяет существование задания по идентификатору. Обновляет только
     * не-{@code null} поля: название, описание, срок выполнения, максимальный балл.
     * </p>
     *
     * @param assignmentId  идентификатор задания для обновления
     * @param title         новое название задания
     * @param description   новое описание задания
     * @param dueDate       новая дата окончания выполнения
     * @param maxScore      новый максимальный балл
     * @return обновлённое задание
     * @throws ResourceNotFoundException если задание с заданным идентификатором не найдено
     */
    public Assignment updateAssignment(Long assignmentId, String title,
                                       String description, LocalDate dueDate,
                                       Integer maxScore) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Задание с идентификатором " + assignmentId + " не найдено"));

        if (title != null) {
            assignment.setTitle(title);
        }
        if (description != null) {
            assignment.setDescription(description);
        }
        if (dueDate != null) {
            assignment.setDueDate(dueDate);
        }
        if (maxScore != null) {
            assignment.setMaxScore(maxScore);
        }

        return assignmentRepository.save(assignment);
    }

    /**
     * Удаляет задание по идентификатору.
     * <p>
     * Проверяет существование задания. Если задание не найдено —
     * выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param assignmentId идентификатор задания для удаления
     * @throws ResourceNotFoundException если задание с заданным идентификатором не найдено
     */
    public void deleteAssignment(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Задание с идентификатором " + assignmentId + " не найдено"));

        assignmentRepository.delete(assignment);
        entityManager.flush();
    }

    /**
     * Возвращает все задания указанного урока.
     * <p>
     * Использует специализированный метод репозитория {@code findByLessonId}
     * для эффективной выборки по идентификатору урока.
     * </p>
     *
     * @param lessonId идентификатор урока
     * @return список заданий урока
     */
    public List<Assignment> getAssignmentsByLessonId(Long lessonId) {
        return assignmentRepository.findByLessonId(lessonId);
    }

    /**
     * Привязывает существующие теги к курсу.
     * <p>
     * Проверяет существование курса и каждого тега.
     * Если курс или какой-либо тег не найден — выбрасывает {@link ResourceNotFoundException}.
     * Связь реализуется через вспомогательную таблицу {@code course_tag}.
     * Каскады {@code PERSIST} и {@code MERGE} на коллекции тегов
     * автоматически обрабатывают запись в таблицу связи.
     * </p>
     *
     * @param courseId идентификатор курса
     * @param tagIds   идентификаторы тегов для привязки
     * @return обновлённый курс с привязанными тегами
     * @throws ResourceNotFoundException если курс или какой-либо тег не найден
     */
    public Course addTagsToCourse(Long courseId, Set<Long> tagIds) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Курс с идентификатором " + courseId + " не найден"));

        if (tagIds == null || tagIds.isEmpty()) {
            return course;
        }

        List<Tag> tags = tagRepository.findAllById(tagIds);

        // Проверка, что все переданные ID были найдены
        Set<Long> foundIds = new HashSet<>();
        for (Tag tag : tags) {
            foundIds.add(tag.getId());
        }

        for (Long tagId : tagIds) {
            if (!foundIds.contains(tagId)) {
                throw new ResourceNotFoundException(
                        "Тег с идентификатором " + tagId + " не найден");
            }
        }

        course.getTags().addAll(tags);
        entityManager.flush();
        return course;
    }

    /**
     * Отвязывает теги от курса.
     * <p>
     * Проверяет существование курса и каждого тега.
     * Если курс не найден — выбрасывает {@link ResourceNotFoundException}.
     * Если какой-либо тег не найден — выбрасывает {@link ResourceNotFoundException}.
     * Удаляет связь из таблицы {@code course_tag}, но не удаляет сами теги из базы данных.
     * Тег остаётся доступным для других курсов.
     * </p>
     *
     * @param courseId идентификатор курса
     * @param tagIds   идентификаторы тегов для удаления из курса
     * @return обновлённый курс без указанных тегов
     * @throws ResourceNotFoundException если курс или какой-либо тег не найден
     */
    public Course removeTagsFromCourse(Long courseId, Set<Long> tagIds) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Курс с идентификатором " + courseId + " не найден"));

        if (tagIds == null || tagIds.isEmpty()) {
            return course;
        }

        List<Tag> tags = tagRepository.findAllById(tagIds);

        // Проверка, что все переданные ID были найдены
        Set<Long> foundIds = new HashSet<>();
        for (Tag tag : tags) {
            foundIds.add(tag.getId());
        }

        for (Long tagId : tagIds) {
            if (!foundIds.contains(tagId)) {
                throw new ResourceNotFoundException(
                        "Тег с идентификатором " + tagId + " не найден");
            }
        }

        course.getTags().removeAll(tags);
        entityManager.flush();
        return course;
    }

    /**
     * Удаляет курс по идентификатору с проверкой отсутствия учебной истории.
     * <p>
     * Перед удалением проверяет, что у курса нет записей (включая отписавшихся студентов),
     * решений, результатов тестов и отзывов. Если какая-либо проверка не пройдена —
     * выбрасывает {@link IllegalStateException} с понятным сообщением.
     * </p>
     * <p>
     * Если проверка пройдена, удаляет задания и структуру тестов, затем курс
     * вместе с модулями и уроками. Общие теги, преподаватель и категория сохраняются.
     * </p>
     *
     * @param courseId идентификатор курса для удаления
     * @throws ResourceNotFoundException если курс с заданным идентификатором не найден
     * @throws IllegalStateException если курс имеет записи, решения, результаты тестов или отзывы
     */
    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Курс с идентификатором " + courseId + " не найден"));

        // Инициализация ленивой коллекции модулей
        Set<Module> modules = course.getModules();
        int moduleCount = modules.size();

        // Собираем ID уроков и заданий из модулей
        Set<Long> assignmentIds = new HashSet<>();
        List<Long> moduleIds = new java.util.ArrayList<>();

        for (Module module : modules) {
            moduleIds.add(module.getId());
            for (Lesson lesson : module.getLessons()) {
                List<Assignment> assignments = assignmentRepository.findByLessonId(lesson.getId());
                for (Assignment assignment : assignments) {
                    assignmentIds.add(assignment.getId());
                }
            }
        }

        // Собираем ID викторин из модулей
        Set<Long> quizIds = new HashSet<>();
        for (Long moduleId : moduleIds) {
            quizRepository.findByModuleId(moduleId).ifPresent(q -> quizIds.add(q.getId()));
        }

        // Проверяем наличие сохранённых решений
        if (!assignmentIds.isEmpty()) {
            long submissionCount = submissionRepository.countByAssignmentIdIn(assignmentIds);
            if (submissionCount > 0) {
                throw new IllegalStateException(
                        "Нельзя удалить курс: есть сохранённые решения студентов. "
                                + "Удаление невозможно, так как решения являются учебной историей.");
            }
        }

        // Проверяем наличие результатов тестов
        if (!quizIds.isEmpty()) {
            long quizSubmissionCount = quizSubmissionRepository.countByQuizIdIn(quizIds);
            if (quizSubmissionCount > 0) {
                throw new IllegalStateException(
                        "Нельзя удалить курс: есть результаты прохождения тестов. "
                                + "Удаление невозможно, так как результаты являются учебной историей.");
            }
        }

        // Проверяем наличие записей (включая отписавшихся — DROPPED)
        long enrollmentCount = enrollmentRepository.countByCourseId(courseId);
        if (enrollmentCount > 0) {
            throw new IllegalStateException(
                    "Нельзя удалить курс: есть записи студентов (включая отписавшихся). "
                            + "Удаление невозможно, пока не удалены все записи.");
        }

        // Проверяем наличие отзывов
        long reviewCount = courseReviewRepository.countByCourseId(courseId);
        if (reviewCount > 0) {
            throw new IllegalStateException(
                    "Нельзя удалить курс: есть отзывы студентов. "
                            + "Удаление невозможно, пока не удалены все отзывы.");
        }

        // Сначала удаляем зависимое содержимое, не затрагивая учебную историю.
        for (Long quizId : quizIds) {
            Quiz quiz = quizRepository.findByIdWithQuestions(quizId).orElseThrow();
            for (Question question : quiz.getQuestions()) {
                question.getOptions().forEach(entityManager::remove);
                question.getOptions().clear();
            }
            entityManager.flush();
            quiz.getQuestions().forEach(entityManager::remove);
            quiz.getQuestions().clear();
            entityManager.flush();
            entityManager.remove(quiz);
        }
        entityManager.flush();
        for (Long assignmentId : assignmentIds) {
            entityManager.remove(entityManager.getReference(Assignment.class, assignmentId));
        }
        entityManager.flush();

        // Модули и уроки удаляются каскадом вместе с курсом.
        courseRepository.delete(course);
    }
}
