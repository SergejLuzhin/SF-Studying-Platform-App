package com.studying.platform.controller;

import com.studying.platform.dto.UserCreateDto;
import com.studying.platform.dto.UserResponseDto;
import com.studying.platform.dto.UserUpdateDto;
import com.studying.platform.entity.User;
import com.studying.platform.service.UserService;

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

/**
 * REST-контроллер для управления пользователями учебной платформы.
 * <p>
 * Предоставляет эндпоинты для создания, чтения, обновления и удаления пользователей.
 * Входные данные принимаются через DTO, валидация выполняется аннотациями
 * Jakarta Bean Validation.
 * </p>
 *
 * @see UserService
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * Сервис для бизнес-логики, связанной с пользователями.
     */
    private final UserService userService;

    /**
     * Конструктор контроллера с внедрением зависимостей через конструктор.
     *
     * @param userService сервис пользователей
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Создаёт нового пользователя.
     * <p>
     * Принимает DTO с адресом электронной почты и ролью,
     * создаёт пользователя через сервисный слой.
     * </p>
     *
     * @param dto данные для создания пользователя
     * @return HTTP 201 с DTO созданного пользователя
     */
    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserCreateDto dto) {
        User user = userService.createUser(dto);
        UserResponseDto response = toResponseDto(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получает пользователя по идентификатору.
     * <p>
     * Возвращает DTO с данными пользователя или ошибку 404, если пользователь не найден.
     * </p>
     *
     * @param userId идентификатор пользователя
     * @return HTTP 200 с DTO пользователя
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable Long userId) {
        User user = userService.getUser(userId);
        UserResponseDto response = toResponseDto(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновляет данные существующего пользователя.
     * <p>
     * Принимает обязательные email и роль, сохраняет оба поля и возвращает
     * обновлённые данные. При отсутствии пользователя возвращается 404,
     * при неверных данных или занятом email — 400.
     * </p>
     *
     * @param userId идентификатор пользователя
     * @param dto    новые адрес электронной почты и роль
     * @return HTTP 200 с актуальным DTO пользователя
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long userId, @Valid @RequestBody UserUpdateDto dto) {
        User user = userService.updateUser(userId, dto);
        UserResponseDto response = toResponseDto(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаляет пользователя по идентификатору.
     * <p>
     * Если у пользователя есть связанные данные, операция может завершиться
     * ошибкой на уровне базы данных (внешние ключи).
     * </p>
     *
     * @param userId идентификатор пользователя для удаления
     * @return HTTP 204 No Content при успешном удалении
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Преобразует сущность пользователя в DTO ответа.
     * <p>
     * Копирует только скалярные поля: идентификатор, email и роль.
     * Связанные коллекции (профиль, записи) не инициализируются.
     * </p>
     *
     * @param user сущность пользователя
     * @return DTO ответа с данными пользователя
     */
    private UserResponseDto toResponseDto(User user) {
        return new UserResponseDto(user.getId(), user.getEmail(), user.getRole());
    }
}
