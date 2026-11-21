# Этап 04: Пользователи и профили

## Цель и границы этапа
Реализовать базовую доменную модель проекта: сущность `User` (с ролями и логином) и сущность `Profile` (с текстовым описанием и ссылкой на пользователя). Настроить связь «один-к-одному» (1:1) и репозитории Spring Data JPA. Валидация данных обеспечивается на уровне базы данных (ограничения `NOT NULL` и `UNIQUE`).

## Связанные файлы и применимые ADR
- **Файлы проекта:**
  - `src/main/resources/application.yml` / `application-test.yml` (контекст БД уже настроен).
  - `src/main/java/com/studying/platform/StudyingPlatformApplication.java` (точка входа).
  - `src/test/java/com/studying/platform/StudyingPlatformApplicationTests.java` (шаблон для интеграционных тестов).
- **Документация:**
  - `docs/data-model.md` (описание схемы данных для User и Profile).
- **ADR:**
  - Существующих утверждённых ADR (статус `Accepted`), которые бы ограничивали данный этап, нет. Индекс ADR пуст.

## Пошаговый план реализации

1.  **Подготовка пакета сущностей**
    - Создать пакет `com.studying.platform.entity`.
    - Создать пакет `com.studying.platform.repository`.

2.  **Реализация Enum `UserRole`**
    - Создать файл `UserRole.java`.
    - Включить значения: `STUDENT`, `TEACHER`, `ADMIN`.

3.  **Реализация сущности `User`**
    - Создать файл `User.java`.
    - Поля:
      - `id` (`Long`) — идентификатор, генерация стратегии `IDENTITY` или `SEQUENCE` (по умолчанию для JPA).
      - `login` (`String`) — уникальный логин/почта. Аннотация `@Column(unique = true, nullable = false)`.
      - `password` (`String`) — хеш пароля. Аннотация `@Column(nullable = false)`.
      - `role` (`UserRole`) — роль пользователя. Аннотация `@Enumerated(EnumType.STRING)`.
    - Связь: `@OneToOne(mappedBy = "user")` для поля `profile` (сущность `Profile` является владельцем связи).

4.  **Реализация сущности `Profile`**
    - Создать файл `Profile.java`.
    - Поля:
      - `id` (`Long`) — идентификатор.
      - `displayName` (`String`) — отображаемое имя. Аннотация `@Column(nullable = false, length = 100)`.
      - `bio` (`String`) — краткая биография. Аннотация `@Lob` (TEXT).
      - `avatarUrl` (`String`) — ссылка на аватар. Аннотация `@Column(length = 500)`.
      - `userId` (`Long`) — внешний ключ. Аннотация `@Column(name = "user_id", unique = true, nullable = false)`.
      - `user` (`User`) — обратная ссылка. Аннотации `@OneToOne`, `@JoinColumn(name = "user_id", unique = true, nullable = false, foreignKey = @ForeignKey(name = "fk_profile_user"))`.
    - Каскадирование: `cascade = {CascadeType.PERSIST, CascadeType.MERGE}` (рекомендуется размещать на стороне `User` в поле `profile` для сохранения вместе с пользователем, либо согласно спецификации БД на владельце, если требуется принудительное сохранение профиля). *Примечание для реализатора: поместить каскады на поле `profile` в сущности `User` для удобства CRUD.*

5.  **Реализация репозиториев**
    - Создать `UserRepository.java` (наследник `CrudRepository<User, Long>`).
    - Создать `ProfileRepository.java` (наследник `CrudRepository<Profile, Long>`).
    - Дополнительно в `UserRepository`: метод `Optional<User> findByLogin(String login)` (если не генерируется автоматически, но Spring Data генерирует его автоматически).

6.  **Написание интеграционных тестов**
    - Создать тестовый класс `UserAndProfileIntegrationTests.java` в пакете тестов.
    - Использовать аннотацию `@DataJpaTest` и `@Testcontainers`.
    - Реализовать тест-кейсы:
      - Сохранение пользователя и профиля (проверка `save` и `findById`).
      - Проверка связи (загрузка профиля вместе с пользователем).
      - Проверка уникальности `login` (перехват исключения `DataIntegrityViolationException` при дубле).
      - Проверка обязательности `displayName` (перехват исключения при сохранении профиля без имени).

## Критерии приёмки и тест-кейсы

1.  **Сущности созданы и маппятся корректно:**
    - Таблицы `users` и `profiles` создаются при старте приложения (или миграции).
    - В таблице `profiles` есть колонка `user_id` с ограничением `UNIQUE`.

2.  **Базовый CRUD:**
    - Можно сохранить `User` и создать для него `Profile`, привязав по ID.
    - Можно прочитать `Profile` и получить ссылку на `User`.

3.  **Ограничения данных (Negative Scenarios):**
    - Попытка создать двух пользователей с одинаковым `login` должна приводить к ошибке сохранения.
    - Попытка создать `Profile` без `displayName` должна приводить к ошибке сохранения.

## Команды проверки
- Запуск тестов: `./mvnw test` (или `mvn test`).
- Проверка генерации DDL в консоли: запуск приложения с логом уровня `DEBUG` для `org.hibernate.SQL` и `org.hibernate.type.descriptor.sql.BasicBinder`.

## Ограничения, риски и блокеры
- **Риски:** При создании сущности `Profile` необходимо убедиться, что `user_id` действительно уникальный, чтобы не нарушить логику «один к одному».
- **Ограничения:** На данном этапе не реализуются методы аутентификации (BCrypt), поле `password` просто сохраняется как строка.
- **Блокеры:** Отсутствуют.

## Список создаваемых/изменяемых файлов
1.  `src/main/java/com/studying/platform/entity/UserRole.java` (создание)
2.  `src/main/java/com/studying/platform/entity/User.java` (создание)
3.  `src/main/java/com/studying/platform/entity/Profile.java` (создание)
4.  `src/main/java/com/studying/platform/repository/UserRepository.java` (создание)
5.  `src/main/java/com/studying/platform/repository/ProfileRepository.java` (создание)
6.  `src/test/java/com/studying/platform/UserAndProfileTests.java` (создание)

STATUS: READY