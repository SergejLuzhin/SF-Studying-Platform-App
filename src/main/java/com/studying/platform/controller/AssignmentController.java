package com.studying.platform.controller;

import com.studying.platform.dto.AssignmentRequestDto;
import com.studying.platform.dto.AssignmentResponseDto;
import com.studying.platform.entity.Assignment;
import com.studying.platform.repository.LessonRepository;
import com.studying.platform.service.AssignmentService;

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
 * REST-контроллер для управления учебными заданиями.
 * <p>
 * Предоставляет эндпоинты для создания, чтения, обновления и удаления заданий,
 * привязанных к урокам. Эндпоинты создания/списка заданий находятся на уровне
 * урока, а CRUD-операции с отдельными заданиями — на уровне заданий.
 * </p>
 *
 * @see AssignmentService
 */
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    /**
     * Сервис для бизнес-логики заданий.
     */
    private final AssignmentService assignmentService;

    /**
     * Репозиторий для работы с уроками.
     */
    private final LessonRepository lessonRepository;

    /**
     * Конструктор контроллера с внедрением зависимостей через конструктор.
     *
     * @param assignmentService сервис заданий
     * @param lessonRepository  репозиторий уроков
     */
    public AssignmentController(AssignmentService assignmentService,
                                LessonRepository lessonRepository) {
        this.assignmentService = assignmentService;
        this.lessonRepository = lessonRepository;
    }

    /**
     * Создаёт новое задание для указанного урока.
     * <p>
     * Принимает DTO с валидацией. Создаёт задание через сервисный слой,
     * проверяя существование урока. Если необязательный lessonId в теле указан,
     * он должен совпадать с идентификатором в пути; иначе возвращается HTTP 400.
     * </p>
     *
     * @param lessonId идентификатор урока из пути, определяющий родителя задания
     * @param dto      данные задания
     * @return HTTP 201 с DTO созданного задания
     */
    @PostMapping("/lessons/{lessonId}/assignments")
    public ResponseEntity<AssignmentResponseDto> createAssignment(
            @PathVariable Long lessonId,
            @Valid @RequestBody AssignmentRequestDto dto) {
        if (dto.getLessonId() != null && !lessonId.equals(dto.getLessonId())) {
            throw new IllegalArgumentException("Идентификатор урока в теле должен совпадать с путём");
        }
        Assignment assignment = assignmentService.createAssignment(
                lessonId,
                dto.getTitle(),
                dto.getDescription(),
                dto.getDueDate(),
                dto.getMaxScore()
        );
        AssignmentResponseDto response = toResponseDto(assignment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получает задание по идентификатору.
     * <p>
     * Возвращает DTO с данными задания или ошибку 404, если задание не найдено.
     * </p>
     *
     * @param assignmentId идентификатор задания
     * @return HTTP 200 с DTO задания
     */
    @GetMapping("/{assignmentId}")
    public ResponseEntity<AssignmentResponseDto> getAssignment(@PathVariable Long assignmentId) {
        Assignment assignment = assignmentService.findAssignment(assignmentId);
        AssignmentResponseDto response = toResponseDto(assignment);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновляет данные существующего задания.
     * <p>
     * Название обязательно; сервис обновляет остальные указанные не-null поля.
     * Необязательный lessonId из тела игнорируется и не меняет родителя задания.
     * </p>
     *
     * @param assignmentId идентификатор задания
     * @param dto          данные для обновления
     * @return HTTP 200 с обновлённым DTO задания
     */
    @PutMapping("/{assignmentId}")
    public ResponseEntity<AssignmentResponseDto> updateAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentRequestDto dto) {
        Assignment assignment = assignmentService.updateAssignment(
                assignmentId,
                dto.getTitle(),
                dto.getDescription(),
                dto.getDueDate(),
                dto.getMaxScore()
        );
        AssignmentResponseDto response = toResponseDto(assignment);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаляет задание по идентификатору.
     * <p>
     * Удаляет задание из базы данных. Если у задания есть сохранённые решения,
     * удаление задания не затрагивает их (каскады не настроены).
     * </p>
     *
     * @param assignmentId идентификатор задания для удаления
     * @return HTTP 204 No Content при успешном удалении
     */
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long assignmentId) {
        assignmentService.deleteAssignment(assignmentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получает список всех заданий указанного урока.
     * <p>
     * Проверяет существование урока. Если урок найден, но заданий нет —
     * возвращается пустой список.
     * </p>
     *
     * @param lessonId идентификатор урока
     * @return HTTP 200 со списком DTO заданий урока
     */
    @GetMapping("/lessons/{lessonId}/assignments")
    public ResponseEntity<List<AssignmentResponseDto>> getAssignmentsByLesson(
            @PathVariable Long lessonId) {
        List<Assignment> assignments = assignmentService.getAssignmentsByLesson(lessonId);
        List<AssignmentResponseDto> response = assignments.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Преобразует сущность задания в DTO ответа.
     * <p>
     * Копирует идентификатор, идентификатор урока, название, описание,
     * срок выполнения и максимальный балл.
     * </p>
     *
     * @param assignment сущность задания
     * @return DTO ответа с данными задания
     */
    private AssignmentResponseDto toResponseDto(Assignment assignment) {
        return new AssignmentResponseDto(
                assignment.getId(),
                assignment.getLesson().getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueDate(),
                assignment.getMaxScore()
        );
    }
}
