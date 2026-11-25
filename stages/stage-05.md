# Этап 05: Категории и курсы

## Цель и границы этапа

Реализовать доменные сущности `Category` и `Course`, репозитории Spring Data JPA и интеграционные тесты. Настроить связи курса с категорией (`@ManyToOne`) и преподавателем (`@ManyToOne` на `User`), а также обратные связи в `Category` и `Course`. Обеспечить корректную валидацию данных на уровне базы (ограничения `NOT NULL`, `UNIQUE`, `FOREIGN KEY`).

**Границы этапа:** создаются только две новые сущности, два репозитория и тестовый класс. Модули, уроки, записи, отзывы и теги — вне объёма. Каскадные удаления для модулей (`orphanRemoval`) и связь `M:M` с `Tag` настраиваются согласно `docs/data-model.md`, но не тестируются детально (для этого потребуется отдельный этап с тестовыми данными).

## Связанные файлы и применимые ADR

- **Существующие файлы проекта:**
  - `src/main/java/com/studying/platform/entity/User.java` — ссылка через `@ManyToOne` (преподаватель).
  - `src/main/java/com/studying/platform/entity/UserRole.java` — для создания тестовых преподавателей.
  - `src/main/java/com/studying/platform/repository/UserRepository.java` — для поиска преподавателя в тестах.
  - `src/main/java/com/studying/platform/StudyingPlatformApplication.java` — точка входа (без изменений).
  - `src/test/java/com/studying/platform/UserAndProfileTests.java` — референс стиля тестирования.
  - `src/main/resources/application.yml` / `application-test.yml` — конфигурация БД без изменений.
- **Документация:**
  - `docs/data-model.md` (разделы 2.3 Category, 2.5 Course, 3. Схема связей, 4. Каскадные операции).
- **ADR:**
  - Существующих утверждённых ADR (статус `Accepted`), которые бы ограничивали данный этап, нет. Индекс ADR пуст.

## Список создаваемых / изменяемых файлов

### 1. `src/main/java/com/studying/platform/entity/Category.java` (создание)

**Описание:** Справочная сущность «Категория курса» — классификатор для курсов (например, «Программирование», «Дизайн»).

**Поля:**

| Поле | Тип | Аннотации / Ограничения |
|---|---|---|
| `id` | `Long` | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `name` | `String` | `@Column(unique = true, nullable = false, length = 100)` |

**Связи:**
- `@OneToMany(mappedBy = "category", fetch = FetchType.LAZY)` — коллекция `courses` (обратная сторона связи с `Course`).

**Каскады:** нет.

**JavaDoc:** содержательный, на русском, для класса, всех полей, конструкторов, геттеров/сеттеров.

### 2. `src/main/java/com/studying/platform/entity/Course.java` (создание)

**Описание:** Сущность «Курс» учебной платформы. Содержит название, описание, продолжительность, дату начала, ссылку на преподавателя (`User`) и категорию (`Category`). Владелец всех внешних связей.

**Поля:**

| Поле | Тип | Аннотации / Ограничения |
|---|---|---|
| `id` | `Long` | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `title` | `String` | `@Column(nullable = false, length = 200)` |
| `description` | `String` | `@Lob`, `@Column(columnDefinition = "TEXT")` |
| `duration` | `Integer` | `@Column(nullable = false)` — минимальное значение 1 (валидация на уровне сервиса) |
| `startDate` | `LocalDate` | `@Column(nullable = false)` |
| `teacher` | `User` | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "teacher_id", nullable = false, foreignKey = @ForeignKey(name = "fk_course_teacher"))` |
| `category` | `Category` | `@ManyToOne(fetch = FetchType.EAGER)`, `@JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_course_category"))` |

**Связи (обратные стороны для будущих сущностей):**

| Поле | Тип | Аннотации |
|---|---|---|
| `modules` | `Set<Module>` | `@OneToMany(mappedBy = "course", fetch = FetchType.LAZY, cascade = {PERSIST, MERGE, REMOVE}, orphanRemoval = true)` |
| `enrollments` | `Set<Enrollment>` | `@OneToMany(mappedBy = "course", fetch = FetchType.LAZY)` |
| `reviews` | `Set<CourseReview>` | `@OneToMany(mappedBy = "course", fetch = FetchType.LAZY)` |
| `tags` | `Set<Tag>` | `@ManyToMany(fetch = FetchType.LAZY, cascade = {PERSIST, MERGE})`, `@JoinTable(name = "course_tag", joinColumns = @JoinColumn(name = "course_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))` |

> **Примечание:** Поля `modules`, `enrollments`, `reviews`, `tags` объявляются для полноты модели и соответствия `docs/data-model.md`. Сущности `Module`, `Enrollment`, `CourseReview`, `Tag` будут реализованы на последующих этапах, но JPA-маппинг настраивается сейчас, чтобы схема БД создавалась корректно.

**Каскады:** `REMOVE` для `Module` (через `orphanRemoval`), `PERSIST/MERGE` для `Tag`.

**JavaDoc:** содержательный, на русском, для класса, всех полей, конструкторов, геттеров/сеттеров.

### 3. `src/main/java/com/studying/platform/repository/CategoryRepository.java` (создание)

**Описание:** Репозиторий для сущности `Category`. Наследник `JpaRepository<Category, Long>`.

**Методы:**
- Базовые CRUD от `JpaRepository` (сохранение, удаление, поиск по ID, список всех).
- `Optional<Category> findByName(String name)` — поиск по имени (используется для проверки уникальности).

### 4. `src/main/java/com/studying/platform/repository/CourseRepository.java` (создание)

**Описание:** Репозиторий для сущности `Course`. Наследник `JpaRepository<Course, Long>`.

**Методы:**
- Базовые CRUD от `JpaRepository`.
- `Optional<Course> findByTitle(String title)` — поиск по названию.

### 5. `src/test/java/com/studying/platform/CategoryAndCourseTests.java` (создание)

**Описание:** Интеграционные тесты для сущностей `Category` и `Course`. Используют `@DataJpaTest` (H2), аналогично `UserAndProfileTests`.

**Сценарии тестирования:**

| # | Название теста | Описание | Ожидаемый результат |
|---|---|---|---|
| 1 | `saveCategoryAndCourse_shouldPersistAndRetrieve` | Создать категорию и курс с преподавателем, сохранить и прочитать | Оба объекта сохранены, связи установлены корректно |
| 2 | `findByTitle_shouldReturnCourse` | Создать курс, найти по названию | Курс найден, данные совпадают |
| 3 | `findByTitle_shouldReturnEmptyForNonExistent` | Поиск несуществующего курса | `Optional.empty()` |
| 4 | `updateCourse_shouldChangeData` | Создать курс, обновить название и описание, сохранить | Данные обновлены |
| 5 | `deleteCourse_shouldRemoveFromDatabase` | Создать курс (без модулей и записей), удалить | Курс удалён |
| 6 | `saveCourse_nullTitle_shouldThrow` | Попытка создать курс с `null` названием | `PropertyValueException` |
| 7 | `saveCourse_nullStartDate_shouldThrow` | Попытка создать курс с `null` датой начала | `PropertyValueException` |
| 8 | `saveCourse_nullTeacher_shouldThrow` | Попытка создать курс без преподавателя | `PropertyValueException` |
| 9 | `saveCourse_nullCategory_shouldThrow` | Попытка создать курс без категории | `PropertyValueException` |
| 10 | `saveCategory_duplicateName_shouldThrow` | Создать две категории с одинаковым именем | `ConstraintViolationException` |
| 11 | `saveCourse_nonExistentTeacher_shouldThrow` | Курс со ссылкой на несуществующего преподавателя | `ConstraintViolationException` / `TransientAssociationException` |
| 12 | `saveCourse_nonExistentCategory_shouldThrow` | Курс со ссылкой на несуществующую категорию | `ConstraintViolationException` / `TransientAssociationException` |
| 13 | `loadCourse_shouldFetchCategory` | Загрузить курс, проверить, что категория доступна | `course.getCategory()` не `null`, имя совпадает |
| 14 | `loadCourse_teacherReference` | Загрузить курс, проверить ссылку на преподавателя | `course.getTeacher()` не `null`, логин совпадает |
| 15 | `saveCategoryWithCourses_shouldPersistCourse` | Создать категорию, добавить в коллекцию `courses`, сохранить категорию | Категория и курс сохранены, `courses` не пустой |

**Структура тестовых методов:**
- Каждый тест начинается с создания необходимых объектов через `entityManager.persist()` + `flush()` (преподаватель, студент, категория).
- Тесты на сохранение связанных сущностей сначала сохраняют объект-зависимость (`User` преподаватель, `Category`), затем связанный объект (`Course`).
- Ожидания оформлены через `assertThrows` для негативных сценариев.

## Пошаговый план реализации

### Шаг 1. Создание сущности `Category`

1. Создать файл `src/main/java/com/studying/platform/entity/Category.java`.
2. Добавить JPA-аннотации: `@Entity`, `@Table(name = "categories")`.
3. Объявить поля: `id` (`Long`, `@Id`, `@GeneratedValue`), `name` (`String`, `@Column(unique = true, nullable = false, length = 100)`).
4. Добавить связь `@OneToMany(mappedBy = "category", fetch = FetchType.LAZY)` с полем `courses` (`Set<Category>`).
5. Добавить публичный конструктор по умолчанию (`public Category() {}`) и конструктор с параметром `name`.
6. Добавить JavaDoc для класса, полей, методов.

### Шаг 2. Создание сущности `Course`

1. Создать файл `src/main/java/com/studying/platform/entity/Course.java`.
2. Добавить JPA-аннотации: `@Entity`, `@Table(name = "courses")`.
3. Объявить поля:
   - `id` (`Long`, `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`).
   - `title` (`String`, `@Column(nullable = false, length = 200)`).
   - `description` (`String`, `@Lob`, `@Column(columnDefinition = "TEXT")`).
   - `duration` (`Integer`, `@Column(nullable = false)`).
   - `startDate` (`LocalDate`, `@Column(nullable = false)`).
   - `teacher` (`User`, `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "teacher_id", nullable = false, foreignKey = @ForeignKey(name = "fk_course_teacher"))`).
   - `category` (`Category`, `@ManyToOne(fetch = FetchType.EAGER)`, `@JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_course_category"))`).
4. Добавить обратные связи (объявления коллекций для будущих сущностей):
   - `modules`, `enrollments`, `reviews`, `tags`.
5. Добавить публичный конструктор по умолчанию и конструктор с параметрами (`title`, `description`, `duration`, `startDate`, `teacher`, `category`).
6. Добавить JavaDoc для всех элементов.

### Шаг 3. Создание репозиториев

1. Создать `CategoryRepository.java` — интерфейс, наследник `JpaRepository<Category, Long>`, метод `findByName`.
2. Создать `CourseRepository.java` — интерфейс, наследник `JpaRepository<Course, Long>`, метод `findByTitle`.
3. Добавить JavaDoc на русском для интерфейсов и методов.

### Шаг 4. Написание интеграционных тестов

1. Создать `CategoryAndCourseTests.java` в пакете `com.studying.platform`.
2. Аннотация `@DataJpaTest`, внедрение `EntityManager`, `CategoryRepository`, `CourseRepository`, `UserRepository`.
3. Реализовать все 15 тест-кейсов (см. таблицу выше).
4. В негативных тестах использовать `assertThrows`:
   - `PropertyValueException` — для `null` в полях `nullable = false`.
   - `ConstraintViolationException` — для нарушений `UNIQUE` и `FOREIGN KEY`.
5. Убедиться, что JavaDoc присутствует для каждого тестового метода.

### Шаг 5. Верификация

1. `mvn clean compile -q` — убедиться, что компиляция проходит без ошибок.
2. `mvn test -q` — запустить все тесты, убедиться, что новые и существующие проходят.
3. Проверить генерацию DDL в логах (уровень `DEBUG` для `org.hibernate.SQL`): таблицы `categories`, `courses`, внешние ключи `fk_course_teacher`, `fk_course_category`, ограничение уникальности `name` в `categories`.

## Критерии приёмки и тест-кейсы

### Критерий 1: Сущности созданы и маппятся корректно
- Таблица `categories` создаётся с колонкой `name` (UNIQUE, NOT NULL, VARCHAR 100).
- Таблица `courses` создаётся с колонками: `title` (NOT NULL), `description` (TEXT), `duration` (INTEGER, NOT NULL), `startDate` (DATE, NOT NULL), `teacher_id` (FK на `users`), `category_id` (FK на `categories`).
- Внешние ключи: `fk_course_teacher`, `fk_course_category`.

### Критерий 2: Базовый CRUD
- Можно создать категорию и курс, привязав их к существующему пользователю (преподавателю).
- Можно прочитать курс по ID и по названию.
- Можно обновить название, описание, дату начала курса.
- Можно удалить курс (без связанных модулей/записей/отзывов).

### Критерий 3: Ограничения данных (Negative Scenarios)
- Дублирование имени категории → `ConstraintViolationException`.
- Курс без названия → `PropertyValueException`.
- Курс без даты начала → `PropertyValueException`.
- Курс без преподавателя → `PropertyValueException`.
- Курс без категории → `PropertyValueException`.
- Курс со ссылкой на несуществующего преподавателя → `ConstraintViolationException` / `TransientAssociationException`.
- Курс со ссылкой на несуществующую категорию → `ConstraintViolationException` / `TransientAssociationException`.

## Команды проверки

| Команда | Описание |
|---|---|
| `mvn clean compile -q` | Компиляция проекта без ошибок |
| `mvn test -q` | Запуск всех тестов (JUnit 5 + H2 через `@DataJpaTest`) |
| `mvn test -q -Dtest=CategoryAndCourseTests` | Запуск только тестов категории и курса |
| `mvn test -q -Dtest=CategoryAndCourseTests#saveCategoryAndCourse_shouldPersistAndRetrieve` | Запуск одного конкретного теста |

> **Примечание:** Тесты `CategoryAndCourseTests` используют `@DataJpaTest`, который загружает только JPA-слой (без полного контекста Spring Boot). Для подключения к реальной PostgreSQL через Testcontainers используется существующий класс `StudyingPlatformApplicationTests` (профиль `test`).

## Ограничения, риски и блокеры

### Ограничения
- Поле `duration` не имеет ограничения минимального значения на уровне JPA (`@Column` без `check`) — валидация `duration >= 1` должна выполняться в сервисном слое (на этапе 07).
- Обратные связи (коллекции `modules`, `enrollments`, `reviews`, `tags`) объявлены, но не тестируются детально — соответствующие сущности реализуются на последующих этапах.
- Связь `M:M` с `Tag` (`@JoinTable(name = "course_tag")`) настраивается, но не проверяется тестами — для этого потребуется этап 14.

### Риски
- **Ленивая загрузка `teacher`:** `@ManyToOne(fetch = FetchType.LAZY)` для поля `teacher` может вызвать `LazyInitializationException` при обращении к преподавателю вне транзакции. В тестах `@DataJpaTest` транзакции управляются автоматически, поэтому проблема проявится только при ручном чтении данных. Это соответствует цели проекта — отработать понимание ленивой загрузки (этап 20).
- **EAGER для категории:** `category` загружается `EAGER` согласно `docs/data-model.md` (справочная сущность). Если в будущем потребуется LAZY для категории, это будет архитектурное решение (ADR).
- **Каскады для модулей:** `orphanRemoval = true` для `modules` настроено заранее. Когда модули будут реализованы (этап 06), каскадное удаление модулей при удалении курса будет работать автоматически.

### Блокеры
- Отсутствуют. Все зависимости покрыты существующими сущностями (`User`, `UserRole`) и конфигурацией.

## Статус

`in_progress`

STATUS: READY
