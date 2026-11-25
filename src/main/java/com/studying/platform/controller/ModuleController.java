package com.studying.platform.controller;

import com.studying.platform.dto.ModuleRequestDto;
import com.studying.platform.dto.ModuleResponseDto;
import com.studying.platform.entity.Module;
import com.studying.platform.service.CourseService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер для управления модулями курса.
 * <p>
 * Предоставляет эндпоинты для чтения, обновления и удаления модулей.
 * Создание модуля выполняется через {@code CourseController}.
 * </p>
 *
 * @see CourseService
 */
@RestController
@RequestMapping("/api/modules")
public class ModuleController {

    /**
     * Сервис для бизнес-логики, связанной с курсами и модулями.
     */
    private final CourseService courseService;

    /**
     * Конструктор контроллера с внедрением зависимостей через конструктор.
     *
     * @param courseService сервис курсов
     */
    public ModuleController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * Получает модуль по идентификатору.
     * <p>
     * Возвращает DTO с данными модуля или ошибку 404, если модуль не найден.
     * </p>
     *
     * @param moduleId идентификатор модуля
     * @return HTTP 200 с DTO модуля
     */
    @GetMapping("/{moduleId}")
    public ResponseEntity<ModuleResponseDto> getModule(@PathVariable Long moduleId) {
        Module module = courseService.findModule(moduleId);
        ModuleResponseDto response = toModuleResponseDto(module);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновляет существующий модуль.
     * <p>
     * Принимает DTO с не-null полями. Обновляет только указанные поля.
     * </p>
     *
     * @param moduleId идентификатор модуля
     * @param dto      данные для обновления
     * @return HTTP 200 с обновлённым DTO модуля
     */
    @PutMapping("/{moduleId}")
    public ResponseEntity<ModuleResponseDto> updateModule(
            @PathVariable Long moduleId,
            @Valid @RequestBody ModuleRequestDto dto) {
        Module module = courseService.updateModule(
                moduleId,
                dto.getTitle(),
                dto.getDescription(),
                dto.getOrderIndex()
        );
        ModuleResponseDto response = toModuleResponseDto(module);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаляет модуль по идентификатору.
     * <p>
     * Удаляет модуль вместе с принадлежащими ему уроками.
     * </p>
     *
     * @param moduleId идентификатор модуля для удаления
     * @return HTTP 204 No Content при успешном удалении
     */
    @DeleteMapping("/{moduleId}")
    public ResponseEntity<Void> deleteModule(@PathVariable Long moduleId) {
        courseService.deleteModule(moduleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Преобразует сущность модуля в DTO ответа.
     * <p>
     * Копирует идентификатор, название, описание, порядковый номер и идентификатор курса.
     * </p>
     *
     * @param module сущность модуля
     * @return DTO ответа с данными модуля
     */
    private ModuleResponseDto toModuleResponseDto(Module module) {
        return new ModuleResponseDto(
                module.getId(),
                module.getTitle(),
                module.getDescription(),
                module.getOrderIndex(),
                module.getCourse().getId()
        );
    }
}
