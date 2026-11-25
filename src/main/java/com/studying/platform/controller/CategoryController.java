package com.studying.platform.controller;

import com.studying.platform.dto.CategoryCreateDto;
import com.studying.platform.dto.CategoryResponseDto;
import com.studying.platform.entity.Category;
import com.studying.platform.service.CategoryService;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер для управления категориями курса учебной платформы.
 * <p>
 * Предоставляет эндпоинты для создания, чтения, обновления и удаления категорий.
 * Входные данные принимаются через DTO, валидация выполняется аннотациями
 * Jakarta Bean Validation.
 * </p>
 *
 * @see CategoryService
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    /**
     * Сервис для бизнес-логики, связанной с категориями.
     */
    private final CategoryService categoryService;

    /**
     * Конструктор контроллера с внедрением зависимостей через конструктор.
     *
     * @param categoryService сервис категорий
     */
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Создаёт новую категорию курса.
     * <p>
     * Принимает DTO с названием категории, создаёт категорию через сервисный слой.
     * </p>
     *
     * @param dto данные для создания категории
     * @return HTTP 201 с DTO созданной категории
     */
    @PostMapping
    public ResponseEntity<CategoryResponseDto> createCategory(
            @Valid @RequestBody CategoryCreateDto dto) {
        Category category = categoryService.createCategory(dto);
        CategoryResponseDto response = categoryService.toResponseDto(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получает список всех категорий.
     * <p>
     * Возвращает массив DTO с данными всех категорий.
     * </p>
     *
     * @return HTTP 200 с массивом DTO категорий
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        List<CategoryResponseDto> response = categoryService.toResponseList(
                categoryService.findAllCategories());
        return ResponseEntity.ok(response);
    }

    /**
     * Получает категорию по идентификатору.
     * <p>
     * Возвращает DTO с данными категории или ошибку 404, если категория не найдена.
     * </p>
     *
     * @param categoryId идентификатор категории
     * @return HTTP 200 с DTO категории
     */
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponseDto> getCategory(
            @PathVariable Long categoryId) {
        Category category = categoryService.getCategory(categoryId);
        CategoryResponseDto response = categoryService.toResponseDto(category);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновляет название существующей категории.
     * <p>
     * Проверяет существование категории и отсутствие дубликата по названию.
     * </p>
     *
     * @param categoryId идентификатор категории
     * @param name       новое название категории
     * @return HTTP 200 с DTO обновлённой категории
     */
    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponseDto> updateCategory(
            @PathVariable Long categoryId, @RequestBody CategoryCreateDto name) {
        Category category = categoryService.updateCategory(categoryId, name.getName());
        CategoryResponseDto response = categoryService.toResponseDto(category);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаляет категорию по идентификатору.
     * <p>
     * Если у категории есть связанные курсы, операция может завершиться
     * ошибкой на уровне базы данных (внешние ключи).
     * </p>
     *
     * @param categoryId идентификатор категории для удаления
     * @return HTTP 204 No Content при успешном удалении
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
