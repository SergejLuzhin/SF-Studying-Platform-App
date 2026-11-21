package com.studying.platform.service;

import com.studying.platform.entity.Course;
import com.studying.platform.entity.Enrollment;
import com.studying.platform.entity.EnrollmentStatus;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.CourseRepository;
import com.studying.platform.repository.EnrollmentRepository;
import com.studying.platform.repository.UserRepository;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для управления записью студентов на курсы учебной платформы.
 * <p>
 * Обеспечивает бизнес-логику записи студента на курс, отписки от курса,
 * а также выборки активных записей. Все публичные методы транзакционны.
 * </p>
 *
 * @see Enrollment
 * @see EnrollmentRepository
 */
@Service
@Transactional
public class EnrollmentService {

    /**
     * Репозиторий для работы с сущностью «Запись на курс».
     */
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Репозиторий для работы с сущностью «Пользователь».
     */
    private final UserRepository userRepository;

    /**
     * Репозиторий для работы с сущностью «Курс».
     */
    private final CourseRepository courseRepository;

    /**
     * Менеджер сущностей для управления транзакциями и сброса контекста.
     */
    private final EntityManager entityManager;

    /**
     * Конструктор сервиса с внедрением зависимостей через конструктор.
     *
     * @param enrollmentRepository репозиторий записей
     * @param userRepository       репозиторий пользователей
     * @param courseRepository     репозиторий курсов
     * @param entityManager        менеджер сущностей JPA
     */
    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             UserRepository userRepository,
                             CourseRepository courseRepository,
                             EntityManager entityManager) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.entityManager = entityManager;
    }

    /**
     * Записывает студента на курс.
     * <p>
     * Проверяет существование пользователя и курса, роль пользователя (должна быть STUDENT),
     * а также отсутствие любой записи студента на курс (независимо от статуса).
     * Согласовано правило: один студент может быть записан на курс только один раз.
     * Повторная запись запрещена даже после отписки — статус DROPPED не реактивируется.
     * Если проверка не пройдена — выбрасывает {@link ResourceNotFoundException} или {@link IllegalStateException}.
     * </p>
     *
     * @param studentId идентификатор студента
     * @param courseId  идентификатор курса
     * @return сохранённая запись
     * @throws ResourceNotFoundException если студент или курс не найдены,
     *                                   или роль пользователя не STUDENT
     * @throws IllegalStateException     если студент уже был записан на курс (ACTIVE или DROPPED)
     */
    public Enrollment enrollStudent(Long studentId, Long courseId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Студент с идентификатором " + studentId + " не найден"));

        if (student.getRole() != UserRole.STUDENT) {
            throw new ResourceNotFoundException(
                    "Пользователь с идентификатором " + studentId + " не является студентом");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Курс с идентификатором " + courseId + " не найден"));

        // Проверка отсутствия любой записи (ACTIVE или DROPPED)
        Optional<Enrollment> existing = enrollmentRepository
                .findByUserIdAndCourseId(studentId, courseId);
        if (existing.isPresent()) {
            throw new IllegalStateException(
                    "Студент с идентификатором " + studentId + " уже был записан на курс с идентификатором " + courseId);
        }

        Enrollment enrollment = new Enrollment(student, course, LocalDateTime.now(), EnrollmentStatus.ACTIVE);
        entityManager.persist(enrollment);
        entityManager.flush();
        return enrollment;
    }

    /**
     * Отписывает студента от курса, меняя статус записи на DROPPED.
     * <p>
     * Проверяет существование активной записи. Если запись не найдена —
     * выбрасывает {@link ResourceNotFoundException}.
     * </p>
     *
     * @param studentId идентификатор студента
     * @param courseId  идентификатор курса
     * @return обновлённая запись со статусом DROPPED
     * @throws ResourceNotFoundException если активная запись не найдена
     */
    public Enrollment dropEnrollment(Long studentId, Long courseId) {
        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseIdAndStatus(studentId, courseId, EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Активная запись студента с идентификатором " + studentId +
                                " на курс с идентификатором " + courseId + " не найдена"));

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        entityManager.flush();
        return enrollment;
    }

    /**
     * Возвращает список курсов, на которые студент активно записан.
     * <p>
     * Метод выполняется внутри транзакции, а {@link EntityGraph} в репозитории
     * обеспечивает загрузку связанных курсов, поэтому скалярные поля
     * (идентификатор и название) доступны после выхода из транзакции.
     * Если студент не найден — выбрасывает {@link ResourceNotFoundException};
     * если студент найден, но активных записей нет — возвращает пустой список.
     * </p>
     *
     * @param studentId идентификатор студента
     * @return список курсов со статусом ACTIVE
     * @throws ResourceNotFoundException если студент с заданным идентификатором не найден
     */
    public List<Course> getStudentCourses(Long studentId) {
        if (!userRepository.existsById(studentId)) {
            throw new ResourceNotFoundException(
                    "Студент с идентификатором " + studentId + " не найден");
        }

        List<Enrollment> enrollments = enrollmentRepository
                .findByUserIdAndStatus(studentId, EnrollmentStatus.ACTIVE);

        List<Course> courses = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Course course = enrollment.getCourse();
            courses.add(course);
        }
        return courses;
    }

    /**
     * Возвращает список студентов, активно записанных на курс.
     * <p>
     * Метод выполняется внутри транзакции, а {@link EntityGraph} в репозитории
     * обеспечивает загрузку связанных пользователей, поэтому скалярные поля
     * (идентификатор и адрес электронной почты) доступны после выхода из транзакции.
     * Если курс не найден — выбрасывает {@link ResourceNotFoundException};
     * если курс найден, но активных записей нет — возвращает пустой список.
     * </p>
     *
     * @param courseId идентификатор курса
     * @return список студентов со статусом ACTIVE
     * @throws ResourceNotFoundException если курс с заданным идентификатором не найден
     */
    public List<User> getCourseStudents(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException(
                    "Курс с идентификатором " + courseId + " не найден");
        }

        List<Enrollment> enrollments = enrollmentRepository
                .findByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);

        List<User> students = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            User student = enrollment.getUser();
            students.add(student);
        }
        return students;
    }

    /**
     * Возвращает статус записи студента на курс.
     * <p>
     * Метод ищет любую запись по паре студент–курс, независимо от статуса.
     * Возвращает {@code ACTIVE} для активной записи, {@code DROPPED} после отписки
     * и {@code null} только если запись полностью отсутствует.
     * </p>
     *
     * @param studentId идентификатор студента
     * @param courseId  идентификатор курса
     * @return статус записи или {@code null}, если запись не найдена
     */
    public EnrollmentStatus getEnrollmentStatus(Long studentId, Long courseId) {
        return enrollmentRepository
                .findByUserIdAndCourseId(studentId, courseId)
                .map(Enrollment::getStatus)
                .orElse(null);
    }
}
