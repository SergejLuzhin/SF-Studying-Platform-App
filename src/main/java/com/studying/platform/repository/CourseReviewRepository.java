package com.studying.platform.repository;

import com.studying.platform.entity.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий для сущности «Отзыв о курсе» учебной платформы.
 * <p>
 * Предоставляет стандартные CRUD-операции для работы с отзывами.
 * Ограничение уникальности (один отзыв от одного студента на один курс)
 * обеспечивается аннотацией {@code @UniqueConstraint} на уровне сущности.
 * </p>
 *
 * @see CourseReview
 */
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {
}
