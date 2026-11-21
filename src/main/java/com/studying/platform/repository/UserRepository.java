package com.studying.platform.repository;

import com.studying.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий для сущности «Пользователь».
 * <p>
 * Обеспечивает базовые CRUD-операции для пользователей учебной платформы,
 * а также поиск по уникальному адресу электронной почты.
 * </p>
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Находит пользователя по уникальному адресу электронной почты.
     *
     * @param email адрес электронной почты
     * @return обязательное значение с пользователем, если найден; пустое значение — если нет
     */
    Optional<User> findByEmail(String email);
}
