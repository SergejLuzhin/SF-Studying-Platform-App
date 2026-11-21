package com.studying.platform.repository;

import com.studying.platform.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий для сущности «Профиль пользователя».
 * <p>
 * Обеспечивает базовые CRUD-операции для профилей пользователей.
 * </p>
 */
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    /**
     * Находит профиль по идентификатору связанного пользователя.
     *
     * @param userId идентификатор пользователя
     * @return обязательное значение с профилем, если найден; пустое значение — если нет
     */
    Optional<Profile> findByUserId(Long userId);
}
