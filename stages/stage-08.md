# Этап 08. Запись на курс

## Цель и границы этапа
Реализовать бизнес-логику записи студента на курс и отписки от него через промежуточную сущность `Enrollment`. Обеспечить корректную работу связей «многие-ко-многим» между `User` и `Course`, добавить репозиторий с кастомными выборками, сервисный слой с валидацией ролей и защитой от дублирования, а также интеграционные тесты.

## Связанные исходные файлы и применимые ADR
- `src/main/java/com/studying/platform/entity/User.java` — требуется добавить связь `@OneToMany` на `Enrollment`.
- `src/main/java/com/studying/platform/entity/Enrollment.java` — уже реализован (поля, связи, `UniqueConstraint`).
- `src/main/java/com/studying/platform/entity/EnrollmentStatus.java` — уже реализован (`ACTIVE`, `DROPPED`).
- `src/main/java/com/studying/platform/entity/Course.java` — содержит коллекцию `enrollments`.
- `src/main/java/com/studying/platform/repository/CourseRepository.java` — образец для стиля репозиториев.
- `src/main/java/com/studying/platform/service/CourseService.java` — образец стиля сервисов (конструкторное внедрение, `@Transactional`, `EntityManager`, `ResourceNotFoundException`).
- `src/main/java/com/studying/platform/exception/ResourceNotFoundException.java` — уже существует.
- `src/test/java/com/studying/platform/CategoryAndCourseTests.java` — образец структуры и паттернов тестов (`@DataJpaTest`, `EntityManager`, JUnit 5).
- `docs/adr/README.md` — ADR не требуются (архитектура `Enrollment` зафиксирована в `task.md` и `plan.md`).

## Порядок действий

1. **Обновить `User.java`**:
   - Добавить поле `@OneToMany(mappedBy = "user", fetch = FetchType.LAZY) private Set<Enrollment> enrollments = new HashSet<>();`
   - *Примечание:* В исходном задании указано `mappedBy = "enrollment"`, однако поле в `Enrollment.java` называется `user`. Исправляем на `mappedBy = "user"` для корректной работы JPA.
   - Добавить геттер/сеттер с JavaDoc.
   - Добавить импорты: `jakarta.persistence.OneToMany`, `java.util.HashSet`, `java.util.Set`, `jakarta.persistence.FetchType`.

2. **Создать `EnrollmentRepository.java`** (`com.studying.platform.repository`):
   - Наследовать `JpaRepository<Enrollment, Long>`.
   - Добавить методы:
     - `Optional<Enrollment> findByUserIdAndCourseIdAndStatus(Long userId, Long courseId, EnrollmentStatus status);`
     - `List<Enrollment> findByUserIdAndStatus(Long userId, EnrollmentStatus status);`
     - `List<Enrollment> findByCourseIdAndStatus(Long courseId, EnrollmentStatus status);`
     - `long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);`
   - Добавить содержательный JavaDoc на русском для интерфейса и каждого метода.

3. **Создать `EnrollmentService.java`** (`com.studying.platform.service`):
   - Конструкторное внедрение: `EnrollmentRepository`, `UserRepository`, `CourseRepository`, `EntityManager`.
   - Аннотации: `@Service`, `@Transactional`.
   - Метод `enrollStudent(Long studentId, Long courseId)`:
     - Проверить существование пользователя и курса (`findById` → `orElseThrow(ResourceNotFoundException)`).
     - Проверить роль пользователя (`UserRole.STUDENT`). Если нет — выбросить `ResourceNotFoundException`.
     - Проверить отсутствие активной записи через `findByUserIdAndCourseIdAndStatus(studentId, courseId, EnrollmentStatus.ACTIVE)`. Если найдена — выбросить `IllegalStateException` с сообщением о дублировании.
     - Создать `Enrollment` с `LocalDateTime.now()` и `ACTIVE`, сохранить через `entityManager.persist(enrollment)`, выполнить `flush()`.
   - Метод `dropEnrollment(Long studentId, Long courseId)`:
     - Найти запись по `findByUserIdAndCourseIdAndStatus(studentId, courseId, EnrollmentStatus.ACTIVE)`. Если нет — выбросить `ResourceNotFoundException`.
     - Установить статус `DROPPED`, сохранить через `flush()`.
   - Метод `getStudentCourses(Long studentId)`:
     - Вернуть список курсов из активных записей студента (через репозиторий или JPQL, извлечь `course` из найденных `Enrollment`).
   - Метод `getCourseStudents(Long courseId)`:
     - Вернуть список студентов из активных записей курса.
   - Метод `getEnrollmentStatus(Long studentId, Long courseId)`:
     - Вернуть статус существующей записи или `null`/исключение при отсутствии.
   - Добавить JavaDoc на русском для класса и каждого публичного метода.

4. **Создать `EnrollmentTests.java`** (`src/test/java/com/studying/platform/`):
   - Использовать `@DataJpaTest` + `@Import(EnrollmentService.class)` + `@Transactional` (для автоотката после каждого теста).
   - Внедрить `EntityManager`, `EnrollmentRepository`, `EnrollmentService`.
   - Реализовать позитивные тесты:
     - `enrollStudent_shouldCreateActiveRecord()` — запись студента на курс.
     - `dropEnrollment_shouldMarkAsDropped()` — отписка и смена статуса.
     - `getStudentCourses_shouldReturnActiveCourses()` — выборка курсов студента.
     - `getCourseStudents_shouldReturnActiveStudents()` — выборка студентов курса.
   - Реализовать негативные тесты:
     - `enrollStudent_withTeacher_shouldThrow()` — попытка записи пользователя с ролью `TEACHER`.
     - `enrollStudent_duplicate_shouldThrow()` — попытка повторной записи на тот же курс.
     - `dropEnrollment_nonExistent_shouldThrow()` — отписка при отсутствии записи.
     - `enrollStudent_nonExistentObjects_shouldThrow()` — передача несуществующих ID студента или курса.
   - Соблюсти стиль `CategoryAndCourseTests.java`: явное сохранение через `entityManager.persist()`, `flush()`, проверки через `assertThrows`, `assertEquals`, `assertNotNull`.

## Критерии приёмки и тест-кейсы

| № | Сценарий | Ожидаемый результат | Статус проверки |
|---|----------|---------------------|-----------------|
| 1 | Запись студента на курс | Создаётся `Enrollment` со статусом `ACTIVE`, датой `NOW`, связь сохраняется в БД | `PASS`/`FAIL` |
| 2 | Запись преподавателя на курс | Выбрасывается `ResourceNotFoundException` с указанием на неверную роль | `PASS`/`FAIL` |
| 3 | Повторная запись того же студента на тот же курс | Выбрасывается `IllegalStateException` (защита на уровне сервиса до обращения к БД) | `PASS`/`FAIL` |
| 4 | Отписка студента (`DROPPED`) | Статус меняется на `DROPPED`, запись не удаляется из БД | `PASS`/`FAIL` |
| 5 | Выборка активных курсов студента | Возвращается корректный `List<Course>` только со статусом `ACTIVE` | `PASS`/`FAIL` |
| 6 | Выборка активных студентов курса | Возвращается корректный `List<User>` только со статусом `ACTIVE` | `PASS`/`FAIL` |
| 7 | Отписка при отсутствии записи | Выбрасывается `ResourceNotFoundException` | `PASS`/`FAIL` |
| 8 | Запись с несуществующим студентом или курсом | Выбрасывается `ResourceNotFoundException` | `PASS`/`FAIL` |
| 9 | Уникальность на уровне БД | `UniqueConstraint` в `@Table` предотвращает дубликаты при параллельных запросах или обходе сервиса | `PASS`/`FAIL` |

## Команды проверки
Проект использует Maven и Java 23. Все тесты запускаются через стандартный плагин:
```bash
mvn verify
```
Для ускоренной проверки только целевых тестов:
```bash
mvn test -Dtest=EnrollmentTests
```
Успешное завершение означает: компиляция без ошибок, все тесты `PASS`, отсутствие `LazyInitializationException` благодаря ленивым загрузкам внутри транзакций или явным запросам.

## Ограничения, риски и блокеры
- **Блокер отсутствует**: архитектура `Enrollment` согласована с `task.md` и `plan.md`, новых архитектурных решений не требуется.
- **Риск дублирования на уровне БД**: `UniqueConstraint` защищает от конкурентных записей, но сервисная проверка `findByUserIdAndCourseIdAndStatus` закрывает 99% сценариев. При высокой нагрузке рекомендуется добавить индекс на `(user_id, course_id, status)` (Hibernate создаст его автоматически по `@Table`).
- **Ленивая загрузка**: коллекции `enrollments` в `User` и `Course` загружаются лениво. Все методы сервиса помечены `@Transactional`, что гарантирует безопасный доступ к связанным объектам.
- **Стиль кода**: строго соблюдается конструкторное внедрение зависимостей, `@Transactional` на уровне класса, JavaDoc на русском, идентификаторы на английском. Отклонения от `CourseService` не допускаются.
- `User.java` требует корректного `mappedBy = "user"` (в задании указано `"enrollment"`, что не соответствует реальному имени поля в `Enrollment.java`). План учитывает это исправление.

STATUS: READY
Файл: `stages/stage-08.md`
Сводка: Подготовлен детальный план реализации записи на курс. Охвачены обновление сущности `User`, создание `EnrollmentRepository` и `EnrollmentService`, интеграционные тесты с позитивными/негативными сценариями. Команды проверки подтверждены через `pom.xml`. ADR не требуются. Готов к реализации.
