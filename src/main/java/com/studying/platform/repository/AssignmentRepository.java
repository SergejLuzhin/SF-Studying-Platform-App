package com.studying.platform.repository;

import com.studying.platform.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий для сущности «Задание».
 * <p>
 * Обеспечивает стандартные CRUD-операции и поиск заданий по идентификатору урока.
 * </p>
 *
 * @see Assignment
 */
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /**
     * Находит все задания заданного урока.
     *
     * @param lessonId идентификатор урока
     * @return список заданий урока
     */
    List<Assignment> findByLessonId(Long lessonId);
}
