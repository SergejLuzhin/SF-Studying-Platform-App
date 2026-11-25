package com.studying.platform.controller;

import com.studying.platform.dto.GradeSubmissionRequestDto;
import com.studying.platform.dto.SubmissionRequestDto;
import com.studying.platform.dto.SubmissionResponseDto;
import com.studying.platform.entity.Submission;
import com.studying.platform.service.SubmissionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST-контроллер для управления сдачей решений учебных заданий.
 * <p>
 * Предоставляет эндпоинты для отправки решений студентами,
 * оценивания решений преподавателями и получения списков решений.
 * </p>
 *
 * @see SubmissionService
 */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    /**
     * Сервис для бизнес-логики решений.
     */
    private final SubmissionService submissionService;

    /**
     * Конструктор контроллера с внедрением зависимостей через конструктор.
     *
     * @param submissionService сервис решений
     */
    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /**
     * Отправляет решение студента на учебное задание.
     * <p>
     * Принимает DTO с валидацией. Через сервисный слой проверяется
     * существование задания, запись студента на курс и отсутствие
     * повторной отправки.
     * </p>
     *
     * @param dto данные для создания решения
     * @return HTTP 201 с DTO созданного решения
     */
    @PostMapping
    public ResponseEntity<SubmissionResponseDto> submitSolution(
            @Valid @RequestBody SubmissionRequestDto dto) {
        Submission submission = submissionService.submitSolution(
                dto.getAssignmentId(),
                dto.getStudentId(),
                dto.getContent()
        );
        SubmissionResponseDto response = toResponseDto(submission);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Выставляет оценку и комментарий преподавателя к решению.
     * <p>
     * Принимает DTO с оценкой и необязательным комментарием.
     * Идентификатор преподавателя передаётся как параметр запроса.
     * Через сервисный слой проверяется роль преподавателя и
     * допустимый диапазон оценки.
     * После обновления решения возвращается его DTO.
     * </p>
     *
     * @param submissionId идентификатор решения
     * @param teacherId    идентификатор преподавателя
     * @param dto          данные для оценки
     * @return HTTP 200 с DTO обновлённого решения
     */
    @PatchMapping("/{submissionId}")
    public ResponseEntity<SubmissionResponseDto> gradeSubmission(
            @PathVariable Long submissionId,
            @RequestParam Long teacherId,
            @Valid @RequestBody GradeSubmissionRequestDto dto) {
        submissionService.gradeSubmission(
                submissionId,
                dto.getScore(),
                dto.getFeedback(),
                teacherId
        );
        Submission submission = submissionService.findSubmissionById(submissionId);
        SubmissionResponseDto response = toResponseDto(submission);
        return ResponseEntity.ok(response);
    }

    /**
     * Возвращает список всех решений указанного задания.
     * <p>
     * Проверяет существование задания. Если задание не найдено —
     * выбрасывается ошибка 404.
     * </p>
     *
     * @param assignmentId идентификатор задания
     * @return HTTP 200 со списком DTO решений
     */
    @GetMapping("/assignments/{assignmentId}/submissions")
    public ResponseEntity<List<SubmissionResponseDto>> getSubmissionsByAssignment(
            @PathVariable Long assignmentId) {
        List<Submission> submissions = submissionService.getSubmissionsByAssignment(assignmentId);
        List<SubmissionResponseDto> response = submissions.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Возвращает список всех решений указанного студента.
     * <p>
     * Проверяет существование студента. Если студент не найден —
     * выбрасывается ошибка 404.
     * </p>
     *
     * @param userId идентификатор студента
     * @return HTTP 200 со списком DTO решений студента
     */
    @GetMapping("/users/{userId}/submissions")
    public ResponseEntity<List<SubmissionResponseDto>> getSubmissionsByStudent(
            @PathVariable Long userId) {
        List<Submission> submissions = submissionService.getSubmissionsByStudent(userId);
        List<SubmissionResponseDto> response = submissions.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Преобразует сущность решения в DTO ответа.
     * <p>
     * Копирует идентификатор решения, идентификаторы задания и студента,
     * текст ответа, дату отправки, оценку и комментарий.
     * </p>
     *
     * @param submission сущность решения
     * @return DTO ответа с данными решения
     */
    private SubmissionResponseDto toResponseDto(Submission submission) {
        return new SubmissionResponseDto(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getStudent().getId(),
                submission.getContent(),
                submission.getSubmittedAt(),
                submission.getScore(),
                submission.getFeedback()
        );
    }
}
