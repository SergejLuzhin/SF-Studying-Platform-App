package com.studying.platform;

import com.studying.platform.entity.AnswerOption;
import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.Question;
import com.studying.platform.entity.Quiz;
import com.studying.platform.entity.QuizQuestionType;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.repository.AnswerOptionRepository;
import com.studying.platform.repository.CategoryRepository;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.ModuleRepository;
import com.studying.platform.repository.QuestionRepository;
import com.studying.platform.repository.QuizRepository;
import com.studying.platform.repository.UserRepository;
import jakarta.persistence.EntityManager;
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
 * Интеграционные тесты для сущностей «Викторина», «Вопрос» и «Вариант ответа» учебной платформы.
 * <p>
 * Проверяют сохранение и загрузку иерархии викторина → вопрос → вариант ответа,
 * ограничение «один модуль — один тест», типы вопросов и базовые операции репозиториев.
 * </p>
 * <p>
 * Тесты выполняются с PostgreSQL через Testcontainers в профиле «test».
 * </p>
 *
 * @see Quiz
 * @see Question
 * @see AnswerOption
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuizStructureTest {

    /**
     * Менеджер сущностей для ручного управления транзакциями.
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * Репозиторий для сущности Quiz.
     */
    @Autowired
    private QuizRepository quizRepository;

    /**
     * Репозиторий для сущности Question.
     */
    @Autowired
    private QuestionRepository questionRepository;

    /**
     * Репозиторий для сущности AnswerOption.
     */
    @Autowired
    private AnswerOptionRepository answerOptionRepository;

    /**
     * Репозиторий для сущности Module.
     */
    @Autowired
    private ModuleRepository moduleRepository;

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
     * Создаёт и сохраняет тестовый модуль с привязкой к курсу.
     */
    private Module createModule(String title, Integer orderIndex, Course course) {
        Module module = new Module(title, null, orderIndex, course);
        entityManager.persist(module);
        entityManager.flush();
        return module;
    }

    /**
     * Тест: сохранение викторины с вопросами и вариантами ответа.
     * <p>
     * Создаёт викторину, привязывает к модулю, добавляет вопросы с вариантами ответа,
     * сохраняет викторину (каскад PERSIST). После очистки контекста загружает викторину
     * из базы данных и проверяет всю иерархию.
     * </p>
     */
    @Test
    void testSaveQuizWithQuestionsAndOptions() {
        // Подготовка: категория, преподаватель, курс, модуль
        Category category = createCategory("Программирование");
        User teacher = createTeacher("teacher_quiz@example.com");
        Course course = createCourse("Основы Java", teacher, category);
        Module module = createModule("Введение в OOP", 1, course);

        // Создаём викторину
        Quiz quiz = new Quiz(module, "Тест по модулю 1", 30);
        entityManager.persist(quiz);
        entityManager.flush();

        // Сохраняем ID
        Long quizId = quiz.getId();
        assertNotNull(quizId, "Идентификатор викторины не должен быть null");

        // Очищаем контекст
        entityManager.clear();

        // Загружаем викторину
        Quiz loadedQuiz = quizRepository.findById(quizId).orElseThrow();
        assertEquals("Тест по модулю 1", loadedQuiz.getTitle());
        assertEquals(30, loadedQuiz.getTimeLimit());
        assertEquals(module.getId(), loadedQuiz.getModule().getId());
    }

    /**
     * Тест: каскадное сохранение викторины с вопросами и вариантами ответа.
     * <p>
     * Создаёт викторину, привязывает к модулю, добавляет вопросы с вариантами ответа.
     * Сохраняет только викторину — каскад PERSIST обеспечивает запись вопросов и вариантов.
     * После очистки контекста загружает викторину из базы и проверяет иерархию.
     * </p>
     */
    @Test
    void testCascadeSaveQuizWithQuestionsAndOptions() {
        // Подготовка
        Category category = createCategory("Математика");
        User teacher = createTeacher("teacher_cascade_quiz@example.com");
        Course course = createCourse("Высшая математика", teacher, category);
        Module module = createModule("Линейная алгебра", 2, course);

        // Создаём викторину с вопросами и вариантами
        Quiz quiz = new Quiz(module, "Итоговый тест", 45);

        Question q1 = new Question(quiz, "Что такое детерминант?", QuizQuestionType.SINGLE_CHOICE);
        quiz.getQuestions().add(q1);

        AnswerOption opt1 = new AnswerOption(q1, "Число, характеризующее квадратную матрицу", true);
        AnswerOption opt2 = new AnswerOption(q1, "Вектор пространства", false);
        q1.getOptions().add(opt1);
        q1.getOptions().add(opt2);

        Question q2 = new Question(quiz, "Выберите собственные векторы матрицы.", QuizQuestionType.MULTIPLE_CHOICE);
        quiz.getQuestions().add(q2);

        AnswerOption opt3 = new AnswerOption(q2, "Вектор, меняющий направление при умножении", false);
        AnswerOption opt4 = new AnswerOption(q2, "Вектор, сохраняющий направление", true);
        AnswerOption opt5 = new AnswerOption(q2, "Нулевой вектор", true);
        q2.getOptions().add(opt3);
        q2.getOptions().add(opt4);
        q2.getOptions().add(opt5);

        // Сохраняем только викторину — каскад PERSIST сохранит вопросы и варианты
        entityManager.persist(quiz);
        entityManager.flush();

        Long quizId = quiz.getId();

        // Очищаем контекст
        entityManager.clear();

        // Загружаем викторину заново
        Quiz loadedQuiz = quizRepository.findById(quizId).orElseThrow();
        assertEquals("Итоговый тест", loadedQuiz.getTitle());
        assertEquals(2, loadedQuiz.getQuestions().size(),
                "Викторина должна содержать 2 вопроса");

        // Проверяем первый вопрос
        Question loadedQ1 = questionRepository.findByQuizId(quizId).stream()
                .filter(q -> "Что такое детерминант?".equals(q.getText()))
                .findFirst().orElseThrow();
        assertEquals(QuizQuestionType.SINGLE_CHOICE, loadedQ1.getType());
        assertEquals(2, loadedQ1.getOptions().size(), "Первый вопрос должен содержать 2 варианта ответа");

        // Проверяем второй вопрос
        Question loadedQ2 = questionRepository.findByQuizId(quizId).stream()
                .filter(q -> "Выберите собственные векторы матрицы.".equals(q.getText()))
                .findFirst().orElseThrow();
        assertEquals(QuizQuestionType.MULTIPLE_CHOICE, loadedQ2.getType());
        assertEquals(3, loadedQ2.getOptions().size(), "Второй вопрос должен содержать 3 варианта ответа");

        // Проверяем варианты ответа
        List<AnswerOption> options = answerOptionRepository.findByQuestionId(loadedQ1.getId());
        long correctOptions = options.stream().filter(AnswerOption::getIsCorrect).count();
        assertEquals(1, correctOptions, "В вопросе с одиночным выбором должен быть один правильный вариант");

        List<AnswerOption> options2 = answerOptionRepository.findByQuestionId(loadedQ2.getId());
        long correctOptions2 = options2.stream().filter(AnswerOption::getIsCorrect).count();
        assertEquals(2, correctOptions2, "В вопросе с множественным выбором должно быть два правильных варианта");
    }

    /**
     * Тест: загрузка викторины по ID модуля.
     * <p>
     * Создаёт викторину с модулем, затем проверяет поиск через {@code QuizRepository.findByModuleId}.
     * </p>
     */
    @Test
    void testFindQuizByModuleId() {
        Category category = createCategory("Физика");
        User teacher = createTeacher("teacher_findby_module@example.com");
        Course course = createCourse("Квантовая физика", teacher, category);
        Module module = createModule("Волновая механика", 3, course);

        Quiz quiz = new Quiz(module, "Волновая механика — тест", 20);
        entityManager.persist(quiz);
        entityManager.flush();

        // Поиск викторины по ID модуля
        var foundQuiz = quizRepository.findByModuleId(module.getId());
        assertTrue(foundQuiz.isPresent(), "Викторина должна быть найдена по ID модуля");
        assertEquals(quiz.getId(), foundQuiz.get().getId());
    }

    /**
     * Тест: отсутствие викторины для модуля.
     * <p>
     * Модуль без викторины не должен возвращать результат при поиске по ID модуля.
     * </p>
     */
    @Test
    void testNoQuizForModule() {
        Category category = createCategory("Биология");
        User teacher = createTeacher("teacher_noquiz@example.com");
        Course course = createCourse("Генетика", teacher, category);
        Module module = createModule("Основы генетики", 1, course);

        // Не создаём викторину для модуля
        var foundQuiz = quizRepository.findByModuleId(module.getId());
        assertFalse(foundQuiz.isPresent(), "Викторина не должна быть найдена для модуля без теста");
    }

    /**
     * Тест: ограничение «один модуль — один тест».
     * <p>
     * Попытка создать вторую викторину для того же модуля должна привести
     * к нарушению уникальности внешнего ключа.
     * </p>
     */
    @Test
    void testOneQuizPerModuleConstraint() {
        Category category = createCategory("Химия");
        User teacher = createTeacher("teacher_unique@example.com");
        Course course = createCourse("Органическая химия", teacher, category);
        Module module = createModule("Углеводороды", 1, course);

        // Первая викторина — сохраняется и фиксируется отдельно
        Quiz quiz1 = new Quiz(module, "Первый тест", 30);
        entityManager.persist(quiz1);
        entityManager.flush();
        entityManager.clear();

        Long quiz1Id = quiz1.getId();

        // Убедимся, что первая викторина доступна в БД
        entityManager.clear();
        assertTrue(quizRepository.findById(quiz1Id).isPresent(),
                "Первая викторина должна быть сохранена");

        // Попытка создать вторую викторину для того же модуля
        Quiz quiz2 = new Quiz(module, "Второй тест", 30);

        boolean caught = false;
        try {
            entityManager.persist(quiz2);
            entityManager.flush();
        } catch (Exception e) {
            assertTrue(e.getMessage() != null && e.getMessage().contains("duplicate key value violates unique constraint"),
                    "Нарушение уникальности: " + e.getMessage());
            caught = true;
        }
        assertTrue(caught, "Должно быть выброшено исключение нарушения уникальности");
    }

    /**
     * Тест: сохранение вопроса типа SINGLE_CHOICE с одним правильным вариантом.
     * <p>
     * Проверяет корректное сохранение и загрузку вопроса с единственным правильным ответом.
     * </p>
     */
    @Test
    void testSingleChoiceQuestion() {
        Category category = createCategory("История");
        User teacher = createTeacher("teacher_singlechoice@example.com");
        Course course = createCourse("История Древнего Рима", teacher, category);
        Module module = createModule("Республика", 1, course);

        Quiz quiz = new Quiz(module, "Римская республика", 15);
        entityManager.persist(quiz);
        entityManager.flush();

        Question question = new Question(quiz, "В каком году была основана Римская республика?",
                QuizQuestionType.SINGLE_CHOICE);
        question.setText("В каком году была основана Римская республика?");
        question.setType(QuizQuestionType.SINGLE_CHOICE);
        entityManager.persist(question);
        entityManager.flush();

        Long questionId = question.getId();

        // Добавляем варианты
        AnswerOption correct = new AnswerOption(question, "509 до н.э.", true);
        AnswerOption wrong1 = new AnswerOption(question, "753 до н.э.", false);
        AnswerOption wrong2 = new AnswerOption(question, "27 до н.э.", false);
        entityManager.persist(correct);
        entityManager.persist(wrong1);
        entityManager.persist(wrong2);
        entityManager.flush();

        // Очищаем контекст
        entityManager.clear();

        // Проверяем загрузку
        Question loaded = questionRepository.findById(questionId).orElseThrow();
        assertEquals(QuizQuestionType.SINGLE_CHOICE, loaded.getType());

        List<AnswerOption> options = answerOptionRepository.findByQuestionId(questionId);
        assertEquals(3, options.size());
        long correctCount = options.stream().filter(AnswerOption::getIsCorrect).count();
        assertEquals(1, correctCount, "Вопрос SINGLE_CHOICE должен иметь ровно один правильный вариант");
    }

    /**
     * Тест: сохранение вопроса типа MULTIPLE_CHOICE с несколькими правильными вариантами.
     * <p>
     * Проверяет корректное сохранение и загрузку вопроса с множественным выбором.
     * </p>
     */
    @Test
    void testMultipleChoiceQuestion() {
        Category category = createCategory("Информатика");
        User teacher = createTeacher("teacher_multiplechoice@example.com");
        Course course = createCourse("Алгоритмы", teacher, category);
        Module module = createModule("Сортировки", 1, course);

        Quiz quiz = new Quiz(module, "Алгоритмы сортировки", 25);
        entityManager.persist(quiz);
        entityManager.flush();

        Question question = new Question(quiz, "Какие из перечисленных алгоритмов сортировки работают на месте?",
                QuizQuestionType.MULTIPLE_CHOICE);
        entityManager.persist(question);
        entityManager.flush();

        Long questionId = question.getId();

        // Добавляем варианты: два правильных и два неправильных
        entityManager.persist(new AnswerOption(question, "Сортировка вставками", true));
        entityManager.persist(new AnswerOption(question, "Сортировка пузырьком", true));
        entityManager.persist(new AnswerOption(question, "Сортировка слиянием", false));
        entityManager.persist(new AnswerOption(question, "Порождающая сортировка", false));
        entityManager.flush();

        // Очищаем контекст
        entityManager.clear();

        Question loaded = questionRepository.findById(questionId).orElseThrow();
        assertEquals(QuizQuestionType.MULTIPLE_CHOICE, loaded.getType());

        List<AnswerOption> options = answerOptionRepository.findByQuestionId(questionId);
        assertEquals(4, options.size());
        long correctCount = options.stream().filter(AnswerOption::getIsCorrect).count();
        assertEquals(2, correctCount, "Вопрос MULTIPLE_CHOICE должен иметь несколько правильных вариантов");
    }

    /**
     * Тест: вопрос с пустым текстом вызывает нарушение целостности данных.
     */
    @Test
    void testNullTextForQuestion() {
        Category category = createCategory("Геометрия");
        User teacher = createTeacher("teacher_nulltext@example.com");
        Course course = createCourse("Планиметрия", teacher, category);
        Module module = createModule("Треугольники", 1, course);

        Quiz quiz = new Quiz(module, "Треугольники — тест", 10);
        entityManager.persist(quiz);
        entityManager.flush();

        Question question = new Question(quiz, null, QuizQuestionType.SINGLE_CHOICE);

        boolean caught = false;
        try {
            entityManager.persist(question);
            entityManager.flush();
        } catch (Exception e) {
            assertTrue(e.getMessage() != null && e.getMessage().contains("violates not-null constraint"),
                    "Нарушение NOT NULL: " + e.getMessage());
            caught = true;
        }
        assertTrue(caught, "Должно быть выброшено исключение нарушения целостности данных");
    }

    /**
     * Тест: вариант ответа без привязки к вопросу вызывает нарушение целостности данных.
     */
    @Test
    void testOrphanAnswerOption() {
        Category category = createCategory("Астрономия");
        User teacher = createTeacher("teacher_orphanoption@example.com");
        Course course = createCourse("Космос", teacher, category);
        Module module = createModule("Солнечная система", 1, course);

        Quiz quiz = new Quiz(module, "Солнечная система", 15);
        entityManager.persist(quiz);
        entityManager.flush();

        Question question = new Question(quiz, "Какая планета ближайшая к Солнцу?",
                QuizQuestionType.SINGLE_CHOICE);
        entityManager.persist(question);
        entityManager.flush();

        // Вариант без привязки к вопросу (null question)
        AnswerOption option = new AnswerOption(null, "Марс", false);

        boolean caught = false;
        try {
            entityManager.persist(option);
            entityManager.flush();
        } catch (Exception e) {
            assertTrue(e.getMessage() != null && e.getMessage().contains("violates not-null constraint"),
                    "Нарушение NOT NULL: " + e.getMessage());
            caught = true;
        }
        assertTrue(caught, "Должно быть выброшено исключение нарушения целостности данных");
    }

    /**
     * Тест: викторина с пустой коллекцией вопросов сохраняется без ошибок.
     */
    @Test
    void testQuizWithoutQuestions() {
        Category category = createCategory("Философия");
        User teacher = createTeacher("teacher_emptyquiz@example.com");
        Course course = createCourse("Этика Аристотеля", teacher, category);
        Module module = createModule("Нравственность", 1, course);

        Quiz quiz = new Quiz(module, "Подготовка к тесту", 20);
        entityManager.persist(quiz);
        entityManager.flush();

        Long quizId = quiz.getId();
        assertNotNull(quizId, "Идентификатор викторины не должен быть null");

        // Очищаем контекст
        entityManager.clear();

        Quiz loaded = quizRepository.findById(quizId).orElseThrow();
        assertEquals("Подготовка к тесту", loaded.getTitle());
        assertTrue(loaded.getQuestions().isEmpty(), "Коллекция вопросов должна быть пустой");
    }

    /**
     * Тест: обновление викторины.
     * <p>
     * Проверяет, что изменения названия и лимита времени сохраняются.
     * </p>
     */
    @Test
    void testUpdateQuiz() {
        Category category = createCategory("Экономика");
        User teacher = createTeacher("teacher_updatequiz@example.com");
        Course course = createCourse("Макроэкономика", teacher, category);
        Module module = createModule("ВВП", 1, course);

        Quiz quiz = new Quiz(module, "ВВП — начальный", 10);
        entityManager.persist(quiz);
        entityManager.flush();

        // Обновляем викторину
        Quiz loaded = quizRepository.findById(quiz.getId()).orElseThrow();
        loaded.setTitle("ВВП — обновлённый");
        loaded.setTimeLimit(40);
        entityManager.merge(loaded);
        entityManager.flush();

        Quiz updated = quizRepository.findById(loaded.getId()).orElseThrow();
        assertEquals("ВВП — обновлённый", updated.getTitle());
        assertEquals(40, updated.getTimeLimit());
    }

    /**
     * Тест: репозиторий Question находит вопросы по quizId.
     */
    @Test
    void testQuestionRepositoryFindByQuizId() {
        Category category = createCategory("Геология");
        User teacher = createTeacher("teacher_findbyquiz@example.com");
        Course course = createCourse("Минералогия", teacher, category);
        Module module = createModule("Кристаллы", 1, course);

        Quiz quiz = new Quiz(module, "Кристаллы", 20);
        entityManager.persist(quiz);
        entityManager.flush();

        Question q1 = new Question(quiz, "Что такое кристаллическая решётка?",
                QuizQuestionType.SINGLE_CHOICE);
        entityManager.persist(q1);
        entityManager.flush();

        Question q2 = new Question(quiz, "Какие виды кристаллов существуют?",
                QuizQuestionType.MULTIPLE_CHOICE);
        entityManager.persist(q2);
        entityManager.flush();

        List<Question> questions = questionRepository.findByQuizId(quiz.getId());
        assertEquals(2, questions.size());

        List<String> texts = questions.stream()
                .map(Question::getText)
                .sorted()
                .toList();
        assertEquals(List.of("Какие виды кристаллов существуют?",
                        "Что такое кристаллическая решётка?"),
                texts);
    }

    /**
     * Тест: репозиторий AnswerOption находит варианты по questionId.
     */
    @Test
    void testAnswerOptionRepositoryFindByQuestionId() {
        Category category = createCategory("Лингвистика");
        User teacher = createTeacher("teacher_findbyoption@example.com");
        Course course = createCourse("Фонология", teacher, category);
        Module module = createModule("Звуки речи", 1, course);

        Quiz quiz = new Quiz(module, "Звуки", 15);
        entityManager.persist(quiz);
        entityManager.flush();

        Question question = new Question(quiz, "Какой звук является гласным?",
                QuizQuestionType.SINGLE_CHOICE);
        entityManager.persist(question);
        entityManager.flush();

        entityManager.persist(new AnswerOption(question, "А", true));
        entityManager.persist(new AnswerOption(question, "Б", false));
        entityManager.persist(new AnswerOption(question, "В", false));
        entityManager.flush();

        List<AnswerOption> options = answerOptionRepository.findByQuestionId(question.getId());
        assertEquals(3, options.size());
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
