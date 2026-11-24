package com.studying.platform.seeder;

import com.studying.platform.entity.AnswerOption;
import com.studying.platform.entity.Assignment;
import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Enrollment;
import com.studying.platform.entity.EnrollmentStatus;
import com.studying.platform.entity.Lesson;
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
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.EnrollmentRepository;
import com.studying.platform.repository.LessonRepository;
import com.studying.platform.repository.ModuleRepository;
import com.studying.platform.repository.QuestionRepository;
import com.studying.platform.repository.QuizRepository;
import com.studying.platform.repository.TagRepository;
import com.studying.platform.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Сидер демонстрационных данных учебной платформы.
 * <p>
 * Запускается автоматически при старте приложения только в случае активации
 * Spring-профиля {@code demo}. Создает полный набор демонстрационных данных:
 * преподавателя, студентов, курс со структурой (модули, уроки, задания),
 * записи студентов на курс и тестовую викторину.
 * </p>
 * <p>
 * Сидер выполняет проверку на наличие преподавателя с демо-адресом электронной почты
 * {@code demo-teacher@platform.test}. Если преподаватель уже существует, заполнение
 * пропускается, что предотвращает создание дубликатов при повторных запусках.
 * Существующие пользовательские данные не затрагиваются.
 * </p>
 * <p>
 * Заполнение выполняется в одной транзакции для обеспечения целостности данных.
 * Порядок создания учитывает зависимости сущностей: категория и теги → пользователи
 * → курс → модули → уроки → задания → записи → викторина.
 * </p>
 *
 * @see ApplicationRunner
 * @see org.springframework.context.annotation.Profile
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    /** Логгер для вывода информации о ходе заполнения демонстрационных данных. */
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    /** Уникальный адрес электронной почты демонстрационного преподавателя. */
    private static final String DEMO_TEACHER_EMAIL = "demo-teacher@platform.test";

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;

    /**
     * Конструктор сидера с внедрением всех необходимых репозиториев.
     *
     * @param userRepository          репозиторий пользователей
     * @param categoryRepository      репозиторий категорий
     * @param tagRepository           репозиторий тегов
     * @param courseRepository        репозиторий курсов
     * @param moduleRepository        репозиторий модулей
     * @param lessonRepository        репозиторий уроков
     * @param assignmentRepository    репозиторий заданий
     * @param enrollmentRepository    репозиторий записей
     * @param quizRepository          репозиторий викторин
     * @param questionRepository      репозиторий вопросов
     * @param answerOptionRepository  репозиторий вариантов ответа
     */
    public DemoDataSeeder(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            TagRepository tagRepository,
            CourseRepository courseRepository,
            ModuleRepository moduleRepository,
            LessonRepository lessonRepository,
            AssignmentRepository assignmentRepository,
            EnrollmentRepository enrollmentRepository,
            QuizRepository quizRepository,
            QuestionRepository questionRepository,
            AnswerOptionRepository answerOptionRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.answerOptionRepository = answerOptionRepository;
    }

    /**
     * Выполняет запуск сидера демонстрационных данных при старте приложения.
     * <p>
     * Проверяет наличие преподавателя с демо-адресом электронной почты.
     * Если преподаватель уже существует — завершает работу без ошибок.
     * В противном случае вызывает метод {@link #seedData()} для создания
     * полного набора демонстрационных данных.
     * </p>
     *
     * @param args аргументы приложения, переданные Spring Boot
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Запуск сидера демонстрационных данных (профиль 'demo')...");

        // Проверяем, был ли демонстрационный преподаватель уже создан
        if (userRepository.existsByEmail(DEMO_TEACHER_EMAIL)) {
            log.info("Демонстрационный преподаватель с email '{}' уже существует. Пропускаем заполнение.", DEMO_TEACHER_EMAIL);
            return;
        }

        seedData();
        log.info("Заполнение демонстрационными данными завершено успешно.");
    }

    /**
     * Выполняет полное заполнение базы данных демонстрационными данными.
     * <p>
     * Создаёт все сущности в порядке зависимостей: категория, теги, пользователи,
     * курс, модули, уроки, задания, записи, викторина с вопросами и вариантами.
     * Все операции выполняются в рамках одной транзакции.
     * </p>
     */
    @Transactional
    public void seedDemoData() {
        // Проверяем, был ли демонстрационный преподаватель уже создан
        if (userRepository.existsByEmail(DEMO_TEACHER_EMAIL)) {
            log.info("Демонстрационный преподаватель с email '{}' уже существует. Пропускаем заполнение.", DEMO_TEACHER_EMAIL);
            return;
        }

        seedData();
    }

    /**
     * Внутренний метод создания демонстрационных данных.
     * <p>
     * Создаёт все сущности в порядке зависимостей: категория, теги, пользователи,
     * курс, модули, уроки, задания, записи, викторина с вопросами и вариантами.
     * </p>
     */
    private void seedData() {
        log.info("Создание категории «Programming»...");
        Category category = createCategory("Programming");

        log.info("Создание тегов...");
        Tag javaTag = createTag("Java");
        Tag hibernateTag = createTag("Hibernate");
        Tag springBootTag = createTag("SpringBoot");

        log.info("Создание демонстрационного преподавателя...");
        User teacher = userRepository.saveAndFlush(new User(DEMO_TEACHER_EMAIL, UserRole.TEACHER));

        log.info("Создание демонстрационных студентов...");
        User studentOne = userRepository.saveAndFlush(new User("demo-student-one@platform.test", UserRole.STUDENT));
        User studentTwo = userRepository.saveAndFlush(new User("demo-student-two@platform.test", UserRole.STUDENT));

        log.info("Создание курса «Intro to Java»...");
        Course course = new Course(
                "Intro to Java",
                "Введение в программирование на Java: основы, работа с базами данных, Spring Boot.",
                40,
                LocalDate.now(),
                teacher,
                category
        );
        course.getTags().add(javaTag);
        course.getTags().add(hibernateTag);
        course.getTags().add(springBootTag);
        course = courseRepository.saveAndFlush(course);

        log.info("Создание модулей курса...");
        Module moduleOne = new Module("Java Basics", "Основы языка Java: переменные, типы данных, циклы.", 1, course);
        moduleOne = moduleRepository.saveAndFlush(moduleOne);

        Module moduleTwo = new Module("Hibernate & JPA", "Работа с базами данных через Hibernate и JPA.", 2, course);
        moduleTwo = moduleRepository.saveAndFlush(moduleTwo);

        log.info("Создание уроков...");
        Lesson lessonOne = lessonRepository.saveAndFlush(
                new Lesson("Variables and Types", "Изучение переменных и типов данных в Java.", null, moduleOne));
        Lesson lessonTwo = lessonRepository.saveAndFlush(
                new Lesson("Hello World Program", "Написание первой программы на Java.", "https://example.com/hello-world", moduleOne));
        Lesson lessonThree = lessonRepository.saveAndFlush(
                new Lesson("Entity Mapping", "Создание JPA-сущностей и маппинг полей.", null, moduleTwo));
        Lesson lessonFour = lessonRepository.saveAndFlush(
                new Lesson("Relationships", "Связи между сущностями: OneToMany, ManyToOne, ManyToMany.", null, moduleTwo));

        log.info("Создание заданий...");
        assignmentRepository.saveAndFlush(new Assignment(lessonOne, "Variables Exercise",
                "Создайте переменные разных типов и выполните операции над ними.", LocalDate.now().plusDays(7), 100));
        assignmentRepository.saveAndFlush(new Assignment(lessonTwo, "Hello World Task",
                "Напишите и запустите программу Hello World на Java.", LocalDate.now().plusDays(7), 50));
        assignmentRepository.saveAndFlush(new Assignment(lessonThree, "Entity Mapping Task",
                "Создайте JPA-сущность User с основными полями.", LocalDate.now().plusDays(14), 100));
        assignmentRepository.saveAndFlush(new Assignment(lessonFour, "Relationships Task",
                "Реализуйте связи между сущностями Course и Module.", LocalDate.now().plusDays(14), 100));

        log.info("Создание записей студентов на курс...");
        enrollmentRepository.saveAndFlush(new Enrollment(studentOne, course, LocalDateTime.now(), EnrollmentStatus.ACTIVE));
        enrollmentRepository.saveAndFlush(new Enrollment(studentTwo, course, LocalDateTime.now(), EnrollmentStatus.ACTIVE));

        log.info("Создание викторины для модуля «Hibernate & JPA»...");
        createQuizForModule(moduleTwo);

        log.info("Демонстрационные данные созданы: 1 категория, 3 тега, 1 преподаватель, 2 студента, 1 курс, "
                + "2 модуля, 4 урока, 4 задания, 2 записи, 1 викторина с 2 вопросами и 6 вариантами ответа.");
    }

    /**
     * Создаёт категорию с указанным названием, если она ещё не существует.
     *
     * @param name название категории
     * @return созданная или существующая категория
     */
    private Category createCategory(String name) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> {
                    Category category = new Category(name);
                    return categoryRepository.saveAndFlush(category);
                });
    }

    /**
     * Создаёт тег с указанным названием, если он ещё не существует.
     *
     * @param name название тега
     * @return созданный или существующий тег
     */
    private Tag createTag(String name) {
        return tagRepository.findByName(name)
                .orElseGet(() -> {
                    Tag tag = new Tag(name);
                    return tagRepository.saveAndFlush(tag);
                });
    }

    /**
     * Создаёт викторину с вопросами и вариантами ответа для заданного модуля.
     * <p>
     * Викторина содержит два вопроса: один с одиночным выбором и один с множественным.
     * Каждый вопрос имеет три варианта ответа, среди которых один или несколько правильных.
     * </p>
     *
     * @param module модуль, для которого создаётся викторина
     */
    private void createQuizForModule(Module module) {
        Quiz quiz = new Quiz(module, "Module 2 Quiz", 30);
        quiz = quizRepository.saveAndFlush(quiz);

        // Вопрос 1: SINGLE_CHOICE — «Какая аннотация отмечает сущность?»
        Question questionOne = new Question(quiz, "Какая аннотация отмечает JPA-сущность?", QuizQuestionType.SINGLE_CHOICE);
        questionOne = questionRepository.saveAndFlush(questionOne);

        answerOptionRepository.saveAndFlush(new AnswerOption(questionOne, "@Entity", true));
        answerOptionRepository.saveAndFlush(new AnswerOption(questionOne, "@Table", false));
        answerOptionRepository.saveAndFlush(new AnswerOption(questionOne, "@Column", false));

        // Вопрос 2: MULTIPLE_CHOICE — «Какие из этих отношений поддерживаются JPA?»
        Question questionTwo = new Question(quiz, "Какие из этих отношений поддерживаются JPA?", QuizQuestionType.MULTIPLE_CHOICE);
        questionTwo = questionRepository.saveAndFlush(questionTwo);

        answerOptionRepository.saveAndFlush(new AnswerOption(questionTwo, "@OneToMany", true));
        answerOptionRepository.saveAndFlush(new AnswerOption(questionTwo, "@ManyToMany", true));
        answerOptionRepository.saveAndFlush(new AnswerOption(questionTwo, "@HasMany", false));
    }
}
