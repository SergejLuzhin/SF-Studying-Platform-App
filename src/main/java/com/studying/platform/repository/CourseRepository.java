package com.studying.platform.repository;

import com.studying.platform.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
