package com.studying.platform.repository;

import com.studying.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий для сущности «Пользователь».
 * <p>
 * Обеспечивает базовые CRUD-операции для пользователей учебной платформы,
 * а также поиск по уникальному логину.
 * </p>
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Находит пользователя по уникальному логину.
     *
     * @param login уникальный логин (электронная почта)
     * @return обязательное значение с пользователем, если найден; пустое значение — если нет
     */
    Optional<User> findByLogin(String login);
}
