package com.studying.platform.repository;

import com.studying.platform.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий для сущности «Тег» учебной платформы.
 * <p>
 * Предоставляет стандартные CRUD-операции для работы с тегами,
 * а также поиск тега по уникальному названию.
 * Уникальность названия тега обеспечивается аннотацией {@code @UniqueConstraint}
 * на уровне сущности и ограничениями базы данных.
 * </p>
 *
 * @see Tag
 */
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * Ищет тег по точному названию.
     *
     * @param name название тега для поиска
     * @return опциональный объект тега, найденного по названию
     */
    Optional<Tag> findByName(String name);
}
