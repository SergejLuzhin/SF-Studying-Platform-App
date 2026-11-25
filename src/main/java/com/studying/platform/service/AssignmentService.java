package com.studying.platform.service;

import com.studying.platform.entity.Assignment;
import com.studying.platform.entity.Lesson;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.AssignmentRepository;
import com.studying.platform.repository.LessonRepository;
import com.studying.platform.repository.SubmissionRepository;

import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для управления учебными заданиями.
 * <p>
 * Обеспечивает бизнес-логику создания, обновления, удаления и получения
 * заданий. Каждое задание привязывается к конкретному уроку.
 * Все публичные методы транзакционны.
 * </p>
 *
 * @see Assignment
 * @see Lesson
 */
@Service
@Transactional
public class AssignmentService {

    /**
     * Репозиторий для работы с сущностью «Задание».
     */
    private final AssignmentRepository assignmentRepository;

    /**
     * Репозиторий для работы с сущностью «Урок».
     */
    private final LessonRepository lessonRepository;

    /**
     * Репозиторий для работы с сущностью «Решение».
     */
    private final SubmissionRepository submissionRepository;

    /**
     * Менеджер сущностей для управления транзакциями и сброса контекста.
     */
    private final EntityManager entityManager;

    /**
     * Конструктор сервиса с внедрением зависимостей через конструктор.
     *
     * @param assignmentRepository репозиторий заданий
     * @param lessonRepository     репозиторий уроков
     * @param submissionRepository репозиторий решений
     * @param entityManager        менеджер сущностей JPA
     */
    public AssignmentService(AssignmentRepository assignmentRepository,
                             LessonRepository lessonRepository,
                             SubmissionRepository submissionRepository,
                             EntityManager entityManager) {
        this.assignmentRepository = assignmentRepository;
        this.lessonRepository = lessonRepository;
        this.submissionRepository = submissionRepository;
        this.entityManager = entityManager;
    }

    /**
     * Создаёт новое задание для указанного урока.
     * <p>
     * Проверяет существование урока. Если урок не найден —
     * выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param lessonId    идентификатор урока
     * @param title       название задания
     * @param description описание задания
     * @param dueDate     срок выполнения
     * @param maxScore    максимальный балл
     * @return сохранённое задание
     * @throws ResourceNotFoundException если урок не найден
     */
    public Assignment createAssignment(Long lessonId, String title, String description,
                                       LocalDate dueDate, Integer maxScore) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Урок с идентификатором " + lessonId + " не найден"));

        Assignment assignment = new Assignment(lesson, title, description, dueDate, maxScore);
        entityManager.persist(assignment);
        entityManager.flush();
        return assignment;
    }

    /**
     * Обновляет данные существующего задания.
     * <p>
     * Проверяет существование задания. Обновляет только не-null поля:
     * название, описание, срок выполнения и максимальный балл.
     * Если задание не найдено — выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param assignmentId идентификатор задания
     * @param title        новое название задания
     * @param description  новое описание задания
     * @param dueDate      новый срок выполнения
     * @param maxScore     новый максимальный балл
     * @return обновлённое задание
     * @throws ResourceNotFoundException если задание не найдено
     */
    public Assignment updateAssignment(Long assignmentId, String title, String description,
                                       LocalDate dueDate, Integer maxScore) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Задание с идентификатором " + assignmentId + " не найдено"));

        if (title != null) {
            assignment.setTitle(title);
        }
        if (description != null) {
            assignment.setDescription(description);
        }
        if (dueDate != null) {
            assignment.setDueDate(dueDate);
        }
        if (maxScore != null) {
            assignment.setMaxScore(maxScore);
        }

        entityManager.flush();
        return assignment;
    }

    /**
     * Удаляет задание по идентификатору.
     * <p>
     * Проверяет существование задания. Если задание не найдено —
     * выбрасывает {@link ResourceNotFoundException}.
     * Удаление задания не затрагивает сохранённые решения студентов —
     * каскады не настроены согласно требованиям проекта.
     * </p>
     *
     * @param assignmentId идентификатор задания для удаления
     * @throws ResourceNotFoundException если задание не найдено
     */
    public void deleteAssignment(Long assignmentId) {
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new ResourceNotFoundException(
                    "Задание с идентификатором " + assignmentId + " не найдено");
        }
        assignmentRepository.deleteById(assignmentId);
    }

    /**
     * Возвращает задание по идентификатору.
     * <p>
     * Проверяет существование задания. Если задание не найдено —
     * выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param assignmentId идентификатор задания
     * @return найденное задание
     * @throws ResourceNotFoundException если задание не найдено
     */
    public Assignment findAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Задание с идентификатором " + assignmentId + " не найдено"));
    }

    /**
     * Возвращает все задания указанного урока.
     * <p>
     * Проверяет существование урока. Если урок не найден —
     * выбрасывает {@link ResourceNotFoundException}.
     * Если заданий у урока нет — возвращает пустой список.
     * </p>
     *
     * @param lessonId идентификатор урока
     * @return список заданий урока
     * @throws ResourceNotFoundException если урок не найден
     */
    public List<Assignment> getAssignmentsByLesson(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new ResourceNotFoundException(
                    "Урок с идентификатором " + lessonId + " не найден");
        }
        return assignmentRepository.findByLessonId(lessonId);
    }
}
