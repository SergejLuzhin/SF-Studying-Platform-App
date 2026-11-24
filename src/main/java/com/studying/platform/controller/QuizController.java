package com.studying.platform.controller;

import com.studying.platform.dto.QuestionRequestDto;
import com.studying.platform.dto.QuestionResponseDto;
import com.studying.platform.dto.QuizRequestDto;
import com.studying.platform.dto.QuizResponseDto;
import com.studying.platform.dto.StartQuizResponseDto;
import com.studying.platform.dto.StartQuizResponseDto.AnswerOptionDto;
import com.studying.platform.dto.StartQuizResponseDto.QuestionWithOptionsDto;
import com.studying.platform.dto.SubmitAnswersRequestDto;
import com.studying.platform.entity.AnswerOption;
import com.studying.platform.entity.Question;
import com.studying.platform.entity.Quiz;
import com.studying.platform.entity.QuizSubmission;
import com.studying.platform.service.QuizService;
import com.studying.platform.service.QuizSubmissionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST-контроллер для управления викторинами учебной платформы.
 * <p>
 * Предоставляет эндпоинты для создания, чтения, обновления и удаления викторин,
 * создания вопросов, прохождения тестов и получения тестов для прохождения.
 * </p>
 *
 * @see QuizService
 * @see QuizSubmissionService
 */
@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    /**
     * Сервис для бизнес-логики викторин.
     */
    private final QuizService quizService;

    /**
     * Сервис для прохождения викторин.
     */
    private final QuizSubmissionService quizSubmissionService;

    /**
     * Конструктор контроллера с внедрением зависимостей через конструктор.
     *
     * @param quizService          сервис викторин
     * @param quizSubmissionService сервис прохождения викторин
     */
    public QuizController(QuizService quizService,
                          QuizSubmissionService quizSubmissionService) {
        this.quizService = quizService;
        this.quizSubmissionService = quizSubmissionService;
    }

    /**
     * Создаёт викторину в указанном модуле.
     * <p>
     * Принимает DTO с валидацией, создаёт викторину через сервисный слой,
     * проверяя существование модуля и отсутствие другого теста в модуле.
     * </p>
     *
     * @param dto данные для создания викторины
     * @return HTTP 201 с DTO созданной викторины
     */
    @PostMapping
    public ResponseEntity<QuizResponseDto> createQuiz(@Valid @RequestBody QuizRequestDto dto) {
        Quiz quiz = quizService.createQuiz(dto.getModuleId(), dto.getTitle(), dto.getTimeLimit());
        QuizResponseDto response = toResponseDto(quiz);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получает викторину по идентификатору.
     * <p>
     * Возвращает DTO с данными викторины или ошибку 404, если викторина не найдена.
     * </p>
     *
     * @param quizId идентификатор викторины
     * @return HTTP 200 с DTO викторины
     */
    @GetMapping("/{quizId}")
    public ResponseEntity<QuizResponseDto> getQuiz(@PathVariable Long quizId) {
        Quiz quiz = quizService.findQuiz(quizId);
        QuizResponseDto response = toResponseDto(quiz);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновляет данные существующей викторины.
     * <p>
     * Принимает DTO с названием и лимитом времени. Обновляет только указанные поля.
     * </p>
     *
     * @param quizId идентификатор викторины
     * @param dto    данные для обновления
     * @return HTTP 200 с обновлённым DTO викторины
     */
    @PutMapping("/{quizId}")
    public ResponseEntity<QuizResponseDto> updateQuiz(
            @PathVariable Long quizId,
            @Valid @RequestBody QuizRequestDto dto) {
        Quiz quiz = quizService.updateQuiz(quizId, dto.getTitle(), dto.getTimeLimit());
        QuizResponseDto response = toResponseDto(quiz);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаляет викторину по идентификатору.
     * <p>
     * Проверяет отсутствие результатов прохождения теста. При наличии результатов —
     * выбрасывается ошибка 400.
     * </p>
     *
     * @param quizId идентификатор викторины для удаления
     * @return HTTP 204 No Content при успешном удалении
     */
    @DeleteMapping("/{quizId}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long quizId) {
        quizService.deleteQuiz(quizId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Создаёт вопрос в указанной викторине.
     * <p>
     * Принимает DTO с текстом и типом вопроса, создаёт вопрос через сервисный слой.
     * </p>
     *
     * @param quizId идентификатор викторины
     * @param dto    данные вопроса
     * @return HTTP 201 с DTO созданного вопроса
     */
    @PostMapping("/{quizId}/questions")
    public ResponseEntity<QuestionResponseDto> createQuestion(
            @PathVariable Long quizId,
            @Valid @RequestBody QuestionRequestDto dto) {
        Question question = quizService.createQuestion(quizId, dto.getText(), dto.getType());
        QuestionResponseDto response = toQuestionResponseDto(question);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получает список вопросов указанной викторины.
     * <p>
     * Метод выполняется внутри транзакции сервисного слоя.
     * </p>
     *
     * @param quizId идентификатор викторины
     * @return HTTP 200 со списком DTO вопросов
     */
    @GetMapping("/{quizId}/questions")
    public ResponseEntity<List<QuestionResponseDto>> getQuestionsByQuiz(@PathVariable Long quizId) {
        List<Question> questions = quizService.getQuestionsByQuiz(quizId);
        List<QuestionResponseDto> response = questions.stream()
                .map(this::toQuestionResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Получает викторину для прохождения.
     * <p>
     * Загружает викторину с вопросами и вариантами ответов через EntityGraph.
     * Признаки правильности вариантов в ответе не возвращаются. Аутентификация и
     * авторизация не реализованы; административные операции не защищены от доступа.
     * Если викторина не найдена — ошибка 404. Если тест без вопросов — ошибка 400.
     * </p>
     *
     * @param quizId идентификатор викторины
     * @return HTTP 200 с DTO викторины, готовой к прохождению
     */
    @GetMapping("/{quizId}/start")
    public ResponseEntity<StartQuizResponseDto> startQuiz(@PathVariable Long quizId) {
        Quiz quiz = quizSubmissionService.startQuiz(quizId);
        StartQuizResponseDto response = toStartQuizResponseDto(quiz);
        return ResponseEntity.ok(response);
    }

    /**
     * Отправляет ответы студента на викторину.
     * <p>
     * Проверяет права студента (активная запись на курс), вычисляет балл
     * и сохраняет результат. Чужие идентификаторы вопросов игнорируются.
     * </p>
     *
     * @param quizId идентификатор викторины
     * @param dto    данные с идентификатором студента и картой ответов
     * @return HTTP 201 с DTO результата прохождения теста
     */
    @PostMapping("/{quizId}/submit")
    public ResponseEntity<com.studying.platform.dto.QuizSubmissionResponseDto> submitAnswers(
            @PathVariable Long quizId,
            @Valid @RequestBody SubmitAnswersRequestDto dto) {
        QuizSubmission submission = quizSubmissionService.submitAnswers(
                quizId, dto.getStudentId(), dto.getAnswers());
        com.studying.platform.dto.QuizSubmissionResponseDto response =
                toSubmissionResponseDto(submission);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Преобразует сущность викторины в DTO ответа.
     * <p>
     * Копирует скалярные поля, включая идентификатор модуля и количество вопросов.
     * </p>
     *
     * @param quiz сущность викторины
     * @return DTO ответа с данными викторины
     */
    private QuizResponseDto toResponseDto(Quiz quiz) {
        return new QuizResponseDto(
                quiz.getId(),
                quiz.getModule().getId(),
                quiz.getTitle(),
                quiz.getTimeLimit(),
                quiz.getQuestions().size()
        );
    }

    /**
     * Преобразует сущность вопроса в DTO ответа.
     * <p>
     * Копирует идентификатор, идентификатор викторины, текст, тип и количество вариантов.
     * </p>
     *
     * @param question сущность вопроса
     * @return DTO ответа с данными вопроса
     */
    private QuestionResponseDto toQuestionResponseDto(Question question) {
        return new QuestionResponseDto(
                question.getId(),
                question.getQuiz().getId(),
                question.getText(),
                question.getType(),
                question.getOptions().size()
        );
    }

    /**
     * Преобразует сущность викторины в DTO для прохождения.
     * <p>
     * Включает вложенные вопросы с вариантами ответов.
     * Метод выполняется внутри транзакции, поэтому ленивые загрузки безопасны.
     * </p>
     *
     * @param quiz сущность викторины с вопросами и вариантами
     * @return DTO викторины для прохождения
     */
    private StartQuizResponseDto toStartQuizResponseDto(Quiz quiz) {
        List<QuestionWithOptionsDto> questionDtos = quiz.getQuestions().stream()
                .map(this::toQuestionWithOptionsDto)
                .collect(Collectors.toList());

        return new StartQuizResponseDto(
                quiz.getId(),
                quiz.getModule().getId(),
                quiz.getTitle(),
                quiz.getTimeLimit(),
                questionDtos
        );
    }

    /**
     * Преобразует сущность вопроса в DTO с идентификаторами и текстами вариантов без правильности.
     *
     * @param question сущность вопроса
     * @return DTO вопроса с вариантами
     */
    private QuestionWithOptionsDto toQuestionWithOptionsDto(Question question) {
        List<AnswerOptionDto> optionDtos = question.getOptions().stream()
                .map(option -> new AnswerOptionDto(
                        option.getId(),
                        option.getText()))
                .collect(Collectors.toList());

        return new QuestionWithOptionsDto(
                question.getId(),
                question.getText(),
                question.getType(),
                optionDtos
        );
    }

    /**
     * Преобразует сущность результата теста в DTO ответа.
     *
     * @param submission сущность результата
     * @return DTO результата прохождения теста
     */
    private com.studying.platform.dto.QuizSubmissionResponseDto toSubmissionResponseDto(
            QuizSubmission submission) {
        return new com.studying.platform.dto.QuizSubmissionResponseDto(
                submission.getId(),
                submission.getQuiz().getId(),
                submission.getStudent().getId(),
                submission.getScore(),
                submission.getTakenAt()
        );
    }
}
