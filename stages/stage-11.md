# Этап 11. Проверка работ (Оценивание и выборка решений)

**Связь с общим планом:** [plan.md](../plan.md) — Этап 11.
**Статус этапа:** `completed`

## 1. Цель и границы этапа

Добавить сервисную логику для выставления оценок студентам и получения списков решений. Реализовать валидацию оценки по отношению к максимальному баллу задания (`maxScore`), проверку существования пользователя (учителя), а также методы получения списков решений по заданию и по студенту с проверкой существования связанных объектов.

В рамках этапа не меняются схемы базы данных (схема БД уже содержит поля `score` и `feedback`).

## 2. Связанные файлы и применимые ADR

### Исходные файлы (требуют изменений)
- `src/main/java/.../service/SubmissionService.java` — добавление методов, полей и логики.
- `src/test/java/.../SubmissionTests.java` — добавление новых тестовых методов.

### Сущности и репозитории (чтение/использование)
- `Submission.java` — поля `score` (Integer, nullable), `feedback` (String, nullable).
- `Assignment.java` — поле `maxScore` (Integer).
- `User.java` — сущность пользователя (требуется для проверки существования учителя).
- `SubmissionRepository.java` — методы `findById`, `findByAssignmentId`, `findByStudentId`.
- `AssignmentRepository.java` — методы `findById`, `existsById`.
- `UserRepository.java` — методы `findById`, `existsById`.

### АDR
- ADR не требуются. Логика оценивания является естественным развитием сервисного слоя и не меняет фундаментальных архитектурных правил.

## 3. Детальный план действий

### Действие 1: Добавить зависимость `UserRepository`
В текущем конструкторе `SubmissionService` инъекция `userRepository` отсутствует.
- **Действие:** Добавить поле `private final UserRepository userRepository;` и соответствующий параметр в конструктор `SubmissionService`.
- **JavaDoc:** Добавить комментарий на русском языке к полю и методу `@Transactional`.

### Действие 2: Реализовать метод `gradeSubmission`
- **Подпись:** `public void gradeSubmission(Long submissionId, Integer score, String feedback, Long teacherId)`
- **Логика:**
  1. Найти `Submission` по `submissionId` через `submissionRepository.findById`. Если объект отсутствует — выбросить `EntityNotFoundException`.
  2. Найти `User` (учителя) по `teacherId` через `userRepository.findById`. Если объект отсутствует — выбросить `EntityNotFoundException`.
  3. Найти `Assignment` по `assignment.getId()` через `assignmentRepository.findById` для проверки поля `maxScore`.
  4. **Валидация оценки:**
     - `score` не может быть меньше `0`.
     - Если `assignment.getMaxScore()` не `null`, то `score <= assignment.getMaxScore()`.
     - При нарушении условий — выбросить `IllegalArgumentException` (или `IllegalStateException`) с текстом, описывающим диапазон (например, `Score must be between 0 and {maxScore}`).
  5. Обновить поля `score` и `feedback` в объекте `Submission`.
  6. Метод аннотирован `@Transactional`, поэтому `submissionRepository.save()` (или автоматическая синхронизация при закрытии транзакции) сохранит изменения.

### Действие 3: Реализовать методы получения списков
- **Метод `getSubmissionsByAssignment(Long assignmentId)`:**
  1. Проверить существование задания через `assignmentRepository.existsById(assignmentId)`. Если нет — выбросить `EntityNotFoundException`.
  2. Вернуть список `submissionRepository.findByAssignmentId(assignmentId)`.
- **Метод `getSubmissionsByStudent(Long studentId)`:**
  1. Проверить существование студента (User) через `userRepository.existsById(studentId)`. Если нет — выбросить `EntityNotFoundException`.
  2. Вернуть список `submissionRepository.findByStudentId(studentId)`.

### Действие 4: Добавить тесты
Добавить новые тестовые методы в `SubmissionTests.java`. Для изоляции тестовых данных использовать `TestDataCleanupHelper`.
- **Позитивные сценарии:**
  - `testGradeSubmissionSuccess` — успешное выставление оценки и комментария.
  - `testGradeSubmissionZeroScore` — выставление оценки 0.
  - `testGradeSubmissionMaxScore` — выставление оценки, равной `maxScore`.
  - `testGradeSubmissionNoUpperLimit` — выставление оценки, когда `maxScore` у задания равен `null`.
  - `testRegradeSubmission` — вызов `gradeSubmission` дважды для одного `Submission` (перезапись).
- **Негативные сценарии:**
  - `testGradeSubmissionNotFound` — попытка оценить несуществующее решение.
  - `testGradeSubmissionNegativeScore` — попытка выставить оценку < 0.
  - `testGradeSubmissionExceedsMaxScore` — попытка выставить оценку > `maxScore`.
  - `testGradeSubmissionTeacherNotFound` — попытка оценить с несуществующим `teacherId`.

### Действие 5: Запуск проверок
- Выполнить сборку и запуск тестов: `mvn verify` или `mvn test`.
- Убедиться, что новые тесты проходят успешно.

## 4. Критерии приёмки

1. **Метод `gradeSubmission` работает корректно:**
   - Оценка и комментарий сохраняются в базе данных.
   - Валидация диапазона срабатывает для оценок меньше `0` и больше `maxScore`.
   - Если `maxScore` равен `null`, оценка допустима при любом неотрицательном значении.
   - Метод позволяет обновлять уже выставленную оценку (перезапись).

2. **Методы выборки списков фильтруют данные:**
   - `getSubmissionsByAssignment` возвращает только работы текущего задания.
   - `getSubmissionsByStudent` возвращает только работы текущего студента.
   - При отсутствии задания/студента метод выбрасывает исключение.

3. **Тестовое покрытие:**
   - Добавлены позитивные и негативные тесты для всех указанных сценариев.
   - Все тесты проходят успешно (`PASS`).

## 5. Команды проверки

1. **Запуск всех проверок:**
   ```bash
   mvn verify
   ```
   *Ожидаемый результат:* Сборка проекта завершается успешно, все интеграционные тесты (включая новые) возвращают статус `PASS`.

2. **Локальная проверка (опционально):**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```
   *Цель:* Убедиться, что приложение запускается без ошибок конфигурации после изменения конструктора сервиса.

## 6. Ограничения, риски и блокеры

- **Изменение конструктора:** Добавление `UserRepository` в `SubmissionService` изменит сигнатуру конструктора. Это требует обновления всех мест, где создаётся `SubmissionService` вручную (если такие есть), и обновления тестовых контекстов.
- **Ленивая загрузка (`LazyLoading`):** При получении `maxScore` из задания (`submission.getAssignment().getMaxScore()`) необходимо находиться внутри транзакции (`@Transactional`). Риск `LazyInitializationException` отсутствует при условии корректного использования `@Transactional` на методе `gradeSubmission`.
- **Тестовые данные:** Для тестов потребуются заранее созданные объекты `Assignment`, `User` (студент и учитель) и `Submission`. Использование `TestDataCleanupHelper` для удаления `Submission` уже реализовано, что упрощает сброс состояния между тестами.
- **Рисков:** Отсутствуют. План не противоречит существующим ADR или требованиям.
