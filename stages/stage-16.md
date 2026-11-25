# Этап 16: Основа REST API

## 1. Цель и границы этапа
Реализация базового REST API для управления сущностями «Пользователь» и «Категория».
Этап закладывает основу для всех последующих API-интерфейсов проекта, внедряя:
*   Слоёную архитектуру контроллеров (REST Controllers).
*   DTO (Data Transfer Objects) с валидацией Jakarta Validation.
*   Централизованную обработку ошибок (`GlobalExceptionHandler`) с единым форматом ответа.

**Границы этапа:**
*   Реализуются только контроллеры и сервисы для `User` и `Category`.
*   Тестируются только переданные эндпоинты.
*   Аутентификация и авторизация не реализуются.

## 2. Связанные исходные файлы и компоненты

### 2.1. Существующие файлы (требуют модификации)
1.  **`src/main/java/com/studying/platform/service/UserService.java`**
    *   *Текущее состояние:* Содержит `createUser(String, UserRole)` и `getUser(Long)`.
    *   *Изменения:* Добавить методы `createUser(UserCreateDto)` и `updateUser(Long, UserUpdateDto)`. Методы должны принимать DTO, выполнять маппинг в сущность или передавать DTO в репозиторий (если репозиторий будет мапить). Для чистоты архитектуры сервис должен оперировать DTO.
2.  **`pom.xml`**
    *   *Текущее состояние:* `spring-boot-starter-web` и `spring-boot-starter-validation` уже подключены.
    *   *Изменения:* Нет.

### 2.2. Файлы для создания
1.  **DTO-классы (`dto`)**:
    *   `UserCreateDto.java` (поля: `email`, `name`, `role`).
    *   `UserUpdateDto.java` (поле: `name`).
    *   `UserResponseDto.java` (поля: `id`, `email`, `name`, `role`).
    *   `CategoryCreateDto.java` (поле: `name`).
    *   `CategoryResponseDto.java` (поля: `id`, `name`).
    *   `ErrorResponseDto.java` (общий формат ответа при ошибках: `timestamp`, `status`, `error`, `message`, `path`).
2.  **Обработка ошибок (`exception`)**:
    *   `GlobalExceptionHandler.java` — `@RestControllerAdvice`, перехватывает `ResourceNotFoundException` (404), `MethodArgumentNotValidException` (400) и общий `Exception` (500).
3.  **Сервисный слой (`service`)**:
    *   `CategoryService.java` (новый). Аналогичен `UserService`, но для категорий.
4.  **Контроллеры (`controller`)**:
    *   `UserController.java` (REST API для пользователей).
    *   `CategoryController.java` (REST API для категорий).
5.  **Тесты (`test`)**:
    *   `RestApiTests.java` — интеграционные тесты (Testcontainers) для проверки CRUD, валидации и обработки ошибок.

## 3. Пошаговый план реализации

### Шаг 1. Создание DTO
Создать DTO-классы с аннотациями валидации (`@NotNull`, `@NotBlank`, `@Email`, `@Size` для строк).
*   `UserCreateDto`: `email`, `name`, `role` (enum `UserRole`).
*   `UserUpdateDto`: `name`.
*   `UserResponseDto`: `id`, `email`, `name`, `role`.
*   `CategoryCreateDto`: `name`.
*   `CategoryResponseDto`: `id`, `name`.
*   `ErrorResponseDto`: `timestamp`, `status`, `error`, `message`, `path`.

### Шаг 2. Глобальная обработка ошибок
Создать `GlobalExceptionHandler`.
*   Метод `handleResourceNotFoundException` -> `ResponseEntity.status(404).body(ErrorResponseDto)`.
*   Метод `handleValidationException` -> `ResponseEntity.status(400).body(ErrorResponseDto)` с извлечением сообщений об ошибках валидации.
*   Метод `handleGenericException` -> `ResponseEntity.status(500)`.

### Шаг 3. Сервисный слой (CategoryService)
Создать `CategoryService` на основе паттерна `UserService`.
*   Внедрить `CategoryRepository`.
*   Метод `createCategory(CategoryCreateDto)`: валидация, проверка уникальности имени, сохранение.
*   Метод `getCategory(Long id)`: поиск, выброс `ResourceNotFoundException`.
*   Метод `updateCategory(Long id, CategoryUpdateDto)` (создать DTO для обновления, если нужно, или использовать `CategoryCreateDto` + `id`).
*   Метод `deleteCategory(Long id)`.

### Шаг 4. Контроллер Users
Создать `UserController` (`@RequestMapping("/api/users")`).
*   `POST /`: вызов `UserService.createUser(UserCreateDto)`. Код `201`.
*   `GET /{id}`: вызов `UserService.getUser(Long)`. Код `200`.
*   `PUT /{id}`: вызов `UserService.updateUser(Long, UserUpdateDto)`. Код `200`.
*   `DELETE /{id}`: вызов `UserService.deleteUser(Long)`. Код `204`.

### Шаг 5. Контроллер Categories
Создать `CategoryController` (`@RequestMapping("/api/categories")`).
*   `POST /`: вызов `CategoryService.createCategory(CategoryCreateDto)`. Код `201`.
*   `GET /`: вызов `CategoryService.findAll()`. Код `200`.
*   `GET /{id}`: вызов `CategoryService.getCategory(Long)`. Код `200`.
*   `PUT /{id}`: вызов `CategoryService.updateCategory(Long, CategoryUpdateDto)`. Код `200`.
*   `DELETE /{id}`: вызов `CategoryService.deleteCategory(Long)`. Код `204`.

### Шаг 6. Обновление UserService
Модифицировать `UserService` для поддержки DTO.
*   Добавить `createUser(UserCreateDto)`.
*   Добавить `updateUser(Long, UserUpdateDto)`.
*   Добавить `deleteUser(Long)`.
*   Оставить старые методы для обратной совместимости или удалить, если они не используются в тестах этапа 1-15. (Для чистоты лучше оставить старые методы как `@Deprecated` или использовать DTO внутри новых методов, вызывая старые). *Решение:* Создать методы-обертки над DTO, которые используют существующую логику.

### Шаг 7. Интеграционные тесты
Создать `RestApiTests` (используя `WebApplicationContext` и `MockMvc`).
*   Проверка успешного создания/чтения/обновления/удаления пользователя и категории.
*   Проверка 400 при невалидном JSON (например, некорректный email).
*   Проверка 404 при обращении к несуществующему ID.
*   Проверка формата JSON в `ErrorResponseDto`.

## 4. Критерии приёмки

1.  **Наличие эндпоинтов:**
    *   `POST /api/users`, `GET /api/users/{id}`, `PUT /api/users/{id}`, `DELETE /api/users/{id}`.
    *   `POST /api/categories`, `GET /api/categories`, `GET /api/categories/{id}`, `PUT /api/categories/{id}`, `DELETE /api/categories/{id}`.
2.  **Коды ответов:**
    *   `200 OK` для успешных GET/PUT.
    *   `201 Created` для успешных POST.
    *   `204 No Content` для успешных DELETE.
    *   `400 Bad Request` при некорректном вводе (невалидный email, пустое имя).
    *   `404 Not Found` при отсутствии объекта по ID.
3.  **Формат ошибок:**
    *   Все ошибки возвращаются в едином JSON-формате (`ErrorResponseDto`).
    *   В ответе присутствуют поля `status`, `error`, `message`.
4.  **Валидация:**
    *   Сервер отклоняет запросы с невалидными данными и возвращает конкретные сообщения об ошибках валидации.

## 5. Команды проверки

1.  **Сборка проекта:**
    ```bash
    mvn clean verify -DskipTests
    ```
2.  **Запуск тестов этапа:**
    ```bash
    mvn test -Dtest=RestApiTests
    ```
3.  **Проверка покрытия тестом нового кода:**
    ```bash
    mvn jacoco:report
    ```
    (После добавления плагина JaCoCo, если он еще не настроен; иначе стандартная проверка в CI/IDE).

## 6. Ограничения, риски и блокеры

*   **Риски:** Конфликт существующих методов `UserService` с новыми сигнатурами.
    *   *Решение:* Использовать оверлоады или адаптеры внутри сервиса.
*   **Риски:** Отсутствие `CategoryService`.
    *   *Решение:* Создать новый файл.
*   **ADR:** Не требуется. Использование Spring MVC и стандартных паттернов валидации является общепринятым и не требует фиксации архитектурного решения.
*   **Блокеры:** Отсутствие.
