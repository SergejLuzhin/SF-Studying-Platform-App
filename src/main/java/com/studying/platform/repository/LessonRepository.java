package com.studying.platform.repository;

import com.studying.platform.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий для сущности «Урок».
 * <p>
 * Обеспечивает стандартные CRUD-операции и поиск уроков по идентификатору модуля.
 * </p>
 *
 * @see Lesson
 */
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /**
     * Находит все уроки заданного модуля, отсортированные по порядковому номеру модуля.
     * <p>
     * Порядок сортировки определяется порядком добавления уроков.
     * </p>
     *
     * @param moduleId идентификатор модуля
     * @return список уроков модуля
     */
    List<Lesson> findByModuleId(Long moduleId);
}
