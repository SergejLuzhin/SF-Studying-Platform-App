package com.studying.platform;

import com.studying.platform.entity.Profile;
import com.studying.platform.entity.User;
import com.studying.platform.entity.UserRole;
import com.studying.platform.repository.ProfileRepository;
import com.studying.platform.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для сущностей «Пользователь» и «Профиль».
 * <p>
 * Проверяют корректное создание, сохранение, чтение и удаление пользователей и профилей,
 * а также соблюдение ограничений базы данных: уникальность логина, обязательность полей,
 * корректность связи «один-к-одному».
 * </p>
 * <p>
 * Тесты выполняются с встроенной H2-базой через {@code @DataJpaTest}.
 * Исключения проверяются на уровне Hibernate (без Spring-обёртки).
 * </p>
 */
@DataJpaTest
class UserAndProfileTests {

    /**
     * Менеджер сущностей для ручного управления транзакциями.
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * Репозиторий для сущности User.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Репозиторий для сущности Profile.
     */
    @Autowired
    private ProfileRepository profileRepository;

    /**
     * Проверка: сохранение пользователя и профиля с проверкой связи.
     * <p>
     * Сначала сохраняется пользователь, затем профиль с привязкой к нему.
     * Проверяется, что оба объекта корректно сохраняются и считываются,
     * а также что связь «один-к-одному» работает.
     * </p>
     */
    @Test
    void saveUserWithProfile_shouldPersistAndRetrieve() {
        // Создание пользователя
        User user = new User("alice@example.com", "password123", UserRole.STUDENT);
        entityManager.persist(user);
        entityManager.flush();

        // Создание профиля с привязкой к сохранённому пользователю
        Profile profile = new Profile(user, "Алиса Иванова");
        profile.setBio("Студентка, изучает Java.");
        profile.setAvatarUrl("https://example.com/alice.jpg");
        entityManager.persist(profile);
        entityManager.flush();

        // Проверка: оба объекта сохранены
        Long profileId = profile.getId();
        assertNotNull(profileId, "Идентификатор профиля не должен быть null после сохранения");

        Long userId = user.getId();
        assertNotNull(userId, "Идентификатор пользователя не должен быть null после сохранения");

        // Чтение профиля по ID
        Profile savedProfile = profileRepository.findById(profileId).orElseThrow();
        assertNotNull(savedProfile.getDisplayName(), "Отображаемое имя должно быть сохранено");
        assertEquals("Алиса Иванова", savedProfile.getDisplayName());
        assertEquals("Студентка, изучает Java.", savedProfile.getBio());
        assertEquals("https://example.com/alice.jpg", savedProfile.getAvatarUrl());

        // Чтение пользователя по ID
        User savedUser = userRepository.findById(userId).orElseThrow();
        assertEquals("alice@example.com", savedUser.getLogin());
        assertEquals("password123", savedUser.getPassword());
        assertEquals(UserRole.STUDENT, savedUser.getRole());
    }

    /**
     * Проверка: поиск пользователя по логину.
     * <p>
     * После сохранения пользователя проверяет, что метод {@code findByLogin}
     * возвращает корректный результат.
     * </p>
     */
    @Test
    void findByLogin_shouldReturnUser() {
        User user = new User("bob@example.com", "securepass", UserRole.TEACHER);
        entityManager.persist(user);
        entityManager.flush();

        // Поиск по логину
        var found = userRepository.findByLogin("bob@example.com");
        assertTrue(found.isPresent(), "Пользователь с логином bob@example.com должен быть найден");
        assertEquals(UserRole.TEACHER, found.get().getRole());
    }

    /**
     * Проверка: поиск пользователя по несуществующему логину.
     * <p>
     * Гарантирует, что метод {@code findByLogin} возвращает пустое значение
     * для отсутствующего логина.
     * </p>
     */
    @Test
    void findByLogin_shouldReturnEmptyForNonExistentLogin() {
        var found = userRepository.findByLogin("nonexistent@example.com");
        assertTrue(found.isEmpty(), "Пользователь с несуществующим логином не должен быть найден");
    }

    /**
     * Проверка: уникальность логина пользователя.
     * <p>
     * Пытается сохранить двух пользователей с одинаковым логином
     * и проверяет, что база данных выбрасывает исключение о нарушении целостности данных.
     * </p>
     */
    @Test
    void saveUser_duplicateLogin_shouldThrowDataIntegrityViolation() {
        User user1 = new User("unique@example.com", "pass1", UserRole.STUDENT);
        entityManager.persist(user1);
        entityManager.flush();

        User user2 = new User("unique@example.com", "pass2", UserRole.TEACHER);

        // Ожидается: нарушение уникального ограничения логина
        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(user2);
            entityManager.flush();
        }, "Сохранение пользователя с дублирующим логином должно вызвать нарушение целостности данных");
    }

    /**
     * Проверка: обязательность поля displayName в профиле.
     * <p>
     * Пытается создать профиль с {@code null} отображаемым именем
     * и проверяет, что база данных отвергает такую операцию.
     * </p>
     */
    @Test
    void saveProfile_nullDisplayName_shouldThrow() {
        User user = new User("noread@example.com", "pass", UserRole.STUDENT);
        entityManager.persist(user);
        entityManager.flush();

        Profile profile = new Profile(user, null);

        assertThrows(PropertyValueException.class, () -> {
            entityManager.persist(profile);
            entityManager.flush();
        }, "Сохранение профиля с null displayName должно вызвать ошибку значения свойства");
    }

    /**
     * Проверка: обязательность связи user в профиле.
     * <p>
     * Пытается создать профиль без привязки к пользователю
     * и проверяет, что Hibernate отвергает такую операцию.
     * </p>
     */
    @Test
    void saveProfile_nullUser_shouldThrow() {
        Profile profile = new Profile(null, "Иван Петров");

        assertThrows(PropertyValueException.class, () -> {
            entityManager.persist(profile);
            entityManager.flush();
        }, "Сохранение профиля без пользователя должно вызвать ошибку значения свойства");
    }

    /**
     * Проверка: уникальность user_id в профиле.
     * <p>
     * Пытается создать два профиля для одного пользователя
     * и проверяет, что база данных отвергает дубликат.
     * </p>
     */
    @Test
    void saveProfile_multipleForSameUser_shouldThrow() {
        User user = new User("twoprofile@example.com", "pass", UserRole.STUDENT);
        entityManager.persist(user);
        entityManager.flush();

        Profile profile1 = new Profile(user, "Первый профиль");
        entityManager.persist(profile1);
        entityManager.flush();

        Profile profile2 = new Profile(user, "Второй профиль");

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(profile2);
            entityManager.flush();
        }, "Создание второго профиля для одного пользователя должно вызвать нарушение целостности данных");
    }

    /**
     * Проверка: связь «один-к-одному» — профиль загружается с ссылкой на пользователя.
     * <p>
     * Сохраняет профиль с привязкой к пользователю, затем загружает профиль
     * по ID и проверяет, что ссылка на пользователя доступна.
     * </p>
     */
    @Test
    void loadProfile_shouldHaveUserReference() {
        User user = new User("reflink@example.com", "pass", UserRole.ADMIN);
        entityManager.persist(user);
        entityManager.flush();

        Profile profile = new Profile(user, "Администратор");
        profile.setBio("Руководитель проекта.");
        entityManager.persist(profile);
        entityManager.flush();

        // Загрузка профиля
        Profile loaded = profileRepository.findById(profile.getId()).orElseThrow();
        assertNotNull(loaded.getUser(), "Профиль должен содержать ссылку на пользователя");
        assertEquals("reflink@example.com", loaded.getUser().getLogin());
        assertEquals(UserRole.ADMIN, loaded.getUser().getRole());
        assertEquals("Администратор", loaded.getDisplayName());
    }

    /**
     * Проверка: обновление профиля пользователя.
     * <p>
     * Сохраняет профиль, обновляет его отображаемое имя и биографию,
     * затем проверяет, что изменения корректно сохранены.
     * </p>
     */
    @Test
    void updateProfile_shouldChangeData() {
        User user = new User("update@example.com", "pass", UserRole.STUDENT);
        entityManager.persist(user);
        entityManager.flush();

        Profile profile = new Profile(user, "Начальное имя");
        entityManager.persist(profile);
        entityManager.flush();

        // Обновление
        Profile loaded = profileRepository.findById(profile.getId()).orElseThrow();
        loaded.setDisplayName("Обновлённое имя");
        loaded.setBio("Новая биография студента.");
        entityManager.merge(loaded);
        entityManager.flush();

        // Проверка обновлённых данных
        Profile updated = profileRepository.findById(profile.getId()).orElseThrow();
        assertEquals("Обновлённое имя", updated.getDisplayName());
        assertEquals("Новая биография студента.", updated.getBio());
    }

    /**
     * Проверка: удаление профиля из базы данных.
     * <p>
     * Сохраняет профиль с привязкой к пользователю, затем удаляет профиль
     * и проверяет, что данные корректно удаляются.
     * </p>
     */
    @Test
    void deleteProfile_shouldRemoveFromDatabase() {
        User user = new User("delete@example.com", "pass", UserRole.TEACHER);
        entityManager.persist(user);
        entityManager.flush();

        Profile profile = new Profile(user, "Удаляемый профиль");
        entityManager.persist(profile);
        entityManager.flush();

        Long profileId = profile.getId();

        // Удаление профиля
        profileRepository.deleteById(profileId);
        entityManager.flush();

        // Проверка: профиль удалён
        assertTrue(profileRepository.findById(profileId).isEmpty(),
                "Профиль должен быть удалён из базы данных");
    }

    /**
     * Проверка: чтение профиля по ID пользователя.
     * <p>
     * Создаёт пользователя и профиль, затем ищет профиль через репозиторий
     * по {@code userId}. Проверяет, что связь через внешний ключ работает корректно.
     * </p>
     */
    @Test
    void findByUserId_shouldReturnProfile() {
        User user = new User("userid@example.com", "pass", UserRole.STUDENT);
        entityManager.persist(user);
        entityManager.flush();

        Profile profile = new Profile(user, "Поиск по ID");
        entityManager.persist(profile);
        entityManager.flush();

        var found = profileRepository.findByUserId(user.getId());
        assertTrue(found.isPresent(), "Профиль по ID пользователя должен быть найден");
        assertEquals("Поиск по ID", found.get().getDisplayName());
    }
}
