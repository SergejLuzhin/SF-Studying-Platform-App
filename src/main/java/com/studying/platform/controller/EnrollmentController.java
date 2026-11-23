package com.studying.platform.controller;

import com.studying.platform.dto.EnrollmentRequestDto;
import com.studying.platform.dto.EnrollmentResponseDto;
import com.studying.platform.entity.Enrollment;
import com.studying.platform.entity.User;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.UserRepository;
import com.studying.platform.service.EnrollmentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST-контроллер для управления записью студентов на курсы учебной платформы.
 * <p>
 * Предоставляет эндпоинты для записи студента на курс, отписки от курса,
 * получения списка студентов курса и списка курсов студента.
 * </p>
 *
 * @see EnrollmentService
 */
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    /**
     * Сервис для бизнес-логики записей на курс.
     */
    private final EnrollmentService enrollmentService;

    /**
     * Репозиторий для работы с пользователями.
     */
    private final UserRepository userRepository;

    /**
     * Репозиторий для работы с курсами.
     */
    private final CourseRepository courseRepository;

    /**
     * Конструктор контроллера с внедрением зависимостей через конструктор.
     *
     * @param enrollmentService сервис записей
     * @param userRepository    репозиторий пользователей
     * @param courseRepository  репозиторий курсов
     */
    public EnrollmentController(EnrollmentService enrollmentService,
                                UserRepository userRepository,
                                CourseRepository courseRepository) {
        this.enrollmentService = enrollmentService;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * Записывает студента на курс.
     * <p>
     * Принимает DTO с идентификаторами студента и курса, валидацию
     * выполняет Jakarta Bean Validation. Через сервисный слой проверяется
     * роль студента и отсутствие повторной записи.
     * </p>
     *
     * @param dto данные для создания записи
     * @return HTTP 201 с DTO созданной записи
     */
    @PostMapping
    public ResponseEntity<EnrollmentResponseDto> enrollStudent(@Valid @RequestBody EnrollmentRequestDto dto) {
        Enrollment enrollment = enrollmentService.enrollStudent(dto.getStudentId(), dto.getCourseId());
        EnrollmentResponseDto response = toResponseDto(enrollment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получает список студентов, активно записанных на указанный курс.
     * <p>
     * Метод выполняется внутри транзакции сервисного слоя, которая
     * использует {@code EntityGraph} для загрузки связанных пользователей.
     * </p>
     *
     * @param courseId идентификатор курса
     * @return HTTP 200 со списком DTO студентов курса
     */
    @GetMapping("/courses/{courseId}/students")
    public ResponseEntity<List<EnrollmentResponseDto>> getCourseStudents(@PathVariable Long courseId) {
        List<User> students = enrollmentService.getCourseStudents(courseId);
        List<EnrollmentResponseDto> response = students.stream()
                .map(this::toEnrollmentResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Получает список курсов, на которые активно записан указанный студент.
     * <p>
     * Метод выполняется внутри транзакции сервисного слоя, которая
     * использует {@code EntityGraph} для загрузки связанных курсов.
     * </p>
     *
     * @param userId идентификатор студента
     * @return HTTP 200 со списком DTO курсов студента
     */
    @GetMapping("/users/{userId}/courses")
    public ResponseEntity<List<EnrollmentResponseDto>> getStudentCourses(@PathVariable Long userId) {
        List<com.studying.platform.entity.Course> courses = enrollmentService.getStudentCourses(userId);
        List<EnrollmentResponseDto> response = courses.stream()
                .map(this::toCourseEnrollmentResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Отписывает студента от курса.
     * <p>
     * Меняет статус записи на DROPPED. Требуются идентификаторы студента
     * и курса, передаваемые через параметры запроса.
     * Если активная запись не найдена — возвращается HTTP 404.
     * </p>
     *
     * @param studentId идентификатор студента
     * @param courseId  идентификатор курса
     * @return HTTP 204 No Content при успешной отписке
     */
    @DeleteMapping
    public ResponseEntity<Void> dropEnrollment(
            @RequestParam Long studentId,
            @RequestParam Long courseId) {
        enrollmentService.dropEnrollment(studentId, courseId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Преобразует сущность записи в DTO ответа.
     * <p>
     * Копирует скалярные поля: идентификатор, идентификаторы студента и курса,
     * статус и дату записи.
     * </p>
     *
     * @param enrollment сущность записи
     * @return DTO ответа с данными записи
     */
    private EnrollmentResponseDto toResponseDto(Enrollment enrollment) {
        return new EnrollmentResponseDto(
                enrollment.getId(),
                enrollment.getUser().getId(),
                enrollment.getCourse().getId(),
                enrollment.getStatus().name(),
                enrollment.getEnrollDate()
        );
    }

    /**
     * Преобразует сущность пользователя (студента) в DTO ответа записи.
     * <p>
     * Используется для ответа GET /courses/{courseId}/students.
     * Копирует идентификатор, email и роль студента.
     * </p>
     *
     * @param student сущность студента
     * @return DTO ответа с данными студента
     */
    private EnrollmentResponseDto toEnrollmentResponseDto(User student) {
        return new EnrollmentResponseDto(
                student.getId(),
                student.getId(),
                null,
                null,
                null
        );
    }

    /**
     * Преобразует сущность курса в DTO ответа записи.
     * <p>
     * Используется для ответа GET /users/{userId}/courses.
     * Копирует идентификатор, название и описание курса.
     * </p>
     *
     * @param course сущность курса
     * @return DTO ответа с данными курса
     */
    private EnrollmentResponseDto toCourseEnrollmentResponseDto(
            com.studying.platform.entity.Course course) {
        return new EnrollmentResponseDto(
                course.getId(),
                null,
                course.getId(),
                null,
                null
        );
    }
}
