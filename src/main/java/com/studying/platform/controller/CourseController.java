package com.studying.platform.controller;

import com.studying.platform.dto.CourseRequestDto;
import com.studying.platform.dto.CourseResponseDto;
import com.studying.platform.dto.CourseStructureResponseDto;
import com.studying.platform.dto.ModuleRequestDto;
import com.studying.platform.dto.ModuleResponseDto;
import com.studying.platform.entity.Category;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Module;
import com.studying.platform.repository.CategoryRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST-контроллер для управления курсами учебной платформы.
 * <p>
 * Предоставляет эндпоинты для создания, чтения, обновления, удаления курсов,
 * получения полной структуры курса с модулями и уроками, а также
 * добавления модулей к курсу.
 * </p>
 *
 * @see CourseService
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    /**
     * Сервис для бизнес-логики, связанной с курсами.
     */
    private final CourseService courseService;

    /**
     * Репозиторий для работы с категориями.
     */
    private final CategoryRepository categoryRepository;

    /**
     * Конструктор контроллера с внедрением зависимостей через конструктор.
     *
     * @param courseService    сервис курсов
     * @param categoryRepository репозиторий категорий
     */
    public CourseController(CourseService courseService, CategoryRepository categoryRepository) {
        this.courseService = courseService;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Создаёт новый курс.
     * <p>
     * Принимает DTO с валидацией, создаёт курс через сервисный слой,
     * проверяя существование преподавателя (роль TEACHER) и категории.
     * </p>
     *
     * @param dto данные для создания курса
     * @return HTTP 201 с DTO созданного курса
     */
    @PostMapping
    public ResponseEntity<CourseResponseDto> createCourse(@Valid @RequestBody CourseRequestDto dto) {
        Course course = courseService.createCourse(
                dto.getTitle(),
                dto.getDescription(),
                dto.getDuration(),
                dto.getStartDate(),
                dto.getTeacherId(),
                dto.getCategoryId()
        );
        CourseResponseDto response = toResponseDto(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получает курс по идентификатору.
     * <p>
     * Возвращает DTO с данными курса или ошибку 404, если курс не найден.
     * </p>
     *
     * @param courseId идентификатор курса
     * @return HTTP 200 с DTO курса
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponseDto> getCourse(@PathVariable Long courseId) {
        Course course = courseService.findCourse(courseId);
        CourseResponseDto response = toResponseDto(course);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновляет данные существующего курса.
     * <p>
     * Выполняет полное обновление: обязательны название, длительность, дата начала,
     * идентификаторы преподавателя и категории; описание может быть {@code null}.
     * Проверяет роль преподавателя и сохраняет все поля в одной транзакции.
     * </p>
     *
     * @param courseId идентификатор курса
     * @param dto      данные для обновления
     * @return HTTP 200 с обновлённым DTO курса
     */
    @PutMapping("/{courseId}")
    public ResponseEntity<CourseResponseDto> updateCourse(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseRequestDto dto) {
        Course course = courseService.updateCourse(
                courseId,
                dto.getTitle(),
                dto.getDescription(),
                dto.getDuration(),
                dto.getStartDate(),
                dto.getTeacherId(),
                dto.getCategoryId()
        );
        CourseResponseDto response = toResponseDto(course);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаляет курс по идентификатору.
     * <p>
     * Проверяет отсутствие учебной истории (записи, решения, результаты тестов, отзывы).
     * При наличии истории выбрасывается ошибка 400.
     * </p>
     *
     * @param courseId идентификатор курса для удаления
     * @return HTTP 204 No Content при успешном удалении
     */
    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получает полную структуру курса с модулями и уроками.
     * <p>
     * Метод выполняется внутри транзакции, поэтому доступ к ленивым
     * коллекциям безопасен. Возвращает DTO с вложенной структурой.
     * </p>
     *
     * @param courseId идентификатор курса
     * @return HTTP 200 с DTO структуры курса
     */
    @GetMapping("/{courseId}/structure")
    public ResponseEntity<CourseStructureResponseDto> getCourseStructure(@PathVariable Long courseId) {
        Course course = courseService.getCourseStructure(courseId);
        CourseStructureResponseDto response = toStructureDto(course);
        return ResponseEntity.ok(response);
    }

    /**
     * Добавляет модуль к существующему курсу.
     * <p>
     * Проверяет существование курса и создаёт модуль через сервисный слой.
     * </p>
     *
     * @param courseId идентификатор курса
     * @param dto      данные модуля
     * @return HTTP 201 с DTO созданного модуля
     */
    @PostMapping("/{courseId}/modules")
    public ResponseEntity<ModuleResponseDto> addModuleToCourse(
            @PathVariable Long courseId,
            @Valid @RequestBody ModuleRequestDto dto) {
        Module module = courseService.addModule(
                courseId,
                dto.getTitle(),
                dto.getDescription(),
                dto.getOrderIndex()
        );
        ModuleResponseDto response = toModuleResponseDto(module);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Преобразует сущность курса в DTO ответа.
     * <p>
     * Копирует скалярные поля, включая название категории.
     * </p>
     *
     * @param course сущность курса
     * @return DTO ответа с данными курса
     */
    private CourseResponseDto toResponseDto(Course course) {
        String categoryName = null;
        Category category = course.getCategory();
        Long categoryId = null;
        if (category != null) {
            categoryName = category.getName();
            categoryId = category.getId();
        }
        return new CourseResponseDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getDuration(),
                course.getStartDate(),
                course.getTeacher().getId(),
                categoryId,
                categoryName
        );
    }

    /**
     * Преобразует сущность курса в DTO структуры курса.
     * <p>
     * Рекурсивно преобразует модули и уроки во вложенные DTO.
     * </p>
     *
     * @param course сущность курса
     * @return DTO структуры курса
     */
    private CourseStructureResponseDto toStructureDto(Course course) {
        List<CourseStructureResponseDto.ModuleStructureDto> moduleDtos = course.getModules().stream()
                .map(this::toModuleStructureDto)
                .collect(Collectors.toList());

        return new CourseStructureResponseDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getDuration(),
                course.getStartDate(),
                course.getTeacher().getId(),
                course.getCategory().getId(),
                moduleDtos
        );
    }

    /**
     * Преобразует сущность модуля в DTO структуры модуля.
     * <p>
     * Включает список уроков в формате DTO.
     * </p>
     *
     * @param module сущность модуля
     * @return DTO структуры модуля
     */
    private CourseStructureResponseDto.ModuleStructureDto toModuleStructureDto(Module module) {
        List<CourseStructureResponseDto.LessonStructureDto> lessonDtos = module.getLessons().stream()
                .map(lesson -> new CourseStructureResponseDto.LessonStructureDto(
                        lesson.getId(),
                        lesson.getTitle(),
                        lesson.getContent(),
                        lesson.getVideoUrl()
                ))
                .collect(Collectors.toList());

        return new CourseStructureResponseDto.ModuleStructureDto(
                module.getId(),
                module.getTitle(),
                module.getDescription(),
                module.getOrderIndex(),
                lessonDtos
        );
    }

    /**
     * Преобразует сущность модуля в DTO ответа.
     * <p>
     * Копирует идентификатор, название, описание, порядковый номер и идентификатор курса.
     * </p>
     *
     * @param module сущность модуля
     * @return DTO ответа с данными модуля
     */
    private ModuleResponseDto toModuleResponseDto(Module module) {
        return new ModuleResponseDto(
                module.getId(),
                module.getTitle(),
                module.getDescription(),
                module.getOrderIndex(),
                module.getCourse().getId()
        );
    }
}
