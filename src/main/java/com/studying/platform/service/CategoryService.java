package com.studying.platform.service;

import com.studying.platform.dto.CategoryCreateDto;
import com.studying.platform.dto.CategoryResponseDto;
import com.studying.platform.entity.Category;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для управления категориями курса учебной платформы.
 * <p>
 * Обеспечивает бизнес-логику создания, чтения, обновления и удаления категорий.
 * Все публичные методы транзакционны.
 * </p>
 *
 * @see Category
 * @see CategoryRepository
 */
@Service
@Transactional
public class CategoryService {

    /**
     * Репозиторий для работы с сущностью «Категория курса».
     */
    private final CategoryRepository categoryRepository;

    /**
     * Конструктор сервиса с внедрением зависимостей через конструктор.
     *
     * @param categoryRepository репозиторий категорий
     */
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Создаёт новую категорию курса с заданным названием.
     * <p>
     * Проверяет отсутствие категории с таким названием.
     * Если категория уже существует — выбрасывает {@link IllegalStateException}.
     * После сохранения вызывает {@code flush} для немедленной записи в базу данных.
     * </p>
     *
     * @param dto данные для создания категории
     * @return сохранённая категория
     * @throws IllegalStateException если категория с таким названием уже существует
     */
    public Category createCategory(CategoryCreateDto dto) {
        if (categoryRepository.findByName(dto.getName()).isPresent()) {
            throw new IllegalStateException(
                    "Категория с названием \"" + dto.getName() + "\" уже существует");
        }

        Category category = new Category(dto.getName());
        Category saved = categoryRepository.saveAndFlush(category);
        return saved;
    }

    /**
     * Находит категорию по идентификатору.
     * <p>
     * Метод выполняется внутри транзакции чтения, поэтому доступ к скалярным
     * полям категории безопасен.
     * </p>
     *
     * @param categoryId идентификатор категории
     * @return категория с заданным идентификатором
     * @throws ResourceNotFoundException если категория не найдена
     */
    @Transactional(readOnly = true)
    public Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Категория с идентификатором " + categoryId + " не найдена"));
    }

    /**
     * Получает список всех категорий.
     * <p>
     * Метод выполняется внутри транзакции чтения.
     * </p>
     *
     * @return список всех категорий
     */
    @Transactional(readOnly = true)
    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Обновляет название существующей категории.
     * <p>
     * Проверяет существование категории по идентификатору и отсутствие
     * другой категории с таким же названием.
     * </p>
     *
     * @param categoryId идентификатор категории для обновления
     * @param name       новое название категории
     * @return обновлённая категория
     * @throws ResourceNotFoundException если категория с заданным идентификатором не найдена
     * @throws IllegalStateException     если категория с таким названием уже существует
     */
    public Category updateCategory(Long categoryId, String name) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Категория с идентификатором " + categoryId + " не найдена"));

        if (!category.getName().equals(name)
                && categoryRepository.findByName(name).isPresent()) {
            throw new IllegalStateException(
                    "Категория с названием \"" + name + "\" уже существует");
        }

        category.setName(name);
        return categoryRepository.save(category);
    }

    /**
     * Преобразует сущность категории в DTO ответа.
     * <p>
     * Создает новый объект DTO, копируя только идентификатор и название.
     * Связанные коллекции (курсы) не инициализируются.
     * </p>
     *
     * @param category сущность категории
     * @return DTO ответа с данными категории
     */
    public CategoryResponseDto toResponseDto(Category category) {
        return new CategoryResponseDto(category.getId(), category.getName());
    }

    /**
     * Преобразует список сущностей категорий в список DTO ответов.
     *
     * @param categories список сущностей категорий
     * @return список DTO ответов
     */
    public List<CategoryResponseDto> toResponseList(List<Category> categories) {
        return categories.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Удаляет категорию по идентификатору.
     * <p>
     * Проверяет существование категории. Если категория не найдена —
     * выбрасывает {@link ResourceNotFoundException}.
     * Если у категории есть связанные курсы, удаление может завершиться
     * ошибкой на уровне базы данных (внешние ключи).
     * </p>
     *
     * @param categoryId идентификатор категории для удаления
     * @throws ResourceNotFoundException если категория с заданным идентификатором не найдена
     * @throws jakarta.persistence.PersistenceException при нарушении ограничений целостности БД
     */
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Категория с идентификатором " + categoryId + " не найдена"));
        categoryRepository.delete(category);
    }
}
