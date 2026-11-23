package com.studying.platform.repository;

import com.studying.platform.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Репозиторий для сущности «Курс» {@link Course}.
 * <p>
 * Обеспечивает базовые операции CRUD, поиск курса по названию
 * и проверку существования курса.
 * Использует Spring Data JPA для автоматической реализации методов.
 * </p>
 */
public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * Ищет курс по точному названию.
     *
     * @param title название курса для поиска
     * @return опциональный объект курса, найденного по названию
     */
    Optional<Course> findByTitle(String title);

    /**
     * Проверяет существование курса по идентификатору.
     *
     * @param id идентификатор курса
     * @return true, если курс с заданным идентификатором существует
     */
    boolean existsById(Long id);

    /**
     * Возвращает количество тегов, связанных с указанным курсом.
     * <p>
     * Использует JPQL-запрос с JOIN для подсчёта тегов
     * без загрузки ленивой коллекции тегов.
     * </p>
     *
     * @param courseId идентификатор курса
     * @return количество тегов курса
     */
    @Query("SELECT COUNT(t) FROM Course c JOIN c.tags t WHERE c.id = ?1")
    long countTagsByCourseId(Long courseId);

    /**
     * Подсчитывает количество записей студентов на указанный курс.
     * <p>
     * Используется для проверки наличия записей (включая отписавшихся)
     * перед удалением курса. Считает все записи независимо от статуса.
     * </p>
     *
     * @param courseId идентификатор курса
     * @return количество записей на курс
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId")
    long countEnrollmentsByCourseId(Long courseId);

    /**
     * Подсчитывает количество отзывов для указанного курса.
     * <p>
     * Используется для проверки наличия отзывов перед удалением курса.
     * </p>
     *
     * @param courseId идентификатор курса
     * @return количество отзывов курса
     */
    @Query("SELECT COUNT(r) FROM CourseReview r WHERE r.course.id = :courseId")
    long countReviewsByCourseId(Long courseId);
}
