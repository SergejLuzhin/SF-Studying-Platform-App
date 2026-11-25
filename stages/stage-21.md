# Этап 21: Демонстрационные данные

## Цель и границы этапа

Реализовать повторяемый механизм заполнения базы данных демонстрационными данными (сеeding), позволяющий преподавателю, студентам и проверяющему быстро развернуть полноценный учебный сценарий: курс со структурой, задания и тест. 

Механизм запускается **только** при активации Spring-профиля `demo` и не затрагивает пользовательские данные. Повторный запуск не должен создавать дубликаты сущностей.

**Не входит в этап:** создание REST-эндпоинта для сидера, очистка всей базы, Docker-конфигурация (это этап 22), обновление README (этап 23).

## Связанные файлы и применимые ADR

- **ADR:** `docs/adr/README.md` — пустой индекс, значимых ADR нет. Архитектурных ограничений на инициализацию данных нет.
- **Сущности:** `User`, `Profile`, `Category`, `Course`, `Module`, `Lesson`, `Assignment`, `Enrollment`, `Quiz`, `Question`, `AnswerOption`, `Tag`
- **Enums:** `UserRole` (STUDENT, TEACHER, ADMIN), `EnrollmentStatus` (ACTIVE, DROPPED), `QuizQuestionType` (SINGLE_CHOICE, MULTIPLE_CHOICE)
- **Репозитории:** `UserRepository`, `CategoryRepository`, `CourseRepository`, `ModuleRepository`, `LessonRepository`, `AssignmentRepository`, `EnrollmentRepository`, `QuizRepository`, `QuestionRepository`, `AnswerOptionRepository`, `TagRepository`
- **Конфигурация:** `src/main/resources/application.yml`, `src/main/resources/application-dev.yml`
- **Тесты (цель):** `src/test/java/com/studying/platform/service/DemoDataSeederTest.java` (название ориентировочное)

## Детальный план действий

### Шаг 1. Изучение структуры и создание пакета сидера

- Убедиться, что репозитории `UserRepository`, `CategoryRepository`, `CourseRepository`, `ModuleRepository`, `LessonRepository`, `AssignmentRepository`, `EnrollmentRepository`, `QuizRepository`, `QuestionRepository`, `AnswerOptionRepository`, `TagRepository` имеют методы `existsByEmail`, `findByName`, `findFirstBy...` и т.п. Если таких методов нет — добавить минимальные методы поиска.
- Создать пакет `com.studying.platform.seeder` (или `com.studying.platform.data`).

### Шаг 2. Создание `DemoDataSeeder`

Создать класс `DemoDataSeeder`, реализующий `ApplicationRunner` (или `CommandLineRunner`), с аннотацией `@Component` и `@Profile("demo")`.

```java
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {
    // внедряемые репозитории
}
```

Метод `run(ApplicationArguments args)` должен:
1. Проверять наличие преподавателя-демо по уникальному email (например, `demo-teacher@platform.test`). Если такой пользователь существует — завершать работу (без ошибок, без дублей).
2. Если преподавателя нет — создавать демонстрационный набор данных.

### Шаг 3. Логика создания данных

Внутри `DemoDataSeeder` реализовать метод `seedDemoData()`, который создаёт объекты в правильном порядке зависимостей (с использованием `@Transactional`):

1. **Категория:** Создать категорию `Programming` (если не существует). Использовать `CategoryRepository.findByName("Programming")` или `CategoryRepository.findFirstByName(...)`.
2. **Теги:** Создать теги `Java`, `Hibernate`, `SpringBoot` (если не существуют). Использовать `TagRepository.findByName(...)`.
3. **Преподаватель:** Создать `User` с ролью `TEACHER`, email `demo-teacher@platform.test`, имя `Demo Instructor`. Сохранить через `UserRepository.save()`.
4. **Студенты:** Создать 2 студента:
   - `student1@platform.test`, имя `Demo Student One`
   - `student2@platform.test`, имя `Demo Student Two`
   - Роли `STUDENT`. Сохранить через `UserRepository.save()`.
5. **Курс:** Создать `Course` с названием `Intro to Java`, привязать к преподавателю и категории. Сохранить через `CourseRepository.save()`.
6. **Модули:** Создать 2 модуля в курсе:
   - Модуль 1: `Java Basics`, `orderIndex = 1`
   - Модуль 2: `Hibernate & JPA`, `orderIndex = 2`
   - Привязать к курсу, сохранить через `ModuleRepository.save()`.
7. **Уроки:** Для каждого модуля создать по 2 урока:
   - Модуль 1: `Variables and Types`, `Hello World Program`
   - Модуль 2: `Entity Mapping`, `Relationships`
   - Привязать к модулям, сохранить через `LessonRepository.save()`.
8. **Задания:** Для каждого урока создать по 1 заданию:
   - `Variables Exercise`, `Hello World Task`, `Entity Mapping Task`, `Relationships Task`
   - Привязать к урокам, сохранить через `AssignmentRepository.save()`.
9. **Записи:** Создать `Enrollment` для каждого студента на курс со статусом `ACTIVE` и текущей датой `LocalDateTime.now()`. Сохранить через `EnrollmentRepository.save()`.
10. **Тест:** Для Модуля 2 (`Hibernate & JPA`) создать `Quiz`:
    - Название `Module 2 Quiz`, привязать к модулю.
    - Добавить 2 вопроса:
      - Вопрос 1 (SINGLE_CHOICE): «Какая аннотация отмечает сущность?», варианты: `@Entity` (правильный), `@Table`, `@Column`.
      - Вопрос 2 (MULTIPLE_CHOICE): «Какие из этих отношений поддерживаются JPA?», варианты: `@OneToMany` (правильный), `@ManyToMany` (правильный), `@HasMany` (неправильный).
    - Сохранить через `QuizRepository.save()` (каскадный PERSIST сохранит вопросы и варианты).
11. **Связка тегов с курсом:** Добавить теги `Java`, `Hibernate`, `SpringBoot` в курс через `course.getTags().add(...)`.

### Шаг 4. Обновление конфигурации

Добавить профиль `demo` в `application-dev.yml` (или создать отдельный `application-demo.yml`). Рекомендуемый подход — добавить в `application-dev.yml`:

```yaml
  config:
    activate:
      on-profile: demo
```

Содержимое `demo` может быть идентично `dev` (те же параметры БД), но это позволит явно отличить профиль. Можно также добавить `logging.level.com.studying.platform.seeder=DEBUG` для наглядности.

### Шаг 5. Автоматизированные тесты

Создать интеграционный тест `DemoDataSeederTest`.

**Вариант A (простой):** Unit-тест, который проверяет логику `DemoDataSeeder` без запуска Spring-контекста (мocking репозиториев). Проверяет, что:
- Если преподаватель уже существует, методы `save` на репозиториях не вызываются.
- Если преподаватель не существует, создаются все ожидаемые сущности.

**Вариант B (рекомендуемый, интеграционный):** Тест с `@SpringBootTest` и `@TestPropertySource(properties = "spring.profiles.active=test,demo")`. Использует тестовую базу через Testcontainers. Проверяет:
- Запуск сидера создаёт преподавателя, студентов, курс, модули, уроки, задания, записи и тест.
- Повторный запуск не создаёт дублей (количество преподавателей с email `demo-teacher@platform.test` остаётся 1).
- Все связи (модули привязаны к курсу, уроки к модулям, задания к урокам, записи к студентам и курсу, вопросы к тесту) корректны.

Реализовать Вариант B с использованием `@DirtiesContext` (чтобы контекст пересоздавался между тестами, если они запускаются отдельно).

### Шаг 6. Проверка схемы

Убедиться, что после сидера таблица `users` содержит ровно 3 записи (1 teacher + 2 students) с правильными ролями; таблица `courses` — 1 запись; `modules` — 2; `lessons` — 4; `assignments` — 4; `enrollments` — 2; `quiz` — 1; `question` — 2; `answeroption` — 6.

## Критерии приёмки

1. **[PASS]** Класс `DemoDataSeeder` существует, аннотирован `@Component`, `@Profile("demo")` и реализует `ApplicationRunner`.
2. **[PASS]** При активации профиля `demo` и запуске приложения создается полный набор демонстрационных данных (1 преподаватель, 2 студента, 1 курс, 2 модуля, 4 урока, 4 задания, 2 записи, 1 тест с 2 вопросами и 6 вариантами).
3. **[PASS]** Повторный запуск (перезапуск приложения с активным профилем `demo`) не создаёт дубликатов преподавателя, студентов или курса.
4. **[PASS]** Профиль `demo` не активируется по умолчанию (при запуске без `--spring.profiles.active=demo` сидер не выполняется).
5. **[PASS]** Существующие пользовательские данные не затрагиваются (сидер проверяет наличие преподавателя по email и выходит, если он уже есть).
6. **[PASS]** Все связи между сущностями корректны (модули привязаны к курсу, уроки к модулям, задания к урокам, записи — к студенту и курсу).
7. **[PASS]** Интеграционный тест `DemoDataSeederTest` проходит успешно, подтверждая создание данных и отсутствие дублей при повторном запуске.
8. **[PASS]** Все 456+ существующих тестов по-прежнему проходят.
9. **[PASS]** JavaDoc на русском языке для всех публичных классов, методов и полей. Идентификаторы на английском.

## Команды проверки

```bash
# Сборка проекта
mvn clean compile

# Запуск всех тестов (Testcontainers + unit)
mvn test

# Запуск приложения с профилем demo (локальная PostgreSQL)
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo

# Проверка данных в PostgreSQL
psql -U postgres -d studying_platform_v2 -c "SELECT role, login FROM users;"
psql -U postgres -d studying_platform_v2 -c "SELECT title FROM courses;"
psql -U postgres -d studying_platform_v2 -c "SELECT count(*) FROM modules;"
psql -U postgres -d studying_platform_v2 -c "SELECT count(*) FROM lessons;"
psql -U postgres -d studying_platform_v2 -c "SELECT count(*) FROM enrollments;"
psql -U postgres -d studying_platform_v2 -c "SELECT count(*) FROM quiz;"
psql -U postgres -d studying_platform_v2 -c "SELECT count(*) FROM question;"

# Повторный запуск и проверка отсутствия дублей
psql -U postgres -d studying_platform_v2 -c "SELECT count(*) FROM users WHERE login='demo-teacher@platform.test';"
# Ожидаемый результат: 1
```

## Ограничения, риски и блокеры

- **Риск:** При использовании `ddl-auto: update` в профиле `dev` и `demo` (если `demo` наследуется от `dev`) при каждом перезапуске Hibernate может пересоздавать схему, стирая сидированные данные. Решение: профиль `demo` не должен перезапускать `ddl-auto: update` полностью. Если это критично — рассмотреть `ddl-auto: validate` или `none` для `demo` профиля. Но поскольку профиль `demo` активируется поверх `dev`, а `dev` использует `update`, данные будут сохраняться (Hibernate не удаляет их при `update`, только добавляет/изменяет колонки).
- **Риск:** Сидер выполняется во время старта приложения, что может увеличить время запуска. Это приемлемо для демонстрационного профиля.
- **Риск:** Если репозитории не имеют методов `findByName` / `existsByEmail` — нужно будет их добавить. Это несущественное расширение интерфейсов.
- **Ограничение:** Сидер не очищает существующие данные и не удаляет их. Если пользователь хочет «чистую» базу — ему нужно очистить её вручную.
- **ADR:** Существенных архитектурных решений не требуется. Создание `DemoDataSeeder` — реализационная деталь, не влияющая на архитектуру проекта.
