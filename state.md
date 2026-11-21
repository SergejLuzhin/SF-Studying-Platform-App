# Текущее состояние проекта

## Текущий этап
Этап 06 — не начат.

## Выполненные этапы
- **Этап 01 (Проектирование модели):** completed. Создан `docs/data-model.md` с описанием 15 JPA-сущностей, связей, каскадов, стратегий загрузки и защиты учебной истории.
- **Этап 02 (Основа Spring Boot):** completed. Настроен Maven (Spring Boot 3.5.3, Java 23), созданы точка входа, профили `dev`/`test`, настроена конфигурация БД с переменными окружения.
- **Этап 03 (Инфраструктура интеграционных тестов):** completed. Расширен `StudyingPlatformApplicationTests.java`: 6 тестовых методов проверяют загрузку Spring-контекста с профилем `test`, наличие DataSource и EntityManagerFactory, живое соединение с PostgreSQL через Testcontainers (`SELECT 1`), активный профиль, изоляцию тестовой базы от рабочей и значение `ddl-auto: create-drop`. Docker поднимает PostgreSQL 16.16 автоматически.
- **Этап 04 (Пользователи и профили):** completed. Созданы сущности `User`, `Profile`, `UserRole`, репозитории `UserRepository` и `ProfileRepository`, связь 1:1 (владелец — Profile, LAZY). 11 интеграционных тестов (позитивные + негативные сценарии).
- **Этап 05 (Категории и курсы):** completed. Созданы сущности `Category`, `Course` (с обратными связями `modules`, `enrollments`, `reviews`, `tags`), `Enrollment`, `CourseReview`, `Tag`, `Module`, `EnrollmentStatus`, репозитории `CategoryRepository` и `CourseRepository`. 15 интеграционных тестов (позитивные + негативные сценарии).

## Блокирующие проблемы
Нет.

## Затронутые файлы (Этап 05)
- `src/main/java/com/studying/platform/entity/Category.java` — создание (сущность Category с полями id, name UNIQUE, Set<Course> courses LAZY).
- `src/main/java/com/studying/platform/entity/Course.java` — создание (сущность Course с полями id, title, description, duration, startDate, teacher @ManyToOne LAZY, category @ManyToOne EAGER, обратные связи modules, enrollments, reviews, tags).
- `src/main/java/com/studying/platform/entity/Enrollment.java` — создание (сущность Enrollment с полями id, user, course, enrollDate, status, unique constraint user_id+course_id).
- `src/main/java/com/studying/platform/entity/EnrollmentStatus.java` — создание (enum ACTIVE/DROPPED).
- `src/main/java/com/studying/platform/entity/CourseReview.java` — создание (сущность CourseReview с полями id, rating, comment, createdAt, course, student, unique constraint student_id+course_id).
- `src/main/java/com/studying/platform/entity/Module.java` — создание (сущность Module с полями id, title, description, orderIndex, course).
- `src/main/java/com/studying/platform/entity/Tag.java` — создание (сущность Tag с полями id, name UNIQUE).
- `src/main/java/com/studying/platform/repository/CategoryRepository.java` — создание (JpaRepository + findByName).
- `src/main/java/com/studying/platform/repository/CourseRepository.java` — создание (JpaRepository + findByTitle).
- `src/test/java/com/studying/platform/CategoryAndCourseTests.java` — создание (15 интеграционных тестов: сохранение, чтение, обновление, удаление, поиск, негативные сценарии null-полей, дубликат категории, несуществующие FK).
- `stages/stage-05.md` — создание детального плана этапа.
- `plan.md` — статус Этапа 05 обновлён на `completed`.

## Ключевые решения
- Сущность `Course` содержит все 4 обратные связи (modules, enrollments, reviews, tags) для полноты модели. Сущности `Module`, `Enrollment`, `CourseReview`, `Tag` созданы как минимальные заглушки с основными полями и связями — они будут дополнены на последующих этапах.
- `Tag` использует `@Column(unique = true)` для уникальности имён тегов.
- `Enrollment` и `CourseReview` имеют `@UniqueConstraint` на паре FK-полей.
- `EnrollmentStatus` — enum ACTIVE/DROPPED.
- Все сущности используют `@GeneratedValue(strategy = GenerationType.IDENTITY)`, `@Table(name = "...")`, `@ForeignKey`.
- `Category.courses` — `@OneToMany(mappedBy = "category", fetch = LAZY)`.

## Проверки (Этап 05)
- `mvn clean compile -q`: PASS
- `mvn test -q`: PASS (Tests run: 32, Failures: 0, Errors: 0, Skipped: 0)
  - `CategoryAndCourseTests`: 15 тестов (PASS)
  - `UserAndProfileTests`: 11 тестов (PASS)
  - `StudyingPlatformApplicationTests`: 6 тестов (PASS)
- `git diff --check`: PASS (нет проблем форматирования)
- Проверка JavaDoc на русском: PASS
- Проверка английских идентификаторов: PASS
- Проверка схемы БД (таблицы categories, courses, внешние ключи, уникальность): PASS
- Критерии приёмки (plan.md): PASS

## Следующее действие
Начать выполнение Этапа 06 (Модули и уроки) из plan.md.
