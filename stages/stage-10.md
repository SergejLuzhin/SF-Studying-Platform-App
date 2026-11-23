# Этап 10. Сдача решений — подробный план

## Цель и границы этапа

Реализовать сущность `Submission` (отправленное решение задания), репозиторий, сервисный метод отправки решения и интеграционные тесты. Обеспечить бизнес-правило: студент может отправить только одно решение на задание и только если он записан на курс, содержащий это задание. Внешний ключ на таблицу `submissions` добавляется в проверки схемы PostgreSQL; каскады удаления **не настраиваются**.

**Границы этапа:**
- Создаются: сущность `Submission`, репозиторий `SubmissionRepository`, метод в `SubmissionService` (или `EnrollmentService`), связь `@OneToMany` в `Assignment`, интеграционные тесты, обновления в `PostgresSchemaTests`.
- Изменяются: `Assignment` (добавление `@OneToMany submissions`), `PostgresSchemaTests` (добавление таблицы `submissions`, FK, уникального ограничения).
- **Не входит** в этап: оценивание решений (`score` / `feedback` создаются как поля сущности, но логика их заполнения — этап 11); REST-эндпоинты — этап 18; каскады удаления; связь `Submission` ↔ `User` в обеих сторонах (достаточно `@ManyToOne` в самой сущности).

## Связанные исходные файлы

### Создаваемые файлы

| Путь | Назначение |
|---|---|
| `src/main/java/com/studying/platform/entity/Submission.java` | Сущность JPA «Решение» |
| `src/main/java/com/studying/platform/repository/SubmissionRepository.java` | Репозиторий Spring Data JPA |
| `src/main/java/com/studying/platform/service/SubmissionService.java` | Сервисный метод `submitSolution` |
| `src/test/java/com/studying/platform/SubmissionTests.java` | Интеграционные тесты сущности, репозитория и сервиса |

### Изменяемые файлы

| Путь | Изменение |
|---|---|
| `src/main/java/com/studying/platform/entity/Assignment.java` | Добавить `@OneToMany(mappedBy = "assignment", fetch = FetchType.LAZY, cascade = {}) private Set<Submission> submissions = new HashSet<>()` |
| `src/test/java/com/studying/platform/TestDataCleanupHelper.java` | Добавить параметр `Set<Long> submissionIds` и JPQL-удаление `DELETE FROM Submission s WHERE s.id IN (:ids)` на первое место (субмиссии зависят от assignment → lesson → module → course) |

### Файлы, проверяемые на корректность

| Путь | Роль |
|---|---|
| `src/test/java/com/studying/platform/PostgresSchemaTests.java` | Обновить списки таблиц, FK и уникальных ограничений |
| `src/main/java/com/studying/platform/service/EnrollmentService.java` | Использовать репозитории `EnrollmentRepository` и `LessonRepository` для проверки записи на курс |

## Применимые ADR

ADR-индекс пуст — существенных утверждённых архитектурных решений, влияющих на этот этап, нет. Правило проекта: TEXT-поля маппятся без `@Lob` (см. `plan.md`); `@ManyToOne` с `fetch = LAZY` (согласовано в `task.md`, раздел «Примечание по ленивой загрузке»).

## Порядок действий

### Действие 1. Создать сущность `Submission`

**Файл:** `src/main/java/com/studying/platform/entity/Submission.java`

**Поля:**

| Поле | Тип | Аннотация | Примечание |
|---|---|---|---|
| `id` | `Long` | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` | PK, автоинкремент |
| `assignment` | `Assignment` | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "assignment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_submission_assignment"))` | Владелец связи; НЕ nullable |
| `student` | `User` | `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_submission_student"))` | Владелец связи; НЕ nullable |
| `submittedAt` | `LocalDateTime` | `@Column(name = "submitted_at", nullable = false)` | Момент отправки |
| `content` | `String` | `@Column(name = "content", columnDefinition = "TEXT", nullable = false)` | TEXT без `@Lob` |
| `score` | `Integer` | `@Column(name = "score")` | Nullable, заполняется при проверке |
| `feedback` | `String` | `@Column(name = "feedback")` | Nullable |

**Ограничения таблицы:**

```java
@Table(name = "submissions", uniqueConstraints = {
    @jakarta.persistence.UniqueConstraint(columnNames = {"assignment_id", "student_id"})
})
```

Составной уникальный constraint `UNIQUE(assignment_id, student_id)` защищает на уровне БД: один студент — одно решение на задание.

**JavaDoc:** на русском, с `@param`/`@return`/`@throws` для конструктора и публичных методов, описаниями на русском для каждого поля.

### Действие 2. Добавить связь в `Assignment`

**Файл:** `src/main/java/com/studying/platform/entity/Assignment.java`

Добавить поле:

```java
/**
 * Решения, отправленные студентами на данное задание.
 * Загружаются лениво. Владелец обратной связи — сущность {@link Submission}.
 * Каскады не настроены — сохранённые решения не должны удаляться при удалении задания.
 */
@OneToMany(mappedBy = "assignment", fetch = FetchType.LAZY)
private Set<Submission> submissions = new HashSet<>();
```

- `cascade` **не указывается** (по умолчанию `CascadeType.MERGE` в Hibernate, но для безопасности явного перечисления нет).
- `orphanRemoval = false` (по умолчанию).
- getter `getSubmissions()` и setter `setSubmissions(Set<Submission>)` с JavaDoc.

### Действие 3. Создать `SubmissionRepository`

**Файл:** `src/main/java/com/studying/platform/repository/SubmissionRepository.java`

```java
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /**
     * Находит все решения заданного задания.
     * @param assignmentId идентификатор задания
     * @return список решений
     */
    List<Submission> findByAssignmentId(Long assignmentId);

    /**
     * Находит все решения данного студента.
     * @param studentId идентификатор студента
     * @return список решений
     */
    List<Submission> findByStudentId(Long studentId);

    /**
     * Находит решение конкретного студента для конкретного задания.
     * <p>
     * Используется для проверки наличия уже отправленного решения
     * перед новой отправкой. Возвращает пустое значение, если решение не найдено.
     * </p>
     * @param assignmentId идентификатор задания
     * @param studentId идентификатор студента
     * @return обязательное значение с решением, если найдено; пустое значение — если нет
     */
    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
}
```

### Действие 4. Создать `SubmissionService`

**Файл:** `src/main/java/com/studying/platform/service/SubmissionService.java`

Сервис с `@Service` и `@Transactional` на классе.

**Зависимости (внедряются через конструктор):**
- `SubmissionRepository` — сохранение и проверка дубликатов
- `AssignmentRepository` — проверка существования задания
- `EnrollmentRepository` — проверка записи на курс
- `LessonRepository` — получение урока из задания
- `EntityManager` — flush после persist

**Метод `submitSolution(Long assignmentId, Long studentId, String content)`**:

1. Найти `Assignment` по `assignmentId`; если не найден — `ResourceNotFoundException("Задание с идентификатором X не найдено")`.
2. Найти `Lesson` по `assignment.getLesson().getId()` (lazy-загрузка внутри транзакции безопасна).
3. Найти `Course` по `lesson.getModule().getCourse().getId()` (lazy-загрузка внутри транзакции безопасна).
4. Проверить, что студент записан на курс (статус ACTIVE):
   ```java
   Optional<Enrollment> enrollment = enrollmentRepository
       .findByUserIdAndCourseIdAndStatus(studentId, courseId, EnrollmentStatus.ACTIVE);
   ```
   Если не найдена — `ResourceNotFoundException("Студент с идентификатором X не записан на курс Y")`.
5. Проверить, что нет уже отправленного решения:
   ```java
   Optional<Submission> existing = submissionRepository
       .findByAssignmentIdAndStudentId(assignmentId, studentId);
   ```
   Если найдена — `IllegalStateException("Студент с идентификатором X уже отправил решение для задания с идентификатором Y")`.
6. Создать `Submission` с `submittedAt = LocalDateTime.now()`, `content`, и связями `assignment` и `student`.
7. `entityManager.persist(submission); entityManager.flush();`
8. Вернуть сохранённый `Submission`.

**JavaDoc:** на русском для класса, конструктора и метода `submitSolution`.

### Действие 5. Написать интеграционные тесты

**Файл:** `src/test/java/com/studying/platform/SubmissionTests.java`

**Аннотации класса:**
```java
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SubmissionService.class)
```

**Зависимости:** `EntityManager`, `TestEntityManager`, `SubmissionRepository`, `AssignmentRepository`, `LessonRepository`, `ModuleRepository`, `CourseRepository`, `CategoryRepository`, `UserRepository`, `EnrollmentRepository`, `SubmissionService`, `DataSource`, `JdbcTemplate`.

**Тест-кейсы (позитивные):**

1. **`submitSolution_shouldSaveSolution`** — полный сценарий: создать преподавателя, категорию, курс, модуль, урок, задание, студента, запись на курс → вызвать `submitSolution` → проверить: `id != null`, `submittedAt != null`, `content` совпадает, `assignment` связан, `student` связан, `score == null`, `feedback == null`. Проверить сохранение в БД через репозиторий.

2. **`submitSolution_textStorageViaJpaAndSql`** — аналогично `AssignmentServiceTests.testTextStorageViaJpaAndSql`: сохранить решение с длинным текстом (50 строк), прочитать через JPA и через `JdbcTemplate`, убедиться в совпадении. TEXT без `@Lob`.

3. **`submissionRepositoryFindByAssignmentId`** — создать 3 решения для одного задания разными студентами, проверить выборку по `assignmentId`.

4. **`submissionRepositoryFindByStudentId`** — создать 2 решения одного студента на разные задания, проверить выборку по `studentId`.

5. **`submissionRepositoryFindByAssignmentIdAndStudentId_existing`** — решение существует → `Optional.isPresent()`.

6. **`submissionRepositoryFindByAssignmentIdAndStudentId_notExisting`** — решения нет → `Optional.isEmpty()`.

7. **`assignment_submissionsCollection`** — после сохранения нескольких решений проверить, что `assignment.getSubmissions()` возвращает корректный размер (нужна инициализация внутри транзакции).

**Тест-кейсы (негативные):**

8. **`submitSolution_nonExistentAssignment_shouldThrow`** — вызвать `submitSolution` с несуществующим `assignmentId` → `ResourceNotFoundException`.

9. **`submitSolution_studentNotEnrolled_shouldThrow`** — студент не записан на курс (нет записи) → `ResourceNotFoundException`.

10. **`submitSolution_teacherCannotSubmit`** — пользователь с ролью TEACHER пытается отправить решение (нет записи на курс, так как только STUDENT может записаться) → `ResourceNotFoundException`.

11. **`submitSolution_duplicateSolution_shouldThrow`** — отправить решение, затем повторить → `IllegalStateException`.

12. **`submitSolution_afterDropEnrollment_shouldThrow`** — записать студента, отправить решение, отписать студента (DROPPED), затем попытаться отправить новое решение → `ResourceNotFoundException` (статус DROPPED, не ACTIVE).

13. **`submission_nullAssignment_shouldFail`** — попытка создать `Submission` с `null` assignment → `ConstraintViolationException`.

14. **`submission_nullStudent_shouldFail`** — попытка создать `Submission` с `null` student → `ConstraintViolationException`.

15. **`submission_nullContent_shouldFail`** — попытка создать `Submission` с `null` content → `ConstraintViolationException`.

16. **`uniqueConstraint_assignmentIdStudentId_shouldPreventDuplicate`** — аналог `EnrollmentTests.uniqueConstraint_userIdCourseId_shouldPreventDuplicate`: создать две записи `Submission` с одинаковой парой `assignment_id` + `student_id` напрямую через `entityManager.persist()` → ожидаем `PersistenceException` при flush.

17. **`databaseIsPostgreSQL`** — проверка, что используется PostgreSQL через Testcontainers.

### Действие 5.5. Обновить `TestDataCleanupHelper`

**Файл:** `src/test/java/com/studying/platform/TestDataCleanupHelper.java`

Добавить поддержку удаления `Submission`:

1. Добавить параметр `Set<Long> submissionIds` в метод `clearTestEntities` (первым параметром после `transactionTemplate`, перед `reviewCourseIds`).
2. Добавить JPQL-удаление на первое место (субмиссии зависят от assignment, который зависит от lesson, module, course):
   ```java
   if (submissionIds != null && !submissionIds.isEmpty()) {
       entityManager.createQuery("DELETE FROM Submission s WHERE s.id IN (:ids)")
               .setParameter("ids", submissionIds)
               .executeUpdate();
   }
   ```
3. Обновить Javadoc метода: добавить описание параметра `submissionIds` и обновить порядок удаления в описании.

**Важно:** все вызывающие тесты (`TestDataCleanupHelperTests`, `CourseServiceTransactionTests`, и новый `SubmissionTests`) должны передавать `Set.of()` для `submissionIds`, если они не создают submissions.

### Действие 6. Обновить `PostgresSchemaTests`

**Файл:** `src/test/java/com/studying/platform/PostgresSchemaTests.java`

1. **`requiredTablesExist`** — добавить `"submissions"` в массив `expectedTables`.

2. **`primaryKeysExist`** — добавить `expectedPks.put("submissions", Set.of("id"))`.

3. **`foreignKeysExist`** — добавить два FK:
   - `{"submissions", "assignment_id", "assignments", "id"}`
   - `{"submissions", "student_id", "users", "id"}`

4. **`uniqueConstraintsExist`** — добавить `expectedConstraints.put("submissions", List.of("assignment_id", "student_id"))`.

5. **`requiredTablesExist` проверка на отсутствие неожиданных таблиц** — текущий тест `PostgresSchemaTests` проверяет только наличие (assertTrue), но не запрещает лишние. Убедиться, что новые таблицы не нарушают существующие проверки.

## Критерии приёмки

| # | Критерий | Статус (заполняется при проверке) |
|---|---|---|
| 1 | Сущность `Submission` создана с полями id, assignment, student, submittedAt, content, score, feedback. | |
| 2 | Текстовое поле `content` маппится без `@Lob` (через `columnDefinition = "TEXT"`). | |
| 3 | Уникальный составной constraint `UNIQUE(assignment_id, student_id)` существует на уровне БД. | |
| 4 | `SubmissionRepository` содержит методы `findByAssignmentId`, `findByStudentId`, `findByAssignmentIdAndStudentId`. | |
| 5 | `SubmissionService.submitSolution` проверяет: существование задания, записку студента на курс (ACTIVE), отсутствие повторного решения. | |
| 6 | При повторной отправке — `IllegalStateException`. | |
| 7 | При отправке незаписанным студентом — `ResourceNotFoundException`. | |
| 8 | `Assignment` содержит `@OneToMany submissions` (`fetch = LAZY`, без каскадов). | |
| 9 | Интеграционные тесты (позитивные и негативные) проходят. | |
| 10 | `PostgresSchemaTests` обновлены: таблица, PK, FK, UNIQUE. | |
| 11 | TEXT-поле `content` сохраняется и читается корректно через JPA и SQL. | |

## Команды проверки

| Команда | Описание |
|---|---|
| `mvn compile test-compile` | Проверка компиляции исходного кода и тестов |
| `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn test` | Полный прогон всех тестов через Testcontainers (PostgreSQL) |
| `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn test -Dtest=SubmissionTests` | Прогон только тестов Submission |
| `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn test -Dtest=PostgresSchemaTests` | Прогон только схемных тестов |

> Все команды используют `JAVA_HOME` для Java 23, как зафиксировано в `state.md`. Тестовый профиль `test` использует Testcontainers.

## Ограничения, риски и блокеры

### Ограничения
- Каскады удаления **не настраиваются**: сохранённые решения не должны удаляться при удалении задания, урока, модуля или курса. Это гарантирует защиту учебной истории (см. `plan.md`, пункт про удаление).
- `@ManyToOne` на `Assignment` и `User` — `fetch = LAZY`. Доступ к полям связанных сущностей требует работы внутри транзакции.

### Риски
- **Цепочка lazy-загрузок:** `submitSolution` идёт по пути `assignment → lesson → module → course`. Все эти переходы — lazy. Риск `LazyInitializationException` устраняется тем, что весь метод выполняется внутри `@Transactional`.
- **Конфликт с `TestDataCleanupHelper`:** если `TestDataCleanupHelper` не поддерживает удаление сущностей `Submission` (таблицы нет в его сигнатуре), тесты могут оставлять данные. Проверить, что очистка работает через репозиторий вручную, или добавить поддержку в хелпер на этом же этапе (если его сигнатура позволяет).
- **Порядок FK в схеме:** Hibernate может создавать FK `submissions.assignment_id → assignments.id` и `submissions.student_id → users.id`. Тесты `PostgresSchemaTests` ожидают точные имена FK. Убедиться, что `@ForeignKey(name = ...)` задано корректно.

### Блокеры
- Нет блокирующих ADR или архитектурных противоречий.
- Если `TestDataCleanupHelper` не позволяет адресно чистить `Submission` — тесты могут конфликтовать друг с другом. Решение: в каждом тесте использовать уникальный префикс email и адресное удаление по ID в `@AfterEach` (как в `CourseServiceTransactionTests`).

## Сводка изменений

| Файл | Действие |
|---|---|
| `src/main/java/com/studying/platform/entity/Submission.java` | Создать |
| `src/main/java/com/studying/platform/entity/Assignment.java` | Изменить: добавить `Set<Submission> submissions` |
| `src/main/java/com/studying/platform/repository/SubmissionRepository.java` | Создать |
| `src/main/java/com/studying/platform/service/SubmissionService.java` | Создать |
| `src/test/java/com/studying/platform/SubmissionTests.java` | Создать (~17 тестов) |
| `src/test/java/com/studying/platform/PostgresSchemaTests.java` | Изменить: добавить `submissions` в таблицы, PK, FK, UNIQUE |
| `src/test/java/com/studying/platform/TestDataCleanupHelper.java` | Изменить: добавить параметр `submissionIds` и JPQL-удаление на первое место |
