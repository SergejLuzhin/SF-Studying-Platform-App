package com.studying.platform.service;

import com.studying.platform.entity.Tag;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.TagRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Выполняет удаление тегов вместе с их адресными связями с курсами.
 */
@Service
public class TagService {
    /** Репозиторий для поиска и удаления тега. */
    private final TagRepository tagRepository;
    /** Менеджер сущностей для адресного удаления связей. */
    private final EntityManager entityManager;

    /**
     * Создаёт сервис удаления тегов.
     *
     * @param tagRepository репозиторий тегов
     * @param entityManager менеджер сущностей для удаления связей
     */
    public TagService(TagRepository tagRepository, EntityManager entityManager) {
        this.tagRepository = tagRepository;
        this.entityManager = entityManager;
    }

    /**
     * В одной транзакции удаляет связи указанного тега и сам тег, сохраняя курсы.
     *
     * @param tagId идентификатор тега
     * @throws ResourceNotFoundException если тег не найден
     */
    @Transactional
    public void deleteTag(Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Тег с идентификатором " + tagId + " не найден"));
        entityManager.createNativeQuery("delete from course_tag where tag_id = :tagId")
                .setParameter("tagId", tagId)
                .executeUpdate();
        tagRepository.delete(tag);
    }
}
