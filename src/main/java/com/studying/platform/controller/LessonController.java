package com.studying.platform.controller;

import com.studying.platform.dto.LessonRequestDto;
import com.studying.platform.dto.LessonResponseDto;
import com.studying.platform.entity.Lesson;
import com.studying.platform.service.CourseService;

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

/**
 * REST-контроллер для управления уроками модуля.
 * <p>
 * Предоставляет эндпоинты для создания, чтения, обновления и удаления уроков.
 * Создание урока выполняется через {@code CourseController} в контексте модуля.
 * </p>
 *
 * @see CourseService
 */
@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    /**
     * Сервис для бизнес-логики, связанной с курсами и уроками.
     */
    private final CourseService courseService;

    /**
     * Конструктор контроллера с внедрением зависимостей через конструктор.
     *
     * @param courseService сервис курсов
     */
    public LessonController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * Получает урок по идентификатору.
     * <p>
     * Возвращает DTO с данными урока или ошибку 404, если урок не найден.
     * </p>
     *
     * @param lessonId идентификатор урока
     * @return HTTP 200 с DTO урока
     */
    @GetMapping("/{lessonId}")
    public ResponseEntity<LessonResponseDto> getLesson(@PathVariable Long lessonId) {
        Lesson lesson = courseService.findLesson(lessonId);
        LessonResponseDto response = toResponseDto(lesson);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновляет существующий урок.
     * <p>
     * Принимает DTO с не-null полями. Обновляет только указанные поля:
     * название, содержание, ссылка на видео.
     * </p>
     *
     * @param lessonId идентификатор урока
     * @param dto      данные для обновления
     * @return HTTP 200 с обновлённым DTO урока
     */
    @PutMapping("/{lessonId}")
    public ResponseEntity<LessonResponseDto> updateLesson(
            @PathVariable Long lessonId,
            @Valid @RequestBody LessonRequestDto dto) {
        Lesson lesson = courseService.updateLesson(
                lessonId,
                dto.getTitle(),
                dto.getContent(),
                dto.getVideoUrl()
        );
        LessonResponseDto response = toResponseDto(lesson);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаляет урок по идентификатору.
     * <p>
     * Удаляет урок из базы данных. Если у урока есть связанные задания
     * или решения, удаление может вызвать ошибку на уровне базы данных.
     * </p>
     *
     * @param lessonId идентификатор урока для удаления
     * @return HTTP 204 No Content при успешном удалении
     */
    @DeleteMapping("/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long lessonId) {
        courseService.deleteLesson(lessonId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Создаёт новый урок в указанном модуле.
     * <p>
     * Принимает DTO с валидацией. Создаёт урок через сервисный слой.
     * </p>
     *
     * @param moduleId идентификатор модуля
     * @param dto      данные для создания урока
     * @return HTTP 201 с DTO созданного урока
     */
    @PostMapping("/modules/{moduleId}/lessons")
    public ResponseEntity<LessonResponseDto> createLesson(
            @PathVariable Long moduleId,
            @Valid @RequestBody LessonRequestDto dto) {
        Lesson lesson = courseService.addLesson(
                moduleId,
                dto.getTitle(),
                dto.getContent(),
                dto.getVideoUrl()
        );
        LessonResponseDto response = toResponseDto(lesson);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Преобразует сущность урока в DTO ответа.
     * <p>
     * Копирует идентификатор, название, содержание, ссылку на видео
     * и идентификатор модуля.
     * </p>
     *
     * @param lesson сущность урока
     * @return DTO ответа с данными урока
     */
    private LessonResponseDto toResponseDto(Lesson lesson) {
        return new LessonResponseDto(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getContent(),
                lesson.getVideoUrl(),
                lesson.getModule().getId()
        );
    }
}
