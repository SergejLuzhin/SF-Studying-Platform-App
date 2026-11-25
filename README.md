# Учебная платформа

Многопользовательское веб-приложение на базе **Spring Boot 3.5.3** для управления учебными курсами, модулями, уроками, заданиями, решениями и тестами. Данные хранятся в **PostgreSQL 16** и обслуживаются через **Hibernate/JPA** с применением Spring Data JPA. Приложение предоставляет **JSON REST API** для управления основными сущностями и выполнения бизнес-операций: записи студентов на курсы, отправки и оценивания решений, прохождения тестов с подсчётом баллов и оставления отзывов. Набор операций зависит от сущности и указан в [разделе 6](#6-rest-api).

---

## Оглавление

- [1. Назначение и возможности](#1-назначение-и-возможности)
- [2. Технологический стек](#2-технологический-стек)
- [3. Архитектура приложения](#3-архитектура-приложения)
  - [3.1. Слои](#31-слои)
  - [3.2. Пакетная структура](#32-пакетная-структура)
  - [3.3. Жизненный цикл запроса](#33-жизненный-цикл-запроса)
- [4. Модель данных](#4-модель-данных)
  - [4.1. Сущности JPA](#41-сущности-jpa)
  - [4.2. Типы связей](#42-типы-связей)
  - [4.3. ER-диаграмма](#43-er-диаграмма)
  - [4.4. Столбцы и ограничения PostgreSQL](#44-столбцы-и-ограничения-postgresql)
- [5. Ленивая загрузка и транзакции](#5-ленивая-загрузка-и-транзакции)
- [6. REST API](#6-rest-api)
  - [6.1. Пользователи](#61-пользователи)
  - [6.2. Категории](#62-категории)
  - [6.3. Курсы и контент](#63-курсы-и-контент)
  - [6.4. Записи на курс](#64-записи-на-курс)
  - [6.5. Задания и решения](#65-задания-и-решения)
  - [6.6. Тесты и викторины](#66-тесты-и-викторины)
  - [6.7. Отзывы и теги](#67-отзывы-и-теги)
  - [6.8. Форматы ответов и ошибок](#68-форматы-ответов-и-ошибок)
- [7. Локальный запуск](#7-локальный-запуск)
  - [7.1. Подготовка базы данных](#71-подготовка-базы-данных)
  - [7.2. Запуск приложения](#72-запуск-приложения)
  - [7.3. Профили Spring](#73-профили-spring)
- [8. Контейнерный запуск (Docker)](#8-контейнерный-запуск-docker)
- [9. Демонстрационный сценарий](#9-демонстрационный-сценарий)
- [10. Тестирование](#10-тестирование)
  - [10.1. Unit-тесты](#101-unit-тесты)
  - [10.2. Интеграционные тесты](#102-интеграционные-тесты)
  - [10.3. Сквозные HTTP-тесты](#103-сквозные-http-тесты)
  - [10.4. Проверка ленивой загрузки](#104-проверка-ленивой-загрузки)
  - [10.5. Проверки схемы PostgreSQL](#105-проверки-схемы-postgresql)
  - [10.6. Проверки Docker-инфраструктуры](#106-проверки-docker-инфраструктуры)
- [11. Правила и ограничения системы](#11-правила-и-ограничения-системы)
  - [11.1. Запись и отписка](#111-запись-и-отписка)
  - [11.2. Удаление и целостность](#112-удаление-и-целостность)
  - [11.3. Решения и оценивание](#113-решения-и-оценивание)
  - [11.4. Тесты и викторины](#114-тесты-и-викторины)
  - [11.5. Отзывы и теги](#115-отзывы-и-теги)
  - [11.6. Роли и доступ](#116-роли-и-доступ)
  - [11.7. Каскады и ленивая загрузка](#117-каскады-и-ленивая-загрузка)
- [12. Переменные окружения](#12-переменные-окружения)
- [13. Подробная инструкция по ручной проверке](#13-подробная-инструкция-по-ручной-проверке)

---

## 1. Назначение и возможности

Приложение моделирует предметную область образовательной платформы, в которой:

- **Преподаватели** создают курсы с категориями, наполняют их модулями и уроками, формируют задания для каждого урока, а также оценивают присланные студентами решения.
- **Студенты** записываются на курсы, отправляют решения заданий, проходят тесты (викторины) с вопросами нескольких типов (единственный или множественный выбор) и оставляют отзывы о курсах.
- **Администраторы** — резервная роль для будущих расширений; операции, требующие роль преподавателя или студента, для неё недоступны.

Ключевые бизнес-сценарии:

1. Создать курс (с преподавателем и категорией) → добавить модули → в каждом модуле уроки → в каждом уроке задания.
2. Записать студента на курс (с проверкой уникальности) → получить списки студентов курса / курсов студента.
3. Студент отправляет решение → преподаватель ставит оценку (0–100) и комментарий.
4. Создать тест для модуля → добавить вопросы с вариантами ответов → студент проходит тест → сохраняется итоговый балл.
5. Студент оставляет отзыв (рейтинг 1–5 + текст).
6. Пометить курс тегами и управлять этими связями.

---

## 2. Технологический стек

| Компонент | Технология | Версия |
|---|---|---|
| Язык | Java | 23 |
| Фреймворк | Spring Boot | 3.5.3 |
| ORM | Hibernate / Spring Data JPA | (входит в Spring Boot 3.5.3) |
| СУБД | PostgreSQL | 16 |
| Сборка | Maven | 3.x |
| Валидация | Jakarta Bean Validation | (spring-boot-starter-validation) |
| Тестирование | JUnit 5 + Spring Boot Test + Testcontainers | |
| Сериализация | Jackson (JSON) | (входит в Spring Boot 3.5.3) |
| Логирование | SLF4J + Logback | (входит в Spring Boot 3.5.3) |

---

## 3. Архитектура приложения

### 3.1. Слои

Проект реализует классическую многослойную архитектуру:

| Слой | Пакет | Назначение |
|---|---|---|
| **Entity** | `com.studying.platform.entity` | JPA-сущности с аннотациями `@Entity`, `@Table`, связями (`@OneToMany`, `@ManyToOne`, `@OneToOne`, `@ManyToMany`). Определяют структуру базы данных. |
| **Repository** | `com.studying.platform.repository` | Spring Data JPA-интерфейсы (`JpaRepository`), наследующие стандартные CRUD-методы и обеспечивающие кастомные JPQL-запросы при необходимости. |
| **Service** | `com.studying.platform.service` | Бизнес-логика: создание и обновление сущностей, валидация правил (роль студента для записи, уникальность решения, готовность теста и т.д.), выполнение в транзакциях (`@Transactional`). |
| **Controller** | `com.studying.platform.controller` | REST-контроллеры (`@RestController`), маршрутизация HTTP-запросов, делегирование вызовов сервисам, сериализация/десериализация JSON. |
| **DTO** | `com.studying.platform.dto` | Объекты передачи данных: запросы (`*RequestDto`) и ответы (`*ResponseDto`), а также специализированные DTO (`CourseStructureResponseDto`, `StartQuizResponseDto`). Отделяют внутреннюю модель от контракта API. |
| **Exception** | `com.studying.platform.exception` | `ResourceNotFoundException` — пользовательское исключение «не найдено»; `GlobalExceptionHandler` — `@ControllerAdvice` для централизованной обработки всех исключений с форматированием в `ErrorResponseDto`. |
| **Seeder** | `com.studying.platform.seeder` | `DemoDataSeeder` — заполнение базы демонстрационными данными при активации профиля `demo`. |

### 3.2. Пакетная структура

```
com.studying.platform
├── StudyingPlatformApplication.java          // Точка входа
├── entity/                                   // 15 JPA-сущностей + 3 enum
│   ├── User, Profile, Course, Category,
│   ├── Enrollment, Module, Lesson,
│   ├── Assignment, Submission,
│   ├── Quiz, Question, AnswerOption,
│   ├── QuizSubmission, CourseReview, Tag,
│   └── UserRole, EnrollmentStatus, QuizQuestionType
├── repository/                               // Spring Data JPA-репозитории
├── service/                                  // Сервисный слой
├── controller/                               // REST-контроллеры (14 шт.)
├── dto/                                      // DTO запросов и ответов
├── exception/                                // Обработка ошибок
└── seeder/                                   // Демо-данные
```

### 3.3. Жизненный цикл запроса

```
HTTP-запрос
    → @RestController (маршрутизация + парсинг JSON в DTO)
    → Service (бизнес-логика, валидация правил, @Transactional)
    → Repository (JPA → Hibernate → JDBC → PostgreSQL)
    → Entity (данные из БД)
    → Service (преобразование Entity → DTO, агрегация данных)
    → @RestController (сериализация DTO → JSON, HTTP-код ответа)
```

Транзакции для операций с составной бизнес-логикой управляются преимущественно сервисами; отдельные контроллеры также обращаются непосредственно к репозиториям.

---

## 4. Модель данных

### 4.1. Сущности JPA

Реализованы **15 JPA-сущностей** и **3 перечисления** (`enum`):

| Сущность | Таблица БД | Назначение |
|---|---|---|
| **User** | `users` | Участник системы (студент, преподаватель, администратор). Поле Java/API `email` хранится в уникальном столбце `login`. |
| **Profile** | `profiles` | Дополнительная информация пользователя (отображаемое имя, биография, аватар). Связь 1:1 с `User`; REST-эндпоинты для профиля отсутствуют. |
| **Category** | `categories` | Классификация курсов (например, «Программирование»). |
| **Course** | `courses` | Учебный курс с названием, описанием, продолжительностью и датой начала. Привязан к категории и преподавателю. |
| **Enrollment** | `enrollments` | Запись студента на курс (статус: `ACTIVE` / `DROPPED`). Уникальность по паре `(user_id, course_id)`. |
| **Module** | `modules` | Тематический раздел курса с порядковым номером (`orderIndex`). |
| **Lesson** | `lessons` | Отдельный урок модуля с содержанием и опциональной ссылкой на видео. |
| **Assignment** | `assignments` | Задание урока: описание, дедлайн, максимальный балл. Привязано к уроку. |
| **Submission** | `submissions` | Решение студента: текст, время отправки, оценка и комментарий преподавателя. Уникальность по паре `(assignment_id, student_id)`. |
| **Quiz** | `quiz` | Викторина модуля (один модуль — не более одного теста). |
| **Question** | `question` | Вопрос теста с текстом и типом (`SINGLE_CHOICE` / `MULTIPLE_CHOICE`). |
| **AnswerOption** | `answeroption` | Вариант ответа на вопрос (текст + флаг правильности). |
| **QuizSubmission** | `quizsubmission` | Результат прохождения теста: балл и дата. |
| **CourseReview** | `course_reviews` | Отзыв студента о курсе: рейтинг (1–5) и текст. Уникальность по паре `(student_id, course_id)`. |
| **Tag** | `tags` | Метка для курса. Связь M:N с `Course` через таблицу `course_tag`. |

В проверенных базах PostgreSQL первичные ключи 15 таблиц сущностей — `bigint GENERATED BY DEFAULT AS IDENTITY`; в JPA используется `@GeneratedValue(strategy = GenerationType.IDENTITY)`. Таблица связи `course_tag` не имеет собственного `id`: её составной первичный ключ — `(course_id, tag_id)`.

### 4.2. Типы связей

| Тип связи | Примеры |
|---|---|
| **1:1 (один-к-одному)** | `User` → `Profile` (`@OneToOne`, `mappedBy`); `Quiz` → `Module` |
| **1:N (один-ко-многим)** | `Category` → `Course`, `Course` → `Module`, `Module` → `Lesson`, `Lesson` → `Assignment`, `Assignment` → `Submission`, `Quiz` → `Question`, `Question` → `AnswerOption`, `User` → `Submission`, `User` → `QuizSubmission`, `User` → `CourseReview`, `User` → `Enrollment`, `Course` → `Enrollment`, `Course` → `CourseReview` |
| **M:1 (многие-к-одному)** | `Course` → `Category`, `Course` → `User` (преподаватель), `Module` → `Course`, `Lesson` → `Module`, `Assignment` → `Lesson`, `Submission` → `Assignment`, `Submission` → `User` (студент), `Question` → `Quiz`, `AnswerOption` → `Question`, `QuizSubmission` → `Quiz`, `QuizSubmission` → `User`, `CourseReview` → `Course`, `CourseReview` → `User`, `Enrollment` → `User`, `Enrollment` → `Course` |
| **M:N (многие-ко-многим)** | `Course` ↔ `Tag` через таблицу `course_tag` (`@ManyToMany`, `@JoinTable`) |

### 4.3. ER-диаграмма

Диаграмма показывает **имена столбцов PostgreSQL**, а не Java-полей или JSON-ключей. `string` обозначает текстовый тип PostgreSQL (`text` или `varchar`), `datetime` — `timestamp without time zone`; все идентификаторы и внешние ключи имеют тип `bigint`.

```mermaid
erDiagram
    USER {
        bigint id PK
        string login
        string role
    }

    PROFILE {
        bigint id PK
        bigint user_id FK
        string display_name
        string bio
        string avatar_url
    }

    COURSE {
        bigint id PK
        string title
        string description
        bigint category_id FK
        bigint teacher_id FK
        int duration
        date start_date
    }

    CATEGORY {
        bigint id PK
        string name
    }

    ENROLLMENT {
        bigint id PK
        bigint user_id FK
        bigint course_id FK
        datetime enroll_date
        string status
    }

    MODULE {
        bigint id PK
        bigint course_id FK
        string title
        int order_index
        string description
    }

    LESSON {
        bigint id PK
        bigint module_id FK
        string title
        string content
        string video_url
    }

    ASSIGNMENT {
        bigint id PK
        bigint lesson_id FK
        string title
        string description
        date due_date
        int max_score
    }

    SUBMISSION {
        bigint id PK
        bigint assignment_id FK
        bigint student_id FK
        datetime submitted_at
        string content
        int score
        string feedback
    }

    QUIZ {
        bigint id PK
        bigint module_id FK
        string title
        int time_limit
    }

    QUESTION {
        bigint id PK
        bigint quiz_id FK
        string text
        string type
    }

    ANSWEROPTION {
        bigint id PK
        bigint question_id FK
        string text
        boolean is_correct
    }

    QUIZSUBMISSION {
        bigint id PK
        bigint quiz_id FK
        bigint student_id FK
        int score
        datetime taken_at
    }

    COURSEREVIEW {
        bigint id PK
        bigint course_id FK
        bigint student_id FK
        int rating
        string comment
        datetime created_at
    }

    TAG {
        bigint id PK
        string name
    }

    COURSE_TAG {
        bigint course_id PK,FK
        bigint tag_id PK,FK
    }

    USER ||--o| PROFILE : has
    USER ||--o{ COURSE : teaches
    USER ||--o{ ENROLLMENT : enrolls
    COURSE ||--o{ ENROLLMENT : has
    CATEGORY ||--o{ COURSE : contains
    COURSE ||--o{ MODULE : has
    MODULE ||--o{ LESSON : has
    MODULE ||--o| QUIZ : may_have
    LESSON ||--o{ ASSIGNMENT : includes
    USER ||--o{ SUBMISSION : submits
    ASSIGNMENT ||--o{ SUBMISSION : receives
    QUIZ ||--o{ QUESTION : contains
    QUESTION ||--o{ ANSWEROPTION : has
    USER ||--o{ QUIZSUBMISSION : takes
    QUIZ ||--o{ QUIZSUBMISSION : results
    USER ||--o{ COURSEREVIEW : writes
    COURSE ||--o{ COURSEREVIEW : receives
    COURSE ||--o{ COURSE_TAG : links
    TAG ||--o{ COURSE_TAG : links
```

### 4.4. Столбцы и ограничения PostgreSQL

Схема ниже сверена с каталогами PostgreSQL в базах `studying_platform_v2` и `studying_demo`. Она описывает структуру, а не содержимое таблиц. При другом профиле или после изменения схемы фактическая база может отличаться.

Во всех таблицах сущностей `id` — `bigint`, `NOT NULL`, первичный ключ с автоматической генерацией. В таблице ниже `?` означает, что столбец допускает `NULL`; остальные перечисленные столбцы обязательны. Все столбцы `*_id` имеют тип `bigint`.

| Таблица | Столбцы помимо `id` |
|---|---|
| `users` | `login varchar(50)`, `role varchar(20)` |
| `profiles` | `user_id`, `display_name varchar(100)`, `bio text?`, `avatar_url varchar(500)?` |
| `categories` | `name varchar(100)` |
| `courses` | `title varchar(200)`, `description text?`, `duration integer`, `start_date date`, `teacher_id`, `category_id` |
| `enrollments` | `user_id`, `course_id`, `enroll_date timestamp`, `status varchar(20)` |
| `modules` | `course_id`, `title varchar(200)`, `description text?`, `order_index integer` |
| `lessons` | `module_id`, `title varchar(200)`, `content text?`, `video_url varchar(255)?` |
| `assignments` | `lesson_id`, `title varchar(200)`, `description text?`, `due_date date?`, `max_score integer?` |
| `submissions` | `assignment_id`, `student_id`, `submitted_at timestamp`, `content text`, `score integer?`, `feedback text?` |
| `quiz` | `module_id`, `title varchar(200)`, `time_limit integer?` |
| `question` | `quiz_id`, `text text`, `type varchar(20)` |
| `answeroption` | `question_id`, `text text`, `is_correct boolean` |
| `quizsubmission` | `quiz_id`, `student_id`, `score integer`, `taken_at timestamp` |
| `course_reviews` | `course_id`, `student_id`, `rating integer`, `comment text?`, `created_at timestamp` |
| `tags` | `name varchar(50)` |
| `course_tag` | `course_id`, `tag_id`; составной первичный ключ, отдельного `id` нет |

Здесь `timestamp` означает `timestamp without time zone`. Внешние ключи и их направления показаны на диаграмме выше. В проверенной схеме PostgreSQL они используют стандартное действие `NO ACTION` при удалении; JPA-каскады и явное удаление сервисом — отдельный механизм.

Ограничения уникальности: `users.login`, `categories.name`, `tags.name`, `profiles.user_id`, `quiz.module_id`, а также пары `enrollments(user_id, course_id)`, `submissions(assignment_id, student_id)` и `course_reviews(student_id, course_id)`. Для таблицы `course_tag` уникальность пары обеспечивается составным первичным ключом.

Проверки `CHECK` в этих базах ограничивают значения `users.role`, `enrollments.status` и `question.type` соответствующими перечислениями. Например, диапазон рейтинга отзыва и допустимость оценки решения проверяются приложением, но отдельных ограничений `CHECK` для них в проверенной схеме нет.

---

## 5. Ленивая загрузка и транзакции

Все коллекционные связи (`@OneToMany`, `@ManyToMany`) имеют `fetch = FetchType.LAZY` — они загружаются из базы данных только при первом явном обращении к объекту или коллекции. Это означает:

- При обычном `@Transactional`-методе в сервисе обращение к коллекции **работает** — сессия Hibernate открыта.
- При обращении к незагруженной коллекции **после закрытия сессии** возникает `LazyInitializationException`. Отсутствие транзакции само по себе не обязательно означает, что сессия уже закрыта.

**Как приложение обходит проблему:**

- Метод `GET /api/courses/{id}/structure` загружает курс, модули и уроки через `CourseService.getCourseStructure()`. Контроллер преобразует загруженную структуру в DTO. Задания получаются отдельным запросом по ID урока.
- В некоторых `@SpringBootTest` используется `spring.jpa.open-in-view=false`, чтобы проверить работу без открытой Hibernate-сессии на протяжении HTTP-запроса.
- В основном приложении `spring.jpa.open-in-view` явно не установлен; в Spring Boot по умолчанию Open EntityManager in View включён. Проверки с отключённым режимом выполняются в тестах.

---

## 6. REST API

Базовый адрес: `http://localhost:8080`
Формат тел запросов и ответов: JSON.
Заголовки: `Content-Type: application/json`.

### 6.1. Пользователи

| Метод | Путь | Описание | Код ответа |
|---|---|---|---|
| `POST` | `/api/users` | Создать пользователя | 201 |
| `GET` | `/api/users/{userId}` | Получить пользователя по ID | 200 / 404 |
| `PUT` | `/api/users/{userId}` | Обновить email и роль | 200 / 400 / 404 |
| `DELETE` | `/api/users/{userId}` | Удалить пользователя | 204 / 404 |

Тело создания: `{"email": "student@example.com", "role": "STUDENT"}`.
При обновлении обязательны оба поля — `email` (уникальный, не более 50 символов) и `role`.

### 6.2. Категории

| Метод | Путь | Описание | Код ответа |
|---|---|---|---|
| `POST` | `/api/categories` | Создать категорию | 201 |
| `GET` | `/api/categories` | Получить все категории | 200 |
| `GET` | `/api/categories/{categoryId}` | Получить по ID | 200 / 404 |
| `PUT` | `/api/categories/{categoryId}` | Обновить название | 200 / 400 / 404 |
| `DELETE` | `/api/categories/{categoryId}` | Удалить категорию | 204 / 404 |

### 6.3. Курсы и контент

| Метод | Путь | Описание | Код ответа |
|---|---|---|---|
| `POST` | `/api/courses` | Создать курс (с категорией и преподавателем) | 201 |
| `GET` | `/api/courses/{courseId}` | Получить курс по ID | 200 / 404 |
| `PUT` | `/api/courses/{courseId}` | Обновить курс | 200 / 400 / 404 |
| `DELETE` | `/api/courses/{courseId}` | Удалить курс | 204 / 400 (если есть история) |
| `GET` | `/api/courses/{courseId}/structure` | Получить структуру курса (модули и уроки) | 200 / 404 |
| `POST` | `/api/courses/{courseId}/modules` | Добавить модуль в курс | 201 |
| `GET` | `/api/modules/{moduleId}` | Получить модуль | 200 / 404 |
| `PUT` | `/api/modules/{moduleId}` | Обновить модуль | 200 / 400 / 404 |
| `DELETE` | `/api/modules/{moduleId}` | Удалить модуль | 204 / 404 |
| `POST` | `/api/lessons/modules/{moduleId}/lessons` | Добавить урок в модуль | 201 |
| `GET` | `/api/lessons/{lessonId}` | Получить урок | 200 / 404 |
| `PUT` | `/api/lessons/{lessonId}` | Обновить урок | 200 / 400 / 404 |
| `DELETE` | `/api/lessons/{lessonId}` | Удалить урок | 204 / 404 |
| `POST` | `/api/courses/{courseId}/tags` | Добавить связи тегов к курсу | 204 |
| `DELETE` | `/api/courses/{courseId}/tags` | Удалить связи тегов от курса | 204 |

Тело создания курса:
```json
{
  "title": "Введение в Java",
  "description": "Основы языка",
  "duration": 40,
  "startDate": "2026-09-01",
  "teacherId": 1,
  "categoryId": 1
}
```

### 6.4. Записи на курс

| Метод | Путь | Описание | Код ответа |
|---|---|---|---|
| `POST` | `/api/enrollments` | Записать студента на курс | 201 / 400 / 404 |
| `DELETE` | `/api/enrollments?studentId=...&courseId=...` | Отписаться от курса | 204 / 404 |
| `GET` | `/api/enrollments/courses/{courseId}/students` | Список активно записанных студентов курса | 200 / 404 |
| `GET` | `/api/enrollments/users/{userId}/courses` | Список курсов с активной записью студента | 200 / 404 |

Тело записи: `{"studentId": 2, "courseId": 1}`. Повторная запись одного студента на тот же курс запрещена, в том числе после отписки (400). Только пользователи с ролью `STUDENT` могут записаться; другая роль возвращает 404.

### 6.5. Задания и решения

| Метод | Путь | Описание | Код ответа |
|---|---|---|---|
| `POST` | `/api/assignments/lessons/{lessonId}/assignments` | Создать задание для урока | 201 |
| `GET` | `/api/assignments/{assignmentId}` | Получить задание | 200 / 404 |
| `PUT` | `/api/assignments/{assignmentId}` | Обновить задание | 200 / 400 / 404 |
| `DELETE` | `/api/assignments/{assignmentId}` | Удалить задание | 204 / 404 |
| `GET` | `/api/assignments/lessons/{lessonId}/assignments` | Задания урока | 200 |
| `POST` | `/api/submissions` | Отправить решение студентом | 201 / 400 / 404 |
| `PATCH` | `/api/submissions/{submissionId}?teacherId=...` | Оценить решение | 200 / 400 / 404 |
| `GET` | `/api/submissions/assignments/{assignmentId}/submissions` | Решения по заданию | 200 |
| `GET` | `/api/submissions/users/{userId}/submissions` | Решения студента | 200 |

Тело создания задания:
```json
{
  "title": "Практика",
  "description": "Выполнить...",
  "dueDate": "2026-12-01",
  "maxScore": 100
}
```

Тело отправки решения:
```json
{"assignmentId": 1, "studentId": 2, "content": "Текст решения"}
```

Одно решение на одно задание — повторная отправка запрещена (возвращает 400).

### 6.6. Тесты и викторины

| Метод | Путь | Описание | Код ответа |
|---|---|---|---|
| `POST` | `/api/quiz` | Создать тест для модуля | 201 |
| `GET` | `/api/quiz/{quizId}` | Получить тест | 200 / 404 |
| `PUT` | `/api/quiz/{quizId}` | Обновить тест | 200 / 400 / 404 |
| `DELETE` | `/api/quiz/{quizId}` | Удалить тест | 204 / 404 |
| `POST` | `/api/quiz/{quizId}/questions` | Добавить вопрос | 201 |
| `GET` | `/api/quiz/{quizId}/questions` | Вопросы теста | 200 |
| `GET` | `/api/quiz/{quizId}/start` | Начать тест (получить вопросы без пометок правильности) | 200 / 400 |
| `POST` | `/api/quiz/{quizId}/submit` | Сдать тест с ответами | 201 / 400 / 404 |
| `GET` | `/api/question/{questionId}` | Получить вопрос | 200 / 404 |
| `PUT` | `/api/question/{questionId}` | Обновить вопрос | 200 / 400 / 404 |
| `DELETE` | `/api/question/{questionId}` | Удалить вопрос | 204 / 404 |
| `GET` | `/api/question/{questionId}/options` | Варианты вопроса | 200 |
| `POST` | `/api/question/{questionId}/options` | Добавить вариант | 201 |
| `GET` | `/api/options/{optionId}` | Получить вариант | 200 / 404 |
| `PUT` | `/api/options/{optionId}` | Обновить вариант | 200 / 400 / 404 |
| `DELETE` | `/api/options/{optionId}` | Удалить вариант | 204 / 404 |
| `GET` | `/api/submissions/quiz/{submissionId}` | Результат теста по ID | 200 / 404 |
| `GET` | `/api/submissions/quiz/student/{studentId}` | Результаты студента | 200 |
| `GET` | `/api/submissions/quiz/quiz/{quizId}` | Результаты по тесту | 200 |

**Процесс прохождения теста:**

1. Получить вопросы через `GET /api/quiz/{quizId}/start` — возвращается список вопросов с вариантами, без пометок `isCorrect`.
2. Отправить ответы через `POST /api/quiz/{quizId}/submit`:
   ```json
   {
     "studentId": 2,
     "answers": { "456": [789], "457": [801, 802] }
   }
   ```
   Ключи — строковые ID вопросов, значения — массивы ID выбранных вариантов.
3. Результат сохраняется в `QuizSubmission` с полем `score` (0–100) и возвращается в ответе.

**Валидация тестов:** `/start` возвращает 400 для теста без вопросов. При сдаче дополнительно проверяются варианты: вопрос типа `SINGLE_CHOICE` должен иметь ровно один правильный вариант, `MULTIPLE_CHOICE` — хотя бы один. Посторонние ID вопросов игнорируются; лишние ID вариантов в ответе не позволяют получить балл за вопрос.

### 6.7. Отзывы и теги

| Метод | Путь | Описание | Код ответа |
|---|---|---|---|
| `POST` | `/api/course-reviews` | Оставить отзыв | 201 / 400 / 404 |
| `GET` | `/api/course-reviews/course/{courseId}` | Отзывы о курсе | 200 |
| `GET` | `/api/course-reviews/student/{studentId}` | Отзывы студента | 200 |
| `POST` | `/api/tags` | Создать тег | 201 |
| `GET` | `/api/tags` | Все теги | 200 |
| `GET` | `/api/tags/{tagId}` | Тег по ID | 200 / 404 |
| `PUT` | `/api/tags/{tagId}` | Обновить тег | 200 / 400 / 404 |
| `DELETE` | `/api/tags/{tagId}` | Удалить тег | 204 / 404 |

Тело отзыва: `{"studentId": 2, "courseId": 1, "rating": 5, "comment": "Отличный курс"}`. Только записанные студенты могут оставлять отзывы. Рейтинг ограничивается диапазоном 1–5. Один отзыв от одного студента на один курс (уникальность на уровне БД).

Тело тегов для курса: `POST /api/courses/{courseId}/tags` с массивом `[1, 2]` — ID тегов.

### 6.8. Форматы ответов и ошибок

**Успешный ответ** (пример):
```json
{
  "id": 1,
  "email": "student@example.com",
  "role": "STUDENT"
}
```

**Ответ об ошибке** (пример):
```json
{
  "timestamp": "2026-11-25T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Студент уже записан на этот курс",
  "path": "/api/enrollments"
}
```

| HTTP-код | Событие |
|---|---|
| `200` | Успешное чтение / обновление |
| `201` | Успешное создание |
| `204` | Успешное удаление (без тела) |
| `400` | Невалидные данные, нарушение бизнес-правил |
| `404` | Ресурс не найден |
| `500` | Внутренняя ошибка сервера (без раскрытия деталей) |

---

## 7. Локальный запуск

### 7.1. Подготовка базы данных

Профиль `dev` по умолчанию подключается к `jdbc:postgresql://localhost:5432/studying_platform_v2` с пользователем и паролем `postgres`. Схема обновляется автоматически (`ddl-auto: update`).

**Не запускайте пример на рабочей базе.** Рекомендуется использовать отдельную базу в Docker-контейнере:

```bash
docker run -d --name studying-demo-db -p 127.0.0.1:55432:5432 \
  -e POSTGRES_DB=studying_demo -e POSTGRES_USER=demo -e POSTGRES_PASSWORD=demo postgres:16
```

### 7.2. Запуск приложения

```bash
# С переменными окружения
export DB_URL=jdbc:postgresql://localhost:55432/studying_demo
export DB_USER=demo
export DB_PASSWORD=demo
export SPRING_PROFILES_ACTIVE=dev

mvn spring-boot:run
```

Сервер начнёт слушать порт **8080**. Дождитесь сообщения о готовности приложения в логах.

### 7.3. Профили Spring

| Профиль | Когда используется | Особенности |
|---|---|---|
| `dev` (по умолчанию) | Локальная разработка | `ddl-auto: update`, подключение к локальной PostgreSQL. |
| `test` | Тесты, активирующие этот профиль | Testcontainers JDBC URL (`jdbc:tc:postgresql:16://...`), `ddl-auto: create-drop`. |
| `docker` | Запуск через Docker Compose | Фиксированные параметры подключения к контейнеру PostgreSQL. |
| `demo` (дополнительный) | Включается вместе с `dev` или `docker` | Запускает `DemoDataSeeder` для заполнения базы демонстрационными данными. |

Профили комбинируются через запятую: `SPRING_PROFILES_ACTIVE=dev,demo`.

---

## 8. Контейнерный запуск (Docker)

```bash
# Собрать JAR (включая тесты)
mvn -B package

# Запустить PostgreSQL и приложение через Docker Compose
docker compose up --build
```

Docker Compose запускает два сервиса: `postgres` (порт 5432 внутри сети) и `app` (порт 8080 на хосте). Профиль `docker` задаёт параметры подключения к PostgreSQL. Именованный том в Compose не объявлен; образ PostgreSQL может использовать анонимный том для данных. Удаление контейнеров само по себе не обязательно удаляет этот том.

Для остановки: `Ctrl+C` или `docker compose stop`.

---

## 9. Демонстрационный сценарий

Для быстрого ознакомления с платформой используется профиль `demo`. Он создаёт полный набор данных: преподавателя, двух студентов, курс «Intro to Java» с двумя модулями, уроками, заданиями, записями и тестом с двумя вопросами. Пример ниже использует изолированную базу и переменные подключения из раздела 7, а также установленный `jq`.

```bash
# Остановить сервер, если запущен
# В терминале с переменными DB_URL, DB_USER и DB_PASSWORD из раздела 7
export SPRING_PROFILES_ACTIVE=dev,demo
mvn spring-boot:run
```

> **Важно:** при повторном запуске сидер проверяет наличие преподавателя `demo-teacher@platform.test`. Если преподаватель найден, заполнение пропускается — дубликаты не создаются.

**Пример полного сценария через curl:**

```bash
# Получить ID из изолированной базы
sql() { docker exec studying-demo-db psql -U demo -d studying_demo -Atc "$1"; }
COURSE_ID=$(sql "select id from courses where title = 'Intro to Java'")
STUDENT_ID=$(sql "select id from users where login = 'demo-student-one@platform.test'")
QUIZ_ID=$(sql "select id from quiz where title = 'Module 2 Quiz'")
LESSON_ID=$(sql "select id from lessons where title = 'Entity Mapping'")

# Просмотр структуры курса
curl -s "http://localhost:8080/api/courses/$COURSE_ID/structure" | jq .

# Просмотр студентов курса
curl -s "http://localhost:8080/api/enrollments/courses/$COURSE_ID/students" | jq .

# Просмотр заданий урока
curl -s "http://localhost:8080/api/assignments/lessons/$LESSON_ID/assignments" | jq .

# Начало теста (вопросы без пометок правильности)
curl -s "http://localhost:8080/api/quiz/$QUIZ_ID/start" | jq .

# Попытка без ответов (результат 0 баллов)
curl -s -X POST "http://localhost:8080/api/quiz/$QUIZ_ID/submit" \
  -H 'Content-Type: application/json' \
  -d "{\"studentId\":$STUDENT_ID,\"answers\":{}}" | jq .

# Результаты тестов студента
curl -s "http://localhost:8080/api/submissions/quiz/student/$STUDENT_ID" | jq .
```

---

## 10. Тестирование

Для интеграционных тестов и тестов инфраструктуры требуется работающий Docker. Число и результаты тестов зависят от текущего состояния проекта и окружения.

```bash
# Компиляция
mvn -B clean compile

# Запуск всех тестов (unit + интеграционные + HTTP)
mvn -B verify
```

### 10.1. Unit-тесты

Тестируют контроллеры в изоляции от Spring-контекста:
- `controller/EnrollmentControllerTest`
- `controller/AssignmentControllerTest`
- `controller/SubmissionControllerTest`
- `controller/QuizReviewTagControllerTest`
- `exception/GlobalExceptionHandlerTest`

### 10.2. Интеграционные тесты

Тестируют полный стек (Spring context + Testcontainers PostgreSQL):

| Тест | Проверяет |
|---|---|
| `UserAndProfileTests` | Создание, чтение, обновление, удаление пользователя и профиля; уникальность email |
| `CategoryAndCourseTests` | CRUD курсов и категорий; внешние ключи |
| `ModuleAndLessonTests` | Структура «курс → модули → уроки»; каскадное сохранение |
| `CourseManagementTests` | Сервисный CRUD курса с модулями и уроками |
| `EnrollmentTests` | Запись и отписка студента; уникальность |
| `AssignmentServiceTests` | CRUD заданий урока |
| `SubmissionTests` | Отправка и проверка решений; уникальность |
| `CourseReviewAndTagTests` | Отзывы и теги; связь M:N |
| `DemoDataSeederTest` | Демо-сидер; повторный запуск без дублей |
| `HttpFixtureTrackerTest` | Тесты вспомогательного механизма очистки фикстур |

### 10.3. Сквозные HTTP-тесты

| Тест | Описание |
|---|---|
| `RestAcceptanceTest` | **Полный учебный сценарий:** создание пользователя → категория → курс → модуль → урок → запись → задание → решение → оценивание → тест → вопрос → вариант → прохождение теста → результат (score=100) → тег → проверки удаления, невалидных ID, malformed JSON. Запуск с `spring.jpa.open-in-view=false`. |
| `MvpFlowTests` | Сквозной сценарий MVP: CRUD-операции через сервисный слой без HTTP. |
| `PartTwoAcceptanceTests` | Дополнительные HTTP-сценарии: бизнес-правила, валидация, целостность. |

### 10.4. Проверка ленивой загрузки

| Тест | Описание |
|---|---|
| `LazyLoadingAndCascadeTests` | Демонстрирует `LazyInitializationException` при доступе к коллекции вне транзакции; показывает решение через сервисный метод в `@Transactional`. Проверяет каскады сохранения и удаления. |

### 10.5. Проверки схемы PostgreSQL

| Тест | Описание |
|---|---|
| `PostgresSchemaTests` | Проверяет создание всех таблиц, первичных и внешних ключей, ограничений уникальности на уровне БД. |
| `PostgresTextStorageTests` | Проверяет корректное сохранение и чтение строковых полей (TEXT) через JPA и прямой SQL на PostgreSQL. |

### 10.6. Проверки Docker-инфраструктуры

| Тест | Описание |
|---|---|
| `DockerInfrastructureTest` | Проверки Docker-контейнеров, портов и образов. |
| `DevProfileStartupTest` | Запуск приложения в профиле `dev`; запуск с профилями `dev,demo`. |

---

## 11. Правила и ограничения системы

Платформа реализует набор бизнес-правил, обеспечивающих корректность учебных процессов и целостность данных. Ниже описаны ограничения на удаление, записи, решения, тесты и другие сущности.

### 11.1. Запись и отписка

- Записаться на курс может **только пользователь с ролью `STUDENT`**. Преподаватели и администраторы не могут записаться.
- Один студент может быть записан на один курс **не более одного раза**. Повторная попытка записи, в том числе после отписки, возвращает HTTP 400; реактивация записи не реализована.
- Студент может отписаться от курса (изменить статус записи на `DROPPED`). После отписки студент **больше не может** отправлять решения, проходить тесты или оставлять отзывы для этого курса, но его учебная история (отправленные решения, результаты тестов, отзывы) **сохраняется**.
- При отписке студента не удаляются никакие связанные данные — только меняется статус записи.

### 11.2. Удаление и целостность

| Ресурс | Правило удаления |
|---|---|
| **Курс** | Можно удалить **только если** у курса нет записей студентов, нет отправленных решений, нет результатов тестов и нет отзывов. Если хотя бы одно из этих условий нарушено — удаление запрещено (HTTP 400). |
| **Модуль** | Уроки удаляются каскадом, но задания и тест не входят в этот каскад. При зависимостях внешние ключи могут заблокировать удаление. |
| **Урок** | Задания не удаляются каскадом; их наличие может заблокировать удаление внешним ключом. |
| **Задание** | Решения не удаляются каскадом; их наличие может заблокировать удаление внешним ключом. |
| **Решение** | REST-эндпоинт удаления не предусмотрен. |
| **Тест (Quiz)** | Удаление запрещено при наличии результатов. Иначе сервис явно удаляет вопросы и варианты. |
| **Вопрос** | Удаление запрещено, если у теста есть результаты. Иначе сервис явно удаляет варианты. |
| **Вариант ответа** | Удаление запрещено, если у теста есть результаты. Иначе удаляется отдельно и может повлиять на готовность теста. |
| **Пользователь** | Связанные данные, например записи на курсы и история, могут заблокировать удаление внешними ключами. |
| **Категория** | Связанные курсы могут заблокировать удаление внешним ключом. |
| **Тег** | При удалении сервис удаляет связи с курсами; сами курсы сохраняются. |
| **Отзыв** | REST-эндпоинт удаления не предусмотрен. |
| **Пользователи, категории, теги** | При удалении курса эти объекты **не удаляются** — они являются общими для платформы. |

### 11.3. Решения и оценивание

- На каждое задание студент может отправить **не более одного решения**. Повторная попытка возвращает HTTP 400.
- Отправить решение может **только записанный студент** (статус записи `ACTIVE`). При отсутствии активной записи возвращается HTTP 404.
- Оценка решения (`score`) не может быть отрицательной или превышать `maxScore` задания, если тот задан. Нарушение возвращает HTTP 400.
- Комментарий преподавателя (`feedback`) к решению — необязательное текстовое поле.
- Решения защищены уникальным составным ограничением на уровне базы данных: `(assignment_id, student_id)`.

### 11.4. Тесты и викторины

- У каждого модуля **не более одного теста** (связь 1:1 через `@OneToOne`).
- Для `/start` тест должен содержать **хотя бы один вопрос**. При сдаче дополнительно проверяется наличие вариантов ответа у каждого вопроса.
- Для типа `SINGLE_CHOICE` должен быть **ровно один** правильный вариант. Для `MULTIPLE_CHOICE` — **хотя бы один** правильный вариант. Неправильная конфигурация не позволяет сдать тест.
- Тест можно проходить **многократно** — каждый проход сохраняется как отдельная запись `QuizSubmission`.
- `timeLimit` хранится в минутах, но сервер не отслеживает время прохождения и не отклоняет просроченные ответы.
- При прохождении теста ответы на вопросы, которых нет в тесте, **игнорируются**. Набор выбранных вариантов должен в точности совпадать с правильным: лишний вариант делает ответ неправильным.
- Результат теста вычисляется автоматически: правильный ответ (или набор правильных ответов для `MULTIPLE_CHOICE`) даёт 100% баллов за вопрос. Итоговый балл — процент правильных ответов.

### 11.5. Отзывы и теги

- Оставить отзыв о курсе может **только записанный студент** (статус записи `ACTIVE`). Незаписанный студент или преподаватель не могут оставить отзыв.
- Один студент может оставить **не более одного отзыва** на один курс (уникальность на уровне БД: `(student_id, course_id)`).
- Рейтинг отзыва ограничен диапазоном **1–5**. Значения вне диапазона возвращают HTTP 400.
- Теги — общие для платформы. Удаление тега открепляет его от всех курсов, но **не удаляет** курсы. Добавление тега к курсу создаёт связь в таблице `course_tag`.

### 11.6. Роли и доступ

В таблице указаны требования к роли пользователя, чей ID передаётся в операции, а не права аутентифицированного клиента.

| Роль | Может создавать курсы | Может записываться | Может отправлять решения | Может оценивать | Может оставлять отзывы |
|---|---|---|---|---|---|
| `TEACHER` | Да, может быть указан преподавателем курса | Нет | Нет | Да | Нет |
| `STUDENT` | Нет, не может быть указан преподавателем курса | Да | Да, при активной записи | Нет | Да, при активной записи |
| `ADMIN` | Нет, не может быть указан преподавателем курса | Нет | Нет | Нет | Нет |

> Проверки ролей применяются к переданным ID как бизнес-правила. В системе **нет механизмов аутентификации и авторизации**: любой клиент может вызвать любой эндпоинт и указать чужой ID. Создание курса не проверяет роль вызывающего клиента, а проверяет роль пользователя в поле `teacherId`.

### 11.7. Каскады и ленивая загрузка

**Каскадные операции:**
- Каскад `REMOVE` настроен для связей «курс → модули» и «модуль → уроки». Задания, тесты, вопросы и варианты не входят в этот каскад; при допустимом удалении курса сервис удаляет соответствующие зависимые данные явно.
- Каскадное удаление не настроено для учебной истории: `Submission` (решения), `QuizSubmission` (результаты тестов), `CourseReview` (отзывы). Ограничения и внешние ключи могут блокировать удаление родительских объектов.

**Ленивая загрузка:**
- Все коллекционные связи (`@OneToMany`, `@ManyToMany`) загружаются лениво (`fetch = LAZY`). Обращение к незагруженной коллекции после закрытия Hibernate-сессии вызывает `LazyInitializationException`.
- REST-контроллеры возвращают DTO, преобразуя в них данные сущностей; необходимая загрузка связей зависит от конкретного сервисного метода.
- `spring.jpa.open-in-view` в основной конфигурации явно не задан и по умолчанию включён. В некоторых тестах он отключается явно для проверки работы вне открытой сессии запроса.

---

## 12. Переменные окружения

| Переменная | По умолчанию | Описание |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Активируемые Spring-профили (можно через запятую: `dev,demo`). |
| `DB_URL` | `jdbc:postgresql://localhost:5432/studying_platform_v2` | URL подключения к PostgreSQL. |
| `DB_USER` | `postgres` | Имя пользователя PostgreSQL. |
| `DB_PASSWORD` | `postgres` | Пароль PostgreSQL. |

Параметры `DB_*` используются профилем `dev`. При запуске в Docker Compose профиль `docker` использует фиксированные значения из `application-docker.yml`, игнорируя переменные `DB_*`.

---

## 13. Подробная инструкция по ручной проверке

Приложение предоставляет **REST API**, а не графический интерфейс: страницы для регистрации, входа или прохождения курса в браузере здесь нет. Запросы ниже можно выполнять в терминале через `curl` либо перенести их в Postman или HTTP-клиент IntelliJ IDEA. Аутентификация не реализована; роли и ID пользователей передаются непосредственно в запросах.

### 13.1. Подготовка

1. Установите **JDK 23**, Maven 3, Docker с запущенным Docker Engine и `curl`. Проверьте `java -version`, `mvn -version` и `docker version`. Примеры рассчитаны на Bash или zsh в macOS/Linux; `jq` не требуется.
2. Из корня проекта запустите отдельную PostgreSQL и приложение по [разделу 7](#7-локальный-запуск). Используйте профиль `dev`, без `demo`, и дождитесь в первом терминале сообщения `Started StudyingPlatformApplication`.
3. Откройте **второй терминал**. Команды ниже выполняйте в нём последовательно: переменные действуют только в текущем терминале.

```bash
BASE=http://localhost:8080
curl -i "$BASE/api/categories"
```

Ожидается `HTTP 200` и JSON-массив категорий (в новой базе — `[]`). Опция `-i` показывает HTTP-статус и заголовки; JSON находится после пустой строки. При создании обычно возвращается `201`, при чтении и обновлении — `200`, при удалении — `204` без тела.

**Важно:** после каждого создания найдите `id` в JSON-ответе и вручную присвойте его указанной переменной, заменив `ЧИСЛО` на фактическое число. ID нельзя считать равными 1: они зависят от состояния базы. При повторном выполнении используйте новые email, название категории и название тега, поскольку они уникальны. Не запускайте сценарий на рабочей базе: он создаёт и изменяет данные.

### 13.2. Создание пользователей и курса

Создайте преподавателя и студента. После каждого запроса сохраните соответствующий `id`:

```bash
curl -i -X POST "$BASE/api/users" -H 'Content-Type: application/json' \
  -d '{"email":"manual-teacher@example.com","role":"TEACHER"}'
TEACHER_ID=ЧИСЛО

curl -i -X POST "$BASE/api/users" -H 'Content-Type: application/json' \
  -d '{"email":"manual-student@example.com","role":"STUDENT"}'
STUDENT_ID=ЧИСЛО

curl -i -X POST "$BASE/api/categories" -H 'Content-Type: application/json' \
  -d '{"name":"Ручная проверка"}'
CATEGORY_ID=ЧИСЛО
```

Создайте курс с полученными ID и сохраните его ID:

```bash
curl -i -X POST "$BASE/api/courses" -H 'Content-Type: application/json' \
  -d "{\"title\":\"Основы Java\",\"description\":\"Пробный курс\",\"duration\":40,\"startDate\":\"2030-09-01\",\"teacherId\":$TEACHER_ID,\"categoryId\":$CATEGORY_ID}"
COURSE_ID=ЧИСЛО

curl -i "$BASE/api/courses/$COURSE_ID"
curl -i "$BASE/api/categories"
```

Проверьте название, категорию и ID преподавателя в ответе. Для изменения курса используйте `PUT` с полным набором обязательных полей:

```bash
curl -i -X PUT "$BASE/api/courses/$COURSE_ID" -H 'Content-Type: application/json' \
  -d "{\"title\":\"Основы Java: обновлено\",\"description\":\"Пробный курс\",\"duration\":40,\"startDate\":\"2030-09-01\",\"teacherId\":$TEACHER_ID,\"categoryId\":$CATEGORY_ID}"
```

Ожидается `200` и обновлённое название; убедиться в сохранении можно повторным `GET` курса.

### 13.3. Наполнение курса

Добавьте модуль, урок и задание, сохраняя ID из каждого ответа:

```bash
curl -i -X POST "$BASE/api/courses/$COURSE_ID/modules" -H 'Content-Type: application/json' \
  -d '{"title":"Первый модуль","description":"Знакомство","orderIndex":1}'
MODULE_ID=ЧИСЛО

curl -i -X POST "$BASE/api/lessons/modules/$MODULE_ID/lessons" -H 'Content-Type: application/json' \
  -d '{"title":"Первый урок","content":"Прочитайте материал урока","videoUrl":null}'
LESSON_ID=ЧИСЛО

curl -i -X POST "$BASE/api/assignments/lessons/$LESSON_ID/assignments" -H 'Content-Type: application/json' \
  -d '{"title":"Практика","description":"Напишите приветствие","dueDate":"2030-12-01","maxScore":100}'
ASSIGNMENT_ID=ЧИСЛО

curl -i "$BASE/api/courses/$COURSE_ID/structure"
curl -i "$BASE/api/assignments/lessons/$LESSON_ID/assignments"
```

В структуре должны быть модуль и урок; задания получаются отдельным последним запросом.

### 13.4. Запись, решение и оценка

Запишите студента и проверьте обе стороны связи:

```bash
curl -i -X POST "$BASE/api/enrollments" -H 'Content-Type: application/json' \
  -d "{\"studentId\":$STUDENT_ID,\"courseId\":$COURSE_ID}"
curl -i "$BASE/api/enrollments/courses/$COURSE_ID/students"
curl -i "$BASE/api/enrollments/users/$STUDENT_ID/courses"
```

Ожидается `201` при записи и `200` со студентом/курсом в соответствующих списках. Повторите первый запрос: повторная активная запись должна вернуть `400`.

Отправьте решение, сохраните его ID и оцените от имени преподавателя:

```bash
curl -i -X POST "$BASE/api/submissions" -H 'Content-Type: application/json' \
  -d "{\"assignmentId\":$ASSIGNMENT_ID,\"studentId\":$STUDENT_ID,\"content\":\"Hello world\"}"
SUBMISSION_ID=ЧИСЛО

curl -i -X PATCH "$BASE/api/submissions/$SUBMISSION_ID?teacherId=$TEACHER_ID" \
  -H 'Content-Type: application/json' -d '{"score":90,"feedback":"Хорошая работа"}'
curl -i "$BASE/api/submissions/assignments/$ASSIGNMENT_ID/submissions"
curl -i "$BASE/api/submissions/users/$STUDENT_ID/submissions"
```

В списках найдите решение с оценкой `90` и комментарием. Повторная отправка решения на то же задание возвращает `400`. Попробуйте оценку `101`: для задания с `maxScore: 100` ожидается `400`.

### 13.5. Создание и прохождение теста

Создайте тест и один вопрос с единственным правильным ответом. Сохраняйте ID из ответов:

```bash
curl -i -X POST "$BASE/api/quiz" -H 'Content-Type: application/json' \
  -d "{\"moduleId\":$MODULE_ID,\"title\":\"Проверка знаний\",\"timeLimit\":10}"
QUIZ_ID=ЧИСЛО

curl -i -X POST "$BASE/api/quiz/$QUIZ_ID/questions" -H 'Content-Type: application/json' \
  -d '{"text":"Сколько будет 2 + 2?","type":"SINGLE_CHOICE"}'
QUESTION_ID=ЧИСЛО

curl -i -X POST "$BASE/api/question/$QUESTION_ID/options" -H 'Content-Type: application/json' \
  -d '{"text":"4","isCorrect":true}'
CORRECT_ID=ЧИСЛО

curl -i -X POST "$BASE/api/question/$QUESTION_ID/options" -H 'Content-Type: application/json' \
  -d '{"text":"5","isCorrect":false}'
WRONG_ID=ЧИСЛО
```

Посмотрите тест глазами студента и отправьте правильный ответ:

```bash
curl -i "$BASE/api/quiz/$QUIZ_ID/start"
curl -i -X POST "$BASE/api/quiz/$QUIZ_ID/submit" -H 'Content-Type: application/json' \
  -d "{\"studentId\":$STUDENT_ID,\"answers\":{\"$QUESTION_ID\":[$CORRECT_ID]}}"
```

В ответе `/start` есть вопрос и варианты без признака правильности. При сдаче ожидается `201` и `score: 100`. Проверьте неправильный ответ и историю:

```bash
curl -i -X POST "$BASE/api/quiz/$QUIZ_ID/submit" -H 'Content-Type: application/json' \
  -d "{\"studentId\":$STUDENT_ID,\"answers\":{\"$QUESTION_ID\":[$WRONG_ID]}}"
curl -i "$BASE/api/submissions/quiz/student/$STUDENT_ID"
curl -i "$BASE/api/submissions/quiz/quiz/$QUIZ_ID"
```

Вторая попытка должна иметь `score: 0`; обе попытки сохраняются. Для `MULTIPLE_CHOICE` в массиве ответа нужно указать **все** правильные ID и не указывать неправильные.

### 13.6. Отзывы и теги

Оставьте отзыв и проверьте список:

```bash
curl -i -X POST "$BASE/api/course-reviews" -H 'Content-Type: application/json' \
  -d "{\"studentId\":$STUDENT_ID,\"courseId\":$COURSE_ID,\"rating\":5,\"comment\":\"Понятный курс\"}"
curl -i "$BASE/api/course-reviews/course/$COURSE_ID"
```

Ожидается `201` и затем отзыв в списке. Рейтинг вне диапазона 1–5 должен вернуть `400`.

Создайте тег, привяжите и отвяжите его:

```bash
curl -i -X POST "$BASE/api/tags" -H 'Content-Type: application/json' \
  -d '{"name":"manual-java"}'
TAG_ID=ЧИСЛО

curl -i -X POST "$BASE/api/courses/$COURSE_ID/tags" -H 'Content-Type: application/json' \
  -d "[$TAG_ID]"
curl -i "$BASE/api/tags"
curl -i -X DELETE "$BASE/api/courses/$COURSE_ID/tags" -H 'Content-Type: application/json' \
  -d "[$TAG_ID]"
```

Привязка и отвязка возвращают `204`; тег остаётся в общем списке после отвязки.

### 13.7. Отписка и отрицательные проверки

```bash
# Несуществующий пользователь: 404
curl -i "$BASE/api/users/999999999"

# Пустое название категории: 400
curl -i -X POST "$BASE/api/categories" -H 'Content-Type: application/json' -d '{"name":""}'

# Отписка: 204
curl -i -X DELETE "$BASE/api/enrollments?studentId=$STUDENT_ID&courseId=$COURSE_ID"

# После отписки прохождение недоступно: 404, активная запись не найдена
curl -i -X POST "$BASE/api/quiz/$QUIZ_ID/submit" -H 'Content-Type: application/json' \
  -d "{\"studentId\":$STUDENT_ID,\"answers\":{\"$QUESTION_ID\":[$CORRECT_ID]}}"

# История попыток сохраняется
curl -i "$BASE/api/submissions/quiz/student/$STUDENT_ID"
```

Также попытка удалить курс с учебной историей через `DELETE /api/courses/$COURSE_ID` должна вернуть `400`. Тело ошибки содержит описание причины; при `204` тела нет.

### 13.8. Завершение и устранение проблем

- Остановите приложение через `Ctrl+C` в первом терминале. Если база запущена командой из раздела 7, её можно остановить через `docker stop studying-demo-db`. Для повторного использования — `docker start studying-demo-db`.
- Данные PostgreSQL могут храниться в анонимном Docker-томе. **Только если данные больше не нужны**, после остановки удалите именно этот контейнер и связанные с ним анонимные тома командой `docker rm -v studying-demo-db`. Для нового чистого запуска повторите создание из раздела 7.
- `Connection refused`: проверьте, что приложение запущено и слушает `8080`, а запрос отправлен во втором терминале.
- Ошибка подключения к БД при старте: проверьте `docker ps`, порт `55432` и переменные `DB_URL`, `DB_USER`, `DB_PASSWORD` в терминале приложения.
- `400` или `404` на шаге сценария: проверьте HTTP-статус предыдущего создания, фактические ID и JSON. Для повторного сценария используйте новые уникальные email, категорию и тег.
- Если порты заняты, остановите конфликтующий сервис либо измените привязку порта и соответствующий URL. Полный список маршрутов приведён в [разделе 6](#6-rest-api).
