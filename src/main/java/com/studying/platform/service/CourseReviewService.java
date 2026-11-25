package com.studying.platform.service;

import com.studying.platform.entity.Course;
import com.studying.platform.entity.CourseReview;
import com.studying.platform.entity.Enrollment;
import com.studying.platform.entity.EnrollmentStatus;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.CourseReviewRepository;
import com.studying.platform.repository.EnrollmentRepository;
import com.studying.platform.repository.UserRepository;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для управления отзывами студентов о курсах учебной платформы.
 * <p>
 * Обеспечивает бизнес-логику создания и чтения отзывов.
 * Проверяет, что отзыв оставляет студент (роль STUDENT),
 * который активно записан на курс, и что отзыв ещё не оставлен.
 * Все публичные методы транзакционны.
 * </p>
 *
 * @see CourseReview
 * @see CourseReviewRepository
 */
@Service
@Transactional
public class CourseReviewService {

    /**
     * Репозиторий для работы с сущностью «Отзыв о курсе».
     */
    private final CourseReviewRepository courseReviewRepository;

    /**
     * Репозиторий для работы с сущностью «Курс».
     */
    private final CourseRepository courseRepository;

    /**
     * Репозиторий для работы с сущностью «Пользователь».
     */
    private final UserRepository userRepository;

    /**
     * Репозиторий для работы с сущностью «Запись на курс».
     */
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Менеджер сущностей для управления транзакциями и сброса контекста.
     */
    private final EntityManager entityManager;

    /**
     * Конструктор сервиса с внедрением зависимостей через конструктор.
     *
     * @param courseReviewRepository   репозиторий отзывов
     * @param courseRepository         репозиторий курсов
     * @param userRepository           репозиторий пользователей
     * @param enrollmentRepository     репозиторий записей
     * @param entityManager            менеджер сущностей JPA
     */
    public CourseReviewService(CourseReviewRepository courseReviewRepository,
                               CourseRepository courseRepository,
                               UserRepository userRepository,
                               EnrollmentRepository enrollmentRepository,
                               EntityManager entityManager) {
        this.courseReviewRepository = courseReviewRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.entityManager = entityManager;
    }

    /**
     * Создаёт новый отзыв студента о курсе.
     * Выполняет полную валидацию:
     * <ol>
     *   <li>Проверяет существование студента — {@link ResourceNotFoundException}, если не найден.</li>
     *   <li>Проверяет роль студента — {@link ResourceNotFoundException}, если роль не STUDENT.</li>
     *   <li>Проверяет существование курса — {@link ResourceNotFoundException}, если не найден.</li>
     *   <li>Проверяет активную запись студента на курс — {@link ResourceNotFoundException}, если нет активной записи.</li>
     *   <li>Проверяет диапазон рейтинга (1–5) — {@link IllegalArgumentException}, если вне диапазона.</li>
     *   <li>Проверяет уникальность отзыва (один студент — один отзыв на курс) — {@link IllegalStateException}, если отзыв уже оставлен.</li>
     * </ol>
     *
     * @param studentId идентификатор студента
     * @param courseId  идентификатор курса
     * @param rating    рейтинг курса от 1 до 5
     * @param comment   текстовый комментарий отзыва
     * @return сохранённый отзыв
     * @throws ResourceNotFoundException если студент или курс не найдены,
     *                                   или роль пользователя не STUDENT,
     *                                   или нет активной записи на курс
     * @throws IllegalArgumentException  если рейтинг вне диапазона 1–5
     * @throws IllegalStateException     если студент уже оставил отзыв на этот курс
     */
    public CourseReview createReview(Long studentId, Long courseId, Integer rating, String comment) {
        // Проверка существования студента
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Студент с идентификатором " + studentId + " не найден"));

        // Проверка роли студента
        if (student.getRole() != UserRole.STUDENT) {
            throw new ResourceNotFoundException(
                    "Пользователь с идентификатором " + studentId + " не является студентом");
        }

        // Проверка существования курса
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Курс с идентификатором " + courseId + " не найден"));

        // Проверка активной записи на курс
        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseIdAndStatus(studentId, courseId, EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Студент с идентификатором " + studentId +
                                " не имеет активной записи на курс с идентификатором " + courseId));

        // Валидация рейтинга
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
                    "Рейтинг должен быть в диапазоне от 1 до 5, получено: " + rating);
        }

        // Проверка уникальности отзыва
        Optional<CourseReview> existingReview = courseReviewRepository
                .findByStudentIdAndCourseId(studentId, courseId);
        if (existingReview.isPresent()) {
            throw new IllegalStateException(
                    "Студент с идентификатором " + studentId + " уже оставил отзыв на курс с идентификатором " + courseId);
        }

        // Создание и сохранение отзыва
        CourseReview review = new CourseReview(rating, comment, LocalDateTime.now(), course, student);
        entityManager.persist(review);
        entityManager.flush();
        return review;
    }

    /**
     * Возвращает список всех отзывов указанного курса.
     * <p>
     * Проверяет существование курса. Если курс не найден —
     * выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param courseId идентификатор курса
     * @return список отзывов курса
     * @throws ResourceNotFoundException если курс с заданным идентификатором не найден
     */
    public List<CourseReview> getReviewsByCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException(
                    "Курс с идентификатором " + courseId + " не найден");
        }

        return courseReviewRepository.findByCourseId(courseId);
    }

    /**
     * Возвращает список всех отзывов, оставленных указанным студентом.
     * <p>
     * Проверяет существование студента. Если студент не найден —
     * выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param studentId идентификатор студента
     * @return список отзывов студента
     * @throws ResourceNotFoundException если студент с заданным идентификатором не найден
     */
    public List<CourseReview> getReviewsByStudent(Long studentId) {
        if (!userRepository.existsById(studentId)) {
            throw new ResourceNotFoundException(
                    "Студент с идентификатором " + studentId + " не найден");
        }

        return courseReviewRepository.findByStudentId(studentId);
    }
}
