package com.studying.platform.controller;

import com.studying.platform.dto.CourseReviewRequestDto;
import com.studying.platform.dto.CourseReviewResponseDto;
import com.studying.platform.entity.CourseReview;
import com.studying.platform.service.CourseReviewService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST-контроллер для управления отзывами студентов о курсах учебной платформы.
 * <p>
 * Предоставляет эндпоинты для создания отзыва студента, получения списка отзывов курса
 * и списка отзывов, оставленных студентом.
 * </p>
 *
 * @see CourseReviewService
 */
@RestController
@RequestMapping("/api/course-reviews")
public class CourseReviewController {

    /**
     * Сервис для бизнес-логики отзывов.
     */
    private final CourseReviewService courseReviewService;

    /**
     * Конструктор контроллера с внедрением зависимостей через конструктор.
     *
     * @param courseReviewService сервис отзывов
     */
    public CourseReviewController(CourseReviewService courseReviewService) {
        this.courseReviewService = courseReviewService;
    }

    /**
     * Создаёт отзыв студента о курсе.
     * <p>
     * Принимает DTO с валидацией, создаёт отзыв через сервисный слой,
     * проверяя роль студента, факт активной записи на курс, диапазон рейтинга
     * и уникальность отзыва.
     * </p>
     *
     * @param dto данные для создания отзыва
     * @return HTTP 201 с DTO созданного отзыва
     */
    @PostMapping
    public ResponseEntity<CourseReviewResponseDto> createReview(
            @Valid @RequestBody CourseReviewRequestDto dto) {
        CourseReview review = courseReviewService.createReview(
                dto.getStudentId(), dto.getCourseId(), dto.getRating(), dto.getComment());
        CourseReviewResponseDto response = toResponseDto(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получает список отзывов указанного курса.
     * <p>
     * Метод выполняется внутри транзакции сервисного слоя.
     * </p>
     *
     * @param courseId идентификатор курса
     * @return HTTP 200 со списком DTO отзывов курса
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<CourseReviewResponseDto>> getReviewsByCourse(
            @PathVariable Long courseId) {
        List<CourseReview> reviews = courseReviewService.getReviewsByCourse(courseId);
        List<CourseReviewResponseDto> response = reviews.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Получает список отзывов, оставленных указанным студентом.
     * <p>
     * Метод выполняется внутри транзакции сервисного слоя.
     * </p>
     *
     * @param studentId идентификатор студента
     * @return HTTP 200 со списком DTO отзывов студента
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<CourseReviewResponseDto>> getReviewsByStudent(
            @PathVariable Long studentId) {
        List<CourseReview> reviews = courseReviewService.getReviewsByStudent(studentId);
        List<CourseReviewResponseDto> response = reviews.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Преобразует сущность отзыва в DTO ответа.
     * <p>
     * Копирует скалярные поля, включая идентификаторы студента и курса.
     * </p>
     *
     * @param review сущность отзыва
     * @return DTO ответа с данными отзыва
     */
    private CourseReviewResponseDto toResponseDto(CourseReview review) {
        return new CourseReviewResponseDto(
                review.getId(),
                review.getStudent().getId(),
                review.getCourse().getId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
