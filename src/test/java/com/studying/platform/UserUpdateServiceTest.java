package com.studying.platform;

import com.studying.platform.dto.UserUpdateDto;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.repository.UserRepository;
import com.studying.platform.service.UserService;
import jakarta.validation.Validation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Проверяет обработку конфликта уникальности при сохранении изменённого пользователя. */
class UserUpdateServiceTest {

    /** Проверяет валидацию DTO при прямом вызове сервиса до изменения сущности. */
    @Test
    void updateUser_invalidDto_shouldNotModifyOrSaveUser() {
        UserRepository repository = mock(UserRepository.class);
        User user = new User("before@test.io", UserRole.STUDENT);
        user.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        UserUpdateDto dto = new UserUpdateDto();
        dto.setEmail("invalid-email");
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            UserService service = new UserService(repository, factory.getValidator());
            assertThrows(ConstraintViolationException.class, () -> service.updateUser(1L, dto));
            assertEquals("before@test.io", user.getEmail());
            assertEquals(UserRole.STUDENT, user.getRole());
            verify(repository, never()).saveAndFlush(any(User.class));
        }
    }

    /** Проверяет клиентское сообщение, если конфликт выявлен базой после предварительной проверки. */
    @Test
    void updateUser_databaseConflict_shouldReturnSafeBusinessError() {
        UserRepository repository = mock(UserRepository.class);
        User user = new User("before@test.io", UserRole.STUDENT);
        user.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.findByEmail("new@test.io")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("внутренняя информация SQL"));
        UserUpdateDto dto = new UserUpdateDto();
        dto.setEmail("new@test.io");
        dto.setRole(UserRole.TEACHER);
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            UserService service = new UserService(repository, factory.getValidator());
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> service.updateUser(1L, dto));
            assertTrue(exception.getMessage().contains("адрес электронной почты"));
            assertFalse(exception.getMessage().contains("SQL"));
            verify(repository).saveAndFlush(user);
        }
    }
}
