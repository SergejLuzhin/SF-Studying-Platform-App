package com.studying.platform.service;

import com.studying.platform.dto.UserCreateDto;
import com.studying.platform.dto.UserUpdateDto;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.exception.ResourceNotFoundException;
import com.studying.platform.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для управления пользователями учебной платформы.
 * <p>
 * Обеспечивает бизнес-логику создания, изменения и чтения пользователей.
 * Все публичные методы записи транзакционны; метод чтения помечен
 * как чтение-только. Валидация входных данных выполняется с помощью
 * Jakarta Bean Validation до момента сохранения в базу данных.
 * Уникальность адреса электронной почты на уровне сервиса дополняет
 * ограничение уникальности базы данных и защищает от дублирования
 * в рамках бизнес-логики.
 * </p>
 *
 * @see User
 * @see UserRepository
 */
@Service
@Transactional
public class UserService {

    /**
     * Репозиторий для работы с сущностью «Пользователь».
     */
    private final UserRepository userRepository;

    /**
     * Валидатор Jakarta Bean Validation для проверки входных данных.
     */
    private final Validator validator;

    /**
     * Конструктор сервиса с внедрением зависимостей через конструктор.
     *
     * @param userRepository репозиторий пользователей
     * @param validator      валидатор Jakarta для проверки входных параметров
     */
    public UserService(UserRepository userRepository, Validator validator) {
        this.userRepository = userRepository;
        this.validator = validator;
    }

    /**
     * Создаёт нового пользователя с заданным адресом электронной почты и ролью.
     * <p>
     * Выполняет валидацию параметров через Jakarta Bean Validation:
     * проверяет корректность формата и длины адреса электронной почты,
     * а также отсутствие {@code null} в роли. Если валидация не пройдена —
     * выбрасывает {@link ConstraintViolationException}.
     * </p>
     * <p>
     * Проверяет отсутствие пользователя с таким адресом электронной почты.
     * Если пользователь уже существует — выбрасывает {@link IllegalStateException}.
     * После сохранения вызывает {@code flush} для немедленной записи в базу данных.
     * </p>
     * <p>
     * Уникальность на уровне сервиса дополняет ограничение базы данных.
     * При конкурентной вставке ограничение БД гарантирует целостность.
     * </p>
     *
     * @param email адрес электронной почты нового пользователя
     * @param role  роль пользователя (STUDENT, TEACHER, ADMIN)
     * @return сохранённый пользователь
     * @throws ConstraintViolationException    если адрес электронной почты или роль не прошли валидацию
     * @throws IllegalStateException             если пользователь с таким адресом электронной почты уже существует
     * @throws jakarta.persistence.PersistenceException при нарушении ограничения уникальности БД (гонка)
     */
    public User createUser(String email, UserRole role) {
        User user = new User(email, role);

        // Валидация входных данных
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        // Проверка дубликата email
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException(
                    "Пользователь с адресом электронной почты " + email + " уже существует");
        }

        User saved = userRepository.saveAndFlush(user);
        return saved;
    }

    /**
     * Находит пользователя по идентификатору.
     * <p>
     * Метод выполняется внутри транзакции чтения, поэтому доступ к скалярным
     * полям пользователя безопасен. Произвольные ленивые коллекции не инициализируются.
     * </p>
     *
     * @param userId идентификатор пользователя
     * @return пользователь с заданным идентификатором
     * @throws ResourceNotFoundException если пользователь с заданным идентификатором не найден
     */
    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Пользователь с идентификатором " + userId + " не найден"));
    }

    /**
     * Создаёт нового пользователя из переданного DTO.
     * <p>
     * Переносит поля DTO в сущность {@code User}, проверяет уникальность
     * адреса электронной почты и сохраняет в базу данных.
     * </p>
     *
     * @param dto данные для создания пользователя
     * @return сохранённый пользователь
     * @throws IllegalStateException если пользователь с таким адресом электронной почты уже существует
     */
    public User createUser(UserCreateDto dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalStateException(
                    "Пользователь с адресом электронной почты " + dto.getEmail() + " уже существует");
        }

        User user = new User(dto.getEmail(), dto.getRole());
        return userRepository.saveAndFlush(user);
    }

    /**
     * Полностью обновляет email и роль пользователя в одной транзакции.
     * Уникальность проверяется сервисом и ограничением базы при немедленном сохранении.
     *
     * @param userId идентификатор изменяемого пользователя
     * @param dto обязательные новые email и роль
     * @return сохранённый пользователь
     * @throws ResourceNotFoundException если пользователь не найден
     * @throws IllegalArgumentException если DTO отсутствует
     * @throws ConstraintViolationException если поля не прошли проверку
     * @throws IllegalStateException если email занят или нарушено ограничение целостности
     */
    public User updateUser(Long userId, UserUpdateDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Пользователь с идентификатором " + userId + " не найден"));
        if (dto == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }
        Set<ConstraintViolation<UserUpdateDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        userRepository.findByEmail(dto.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new IllegalStateException("Указанный адрес электронной почты уже используется");
            }
        });
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException(
                    "Не удалось обновить пользователя: адрес электронной почты занят или нарушены ограничения данных",
                    exception);
        }
    }

    /**
     * Удаляет пользователя по идентификатору.
     * <p>
     * Проверяет существование пользователя и удаляет его из базы данных.
     * Если у пользователя есть связанные данные, удаление может завершиться
     * ошибкой на уровне базы данных (внешние ключи).
     * </p>
     *
     * @param userId идентификатор пользователя для удаления
     * @throws ResourceNotFoundException если пользователь с заданным идентификатором не найден
     * @throws jakarta.persistence.PersistenceException при нарушении ограничений целостности БД
     */
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException(
                    "Пользователь с идентификатором " + userId + " не найден");
        }
        userRepository.deleteById(userId);
    }
}
