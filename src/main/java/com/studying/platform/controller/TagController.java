package com.studying.platform.controller;

import com.studying.platform.dto.TagRequestDto;
import com.studying.platform.dto.TagResponseDto;
import com.studying.platform.entity.Tag;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.TagRepository;

import jakarta.validation.Valid;

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

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST-контроллер для управления тегами учебной платформы.
 * <p>
 * Предоставляет эндпоинты для создания, чтения, обновления и удаления тегов.
 * Название тега уникально на уровне базы данных.
 * </p>
 *
 * @see Tag
 */
@RestController
@RequestMapping("/api/tags")
public class TagController {

    /**
     * Репозиторий для работы с тегами.
     */
    private final TagRepository tagRepository;

    /**
     * Конструктор контроллера с внедрением зависимостей через конструктор.
     *
     * @param tagRepository репозиторий тегов
     */
    public TagController(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * Создаёт новый тег.
     * <p>
     * Принимает DTO с валидацией, создаёт тег через репозиторий.
     * Уникальность названия обеспечивается ограничением UNIQUE в базе данных.
     * </p>
     *
     * @param dto данные для создания тега
     * @return HTTP 201 с DTO созданного тега
     */
    @PostMapping
    public ResponseEntity<TagResponseDto> createTag(@Valid @RequestBody TagRequestDto dto) {
        Tag tag = new Tag(dto.getName());
        tag = tagRepository.save(tag);
        TagResponseDto response = toResponseDto(tag);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Возвращает список всех тегов.
     *
     * @return HTTP 200 со списком всех тегов
     */
    @GetMapping
    public ResponseEntity<List<TagResponseDto>> getAllTags() {
        List<Tag> tags = tagRepository.findAll();
        List<TagResponseDto> response = tags.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Получает тег по идентификатору.
     * <p>
     * Возвращает DTO с данными тега или ошибку 404, если тег не найден.
     * </p>
     *
     * @param tagId идентификатор тега
     * @return HTTP 200 с DTO тега
     */
    @GetMapping("/{tagId}")
    public ResponseEntity<TagResponseDto> getTag(@PathVariable Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Тег с идентификатором " + tagId + " не найден"));
        TagResponseDto response = toResponseDto(tag);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновляет название существующего тега.
     * <p>
     * Проверяет существование тега. Если тег не найден — ошибка 404.
     * </p>
     *
     * @param tagId идентификатор тега
     * @param dto   данные для обновления
     * @return HTTP 200 с обновлённым DTO тега
     */
    @PutMapping("/{tagId}")
    public ResponseEntity<TagResponseDto> updateTag(
            @PathVariable Long tagId,
            @Valid @RequestBody TagRequestDto dto) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Тег с идентификатором " + tagId + " не найден"));
        tag.setName(dto.getName());
        tag = tagRepository.save(tag);
        TagResponseDto response = toResponseDto(tag);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаляет тег по идентификатору.
     * <p>
     * Проверяет существование тега. Если тег не найден — ошибка 404.
     * Связи с курсами (таблица course_tag) удаляются автоматически через каскад.
     * </p>
     *
     * @param tagId идентификатор тега для удаления
     * @return HTTP 204 No Content при успешном удалении
     */
    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long tagId) {
        if (!tagRepository.existsById(tagId)) {
            throw new ResourceNotFoundException(
                    "Тег с идентификатором " + tagId + " не найден");
        }
        tagRepository.deleteById(tagId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Преобразует сущность тега в DTO ответа.
     *
     * @param tag сущность тега
     * @return DTO ответа с данными тега
     */
    private TagResponseDto toResponseDto(Tag tag) {
        return new TagResponseDto(tag.getId(), tag.getName());
    }
}
