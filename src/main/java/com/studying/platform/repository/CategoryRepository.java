package com.studying.platform.repository;

import com.studying.platform.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий для сущности «Категория курса» {@link Category}.
 * <p>
 * Обеспечивает базовые операции CRUD и поиск категории по имени.
 * Использует Spring Data JPA для автоматической реализации методов.
 * </p>
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Ищет категорию по точному названию.
     *
     * @param name название категории для поиска
     * @return опциональный объект категории, найденной по имени
     */
    Optional<Category> findByName(String name);
}
