# Этап 09: Сущность Assignment и управление заданиями урока

## 1. Цель и границы этапа

Реализовать сущность `Assignment` (Задание), репозиторий, сервисный слой управления заданиями и обновить связанные объекты (`Lesson`) для двунаправленной связи. Добавить интеграционные тесты и обновить проверки схемы PostgreSQL.

**Границы этапа:**
- Создание `Assignment` с полями: `id` (PK), `lesson` (FK), `title`, `description` (TEXT, без `@Lob`), `dueDate`, `maxScore`.
- Реализация `AssignmentRepository` с базовыми CRUD-методами и поиском по `lessonId`.
- Обновление `Lesson` для связи «один-ко-многим» с заданиями (`fetch = FetchType.LAZY`).
- Добавление бизнес-методов в `CourseService` для управления заданиями (обоснование: сервис `CourseService` уже инкапсулирует CRUD курса, модулей и уроков; добавление логики заданий в него сохраняет целостность домена учебной структуры и избегает фрагментации сервиса на ранних этапах MVP).
- Обновление `PostgresSchemaTests` для проверки новой таблицы, первичного ключа, внешнего ключа и ограничений.
- Написание интеграционных тестов с Testcontainers (профиль `test`).

**Не входит в этап:** REST-контроллеры, DTO, валидация через Bean Validation, каскадные удаления решений (`Submission`), которые реализуются на этапе 10.

---

## 2. Связанные файлы и применимые ADR

- **Сущности:**
  - `src/main/java/com/studying/platform/entity/Lesson.java` (изменение)
  - `src/main/java/com/studying/platform/entity/Assignment.java` (создание)
- **Репозитории:**
  - `src/main/java/com/studying/platform/repository/LessonRepository.java` (чтение, без изменений структуры)
  - `src/main/java/com/studying/platform/repository/AssignmentRepository.java` (создание)
- **Сервисы:**
  - `src/main/java/com/studying/platform/service/CourseService.java` (добавление методов управления заданиями)
- **Тесты:**
  - `src/test/java/com/studying/platform/PostgresSchemaTests.java` (обновление проверок схемы)
  - `src/test/java/com/studying/platform/AssignmentServiceTests.java` (создание)
- **ADR:** Индекс `docs/adr/README.md` пуст. Принятых ADR, влияющих на этот этап, нет. Новое ADR создавать не требуется.

---

## 3. Пошаговый план реализации

### 3.1. Создание сущности `Assignment`
1. Создать `Assignment.java` в пакете `com.studying.platform.entity`.
2. Определить поля:
   - `Long id` (PK, `GenerationType.IDENTITY`)
   - `Lesson lesson` (`@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "lesson_id", nullable = false, foreignKey = @ForeignKey(name = "fk_assignment_lesson"))`)
   - `String title` (`@Column(nullable = false, length = 200)`)
   - `String description` (`@Column(name = "description", columnDefinition = "TEXT")` — **без `@Lob`**, согласно требованиям)
   - `LocalDate dueDate` (`@Column(name = "due_date")`)
   - `Integer maxScore` (`@Column(name = "max_score")`)
3. Реализовать публичный конструктор по умолчанию и инициализирующий конструктор.
4. Сгенерировать геттеры, сеттеры и содержательный JavaDoc на русском для всех классов и методов.
5. Сохранить файл.

### 3.2. Обновление сущности `Lesson`
1. В `Lesson.java` добавить поле:
   ```java
   @OneToMany(mappedBy = "lesson", fetch = FetchType.LAZY)
   private Set<Assignment> assignments = new HashSet<>();
   ```
2. Добавить геттер `getAssignments()` и сеттер `setAssignments(Set<Assignment> assignments)`.
3. Дополнить JavaDoc на русском, описывающий связь «один-ко-многим» с заданиями.
4. Сохранить файл.

### 3.3. Создание репозитория `AssignmentRepository`
1. Создать `AssignmentRepository.java` в пакете `com.studying.platform.repository`.
2. Наследовать от `JpaRepository<Assignment, Long>`.
3. Добавить метод:
   ```java
   List<Assignment> findByLessonId(Long lessonId);
   ```
4. Добавить JavaDoc на русском.
5. Сохранить файл.

### 3.4. Обновление `CourseService` (управление заданиями)
1. Внедрить `AssignmentRepository` и `LessonRepository` через конструктор (если ещё не внедрён `LessonRepository`, убедиться, что он есть).
2. Добавить методы:
   - `createAssignment(Long lessonId, String title, String description, LocalDate dueDate, Integer maxScore)` — создаёт и сохраняет задание, проверяет существование урока.
   - `updateAssignment(Long assignmentId, String title, String description, LocalDate dueDate, Integer maxScore)` — обновляет поля, возвращает сохранённый объект.
   - `deleteAssignment(Long assignmentId)` — удаляет задание по идентификатору.
   - `getAssignmentsByLessonId(Long lessonId)` — возвращает список заданий урока.
3. Все публичные методы пометить `@Transactional` (на уровне класса уже есть `@Transactional`, дублировать не нужно).
4. Использовать существующий паттерн `entityManager.persist/merge` и `flush`, как в `addLesson`.
5. Дополнить JavaDoc на русском для всех новых методов.
6. Сохранить файл.

### 3.5. Обновление интеграционных тестов схемы (`PostgresSchemaTests`)
1. Добавить `"assignments"` в массив `expectedTables` метода `requiredTablesExist()`.
2. Добавить `"assignments", Set.of("id")` в `expectedPks` метода `primaryKeysExist()`.
3. Добавить `{"assignments", "lesson_id", "lessons", "id"}` в массив `expectedFks` метода `foreignKeysExist()`.
4. Убедиться, что тесты на уникальные ограничения (`uniqueConstraintsExist`) не падают (для `assignments` уникальных ограничений не требуется, но проверить отсутствие ложных срабатываний).
5. Сохранить файл.

### 3.6. Создание тестов `AssignmentServiceTests`
1. Создать `AssignmentServiceTests.java` в пакете `com.studying.platform`.
2. Аннотировать класс: `@DataJpaTest`, `@ActiveProfiles("test")`, `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`.
3. Внедрить `EntityManager`, `AssignmentRepository`, `LessonRepository`, `CourseRepository`, `ModuleRepository`, `UserRepository`, `CategoryRepository`, `DataSource`.
4. Реализовать вспомогательные методы создания тестовых данных (`createCategory`, `createTeacher`, `createCourse`, `createModule`, `createLesson`) по аналогии с `ModuleAndLessonTests`.
5. Реализовать следующие содержательные тесты (не менее 4):
   - `testCreateAssignmentWithLesson()` — создание задания, проверка ID, полей, связи с уроком.
   - `testUpdateAssignmentFields()` — обновление title, description, dueDate, maxScore; проверка сохранности текста через JPA.
   - `testDeleteAssignment()` — удаление задания, проверка отсутствия в БД.
   - `testNullLessonForAssignment()` — сохранение задания без урока должно вызывать `ConstraintViolationException`.
   - `testTextStorageViaJpaAndSql()` — сохранение длинного текста в `description`, чтение через JPA и прямой SQL на PostgreSQL (верификация TEXT-поля).
6. Добавить JavaDoc на русском для класса и методов.
7. Сохранить файл.

---

## 4. Критерии приёмки и тест-кейсы

| № | Критерий приёмки | Статус | Тест-кейс / Проверка |
|---|-------------------|--------|-----------------------|
| 1 | Сущность `Assignment` создана, содержит все поля, связи настроены корректно | Ожидается | `AssignmentServiceTests.testCreateAssignmentWithLesson()` |
| 2 | `Lesson` имеет связь `OneToMany` с `Assignment`, `fetch = LAZY` | Ожидается | Проверка аннотаций и работы `getAssignments()` внутри транзакции |
| 3 | `AssignmentRepository` поддерживает CRUD и поиск по `lessonId` | Ожидается | `findByLessonId()` возвращает корректный список |
| 4 | Сервисные методы CRUD работают корректно, проверяют существование урока | Ожидается | `createAssignment`, `updateAssignment`, `deleteAssignment` |
| 5 | Поле `description` сохраняется как TEXT без `@Lob`, текст читается корректно | Ожидается | `testTextStorageViaJpaAndSql()` |
| 6 | Внешний ключ `lesson_id` гарантирует отсутствие задания без урока | Ожидается | `testNullLessonForAssignment()` |
| 7 | Таблица `assignments` присутствует в схеме, PK `{id}`, FK `lesson_id -> lessons.id` валидирован | Ожидается | Обновлённые проверки в `PostgresSchemaTests` |
| 8 | Все тесты проекта проходят (не менее 154 PASS, новые не ломают существующие) | Ожидается | Команда проверки (см. раздел 5) |

---

## 5. Команды проверки

Все команды выполняются из корня проекта. Профиль `test` автоматически использует Testcontainers.

```bash
# 1. Компиляция проекта (проверка отсутствия ошибок сборки)
./mvnw compile -q

# 2. Запуск обновлённых тестов схемы
./mvnw test -Dtest=PostgresSchemaTests -Dspring.profiles.active=test -q

# 3. Запуск новых интеграционных тестов Assignment
./mvnw test -Dtest=AssignmentServiceTests -Dspring.profiles.active=test -q

# 4. Полный прогон всех тестов (верификация, что новые изменения не сломали этапы 01–08)
./mvnw test -Dspring.profiles.active=test -q
```

Ожидаемый результат: все тесты завершаются со статусом `PASS`. В логах `PostgresSchemaTests` отображается создание таблицы `assignments`, в `AssignmentServiceTests` — успешные CRUD-операции и проверка TEXT-поля.

---

## 6. Риски, ограничения и блокеры

| Риск / Блокер | Описание | Митигация |
|---------------|----------|-----------|
| Конфликт с каскадами | На этапе 10 `Submission` создаётся без каскадов удаления. Если сейчас настроить `CascadeType.REMOVE` на связи `Lesson -> Assignment`, удаление урока удалит задания, что может нарушить логику этапа 10. | Не добавлять `cascade = {PERSIST, MERGE, REMOVE}` и `orphanRemoval = true` на связь `Lesson -> Assignment`. Оставить базовый `fetch = LAZY`. |
| Отсутствие `@Lob` для `description` | Требование проекта: текстовые поля маппятся без `@Lob`. Hibernate может вести себя иначе при работе с большими объёмами данных в некоторых дистрибутивах PostgreSQL. | Использовать `columnDefinition = "TEXT"` явно. В тестах проверить сохранение и чтение текста через JPA и прямой SQL. |
| Дублирование логики в `CourseService` | Добавление методов управления заданиями в `CourseService` увеличивает его размер. | На этапе 16 (REST API) и 18 (API записей и заданий) вынести логику в отдельные контроллеры/сервисы при необходимости. Сейчас приоритет — целостность MVP. |
| Изменение `PostgresSchemaTests` | Добавление новых таблиц может потребовать синхронизации с ожидаемыми списками. | Вносить изменения только в целевые массивы/мапы теста, не трогать логику валидации. |

---

## 7. ADR Required

На данном этапе существенных архитектурных решений не требуется. Изменения согласуются с утверждёнными принципами проекта (LAZY-загрузка, TEXT без `@Lob`, Testcontainers, разделение домена по пакетам). Новое ADR создавать не нужно.

**Статус:** `READY`
**Файл плана:** `stages/stage-09.md`
**Сводка:** Подготовлен детальный пошаговый план создания сущности `Assignment`, обновления `Lesson`, репозитория, сервисного слоя, тестов и проверок схемы PostgreSQL. Все действия соответствуют структуре пакета `com.studying.platform`, требованиям к JavaDoc, языку идентификаторов и ограничениям проекта.
