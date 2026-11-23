# Этап 18. API записи, заданий и решений

## Цель и границы этапа

Реализовать REST-контроллеры и DTO для трёх предметных областей:

1. **Записи на курс** (`Enrollment`) — запись, отписка, получение списков.
2. **Задания** (`Assignment`) — CRUD-операции, привязка к урокам.
3. **Решения** (`Submission`) — отправка решений, оценивание, списки решений.

**Сервисный слой** (`EnrollmentService`, `SubmissionService`) уже реализован и содержит все необходимые бизнес-методы. В рамках этапа создаётся только **новый сервис** `AssignmentService`, три **контроллера** и набор **DTO**.

Границы: этап не затрагивает сущности, репозитории, существующие контроллеры или глобальный обработчик ошибок. Не планируется ничего после этапа 18.

## Связанные файлы и применимые ADR

### Существующие файлы (только для чтения / вызова)

| Путь | Роль |
|---|---|
| `src/main/java/…/entity/Enrollment.java` | Сущность записи |
| `src/main/java/…/entity/Assignment.java` | Сущность задания |
| `src/main/java/…/entity/Submission.java` | Сущность решения |
| `src/main/java/…/entity/User.java` | Сущность пользователя (для ролей) |
| `src/main/java/…/entity/Lesson.java` | Сущность урока (для привязки задания) |
| `src/main/java/…/repository/EnrollmentRepository.java` | Репозиторий записей |
| `src/main/java/…/repository/AssignmentRepository.java` | Репозиторий заданий |
| `src/main/java/…/repository/SubmissionRepository.java` | Репозиторий решений |
| `src/main/java/…/service/EnrollmentService.java` | Сервис записей (уже реализован) |
| `src/main/java/…/service/SubmissionService.java` | Сервис решений (уже реализован) |
| `src/main/java/…/controller/CourseController.java` | Ориентир стиля контроллеров |
| `src/main/java/…/controller/LessonController.java` | Ориентир стиля контроллеров |
| `src/main/java/…/exception/GlobalExceptionHandler.java` | Глобальный обработчик ошибок |
| `src/main/java/…/dto/ErrorResponseDto.java` | Формат ответов об ошибках |

### ADR
Индекс `docs/adr/README.md` пуст — нет утверждённых ADR, ограничивающих текущий этап.

## Создаваемые и изменяемые файлы

### DTO (создаются)

| Файл | Назначение |
|---|---|
| `src/main/java/…/dto/EnrollmentRequestDto.java` | Тело POST `/api/enrollments` |
| `src/main/java/…/dto/EnrollmentResponseDto.java` | Ответ GET-запросов на записи |
| `src/main/java/…/dto/AssignmentRequestDto.java` | Тело POST/PUT для заданий |
| `src/main/java/…/dto/AssignmentResponseDto.java` | Ответ GET-запросов на задания |
| `src/main/java/…/dto/SubmissionRequestDto.java` | Тело POST `/api/submissions` |
| `src/main/java/…/dto/SubmissionResponseDto.java` | Ответ GET-запросов на решения |
| `src/main/java/…/dto/GradeSubmissionRequestDto.java` | Тело PATCH `/api/submissions/{id}` |

### Сервисы (создаются)

| Файл | Назначение |
|---|---|
| `src/main/java/…/service/AssignmentService.java` | CRUD для заданий: создание, обновление, удаление, поиск, получение по уроку |

### Контроллеры (создаются)

| Файл | Эндпоинты |
|---|---|
| `src/main/java/…/controller/EnrollmentController.java` | `POST /enrollments`, `GET /courses/{courseId}/enrollments`, `GET /users/{userId}/enrollments`, `DELETE /enrollments/{courseId}` |
| `src/main/java/…/controller/AssignmentController.java` | `POST /lessons/{lessonId}/assignments`, `GET /assignments/{id}`, `PUT /assignments/{id}`, `DELETE /assignments/{id}`, `GET /lessons/{lessonId}/assignments` |
| `src/main/java/…/controller/SubmissionController.java` | `POST /submissions`, `PATCH /submissions/{id}`, `GET /assignments/{assignmentId}/submissions`, `GET /users/{userId}/submissions` |

### Тесты (создаются)

| Файл | Назначение |
|---|---|
| `src/test/java/…/controller/EnrollmentControllerTest.java` | HTTP-тесты контроллера записей |
| `src/test/java/…/controller/AssignmentControllerTest.java` | HTTP-тесты контроллера заданий |
| `src/test/java/…/controller/SubmissionControllerTest.java` | HTTP-тесты контроллера решений |

## Порядок действий

### Действие 1. Создание DTO

1. Создать `EnrollmentRequestDto` — поле `studentId` (Long), `courseId` (Long). Использовать `jakarta.validation.constraints.NotNull`.
2. Создать `EnrollmentResponseDto` — поля `id`, `studentId`, `courseId`, `status`, `enrollDate`. Конструктор с параметрами.
3. Создать `AssignmentRequestDto` — поля `lessonId`, `title`, `description`, `dueDate`, `maxScore`. `@Valid` на входе.
4. Создать `AssignmentResponseDto` — поля `id`, `lessonId`, `title`, `description`, `dueDate`, `maxScore`.
5. Создать `SubmissionRequestDto` — поля `assignmentId`, `studentId`, `content`. `@NotNull` на `content`.
6. Создать `SubmissionResponseDto` — поля `id`, `assignmentId`, `studentId`, `content`, `submittedAt`, `score`, `feedback`.
7. Создать `GradeSubmissionRequestDto` — поля `score` (Integer), `feedback` (String, nullable).

**Стиль:** каждый DTO — класс с публичным конструктором по умолчанию (для десериализации), конструктором со всеми параметрами, геттерами/сеттерами, JavaDoc на русском для каждого поля и метода.

### Действие 2. Создание `AssignmentService`

Создать `AssignmentService.java` в пакете `service`:

- Внедрить через конструктор: `AssignmentRepository`, `LessonRepository`, `EntityManager`.
- Методы:

  - `Assignment createAssignment(Long lessonId, String title, String description, LocalDate dueDate, Integer maxScore)` — проверить существование урока, создать и сохранить задание.
  - `Assignment updateAssignment(Long assignmentId, String title, String description, LocalDate dueDate, Integer maxScore)` — найти задание, обновить не-null поля.
  - `void deleteAssignment(Long assignmentId)` — найти и удалить задание.
  - `Assignment findAssignment(Long assignmentId)` — найти по ID.
  - `List<Assignment> getAssignmentsByLesson(Long lessonId)` — найти по lessonId.

**Стиль:** аннотации `@Service`, `@Transactional`, JavaDoc на русском для каждого метода, выброс `ResourceNotFoundException` при отсутствии ресурсов.

### Действие 3. Создание `EnrollmentController`

Создать `EnrollmentController.java` в пакете `controller`:

- Внедрить: `EnrollmentService`, `UserRepository`, `CourseRepository`.
- `@RequestMapping("/api/enrollments")`.
- Эндпоинты:

  - **`POST`** `/api/enrollments` — `enrollStudent`
    - Тело: `EnrollmentRequestDto` (`studentId`, `courseId`), `@Valid`.
    - Вызов: `enrollmentService.enrollStudent(dto.getStudentId(), dto.getCourseId())`.
    - Ответ: `201 Created` + `EnrollmentResponseDto`.
    - Ошибки перехватываются `GlobalExceptionHandler` (`ResourceNotFoundException` → 404, `IllegalStateException` → 400).

  - **`GET`** `/api/courses/{courseId}/enrollments` — студенты курса.
    - Вызов: `enrollmentService.getCourseStudents(courseId)`.
    - Ответ: `200 OK` + `List<UserResponseDto>`.

  - **`GET`** `/api/users/{userId}/enrollments` — курсы студента.
    - Вызов: `enrollmentService.getStudentCourses(userId)`.
    - Ответ: `200 OK` + `List<CourseResponseDto>`.

  - **`DELETE`** `/api/enrollments/{courseId}` — отписка.
    - В теле запроса (или параметре) `studentId` для идентификации студента.
    - Вызов: `enrollmentService.dropEnrollment(studentId, courseId)`.
    - Ответ: `204 No Content`.

**Стиль:** аннотации `@RestController`, `@RequestMapping`, `@PostMapping`, `@GetMapping`, `@DeleteMapping`, `@PathVariable`, `@RequestBody`. JavaDoc на русском для каждого метода. Конвертация сущностей в DTO через приватные методы.

### Действие 4. Создание `AssignmentController`

Создать `AssignmentController.java` в пакете `controller`:

- Внедрить: `AssignmentService`, `LessonRepository`.
- `@RequestMapping("/api/assignments")`.
- Эндпоинты:

  - **`POST`** `/api/lessons/{lessonId}/assignments` — создать задание.
    - Тело: `AssignmentRequestDto`.
    - Ответ: `201 Created` + `AssignmentResponseDto`.

  - **`GET`** `/api/assignments/{id}` — получить задание.
    - Ответ: `200 OK` + `AssignmentResponseDto`.

  - **`PUT`** `/api/assignments/{id}` — обновить задание.
    - Тело: `AssignmentRequestDto`.
    - Ответ: `200 OK` + `AssignmentResponseDto`.

  - **`DELETE`** `/api/assignments/{id}` — удалить задание.
    - Ответ: `204 No Content`.

  - **`GET`** `/api/lessons/{lessonId}/assignments` — список заданий урока.
    - Вызов: `assignmentService.getAssignmentsByLesson(lessonId)`.
    - Ответ: `200 OK` + `List<AssignmentResponseDto>`.

### Действие 5. Создание `SubmissionController`

Создать `SubmissionController.java` в пакете `controller`:

- Внедрить: `SubmissionService`.
- `@RequestMapping("/api/submissions")`.
- Эндпоинты:

  - **`POST`** `/api/submissions` — отправить решение.
    - Тело: `SubmissionRequestDto` (`assignmentId`, `studentId`, `content`), `@Valid`.
    - Вызов: `submissionService.submitSolution(dto.getAssignmentId(), dto.getStudentId(), dto.getContent())`.
    - Ответ: `201 Created` + `SubmissionResponseDto`.
    - Ошибки: `ResourceNotFoundException` (404) — задание не найдено / студент не записан; `IllegalStateException` (400) — уже отправлено решение.

  - **`PATCH`** `/api/submissions/{id}` — оценить решение.
    - Тело: `GradeSubmissionRequestDto` (`score`, `feedback`).
    - Также нужен `teacherId` (можно через заголовок или параметр запроса, или через тело — согласовать с `SubmissionService.gradeSubmission`, который принимает `teacherId` как отдельный параметр).
    - Вызов: `submissionService.gradeSubmission(id, dto.getScore(), dto.getFeedback(), teacherId)`.
    - Ответ: `200 OK` + `SubmissionResponseDto` (с обновлёнными полями).
    - Ошибки: `ResourceNotFoundException` (404) — решение/преподаватель не найден; `IllegalArgumentException` (400) — оценка вне диапазона / не преподаватель.

  - **`GET`** `/api/assignments/{assignmentId}/submissions` — решения задания.
    - Вызов: `submissionService.getSubmissionsByAssignment(assignmentId)`.
    - Ответ: `200 OK` + `List<SubmissionResponseDto>`.

  - **`GET`** `/api/users/{userId}/submissions` — решения пользователя.
    - Вызов: `submissionService.getSubmissionsByStudent(userId)`.
    - Ответ: `200 OK` + `List<SubmissionResponseDto>`.

### Действие 6. Написание тестов контроллеров

Каждый тестовый класс — интеграционный тест с `@SpringBootTest` + `@AutoConfigureMockMvc`, профилем `test` (PostgreSQL через Testcontainers).

#### `EnrollmentControllerTest`

Позитивные сценарии:
- Студент записывается на курс → 201.
- Получение списка студентов курса → 200, корректная длина.
- Получение списка курсов студента → 200, корректная длина.
- Отписка студента от курса → 204.

Негативные сценарии:
- Преподаватель пытается записаться → 404 (не является студентом).
- Повторная запись того же студента на тот же курс → 400 (duplicate).
- Отписка при отсутствии записи → 404.
- Несуществующий студент → 404.
- Несуществующий курс → 404.

#### `AssignmentControllerTest`

Позитивные сценарии:
- Создание задания для урока → 201.
- Получение задания по ID → 200.
- Обновление задания → 200, поля обновлены.
- Удаление задания → 204.
- Получение списка заданий урока → 200, корректная длина.

Негативные сценарии:
- Создание задания для несуществующего урока → 404.
- Обновление несуществующего задания → 404.
- Удаление несуществующего задания → 404.

#### `SubmissionControllerTest`

Позитивные сценарии:
- Студент отправляет решение → 201.
- Преподаватель оценивает решение → 200, оценка обновлена.
- Получение списка решений по заданию → 200.
- Получение списка решений по студенту → 200.

Негативные сценарии:
- Отправка решения на несуществующее задание → 404.
- Отправка решения студентом, не записанным на курс → 404.
- Повторная отправка решения (уже есть) → 400.
- Отрицательная оценка → 400.
- Оценка выше maxScore → 400.
- Оценка не-преподавателем → 400.
- Оценка несуществующего решения → 404.

## Критерии приёмки

1. Все 10 эндпоинтов (перечисленные выше) реализованы и доступны по заданным URL-путям.
2. Каждый эндпоинт возвращает HTTP-код, соответствующий сценарию (201/200/204 для успеха, 404 для NotFound, 400 для бизнес-правил/валидации).
3. Тело ответа — валидный JSON, сериализуемый в соответствующий DTO.
4. Бизнес-правила enforced:
   - Запись только студента, запрет дублирования (ACTIVE или DROPPED).
   - Отправка решения только записанным студентом, запрет повторной отправки.
   - Оценивание только преподавателем, валидация диапазона.
5. `AssignmentService` обеспечивает полный CRUD для заданий.
6. Тесты покрывают позитивные и негативные сценарии для каждого контроллера.
7. JavaDoc на русском для всех новых классов, методов и полей.

## Команды проверки

Проектом используется Maven. Тесты выполняются через Testcontainers с PostgreSQL:

```bash
mvn clean test -Dtest=EnrollmentControllerTest
mvn clean test -Dtest=AssignmentControllerTest
mvn clean test -Dtest=SubmissionControllerTest
```

Для проверки сборки всего проекта:
```bash
mvn clean compile
```

Для запуска всех тестов:
```bash
mvn clean test
```

**Важно:** существование этих команд подтверждено наличием `pom.xml` и тестов в проекте (этапы 1–17 успешно выполнены с аналогичными командами).

## Ограничения, риски и блокеры

- **`AssignmentService` отсутствует** — это не блокер, а ожидаемое действие этапа. Нужно создать сервис с нуля.
- **`teacherId` для оценивания** — `SubmissionService.gradeSubmission()` принимает `teacherId` как отдельный параметр. В `SubmissionController` потребуется извлечь его из запроса (например, через `@RequestParam("teacherId")` или `@RequestHeader`). Это требует уточнения дизайна; предлагаю использовать `@RequestParam("teacherId")` как URL-параметр.
- **Ленивые загрузки** — при конвертации сущностей в DTO нужно гарантировать, что все скалярные поля (ID, name и т.д.) доступны вне транзакции. Сущности `User`, `Course`, `Assignment` используют скалярные поля, которые доступны после выхода из транзакции. Для списков (`getCourseStudents`, `getStudentCourses`) уже используется `@EntityGraph` в репозиториях, поэтому проблем быть не должно.
- **Нет ADR-блокеров** — индекс ADR пуст, противоречий нет.
- **Не планируются этапы после 18** — план ограничен текущим этапом.

## ADR_REQUIRED

Отсутствует. Существующий код и сервисы согласованы с требованиями этапа.

## Итоговый статус

`STATUS: READY` — детальный план этапа 18 готов к реализации. Путь файла: `stages/stage-18.md`.
