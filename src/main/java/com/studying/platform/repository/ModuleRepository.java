package com.studying.platform.repository;

import com.studying.platform.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий для сущности «Модуль курса».
 * <p>
 * Обеспечивает стандартные CRUD-операции и специализированный поиск
 * модуля в курсе по порядковому номеру.
 * </p>
 *
 * @see Module
 */
public interface ModuleRepository extends JpaRepository<Module, Long> {

    /**
     * Находит все модули в заданном курсе с указанным порядковым номером.
     *
     * @param courseId   идентификатор курса
     * @param orderIndex порядковый номер модуля
     * @return список модулей с указанным порядковым номером
     */
    List<Module> findByCourseIdAndOrderIndex(Long courseId, Integer orderIndex);
}
