# Этап 15. Удаление и целостность

## 1. Описание этапа

Реализовать согласованные правила удаления курса и его зависимых объектов с защитой записей и учебной истории. Курс без записей и учебной истории можно удалить вместе с его содержимым (модули и уроки удаляются каскадом). При наличии записей, ранее сохранённых решений, результатов тестов или отзывов удаление запрещено, в том числе после отписки студентов. Общие пользователи, категории и теги при удалении курса не затрагиваются.

Отписка — отдельная операция, не удаляющая учебную историю.

## 2. Связанные файлы

### Исходные файлы (изменяемые)
- `src/main/java/com/studying/platform/repository/SubmissionRepository.java` — добавить метод `countByAssignmentIds`
- `src/main/java/com/studying/platform/repository/QuizSubmissionRepository.java` — добавить метод `countByQuizIds`
- `src/main/java/com/studying/platform/repository/CourseRepository.java` — добавить методы `countEnrollmentsByCourseId` и `countReviewsByCourseId`
- `src/main/java/com/studying/platform/repository/AssignmentRepository.java` — добавить метод `countByLessonIds` (для оптимизации подсчёта submissions)
- `src/main/java/com/studying/platform/repository/QuizRepository.java` — добавить метод `countByModuleIds` (для оптимизации подсчёта quiz submissions)
- `src/main/java/com/studying/platform/service/CourseService.java` — добавить метод `deleteCourse` и новые зависимости репозиториев

### Исходные файлы (неизменяемые, только ссылки)
- `src/main/java/com/studying/platform/entity/Course.java` — каскады: modules `REMOVE`, enrollments/reviews `none`
- `src/main/java/com/studying/platform/entity/Module.java` — каскады: lessons `REMOVE`
- `src/main/java/com/studying/platform/entity/Assignment.java` — каскады: submissions `none`
- `src/main/java/com/studying/platform/entity/Submission.java` — защищённая учебная история
- `src/main/java/com/studying/platform/entity/Quiz.java` — OneToOne к Module, quizSubmissions `none`
- `src/main/java/com/studying/platform/entity/QuizSubmission.java` — защищённая учебная история
- `src/main/java/com/studying/platform/entity/CourseReview.java` — защищённая учебная история
- `src/main/java/com/studying/platform/entity/Enrollment.java` — не удаляется при удалении курса
- `src/main/java/com/studying/platform/entity/User.java` — общий объект, не затрагивается
- `src/main/java/com/studying/platform/entity/Category.java` — общий объект, не затрагивается
- `src/main/java/com/studying/platform/entity/Tag.java` — общий объект, не затрагивается
- `src/main/java/com/studying/platform/exception/ResourceNotFoundException.java` — существующий класс

### Тестовые файлы (новые и изменяемые)
- `src/test/java/com/studying/platform/CourseDeletionTests.java` — **новый** файл интеграционных тестов (~12 тестов)
- `src/test/java/com/studying/platform/CourseManagementTests.java` — обновить `setUp()` для конструктора `CourseService`
- `src/test/java/com/studying/platform/PostgresSchemaTests.java` — проверить FK на `submissions`, `quizsubmission`, `course_reviews`, `enrollments` (уже покрыты в `foreignKeysExist`)

### АDR
Нет утверждённых ADR, которые бы относились к этому этапу. ADR-индекс пуст.

## 3. Пошаговый план реализации

### Шаг 1. Дополнительные методы репозиториев

#### 1.1. `SubmissionRepository` — `countByAssignmentIds`
```java
long countByAssignmentIds(Collection<Long> assignmentIds);
```
JPQL: `SELECT COUNT(s) FROM Submission s WHERE s.assignment.id IN :assignmentIds`

#### 1.2. `QuizSubmissionRepository` — `countByQuizIds`
```java
long countByQuizIds(Collection<Long> quizIds);
```
JPQL: `SELECT COUNT(qs) FROM QuizSubmission qs WHERE qs.quiz.id IN :quizIds`

#### 1.3. `CourseRepository` — `countEnrollmentsByCourseId` и `countReviewsByCourseId`
```java
long countEnrollmentsByCourseId(Long courseId);
```
JPQL: `SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId`

```java
long countReviewsByCourseId(Long courseId);
```
JPQL: `SELECT COUNT(r) FROM CourseReview r WHERE r.course.id = :courseId`

#### 1.4. `AssignmentRepository` — `countByLessonIds` (опционально для оптимизации)
```java
long countByLessonIdIn(Collection<Long> lessonIds);
```
Можно использовать для быстрой проверки наличия заданий.

#### 1.5. `QuizRepository` — `countByModuleIds` (опционально для оптимизации)
```java
long countByModuleIds(Collection<Long> moduleIds);
```
Или `findByModuleId` уже есть — можно использовать для проверки.

### Шаг 2. Обновление `CourseService`

#### 2.1. Добавить новые зависимости в конструктор
- `SubmissionRepository submissionRepository`
- `QuizSubmissionRepository quizSubmissionRepository`
- `QuizRepository quizRepository`
- `EnrollmentRepository enrollmentRepository`
- `CourseReviewRepository courseReviewRepository`

#### 2.2. Метод `deleteCourse(Long courseId)`
Публичный транзакционный метод:

```
1. Найти курс по ID → ResourceNotFoundException если нет
2. Инициализировать ленивую коллекцию modules (доступ к модулям)
3. Собрать все lesson IDs из всех модулей (через module.getLessons())
4. Собрать все assignment IDs из всех уроков (через assignmentRepository.findByLessonId())
5. Собрать все quiz IDs из модулей (через quizRepository.findByModuleId())
6. Проверить:
   - submissionRepository.countByAssignmentIds(assignmentIds) == 0
   - quizSubmissionRepository.countByQuizIds(quizIds) == 0
   - enrollmentRepository.countEnrollmentsByCourseId(courseId) == 0
   - courseReviewRepository.countReviewsByCourseId(courseId) == 0
7. Если любая проверка не пройдена → IllegalStateException с понятным сообщением
8. courseRepository.delete(course)
```

**Важные нюансы:**
- Проверка `enrollments` должна учитывать записи любого статуса (и ACTIVE, и DROPPED). Метод `countEnrollmentsByCourseId` считает все записи курса.
- Проверка `submissions` и `quizSubmissions` должна учитывать все записи, включая те, что остались после отписки.
- Метод должен быть `@Transactional` (уже есть на классе, но убедиться, что rollback при исключении работает).

### Шаг 3. Обновление конструктора `CourseService`

Текущий конструктор `CourseService` принимает 8 параметров. Новый конструктор должен принимать 13 параметров:

```java
public CourseService(CourseRepository courseRepository,
                     ModuleRepository moduleRepository,
                     LessonRepository lessonRepository,
                     UserRepository userRepository,
                     CategoryRepository categoryRepository,
                     EntityManager entityManager,
                     AssignmentRepository assignmentRepository,
                     TagRepository tagRepository,
                     SubmissionRepository submissionRepository,
                     QuizSubmissionRepository quizSubmissionRepository,
                     QuizRepository quizRepository,
                     EnrollmentRepository enrollmentRepository,
                     CourseReviewRepository courseReviewRepository)
```

### Шаг 4. Интеграционные тесты — `CourseDeletionTests.java`

Тестовый класс использует `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@ActiveProfiles("test")` — та же модель, что в `CourseManagementTests.java`.

#### 4.1. Позитивные тесты

**`deleteEmptyCourse_shouldSucceed`**
- Создать преподавателя, категорию, курс (без модулей).
- Вызвать `courseService.deleteCourse(courseId)`.
- Проверить: `courseRepository.findById(courseId)` возвращает `Optional.empty()`.

**`deleteCourseWithModulesAndLessons_shouldCascadeRemoveContent`**
- Создать курс с модулем, уроком, заданием (но без submissions).
- Вызвать `deleteCourse(courseId)`.
- Проверить: курс, модуль, урок, задание удалены.
- Проверить: `moduleRepository.findById(moduleId)` — `Optional.empty()`.
- Проверить: `lessonRepository.findById(lessonId)` — `Optional.empty()`.

**`deleteCourseAfterStudentsDropOut_shouldSucceed`**
- Создать курс, модуль, урок, задание, студента, запись (ACTIVE), отписать студента (DROPPED).
- Вызвать `deleteCourse(courseId)`.
- Проверить: курс удалён, запись (DROPPED) — сохранена.

#### 4.2. Негативные тесты

**`deleteCourseWithActiveEnrollments_shouldFail`**
- Создать курс, студента, активную запись.
- Вызвать `deleteCourse` → `IllegalStateException`.
- Проверить: курс не удалён.

**`deleteCourseWithDroppedEnrollments_shouldFail`**
- Создать курс, студента, запись → отписаться (DROPPED).
- Вызвать `deleteCourse` → `IllegalStateException`.
- Проверить: курс не удалён.

**`deleteCourseWithSubmissions_shouldFail`**
- Создать курс, модуль, урок, задание, студента, запись, решение.
- Вызвать `deleteCourse` → `IllegalStateException`.
- Проверить: курс не удалён, решение сохранено.

**`deleteCourseWithQuizSubmissions_shouldFail`**
- Создать курс, модуль, викторину, студента, запись, результат теста.
- Вызвать `deleteCourse` → `IllegalStateException`.
- Проверить: курс не удалён, результат сохранён.

**`deleteCourseWithReviews_shouldFail`**
- Создать курс, студента, запись, отзыв.
- Вызвать `deleteCourse` → `IllegalStateException`.
- Проверить: курс не удалён, отзыв сохранён.

**`deleteNonExistentCourse_shouldThrow`**
- Вызвать `deleteCourse(99999L)` → `ResourceNotFoundException`.

#### 4.3. Защита общих объектов

**`deleteCourse_shouldNotAffectTeacher`**
- Создать курс с преподавателем, удалить курс.
- Проверить: `userRepository.findById(teacherId)` возвращает курс.

**`deleteCourse_shouldNotAffectCategory`**
- Создать курс с категорией, удалить курс.
- Проверить: `categoryRepository.findById(categoryId)` возвращает категорию.

**`deleteCourse_shouldNotAffectTags`**
- Создать курс с тегами, удалить курс.
- Проверить: `tagRepository.findById(tagId)` возвращает тег.
- Проверить: тег не удалён из таблицы `tags`.

## 4. Критерии приёмки

| № | Критерий | Статус |
|---|----------|--------|
| 1 | Метод `deleteCourse(Long)` реализован в `CourseService` с проверками | |
| 2 | Пустой курс удаляется вместе с содержимым | |
| 3 | Курс с модулями и уроками (без истории) удаляется, содержимое cascade-удалено | |
| 4 | Курс с активными записями не удаляется | |
| 5 | Курс с отписавшимися студентами (DROPPED) не удаляется | |
| 6 | Курс с решениями не удаляется | |
| 7 | Курс с результатами тестов не удаляется | |
| 8 | Курс с отзывами не удаляется | |
| 9 | После удаления курса преподаватель, категория и теги сохранены | |
| 10 | Отписка не удаляет учебную историю (существующий `EnrollmentService.dropEnrollment`) | |
| 11 | Все 12 интеграционных тестов проходят | |
| 12 | FK-ограничения на `submissions`, `quizsubmission`, `course_reviews`, `enrollments` остаются в схеме | |

## 5. Команды проверки

### Компиляция
```bash
mvn compile -q
```

### Прогон всех тестов (с Testcontainers и PostgreSQL)
```bash
mvn test -q
```

### Прогон только тестов удаления курса
```bash
mvn test -Dtest=CourseDeletionTests -q
```

### Прогон только схемы PostgreSQL
```bash
mvn test -Dtest=PostgresSchemaTests#foreignKeysExist -q
```

### Прогон только существующих тестов CourseManagement (после обновления конструктора)
```bash
mvn test -Dtest=CourseManagementTests -q
```

## 6. Ограничения, риски и блокеры

### Ограничения
- `CourseService` помечен `@Transactional` на уровне класса. Все новые методы унаследуют эту аннотацию.
- При удалении курса каскад `REMOVE` на `modules` (`orphanRemoval=true`) автоматически удалит все модули и их уроки.
- `Assignment`, `Submission`, `QuizSubmission`, `CourseReview`, `Enrollment` не имеют каскада `REMOVE` — они не удаляются при удалении курса.

### Риски
1. **Пустой `assignmentIds`/`quizIds`**: при вызове `countByAssignmentIds(emptyList)` — JPQL `IN ()` может вызвать синтаксическую ошибку. Решение: пропустить проверку, если список пуст.
2. **Массовая загрузка модулей/уроков**: при большом курсе коллекция `modules` может быть большой. Но для учебного проекта это не проблема.
3. **Изменение конструктора `CourseService`**: все существующие тесты и код, использующий `CourseService`, должны быть обновлены для нового конструктора (13 параметров вместо 8).

### Блокеры
- Нет ADR-блокеров.
- Для прохождения тестов требуется Docker для Testcontainers PostgreSQL.

### Примечание по JPQL `IN ()`
При пустом списке assignment IDs (курс без уроков) вызов `countByAssignmentIds(Collections.emptyList())` может вызвать JPA-ошибку. В `deleteCourse` нужно добавить проверку: если список пуст — пропустить эту проверку (считаем, что submissions нет).

```java
if (!assignmentIds.isEmpty() && submissionRepository.countByAssignmentIds(assignmentIds) > 0) {
    throw new IllegalStateException("Курс имеет сохранённые решения");
}
```
