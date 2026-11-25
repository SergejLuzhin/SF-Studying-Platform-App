# Этап 17. API курсов и содержания — `completed`

## Цель и границы этапа

Реализовать REST API для управления курсами, модулями и уроками учебной платформы.
Эндпоинты обеспечивают создание, чтение, обновление, удаление и получение полной структуры курса,
а также CRUD-операции для модулей и уроков. При удалении курса бизнес-правила (записи, решения,
результаты тестов, отзывы) проверяются на уровне сервисного слоя и возвращают ошибку 400
через существующий `GlobalExceptionHandler`.

**Включает:**
- Контроллер `CourseController` — CRUD курсов, получение структуры, добавление модулей.
- Контроллер `ModuleController` — CRUD модулей.
- Контроллер `LessonController` — CRUD уроков.
- DTO для запросов и ответов курсов, модулей, уроков и структуры курса.
- Обновление `RestApiTests.java` — интеграционные HTTP-тесты всех эндпоинтов, включая негативные сценарии.

**Не включает:**
- Эндпоинты записи/отписки (Этап 18), заданий/сдач (Этап 18), тестов/отзывов/тегов (Этап 19).
- Аутентификацию и авторизацию (не входят в объём проекта).

## Связанные исходные файлы и применимые ADR

### Существующие файлы (чтение, без изменений, кроме тестов):

| Файл | Назначение |
|---|---|
| `src/main/java/com/studying/platform/entity/Course.java` | Сущность курса |
| `src/main/java/com/studying/platform/entity/Module.java` | Сущность модуля |
| `src/main/java/com/studying/platform/entity/Lesson.java` | Сущность урока |
| `src/main/java/com/studying/platform/entity/Enrollment.java` | Сущность записи (для проверки истории) |
| `src/main/java/com/studying/platform/entity/EnrollmentStatus.java` | Enum статусов записи |
| `src/main/java/com/studying/platform/service/CourseService.java` | Бизнес-логика курсов, модулей, уроков, удаление с проверкой истории |
| `src/main/java/com/studying/platform/repository/CourseRepository.java` | Репозиторий курсов |
| `src/main/java/com/studying/platform/repository/ModuleRepository.java` | Репозиторий модулей |
| `src/main/java/com/studying/platform/repository/LessonRepository.java` | Репозиторий уроков |
| `src/main/java/com/studying/platform/repository/EnrollmentRepository.java` | Репозиторий записей |
| `src/main/java/com/studying/platform/exception/GlobalExceptionHandler.java` | Глобальный обработчик ошибок (IllegalStateException → 400, ResourceNotFoundException → 404) |
| `src/main/java/com/studying/platform/dto/ErrorResponseDto.java` | Единый формат ошибок |

### Файлы, которые будут созданы:

1. `src/main/java/com/studying/platform/controller/CourseController.java`
2. `src/main/java/com/studying/platform/controller/ModuleController.java`
3. `src/main/java/com/studying/platform/controller/LessonController.java`
4. `src/main/java/com/studying/platform/dto/CourseRequestDto.java`
5. `src/main/java/com/studying/platform/dto/CourseResponseDto.java`
6. `src/main/java/com/studying/platform/dto/CourseStructureResponseDto.java`
7. `src/main/java/com/studying/platform/dto/ModuleRequestDto.java`
8. `src/main/java/com/studying/platform/dto/ModuleResponseDto.java`
9. `src/main/java/com/studying/platform/dto/LessonRequestDto.java`
10. `src/main/java/com/studying/platform/dto/LessonResponseDto.java`

### Файл, который будет изменён:

1. `src/test/java/com/studying/platform/RestApiTests.java` — добавление тестов для курсов, модулей, уроков и структуры.

### ADR:

- Архитектурных решений (ADR), ограничивающих данный этап, не выявлено. Индекс `docs/adr/README.md` пуст, существенных архитектурных решений в проекте нет.

## Порядок действий

### 1. DTO для курсов

Создать три DTO-класса в пакете `com.studying.platform.dto`:

#### 1.1 `CourseRequestDto` — запрос создания/обновления курса

- Поля: `title` (NotBlank, Size max 200), `description` (nullable, TEXT), `duration` (NotNull, >= 1), `startDate` (NotNull), `teacherId` (NotNull), `categoryId` (NotNull).
- Конструкторы: по умолчанию (для десериализации) и полный.
- Геттеры и сеттеры.
- JavaDoc на русском для класса, полей, конструкторов, геттеров, сеттеров.

#### 1.2 `CourseResponseDto` — ответ с данными курса

- Поля: `id`, `title`, `description`, `duration`, `startDate`, `teacherId`, `categoryId`, `categoryName`.
- Конструкторы: по умолчанию и полный.
- Геттеры и сеттеры.
- JavaDoc на русском.

#### 1.3 `CourseStructureResponseDto` — ответ со структурой курса (вложенные модули и уроки)

- Поля: `id`, `title`, `description`, `duration`, `startDate`, `teacherId`, `categoryId`, `modules` (список вложенных модулей).
- Вложенный класс `ModuleStructureDto`: `id`, `title`, `description`, `orderIndex`, `lessons` (список уроков).
- Вложенный класс `LessonStructureDto`: `id`, `title`, `content`, `videoUrl`.
- JavaDoc на русском.

### 2. DTO для модулей

Создать два DTO-класса:

#### 2.1 `ModuleRequestDto` — запрос создания/обновления модуля

- Поля: `title` (NotBlank, Size max 200), `description` (nullable, TEXT), `orderIndex` (NotNull).
- JavaDoc, конструкторы, геттеры, сеттеры.

#### 2.2 `ModuleResponseDto` — ответ с данными модуля

- Поля: `id`, `title`, `description`, `orderIndex`, `courseId`.
- JavaDoc, конструкторы, геттеры, сеттеры.

### 3. DTO для уроков

Создать два DTO-класса:

#### 3.1 `LessonRequestDto` — запрос создания/обновления урока

- Поля: `title` (NotBlank, Size max 200), `content` (nullable, TEXT), `videoUrl` (nullable).
- JavaDoc, конструкторы, геттеры, сеттеры.

#### 3.2 `LessonResponseDto` — ответ с данными урока

- Поля: `id`, `title`, `content`, `videoUrl`, `moduleId`.
- JavaDoc, конструкторы, геттеры, сеттеры.

### 4. Контроллер `CourseController`

Создать `CourseController` в пакете `com.studying.platform.controller`:

#### 4.1 `POST /api/courses` — создание курса

- Принимает `CourseRequestDto`, валидация через `@Valid`.
- Вызывает `courseService.createCourse()` с параметрами из DTO.
- Возвращает `ResponseEntity<CourseResponseDto>` с кодом 201.
- JavaDoc на русском.

#### 4.2 `GET /api/courses/{courseId}` — получение курса

- Принимает `@PathVariable Long courseId`.
- Вызывает `courseService.getCourse(courseId)` (или `findById`).
- Возвращает `ResponseEntity<CourseResponseDto>` с кодом 200.
- 404 при отсутствии курса (через `GlobalExceptionHandler`).
- JavaDoc на русском.

#### 4.3 `PUT /api/courses/{courseId}` — обновление курса

- Принимает `@PathVariable Long courseId` и `@RequestBody CourseRequestDto`.
- Вызывает `courseService.updateCourse()` с параметрами из DTO (только не-null поля).
- Возвращает `ResponseEntity<CourseResponseDto>` с кодом 200.
- JavaDoc на русском.

#### 4.4 `DELETE /api/courses/{courseId}` — удаление курса

- Принимает `@PathVariable Long courseId`.
- Вызывает `courseService.deleteCourse()`, которая внутри проверяет наличие записей, решений, результатов тестов и отзывов.
- При наличии истории `IllegalStateException` → `GlobalExceptionHandler` → 400.
- Возвращает `ResponseEntity<Void>` с кодом 204.
- JavaDoc на русском.

#### 4.5 `GET /api/courses/{courseId}/structure` — получение полной структуры курса

- Вызывает `courseService.getCourseStructure(courseId)`.
- Преобразует курс со всеми модулями и уроками в `CourseStructureResponseDto`.
- Возвращает `ResponseEntity<CourseStructureResponseDto>` с кодом 200.
- JavaDoc на русском.

#### 4.6 `POST /api/courses/{courseId}/modules` — добавление модуля к курсу

- Принимает `@PathVariable Long courseId` и `@RequestBody ModuleRequestDto`.
- Вызывает `courseService.addModule(courseId, title, description, orderIndex)`.
- Возвращает `ResponseEntity<ModuleResponseDto>` с кодом 201.
- JavaDoc на русском.

### 5. Контроллер `ModuleController`

Создать `ModuleController` в пакете `com.studying.platform.controller`:

#### 5.1 `GET /api/modules/{moduleId}` — получение модуля

- Возвращает `ModuleResponseDto` с кодом 200.
- JavaDoc на русском.

#### 5.2 `PUT /api/modules/{moduleId}` — обновление модуля

- Принимает `ModuleRequestDto`, обновляет поля модуля через `CourseService` или напрямую.
- Возвращает `ModuleResponseDto` с кодом 200.
- JavaDoc на русском.

#### 5.3 `DELETE /api/modules/{moduleId}` — удаление модуля

- Удаляет модуль через `CourseService` или напрямую через репозиторий.
- Возвращает 204 No Content.
- JavaDoc на русском.

### 6. Контроллер `LessonController`

Создать `LessonController` в пакете `com.studying.platform.controller`:

#### 6.1 `GET /api/lessons/{lessonId}` — получение урока

- Возвращает `LessonResponseDto` с кодом 200.
- JavaDoc на русском.

#### 6.2 `PUT /api/lessons/{lessonId}` — обновление урока

- Принимает `LessonRequestDto`, обновляет поля урока.
- Возвращает `LessonResponseDto` с кодом 200.
- JavaDoc на русском.

#### 6.3 `DELETE /api/lessons/{lessonId}` — удаление урока

- Удаляет урок через `CourseService` или напрямую через репозиторий.
- Возвращает 204 No Content.
- JavaDoc на русском.

### 7. Обновление `RestApiTests.java`

Добавить интеграционные HTTP-тесты в существующий класс `RestApiTests`:

- Использовать существующие паттерны: `MockMvc`, `ObjectMapper`, `TransactionTemplate`, `JdbcTemplate`.
- Добавить вспомогательные методы `createCourse()` и `createModule()` по аналогии с `createUser()` и `createCategory()`.
- Добавить вспомогательный список `createdCourseIds` для очистки данных.
- Обновить `cleanUp()` — добавить удаление модулей (они удаляются каскадом с курсом, поэтому порядок: модули → курсы → категории → пользователи).

## Критерии приёмки

1. Все контроллеры (`CourseController`, `ModuleController`, `LessonController`) существуют, аннотированы `@RestController` и `@RequestMapping`, используют constructor injection и имеют JavaDoc.
2. Все DTO-классы (10 штук) существуют с корректными полями, конструкторами, геттерами, сеттерами и JavaDoc.
3. `POST /api/courses` — возвращает 201 с `CourseResponseDto`.
4. `GET /api/courses/{id}` — возвращает 200 с `CourseResponseDto`, 404 при отсутствии.
5. `PUT /api/courses/{id}` — возвращает 200 с обновлённым `CourseResponseDto`, 404 при отсутствии.
6. `DELETE /api/courses/{id}` — возвращает 204 при успешном удалении, 404 при отсутствии, 400 при наличии истории.
7. `GET /api/courses/{id}/structure` — возвращает 200 с `CourseStructureResponseDto`, содержащим вложенные модули и уроки.
8. `POST /api/courses/{id}/modules` — возвращает 201 с `ModuleResponseDto`, 404 при отсутствии курса.
9. `GET /api/modules/{id}` — возвращает 200 с `ModuleResponseDto`, 404 при отсутствии.
10. `PUT /api/modules/{id}` — возвращает 200 с обновлённым `ModuleResponseDto`.
11. `DELETE /api/modules/{id}` — возвращает 204.
12. `GET /api/lessons/{id}` — возвращает 200 с `LessonResponseDto`, 404 при отсутствии.
13. `PUT /api/lessons/{id}` — возвращает 200 с обновлённым `LessonResponseDto`.
14. `DELETE /api/lessons/{id}` — возвращает 204.
15. Все тесты из `RestApiTests` (включая существующие для пользователей и категорий) проходят успешно.
16. Единый формат ошибок `ErrorResponseDto` возвращается для всех типов ошибок (400, 404).

## Список проверяемых сценариев для интеграционных HTTP-тестов

### Позитивные сценарии

| № | Тест | Метод | Путь | Ожидаемый код | Описание |
|---|---|---|---|---|---|
| T17-01 | `createCourse_shouldReturn201` | POST | `/api/courses` | 201 | Создание курса с валидными данными (teacher, category, duration >= 1) |
| T17-02 | `getCourse_shouldReturn200` | GET | `/api/courses/{id}` | 200 | Получение курса по ID после создания |
| T17-03 | `updateCourse_shouldReturn200` | PUT | `/api/courses/{id}` | 200 | Обновление названия и описания курса |
| T17-04 | `deleteCourse_shouldReturn204` | DELETE | `/api/courses/{id}` | 204 | Удаление пустого курса (без записей, решений, отзывов) |
| T17-05 | `getCourseStructure_shouldReturn200` | GET | `/api/courses/{id}/structure` | 200 | Получение полной структуры с модулями и уроками |
| T17-06 | `addModule_shouldReturn201` | POST | `/api/courses/{id}/modules` | 201 | Добавление модуля к курсу |
| T17-07 | `getModule_shouldReturn200` | GET | `/api/modules/{id}` | 200 | Получение модуля по ID |
| T17-08 | `updateModule_shouldReturn200` | PUT | `/api/modules/{id}` | 200 | Обновление модуля |
| T17-09 | `deleteModule_shouldReturn204` | DELETE | `/api/modules/{id}` | 204 | Удаление модуля |
| T17-10 | `addLesson_shouldReturn201` | POST | `/api/courses/{id}/modules/{mid}/lessons` | 201 | Добавление урока к модулю (если эндпоинт `POST /api/modules/{mid}/lessons`) |
| T17-11 | `getLesson_shouldReturn200` | GET | `/api/lessons/{id}` | 200 | Получение урока по ID |
| T17-12 | `updateLesson_shouldReturn200` | PUT | `/api/lessons/{id}` | 200 | Обновление урока |
| T17-13 | `deleteLesson_shouldReturn204` | DELETE | `/api/lessons/{id}` | 204 | Удаление урока |
| T17-14 | `fullCycle_createCourseWithStructure` | POST+GET+POST | `/api/courses` + `/api/courses/{id}/modules` + `/api/modules/{mid}/lessons` | 201+200+201 | Полный цикл: создать курс → добавить модуль → добавить урок → получить структуру |
| T17-15 | `errorResponseFormat_course_shouldHaveRequiredFields` | GET | `/api/courses/99999` | 404 | Формат JSON ошибки 404: поля `timestamp`, `status`, `error`, `message`, `path` |

### Негативные сценарии

| № | Тест | Метод | Путь | Ожидаемый код | Описание |
|---|---|---|---|---|---|
| T17-N1 | `getCourse_notFound_shouldReturn404` | GET | `/api/courses/99999` | 404 | Чтение несуществующего курса |
| T17-N2 | `updateCourse_notFound_shouldReturn404` | PUT | `/api/courses/99999` | 404 | Обновление несуществующего курса |
| T17-N3 | `deleteCourse_notFound_shouldReturn404` | DELETE | `/api/courses/99999` | 404 | Удаление несуществующего курса |
| T17-N4 | `createCourse_nullTitle_shouldReturn400` | POST | `/api/courses` | 400 | Пустое название курса (валидация `@NotBlank`) |
| T17-N5 | `createCourse_nullTeacher_shouldReturn400` | POST | `/api/courses` | 400 | `teacherId = null` (валидация `@NotNull`) |
| T17-N6 | `createCourse_invalidDuration_shouldReturn400` | POST | `/api/courses` | 400 | `duration = 0` или `null` (валидация `@NotNull` + бизнес-проверка) |
| T17-N7 | `createCourse_nonExistentTeacher_shouldReturn404` | POST | `/api/courses` | 404 | Указан ID несуществующего преподавателя |
| T17-N8 | `createCourse_nonExistentCategory_shouldReturn404` | POST | `/api/courses` | 404 | Указан ID несуществующей категории |
| T17-N9 | `deleteCourse_withEnrollments_shouldReturn400` | DELETE | `/api/courses/{id}` | 400 | Удаление курса с активной записью студента |
| T17-N10 | `addModule_nonExistentCourse_shouldReturn404` | POST | `/api/courses/99999/modules` | 404 | Добавление модуля к несуществующему курсу |
| T17-N11 | `getModule_notFound_shouldReturn404` | GET | `/api/modules/99999` | 404 | Чтение несуществующего модуля |
| T17-N12 | `getLesson_notFound_shouldReturn404` | GET | `/api/lessons/99999` | 404 | Чтение несуществующего урока |
| T17-N13 | `createCourse_emptyBody_shouldReturn400` | POST | `/api/courses` | 400 | Пустой JSON в теле запроса |
| T17-N14 | `createModule_nullTitle_shouldReturn400` | POST | `/api/courses/{id}/modules` | 400 | Пустое название модуля |
| T17-N15 | `createLesson_nullTitle_shouldReturn400` | POST | `/api/modules/{mid}/lessons` | 400 | Пустое название урока |
| T17-N16 | `deleteCourse_withSubmissions_shouldReturn400` | DELETE | `/api/courses/{id}` | 400 | Удаление курса с сохранёнными решениями студентов |

## Команды проверки

Все команды выполняются из корня проекта:

1. **Компиляция:**
   ```bash
   JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn clean compile -q
   ```

2. **Запуск всех тестов (включая существующие и новые для курса/модулей/уроков):**
   ```bash
   JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn test -q
   ```
   - Проходят все существующие тесты (Этапы 01–16, ~324 теста) + новые тесты REST API для курсов/модулей/уроков (~20–30 тестов).

3. **Запуск только тестов REST API (для быстрой проверки):**
   ```bash
   JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn test -Dtest=RestApiTests -q
   ```

4. **Проверка отсутствия проблем с форматированием:**
   ```bash
   JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn compile -q && git diff --check
   ```

## Ограничения, риски и блокеры

### Ограничения

- REST API не включает проверку ролей (аутентификация и авторизация не входят в объём проекта).
- Удаление модуля или урока не проверяет наличие связанных сущностей (например, заданий в уроке) — это будет реализовано на этапе 18 (задания).
- `GlobalExceptionHandler` обрабатывает `IllegalStateException` как 400 Bad Request. Это соответствует текущему паттерну (Этап 16).

### Риски

1. **Каскадное удаление модуля с уроками и заданиями.** Модуль имеет `orphanRemoval = true` и каскад REMOVE для уроков. При удалении модуля через `entityManager.remove()` все уроки удаляются. Задания (привязаны к урокам) могут вызвать `PersistenceException`, если у них есть связанные решения (`Submission`). Решение: удалять модуль только если у его уроков нет решений, либо реализовать поэтапное удаление через `CourseService`.
2. **Порядок очистки тестовых данных в `@AfterEach`.** Модули удаляются каскадом с уроками, курсы — с модулями. Неправильный порядок может вызвать `IntegrityConstraintViolationException`. Решение: удалять модули перед курсами.
3. **Дублирование ID в тестах.** При параллельном выполнении тестов возможны коллизии ID. Решение: использовать уникальные данные (префиксы, timestamp) в названиях курсов и модулей.

### Блокеры

- Отсутствуют. Все необходимые сервисные методы (`createCourse`, `updateCourse`, `addModule`, `addLesson`, `getCourseStructure`, `deleteCourse`) реализованы в `CourseService` на этапах 07 и 15.

## Примечания для реализации

- Следовать существующему паттерну контроллеров: `@RestController`, `@RequestMapping`, `@Valid @RequestBody`, `ResponseEntity`, конструкторное внедрение зависимостей, JavaDoc на русском.
- Следовать существующему паттерну DTO: классы без аннотаций, публичные поля с геттерами/сеттерами, валидация через Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@Size`), конструктор по умолчанию + конструктор с параметрами.
- Структура курса (`CourseStructureResponseDto`) должна содержать вложенные DTO для модулей и уроков, чтобы не возвращать JPA-сущности напрямую в ответе API.
- Все эндпоинты должны возвращать HTTP-коды: 201 (создание), 200 (чтение/обновление), 204 (удаление), 400 (валидация/бизнес-правила), 404 (не найден).
- При удалении курса с историей (записи, решения, результаты тестов, отзывы) сервис выбрасывает `IllegalStateException`, который `GlobalExceptionHandler` преобразует в 400. Это не требует отдельных ADR или изменений обработчика.
- Порядок эндпоинтов для уроков: `POST /api/modules/{moduleId}/lessons` (а не `/api/courses/{courseId}/modules/{moduleId}/lessons`) — для краткости путей, следует REST-рекомендациям вложенных ресурсов.
