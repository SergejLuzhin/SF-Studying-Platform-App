package com.studying.platform.controller;

import com.studying.platform.dto.AnswerOptionRequestDto;
import com.studying.platform.dto.AnswerOptionResponseDto;
import com.studying.platform.dto.QuestionRequestDto;
import com.studying.platform.dto.QuestionResponseDto;
import com.studying.platform.entity.AnswerOption;
import com.studying.platform.entity.Question;
import com.studying.platform.entity.Quiz;
import com.studying.platform.repository.QuizRepository;
import com.studying.platform.service.AnswerOptionService;
import com.studying.platform.service.QuestionService;
import com.studying.platform.service.QuizService;

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
 * REST-контроллер для управления вопросами и вариантами ответов викторины.
 * <p>
 * Предоставляет эндпоинты для создания, чтения, обновления и удаления вопросов,
 * а также создания, чтения, обновления и удаления вариантов ответов.
 * </p>
 *
 * @see QuestionService
 * @see AnswerOptionService
 */
@RestController
@RequestMapping("/api/question")
public class QuestionController {

    /**
     * Сервис для бизнес-логики вопросов.
     */
    private final QuestionService questionService;

    /**
     * Сервис для бизнес-логики вариантов ответов.
     */
    private final AnswerOptionService answerOptionService;

    /**
     * Репозиторий для работы с викторинами.
     */
    private final QuizRepository quizRepository;

    /**
     * Конструктор контроллера с внедрением зависимостей через конструктор.
     *
     * @param questionService      сервис вопросов
     * @param answerOptionService  сервис вариантов ответов
     * @param quizRepository       репозиторий викторин
     */
    public QuestionController(QuestionService questionService,
                              AnswerOptionService answerOptionService,
                              QuizRepository quizRepository) {
        this.questionService = questionService;
        this.answerOptionService = answerOptionService;
        this.quizRepository = quizRepository;
    }

    /**
     * Создаёт вопрос в указанной викторине.
     * <p>
     * Проверяет существование викторины, создаёт вопрос через сервисный слой.
     * </p>
     *
     * @param quizId идентификатор викторины
     * @param dto    данные вопроса
     * @return HTTP 201 с DTO созданного вопроса
     */
    @PostMapping("/quiz/{quizId}/questions")
    public ResponseEntity<QuestionResponseDto> createQuestion(
            @PathVariable Long quizId,
            @Valid @RequestBody QuestionRequestDto dto) {
        Question question = questionService.createQuestion(quizId, dto.getText(), dto.getType());
        QuestionResponseDto response = toResponseDto(question);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получает вопрос по идентификатору.
     * <p>
     * Возвращает DTO с данными вопроса или ошибку 404, если вопрос не найден.
     * </p>
     *
     * @param questionId идентификатор вопроса
     * @return HTTP 200 с DTO вопроса
     */
    @GetMapping("/{questionId}")
    public ResponseEntity<QuestionResponseDto> getQuestion(@PathVariable Long questionId) {
        Question question = questionService.findQuestion(questionId);
        QuestionResponseDto response = toResponseDto(question);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновляет данные существующего вопроса.
     * <p>
     * Принимает DTO с текстом и типом вопроса. Обновляет только указанные поля.
     * </p>
     *
     * @param questionId идентификатор вопроса
     * @param dto        данные для обновления
     * @return HTTP 200 с обновлённым DTO вопроса
     */
    @PutMapping("/{questionId}")
    public ResponseEntity<QuestionResponseDto> updateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionRequestDto dto) {
        Question question = questionService.updateQuestion(questionId, dto.getText(), dto.getType());
        QuestionResponseDto response = toResponseDto(question);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаляет вопрос по идентификатору.
     * <p>
     * Связанные варианты ответов не удаляются автоматически
     * (каскад настроен только на PERSIST).
     * </p>
     *
     * @param questionId идентификатор вопроса для удаления
     * @return HTTP 204 No Content при успешном удалении
     */
    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long questionId) {
        questionService.deleteQuestion(questionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Создаёт вариант ответа для указанного вопроса.
     * <p>
     * Проверяет существование вопроса и создаёт вариант через сервисный слой.
     * </p>
     *
     * @param questionId идентификатор вопроса
     * @param dto        данные варианта ответа
     * @return HTTP 201 с DTO созданного варианта
     */
    @PostMapping("/{questionId}/options")
    public ResponseEntity<AnswerOptionResponseDto> createAnswerOption(
            @PathVariable Long questionId,
            @Valid @RequestBody AnswerOptionRequestDto dto) {
        AnswerOption option = answerOptionService.createAnswerOption(questionId, dto.getText(), dto.getIsCorrect());
        AnswerOptionResponseDto response = toOptionResponseDto(option);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получает вариант ответа по идентификатору.
     * <p>
     * Возвращает DTO с данными варианта или ошибку 404, если вариант не найден.
     * </p>
     *
     * @param optionId идентификатор варианта ответа
     * @return HTTP 200 с DTO варианта
     */
    @GetMapping("/{optionId}/options")
    public ResponseEntity<AnswerOptionResponseDto> getAnswerOption(@PathVariable Long optionId) {
        AnswerOption option = answerOptionService.findAnswerOption(optionId);
        AnswerOptionResponseDto response = toOptionResponseDto(option);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновляет данные существующего варианта ответа.
     * <p>
     * Принимает DTO с текстом и признаком правильности варианта.
     * </p>
     *
     * @param optionId идентификатор варианта
     * @param dto      данные для обновления
     * @return HTTP 200 с обновлённым DTO варианта
     */
    @PutMapping("/{optionId}/options")
    public ResponseEntity<AnswerOptionResponseDto> updateAnswerOption(
            @PathVariable Long optionId,
            @Valid @RequestBody AnswerOptionRequestDto dto) {
        AnswerOption option = answerOptionService.updateAnswerOption(optionId, dto.getText(), dto.getIsCorrect());
        AnswerOptionResponseDto response = toOptionResponseDto(option);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаляет вариант ответа по идентификатору.
     *
     * @param optionId идентификатор варианта для удаления
     * @return HTTP 204 No Content при успешном удалении
     */
    @DeleteMapping("/{optionId}/options")
    public ResponseEntity<Void> deleteAnswerOption(@PathVariable Long optionId) {
        answerOptionService.deleteAnswerOption(optionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Преобразует сущность вопроса в DTO ответа.
     * <p>
     * Копирует скалярные поля, включая идентификатор викторины и количество вариантов.
     * </p>
     *
     * @param question сущность вопроса
     * @return DTO ответа с данными вопроса
     */
    private QuestionResponseDto toResponseDto(Question question) {
        return new QuestionResponseDto(
                question.getId(),
                question.getQuiz().getId(),
                question.getText(),
                question.getType(),
                question.getOptions().size()
        );
    }

    /**
     * Преобразует сущность варианта ответа в DTO ответа.
     * <p>
     * Копирует идентификатор, идентификатор вопроса, текст и признак правильности.
     * </p>
     *
     * @param option сущность варианта ответа
     * @return DTO ответа с данными варианта
     */
    private AnswerOptionResponseDto toOptionResponseDto(AnswerOption option) {
        return new AnswerOptionResponseDto(
                option.getId(),
                option.getQuestion().getId(),
                option.getText(),
                option.getIsCorrect()
        );
    }
}
