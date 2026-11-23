package com.studying.platform.repository;

import com.studying.platform.entity.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для сущности «Отзыв о курсе» учебной платформы.
 * <p>
 * Предоставляет стандартные CRUD-операции, а также методы
 * для поиска отзывов по студенту и по курсу, и проверки уникальности отзыва.
 * Ограничение уникальности (один отзыв от одного студента на один курс)
 * обеспечивается аннотацией {@code @UniqueConstraint} на уровне сущности.
 * </p>
 *
 * @see CourseReview
 */
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    /**
     * Ищет отзыв по идентификатору студента и идентификатору курса.
     * <p>
     * Используется для проверки уникальности отзыва —
     * один студент может оставить только один отзыв на один курс.
     * </p>
     *
     * @param studentId идентификатор студента
     * @param courseId  идентификатор курса
     * @return опциональный объект отзыва, если отзыв существует
     */
    Optional<CourseReview> findByStudentIdAndCourseId(Long studentId, Long courseId);

    /**
     * Возвращает все отзывы для указанного курса.
     *
     * @param courseId идентификатор курса
     * @return список отзывов курса
     */
    List<CourseReview> findByCourseId(Long courseId);

    /**
     * Возвращает все отзывы, оставленные указанным студентом.
     *
     * @param studentId идентификатор студента
     * @return список отзывов студента
     */
    List<CourseReview> findByStudentId(Long studentId);

    /**
     * Подсчитывает количество отзывов для указанного курса.
     * <p>
     * Используется для проверки наличия отзывов перед удалением курса.
     * </p>
     *
     * @param courseId идентификатор курса
     * @return количество отзывов курса
     */
    @Query("SELECT COUNT(r) FROM CourseReview r WHERE r.course.id = ?1")
    long countByCourseId(Long courseId);
}
