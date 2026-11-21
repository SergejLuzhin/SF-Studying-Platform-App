# Этап 06 — Модули и уроки

## Сводка

| Параметр       | Значение                       |
|----------------|--------------------------------|
| Номер этапа    | 06                             |
| Статус         | pending                        |
| Зависит от     | Этапы 01–05 (completed)        |
| Блокеры        | Нет                            |

## Цель и границы этапа

Реализовать сущность `Lesson` (урок), связать её с `Module` (модуль), добавить коллекцию `lessons` в `Module` с каскадными операциями. Создать Spring Data JPA-репозитории для `Module` и `Lesson`. Реализовать интеграционные тесты, проверяющие сохранение, каскадное сохранение, каскадное удаление, чтение структуры «курс → модули → уроки», а также негативные сценарии.

**В границы НЕ входит:** сущности `Assignment`, `Quiz`, `Submission` и другие — они будут реализованы на последующих этапах.

## Связанные файлы

### Существующие (нужно изменить)
- `src/main/java/com/studying/platform/entity/Module.java` — добавить поле `lessons` (`Set<Lesson>`) с `@OneToMany`.
- `src/main/java/com/studying/platform/entity/Course.java` — **не изменять** (каскады для `modules` уже настроены корректно: `PERSIST`, `MERGE`, `REMOVE`, `orphanRemoval=true`).

### Создаваемые файлы
- `src/main/java/com/studying/platform/entity/Lesson.java` — новая сущность.
- `src/main/java/com/studying/platform/repository/ModuleRepository.java` — новый интерфейс.
- `src/main/java/com/studying/platform/repository/LessonRepository.java` — новый интерфейс.
- `src/test/java/com/studying/platform/ModuleAndLessonTests.java` — интеграционные тесты.

### Применимые ADR
ADR на данный момент отсутствуют (`docs/adr/README.md` пуст). Существующие архитектурные решения соответствуют `docs/data-model.md` (Этап 01).

## Архитектурные решения и обоснование

### 1. Каскады в Module.lessons
- **Решение:** `@OneToMany(mappedBy = "module", fetch = LAZY, cascade = {PERSIST, MERGE, REMOVE}, orphanRemoval = true)`.
- **Обоснование:** Полностью аналогично каскадам в `Course.modules` (Этап 05). Модуль владеет уроками, и при удалении модуля все его уроки должны быть удалены. `orphanRemoval=true` позволяет удалять отдельные уроки при удалении из коллекции. `REMOVE` в cascade обеспечивает каскадное удаление через цепочку: `Course` → `Module` → `Lesson`.

### 2. Владелец связи Lesson → Module
- **Решение:** `Module` — inverse side (`mappedBy = "module"`), `Lesson` — owner side с `@ManyToOne @JoinColumn`.
- **Обоснование:** Стандартная JPA-практика: связь управляется со стороны «многих». `Lesson.module` — `@ManyToOne(fetch = FetchType.LAZY)` (явно указываем LAZY, несмотря на стандарт EAGER, чтобы избежать лишних join-запросов при загрузке модулей).

### 3. Nullable для полей Lesson
- `title` — `nullable = false`, `length = 200` (аналогично `Module.title`).
- `content` — `TEXT`, nullable (урок может не содержать текстового контента).
- `videoUrl` — `nullable` (видео не обязательно).
- `module` — `nullable = false` (урок не может существовать без модуля).

### 4. Таблица БД
- `lessons` — имя таблицы (единственное число, по соглашению проекта).
- Внешний ключ: `fk_lesson_module` на `modules.id`.

## Пошаговый план действий

### Действие 1. Создать сущность `Lesson`
- Создать файл `src/main/java/com/studying/platform/entity/Lesson.java`.
- Поля:
  - `id` — `Long`, `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
  - `title` — `String`, `@Column(nullable = false, length = 200)`.
  - `content` — `String`, `@Lob`, `@Column(columnDefinition = "TEXT")`, nullable.
  - `videoUrl` — `String`, nullable.
  - `module` — `Module`, `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "module_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lesson_module"))`.
- Конструкторы: default (для JPA) + full (title, content, videoUrl, module).
- Все getter/setter с JavaDoc на русском.
- `@Entity`, `@Table(name = "lessons")`.

### Действие 2. Обновить `Module.java` — добавить коллекцию `lessons`
- Добавить поле:
  ```java
  @OneToMany(mappedBy = "module", fetch = FetchType.LAZY,
          cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
          orphanRemoval = true)
  private Set<Lesson> lessons = new HashSet<>();
  ```
- Добавить `import jakarta.persistence.CascadeType` (если ещё не импортирован).
- Добавить getter/setter для `lessons` с JavaDoc на русском.
- Обновить JavaDoc класса, добавив упоминание коллекции `lessons`.

### Действие 3. Создать `ModuleRepository`
- Создать `src/main/java/com/studying/platform/repository/ModuleRepository.java`.
- `public interface ModuleRepository extends JpaRepository<Module, Long>`.
- Добавить метод `Optional<Module> findByCourseAndOrderIndex(Long courseId, Integer orderIndex)` для поиска модуля в курсе по порядковому номеру.

### Действие 4. Создать `LessonRepository`
- Создать `src/main/java/com/studying/platform/repository/LessonRepository.java`.
- `public interface LessonRepository extends JpaRepository<Lesson, Long>`.
- Добавить метод `List<Lesson> findByModuleOrder(Long moduleId)` — получение уроков модуля (опционально, через `@Query`).

### Действие 5. Создать интеграционные тесты `ModuleAndLessonTests`
- Создать `src/test/java/com/studying/platform/ModuleAndLessonTests.java`.
- Аннотации: `@SpringBootTest`, `@Transactional` (для отката после каждого теста).
- Внедрить: `CourseRepository`, `ModuleRepository`, `LessonRepository`, `UserRepository` (для создания преподавателя), `CategoryRepository` (для создания категории).
- Тестовые методы:

  1. **`testSaveModuleWithCourse`** — создать Course с teacher и category, сохранить; создать Module с этим курсом, сохранить; проверить id не null.
  2. **`testSaveLessonWithModule`** — создать курс + модуль; создать Lesson с модулем, сохранить; проверить связь `lesson.getModule().getId()`.
  3. **`testCascadeSaveCourseWithModulesAndLessons`** — создать Course; добавить 2 Module; для каждого модуля добавить 2 Lesson; вызвать `courseRepository.save(course)`; проверить, что все модули и уроки сохранены (проверка по count или getOne).
  4. **`testCascadeDeleteCourseRemovesModulesAndLessons`** — создать полный каскад (course → modules → lessons); удалить course; попытаться прочитать module и lesson по id — должны быть удалены (или вызвать `getById` и поймать `EntityNotFoundException`).
  5. **`testReadCourseStructureInTransaction`** — создать полный каскад; в новой транзакции загрузить Course; проверить, что `course.getModules()` не пуст, каждый модуль имеет `getLessons()` не пуст.
  6. **`testOrphanRemoval`** — загрузить модуль с уроками; удалить один урок из коллекции `module.getLessons()`; сохранить; проверить, что в БД остался только один урок.
  7. **`testNullTitleForLesson`** — создать Lesson с `title = null`; сохранить — ожидается `DataIntegrityViolationException` или `ConstraintViolationException`.
  8. **`testNullModuleForLesson`** — создать Lesson с `module = null`; сохранить — ожидается `DataIntegrityViolationException`.
  9. **`testModuleOrderIndex`** — создать два модуля с одинаковым `orderIndex` в одном курсе (должно допускаться, т.к. уникальность не задана); проверить, что оба сохраняются.

### Действие 6. Проверить `Course.java` — каскады
- Убедиться, что `Course.modules` содержит `cascade = {PERSIST, MERGE, REMOVE}` и `orphanRemoval = true` — **уже настроено**.
- Никаких изменений не требуется.

### Действие 7. Добавить JavaDoc на русском
- Все новые/изменённые классы, методы, поля должны иметь JavaDoc на русском языке.
- Использовать `@param`, `@return`, `@throws` с английскими именами параметров и типов.

### Действие 8. Команды проверки
- `mvn clean compile -q` — компиляция должна пройти без ошибок.
- `mvn test -q -Dtest=ModuleAndLessonTests` — тесты должны пройти.
- `mvn test -q` — **все** тесты проекта (включая предыдущие этапы) должны пройти.

## Критерии приёмки

| № | Критерий | Статус |
|---|----------|--------|
| 1 | Сущность `Lesson` создана с полями id, title, content, videoUrl, module | ☐ |
| 2 | `Module` содержит коллекцию `lessons` с `@OneToMany` (LAZY, каскады, orphanRemoval) | ☐ |
| 3 | Репозитории `ModuleRepository` и `LessonRepository` созданы и расширяют `JpaRepository` | ☐ |
| 4 | Каскадное сохранение: создание курса с модулями и уроками работает через один save | ☐ |
| 5 | Каскадное удаление: удаление курса удаляет модули и уроки | ☐ |
| 6 | Удаление из коллекции `lessons` удаляет соответствующие записи (orphanRemoval) | ☐ |
| 7 | Негативные сценарии: null title и null module для Lesson вызывают исключения | ☐ |
| 8 | Чтение структуры «курс → модули → уроки» работает в транзакции | ☐ |
| 9 | `mvn clean compile -q` проходит без ошибок | ☐ |
| 10 | `mvn test -q` — все тесты проекта проходят (включая предыдущие этапы) | ☐ |
| 11 | Все JavaDoc на русском, идентификаторы на английском | ☐ |

## Риски и блокеры

- **Риск:** `orphanRemoval=true` в `Course.modules` уже настроен, но без `lessons` в `Module` каскадное удаление уроков при удалении курса может быть неполным (если уроки не привязаны к модулю через orphanRemoval). После добавления `Module.lessons` с `orphanRemoval=true` эта проблема решается.
- **Риск:** `@ManyToOne(fetch = FetchType.LAZY)` для `Lesson.module` — при тестировании доступа к модулю из урока вне транзакции может возникнуть `LazyInitializationException`. Это ожидаемое поведение, отражённое в `task.md`.
- **Блокер:** Нет. ADR отсутствуют, противоречий с существующими решениями нет.

## Изменяемые файлы (итого)

| Файл | Действие |
|------|----------|
| `src/main/java/com/studying/platform/entity/Lesson.java` | Создать |
| `src/main/java/com/studying/platform/entity/Module.java` | Изменить (добавить поле `lessons`, getter/setter) |
| `src/main/java/com/studying/platform/repository/ModuleRepository.java` | Создать |
| `src/main/java/com/studying/platform/repository/LessonRepository.java` | Создать |
| `src/test/java/com/studying/platform/ModuleAndLessonTests.java` | Создать |
| `stages/stage-06.md` | Создать (этот файл) |
| `plan.md` | Обновить статус Этапа 06 на `completed` |
| `state.md` | Обновить (добавить запись об Этапе 06) |

## ADR_REQUIRED

Нет. Существующая архитектура (каскады, LAZY, владение связями) полностью согласована с требованиями этапа и предыдущими решениями.
