package com.studying.platform.repository;

import com.studying.platform.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий для сущности «Тег» учебной платформы.
 * <p>
 * Предоставляет стандартные CRUD-операции для работы с тегами.
 * Уникальность названия тега обеспечивается аннотацией {@code @UniqueConstraint}
 * на уровне сущности и ограничениями базы данных.
 * </p>
 *
 * @see Tag
 */
public interface TagRepository extends JpaRepository<Tag, Long> {
}
