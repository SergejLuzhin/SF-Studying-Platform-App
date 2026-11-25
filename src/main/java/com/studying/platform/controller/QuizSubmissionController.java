package com.studying.platform.controller;

import com.studying.platform.dto.QuizSubmissionResponseDto;
import com.studying.platform.entity.QuizSubmission;
import com.studying.platform.service.QuizSubmissionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST-контроллер для просмотра результатов прохождения викторин.
 * <p>
 * Предоставляет эндпоинты для получения результата теста по идентификатору,
 * а также списков результатов по студенту и викторине.
 * </p>
 *
 * @see com.studying.platform.entity.QuizSubmission
 */
@RestController
@RequestMapping("/api/submissions/quiz")
public class QuizSubmissionController {

    /**
     * Сервис для прохождения викторин.
     */
    private final QuizSubmissionService quizSubmissionService;

    /**
     * Конструктор контроллера с внедрением зависимостей через конструктор.
     *
     * @param quizSubmissionService сервис прохождения викторин
     */
    public QuizSubmissionController(QuizSubmissionService quizSubmissionService) {
        this.quizSubmissionService = quizSubmissionService;
    }

    /**
     * Получает результат прохождения викторины по идентификатору.
     * <p>
     * Возвращает DTO с результатом или ошибку 404, если результат не найден.
     * </p>
     *
     * @param submissionId идентификатор результата
     * @return HTTP 200 с DTO результата
     */
    @GetMapping("/{submissionId}")
    public ResponseEntity<QuizSubmissionResponseDto> getQuizSubmission(
            @PathVariable Long submissionId) {
        QuizSubmission submission = quizSubmissionService.findQuizSubmissionById(submissionId);
        QuizSubmissionResponseDto response = toResponseDto(submission);
        return ResponseEntity.ok(response);
    }

    /**
     * Получает список результатов указанного студента.
     * <p>
     * Метод выполняется внутри транзакции сервисного слоя.
     * </p>
     *
     * @param studentId идентификатор студента
     * @return HTTP 200 со списком DTO результатов студента
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<QuizSubmissionResponseDto>> getSubmissionsByStudent(
            @PathVariable Long studentId) {
        List<QuizSubmission> submissions = quizSubmissionService.getSubmissionsByStudent(studentId);
        List<QuizSubmissionResponseDto> response = submissions.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Получает список результатов указанной викторины.
     * <p>
     * Метод выполняется внутри транзакции сервисного слоя.
     * </p>
     *
     * @param quizId идентификатор викторины
     * @return HTTP 200 со списком DTO результатов викторины
     */
    @GetMapping("/quiz/{quizId}")
    public ResponseEntity<List<QuizSubmissionResponseDto>> getSubmissionsByQuiz(
            @PathVariable Long quizId) {
        List<QuizSubmission> submissions = quizSubmissionService.getSubmissionsByQuiz(quizId);
        List<QuizSubmissionResponseDto> response = submissions.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Преобразует сущность результата в DTO ответа.
     * <p>
     * Копирует идентификатор, идентификаторы викторины и студента, балл и дату.
     * </p>
     *
     * @param submission сущность результата
     * @return DTO результата
     */
    private QuizSubmissionResponseDto toResponseDto(QuizSubmission submission) {
        return new QuizSubmissionResponseDto(
                submission.getId(),
                submission.getQuiz().getId(),
                submission.getStudent().getId(),
                submission.getScore(),
                submission.getTakenAt()
        );
    }
}
