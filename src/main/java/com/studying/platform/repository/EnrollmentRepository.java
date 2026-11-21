package com.studying.platform.repository;

import com.studying.platform.entity.Enrollment;
import com.studying.platform.entity.EnrollmentStatus;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

/**
 * Репозиторий для сущности «Запись на курс» {@link Enrollment}.
 * <p>
 * Обеспечивает базовые CRUD-операции и кастомные выборки записей
 * по пользователю, курсу и статусу. Использует Spring Data JPA
 * для автоматической реализации методов.
 * </p>
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * Находит активную запись студента на курс по идентификаторам пользователя и курса.
     *
     * @param userId    идентификатор пользователя (студента)
     * @param courseId  идентификатор курса
     * @param status    статус записи
     * @return обязательное значение с записью, если найдена; пустое значение — если нет
     */
    Optional<Enrollment> findByUserIdAndCourseIdAndStatus(Long userId, Long courseId, EnrollmentStatus status);

    /**
     * Находит любую запись студента на курс по идентификаторам пользователя и курса,
     * независимо от статуса.
     * <p>
     * Используется для проверки правила «один студент — одна запись на курс»:
     * если запись существует (любого статуса), повторная запись запрещена.
     * </p>
     *
     * @param userId   идентификатор пользователя (студента)
     * @param courseId идентификатор курса
     * @return обязательное значение с записью, если найдена; пустое значение — если нет
     */
    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    /**
     * Находит все активные записи студента.
     * <p>
     * С помощью {@link EntityGraph} загружает связанные курсы,
     * чтобы избежать {@code LazyInitializationException} при доступе
     * к полям курса после завершения транзакции.
     * </p>
     *
     * @param userId  идентификатор пользователя (студента)
     * @param status  статус записи
     * @return список записей студента с указанным статусом
     */
    @EntityGraph(attributePaths = "course")
    List<Enrollment> findByUserIdAndStatus(Long userId, EnrollmentStatus status);

    /**
     * Находит все активные записи на курс.
     * <p>
     * С помощью {@link EntityGraph} загружает связанных пользователей,
     * чтобы избежать {@code LazyInitializationException} при доступе
     * к полям пользователя после завершения транзакции.
     * </p>
     *
     * @param courseId идентификатор курса
     * @param status   статус записи
     * @return список записей на курс с указанным статусом
     */
    @EntityGraph(attributePaths = "user")
    List<Enrollment> findByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    /**
     * Подсчитывает количество активных записей на курс.
     *
     * @param courseId идентификатор курса
     * @param status   статус записи
     * @return количество записей с указанным статусом
     */
    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
}
