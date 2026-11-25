# Этап 07. Управление курсом — `pending`

## Цель и границы этапа

Реализовать сервисный слой для управления курсами, модулями и уроками в рамках сущной `CourseService`. Реализовать бизнес-логику создания, обновления, добавления содержимого и получения полной структуры курса. Добавить интеграционные тесты, проверяющие корректность работы сервисного слоя, включая проверки валидации ролей и существования внешних сущностей.

Этот этап завершает работу над базовой структурой «Курс → Модуль → Урок» и подготавливает почву для реализации записей на курс (Этап 08).

## Связанные исходные файлы и применимые ADR

### Исходные файлы (чтение):
- `src/main/java/com/studying/platform/entity/Course.java`
- `src/main/java/com/studying/platform/entity/Module.java`
- `src/main/java/com/studying/platform/entity/Lesson.java`
- `src/main/java/com/studying/platform/entity/User.java`
- `src/main/java/com/studying/platform/entity/Category.java`
- `src/main/java/com/studying/platform/repository/CourseRepository.java`
- `src/main/java/com/studying/platform/repository/ModuleRepository.java`
- `src/main/java/com/studying/platform/repository/LessonRepository.java`
- `src/main/java/com/studying/platform/repository/UserRepository.java`
- `src/main/java/com/studying/platform/repository/CategoryRepository.java`

### Создаваемые/изменяемые файлы:
1.  `src/main/java/com/studying/platform/service/CourseService.java` (Создание).
2.  `src/main/java/com/studying/platform/exception/ResourceNotFoundException.java` (Создание — для единообразной обработки ошибок, если ещё не создан).
3.  `src/test/java/com/studying/platform/CourseManagementTests.java` (Создание).

### ADR:
- Архитектурных решений (ADR), ограничивающих данный этап, не выявлено.

## Порядок действий

### 1. Создание исключения для отсутствующих объектов
- Создать класс `ResourceNotFoundException` в пакете `com.studying.platform.exception`.
- Исключение должно наследоваться от `RuntimeException`.
- Должно содержать конструктор с параметрами `(String message)` и `(String message, Throwable cause)`.
- Добавить JavaDoc на русском языке.

### 2. Реализация `CourseService`
- Создать класс `CourseService` в пакете `com.studying.platform.service`.
- Аннотировать класс `@Service` и `@Transactional`.
- Внедрить зависимости через конструктор (рекомендуемый подход):
  - `CourseRepository courseRepository`
  - `ModuleRepository moduleRepository`
  - `LessonRepository lessonRepository`
  - `UserRepository userRepository`
  - `CategoryRepository categoryRepository`

#### Методы сервиса:

1.  **`createCourse(...)`**
    -   **Параметры:** `title`, `description`, `duration`, `startDate`, `teacherId`, `categoryId`.
    -   **Логика:**
        -   Найти пользователя по `teacherId` через `userRepository.findById(teacherId)`. Если отсутствует — выбросить `ResourceNotFoundException`.
        -   Проверить, что у пользователя роль `TEACHER`. Если нет — выбросить `ResourceNotFoundException` (или специфическое исключение валидации).
        -   Найти категорию по `categoryId` через `categoryRepository.findById(categoryId)`. Если отсутствует — выбросить `ResourceNotFoundException`.
        -   Создать объект `Course`, установить поля.
        -   Сохранить курс через `courseRepository.save(course)`.
        -   Вернуть сохранённый курс.

2.  **`updateCourse(...)`**
    -   **Параметры:** `courseId`, обновляемые поля (`title`, `description`, `duration`, `startDate`).
    -   **Логика:**
        -   Найти курс по `courseId`. Если отсутствует — выбросить `ResourceNotFoundException`.
        -   Обновить поля объекта (сеттеры).
        -   Сохранить через `courseRepository.save(course)` или `entityManager.merge`.
        -   Вернуть обновлённый курс.

3.  **`addModule(...)`**
    -   **Параметры:** `courseId`, `title`, `description`, `orderIndex`.
    -   **Логика:**
        -   Найти курс по `courseId`. Если отсутствует — выбросить `ResourceNotFoundException`.
        -   Создать `Module`, установить поля и ссылку на курс (`setCourse(course)`).
        -   Добавить модуль в коллекцию модулей курса (`course.getModules().add(module)`).
        -   Сохранить курс (сработает каскадное сохранение модуля).
        -   Вернуть созданный модуль.

4.  **`addLesson(...)`**
    -   **Параметры:** `moduleId`, `title`, `content`, `videoUrl`.
    -   **Логика:**
        -   Найти модуль по `moduleId`. Если отсутствует — выбросить `ResourceNotFoundException`.
        -   Создать `Lesson`, установить поля и ссылку на модуль (`setModule(module)`).
        -   Добавить урок в коллекцию уроков модуля (`module.getLessons().add(lesson)`).
        -   Сохранить модуль (сработает каскадное сохранение урока).
        -   Вернуть созданный урок.

5.  **`getCourseStructure(...)`**
    -   **Параметры:** `courseId`.
    -   **Логика:**
        -   Найти курс по `courseId`. Если отсутствует — выбросить `ResourceNotFoundException`.
        -   Так как метод помечен `@Transactional` (или аннотацией на уровне класса), можно безопасно обращаться к ленивым коллекциям (`modules`, `lessons`).
        -   Инициализировать коллекцию модулей (`course.getModules().size()`) и для каждого модуля инициализировать коллекцию уроков.
        -   Вернуть объект `Course` с полной структурой.

### 3. Написание интеграционных тестов (`CourseManagementTests`)
- Создать класс `CourseManagementTests` в пакете `com.studying.platform`.
- Аннотировать класс `@DataJpaTest`.
- Внедрить `EntityManager`, все репозитории, и новый `CourseService`.
- Пометить тесты аннотацией `@Transactional` (для автоматического отката изменений между тестами).

**Необходимые тест-кейсы:**

1.  **`testCreateCourse_Success`**: Создать преподавателя и категорию, вызвать `createCourse`. Проверить, что курс сохранён и ссылки установлены.
2.  **`testCreateCourse_NonExistentTeacher`**: Вызвать `createCourse` с несуществующим `teacherId`. Проверить выброс `ResourceNotFoundException`.
3.  **`testCreateCourse_WrongRole`**: Создать пользователя со ролью `STUDENT`, вызвать `createCourse`. Проверить выброс исключения (роль не `TEACHER`).
4.  **`testCreateCourse_NonExistentCategory`**: Вызвать `createCourse` с несуществующим `categoryId`. Проверить выброс `ResourceNotFoundException`.
5.  **`testUpdateCourse_Success`**: Создать курс, вызвать `updateCourse`, проверить обновлённые данные.
6.  **`testUpdateCourse_NonExistent`**: Вызвать `updateCourse` с отсутствующим `courseId`. Проверить исключение.
7.  **`testAddModule_Success`**: Создать курс, вызвать `addModule`. Проверить, что модуль добавлен в курс и сохранён.
8.  **`testAddLesson_Success`**: Создать курс и модуль, вызвать `addLesson`. Проверить, что урок добавлен в модуль и сохранён.
9.  **`testGetCourseStructure_Success`**: Создать курс с модулем и уроком, вызвать `getCourseStructure`. Проверить, что структура полностью загружена (модули и уроки не `null` и не пусты).
10. **`testGetCourseStructure_NonExistent`**: Проверить исключение при отсутствии курса.

## Критерии приёмки и тест-кейсы

### Критерии приёмки
1.  Класс `CourseService` существует и содержит все требуемые методы.
2.  Все методы сервиса помечены транзакционностью.
3.  Валидация роли преподавателя (`TEACHER`) реализована.
4.  Исключения `ResourceNotFoundException` выбрасываются при отсутствии сущностей.
5.  Каскадное сохранение модулей и уроков через сервис работает корректно.
6.  Метод `getCourseStructure` возвращает загруженные коллекции (без `LazyInitializationException`).

### Тест-кейсы (включая негативные)
-   Позитивные:
    -   Создание курса.
    -   Обновление курса.
    -   Добавление модуля.
    -   Добавление урока.
    -   Получение структуры.
-   Негативные:
    -   Преподаватель не найден.
    -   Преподаватель не имеет роль `TEACHER`.
    -   Категория не найдена.
    -   Курс/Модуль не найдены при обновлении/добавлении содержимого.

## Команды проверки

Все команды выполняются из корня проекта:

1.  **Компиляция:**
    ```bash
    mvn clean compile -q
    ```
2.  **Запуск тестов:**
    ```bash
    mvn test -q
    ```
    -   Должны пройти все существующие тесты (Этапы 01–06) и новые тесты (`CourseManagementTests`).
3.  **Проверка покрытия (опционально, если настроен):**
    ```bash
    mvn verify -q
    ```

## Ограничения, риски и блокеры

-   **Риски:** Ошибки в каскадных операциях (дублирование данных или потеря связей). Необходимо тщательно тестировать `addModule` и `addLesson`.
-   **Риски:** `LazyInitializationException` при чтении структуры вне транзакции. Решение: использование `@Transactional` в сервисе и корректная инициализация коллекций.
-   **Ограничения:** На данном этапе не реализуются права доступа на основе ролей (кроме проверки роли TEACHER при создании). Это будет сделано позже или через REST, если потребуется.
-   **Блокеры:** Отсутствуют.
