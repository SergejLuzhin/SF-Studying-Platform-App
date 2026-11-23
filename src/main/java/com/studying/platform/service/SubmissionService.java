package com.studying.platform.service;

import com.studying.platform.entity.Assignment;
import com.studying.platform.entity.Course;
import com.studying.platform.entity.Enrollment;
import com.studying.platform.entity.EnrollmentStatus;
import com.studying.platform.entity.Lesson;
import com.studying.platform.entity.Module;
import com.studying.platform.entity.Submission;
import com.studying.platform.entity.User;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.AssignmentRepository;
import com.studying.platform.repository.EnrollmentRepository;
import com.studying.platform.repository.LessonRepository;
import com.studying.platform.repository.SubmissionRepository;
import com.studying.platform.repository.UserRepository;

import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для управления сдачей решений учебных заданий.
 * <p>
 * Обеспечивает бизнес-логику отправки решений студентами (проверка существования задания,
 * записи студента на курс и отсутствия повторной сдачи), а также выставления оценок
 * преподавателями и получения списков решений по заданию и студенту.
 * Все публичные методы транзакционны.
 * </p>
 *
 * @see Submission
 * @see Assignment
 */
@Service
@Transactional
public class SubmissionService {

    /**
     * Репозиторий для работы с сущностью «Решение».
     */
    private final SubmissionRepository submissionRepository;

    /**
     * Репозиторий для работы с сущностью «Задание».
     */
    private final AssignmentRepository assignmentRepository;

    /**
     * Репозиторий для работы с сущностью «Запись на курс».
     */
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Репозиторий для работы с сущностью «Урок».
     */
    private final LessonRepository lessonRepository;

    /**
     * Репозиторий для работы с сущностью «Пользователь».
     */
    private final UserRepository userRepository;

    /**
     * Менеджер сущностей для управления транзакциями и сброса контекста.
     */
    private final EntityManager entityManager;

    /**
     * Конструктор сервиса с внедрением зависимостей через конструктор.
     *
     * @param submissionRepository     репозиторий решений
     * @param assignmentRepository     репозиторий заданий
     * @param enrollmentRepository     репозиторий записей на курс
     * @param lessonRepository         репозиторий уроков
     * @param userRepository           репозиторий пользователей
     * @param entityManager            менеджер сущностей JPA
     */
    public SubmissionService(SubmissionRepository submissionRepository,
                             AssignmentRepository assignmentRepository,
                             EnrollmentRepository enrollmentRepository,
                             LessonRepository lessonRepository,
                             UserRepository userRepository,
                             EntityManager entityManager) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    /**
     * Отправляет решение студента на учебное задание.
     * <p>
     * Выполняет следующие проверки:
     * <ol>
     *   <li>Существование задания по идентификатору;</li>
     *   <li>Запись студента на курс, содержащий задание (статус ACTIVE);</li>
     *   <li>Отсутствие уже отправленного решения данным студентом на это задание.</li>
     * </ol>
     * Если какая-либо проверка не пройдена — выбрасывает исключение.
     * При успешной отправке создаётся запись с текущей датой и временем.
     * </p>
     *
     * @param assignmentId идентификатор задания
     * @param studentId    идентификатор студента
     * @param content      текст ответа студента
     * @return сохранённое решение
     * @throws ResourceNotFoundException если задание не найдено или студент не записан на курс
     * @throws IllegalStateException     если студент уже отправил решение на это задание
     */
    public Submission submitSolution(Long assignmentId, Long studentId, String content) {
        // 1. Проверка существования задания
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Задание с идентификатором " + assignmentId + " не найдено"));

        // 2. Получение курса через цепочку lazy-связей (безопасно внутри @Transactional)
        Lesson lesson = lessonRepository.findById(assignment.getLesson().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Урок с идентификатором " + assignment.getLesson().getId() + " не найден"));

        Module module = lesson.getModule();
        Course course = module.getCourse();

        // 3. Проверка записи студента на курс (статус ACTIVE)
        Optional<Enrollment> enrollment = enrollmentRepository
                .findByUserIdAndCourseIdAndStatus(studentId, course.getId(), EnrollmentStatus.ACTIVE);

        if (enrollment.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Студент с идентификатором " + studentId + " не записан на курс " + course.getId());
        }

        // 4. Проверка отсутствия повторного решения
        Optional<Submission> existing = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId);

        if (existing.isPresent()) {
            throw new IllegalStateException(
                    "Студент с идентификатором " + studentId +
                            " уже отправил решение для задания с идентификатором " + assignmentId);
        }

        // 5. Создание и сохранение решения
        User student = entityManager.getReference(User.class, studentId);
        Submission submission = new Submission(assignment, student, content);

        entityManager.persist(submission);
        entityManager.flush();
        return submission;
    }

    /**
     * Выставляет оценку и комментарий преподавателя к решению студента.
     * <p>
     * Выполняет следующие проверки:
     * <ol>
     *   <li>Существование решения по идентификатору;</li>
     *   <li>Существование преподавателя (пользователя) по идентификатору;</li>
     *   <li>Допустимый диапазон оценки: оценка не может быть меньше 0, а при наличии
     *       максимального балла задания — не может превышать его.</li>
     * </ol>
     * При успешной проверке обновляются поля {@code score} и {@code feedback} решения.
     * Метод допускает перезапись ранее выставленной оценки.
     * </p>
     *
     * @param submissionId идентификатор решения
     * @param score        оценка студента
     * @param feedback     комментарий преподавателя (может быть {@code null})
     * @param teacherId    идентификатор преподавателя
     * @throws ResourceNotFoundException если решение или преподаватель не найдены
     * @throws IllegalArgumentException  если оценка выходит за допустимый диапазон
     */
    public void gradeSubmission(Long submissionId, Integer score, String feedback, Long teacherId) {
        // 1. Проверка существования решения
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Решение с идентификатором " + submissionId + " не найдено"));

        // 2. Проверка существования преподавателя
        userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Преподаватель с идентификатором " + teacherId + " не найден"));

        // 3. Валидация диапазона оценки
        if (score < 0) {
            throw new IllegalArgumentException("Оценка не может быть отрицательной, получено: " + score);
        }

        // 4. Проверка верхней границы (maxScore задания)
        Integer maxScore = submission.getAssignment().getMaxScore();
        if (maxScore != null && score > maxScore) {
            throw new IllegalArgumentException(
                    "Оценка не может превышать максимальный балл задания (максимум: " + maxScore + "), получено: " + score);
        }

        // 5. Обновление оценки и комментария
        submission.setScore(score);
        submission.setFeedback(feedback);
    }

    /**
     * Возвращает список всех решений заданного задания.
     * <p>
     * Проверяет существование задания. Если задание не найдено — выбрасывает исключение.
     * </p>
     *
     * @param assignmentId идентификатор задания
     * @return список решений задания
     * @throws ResourceNotFoundException если задание не найдено
     */
    public List<Submission> getSubmissionsByAssignment(Long assignmentId) {
        assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Задание с идентификатором " + assignmentId + " не найдено"));
        return submissionRepository.findByAssignmentId(assignmentId);
    }

    /**
     * Возвращает список всех решений данного студента.
     * <p>
     * Проверяет существование студента. Если студент не найден — выбрасывает исключение.
     * </p>
     *
     * @param studentId идентификатор студента
     * @return список решений студента
     * @throws ResourceNotFoundException если студент не найден
     */
    public List<Submission> getSubmissionsByStudent(Long studentId) {
        userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Студент с идентификатором " + studentId + " не найден"));
        return submissionRepository.findByStudentId(studentId);
    }
}
