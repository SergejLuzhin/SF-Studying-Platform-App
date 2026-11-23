# Этап 12: Структура тестов (Quiz, Question, AnswerOption)

## 1. Цель и границы этапа
Создание сущностей `Quiz`, `Question`, `AnswerOption`, соответствующих репозиториев и enum `QuizQuestionType`, а также интеграционных тестов для проверки структуры и ограничений.

**В границы этапа входят:**
- Создание сущностей JPA и их связей.
- Создание репозиториев Spring Data JPA.
- Создание интеграционных тестов для новых сущностей.

**За пределами этапа:**
- Прохождение тестов пользователем (QuizSubmission) — этап 13.
- REST-контроллеры — этапы 16-19.

## 2. Связанные файлы и ADR
- **Сущности:** `src/main/java/com/studying/platform/entity/`
- **Репозитории:** `src/main/java/com/studying/platform/repository/`
- **Тесты:** `src/test/java/com/studying/platform/`
- **ADR:** Существенных архитектурных решений не требуется (индекс `docs/adr/README.md` пуст).

## 3. Порядок действий

### Шаг 1. Enum `QuizQuestionType`
- Файл: `src/main/java/com/studying/platform/entity/QuizQuestionType.java`
- Содержимое: `SINGLE_CHOICE`, `MULTIPLE_CHOICE`.

### Шаг 2. Сущность `Quiz`
- Файл: `src/main/java/com/studying/platform/entity/Quiz.java`
- Поля: `id`, `module` (OneToOne), `title`, `timeLimit`, `questions` (OneToMany).
- Связи:
  - `@OneToOne` с `Module` (владелец связи). Поле `module_id` уникально.
  - `@OneToMany` на `Question` (PERSIST).
- Стиль маппинга: как в `Module.java` (LAZY, jakarta.persistence).

### Шаг 3. Сущность `Question`
- Файл: `src/main/java/com/studying/platform/entity/Question.java`
- Поля: `id`, `quiz` (ManyToOne), `text`, `type` (QuizQuestionType), `options` (OneToMany).
- Связи:
  - `@ManyToOne` с `Quiz`.
  - `@OneToMany` на `AnswerOption` (PERSIST).

### Шаг 4. Сущность `AnswerOption`
- Файл: `src/main/java/com/studying/platform/entity/AnswerOption.java`
- Поля: `id`, `question` (ManyToOne), `text`, `isCorrect`.
- Связи: `@ManyToOne` с `Question`.

### Шаг 5. Репозитории
- `QuizRepository.java`
- `QuestionRepository.java`
- `AnswerOptionRepository.java`
- Все интерфейсы расширяют `JpaRepository` и `JpaSpecificationExecutor`.

### Шаг 6. Тесты
- `src/test/java/com/studying/platform/QuizStructureTest.java`
- Проверка сохранения и загрузки иерархии (Quiz -> Question -> AnswerOption).
- Проверка ограничений (уникальность связи с модулем).

## 4. Критерии приёмки
- Все сущности созданы, компилируются, маппинги соответствуют заданию.
- Репозитории существуют и наследуются от Spring Data JPA интерфейсов.
- Интеграционные тесты проходят успешно.
- Связь Quiz -> Module реализована как OneToOne (один модуль — один тест).
- Связь Question -> AnswerOption поддерживает множественные правильные варианты.

## 5. Тест-кейсы

### Интеграционные тесты (QuizStructureTest)
1. **Сохранение Quiz с Question и AnswerOption**
   - Создать Quiz, привязать к Module.
   - Добавить Question в Quiz.
   - Добавить AnswerOption в Question.
   - Сохранить Quiz (каскад).
   - Загрузить Quiz по ID и проверить структуру.
2. **Ограничение OneToOne (Quiz -> Module)**
   - Попытаться создать второй Quiz для того же Module (должна возникнуть ошибка уникальности или дубликата на уровне БД/приложения).
3. **Типы вопросов**
   - Проверить сохранение `SINGLE_CHOICE` и `MULTIPLE_CHOICE`.

## 6. Команды проверки
```bash
mvn clean compile
mvn test -Dtest=QuizStructureTest
```
*Проверка существующих тестов:* `mvn test` (убедиться, что новые сущности не ломают контекст).

## 7. Риски и блокеры
- Конфликт с существующими маппингами `Module` (в `Module` уже есть `@OneToMany` на `Lesson`). Добавление `@OneToOne` на `Quiz` в `Quiz` (владелец связи) безопасно.
- Отсутствие `QuizSubmission` не влияет на структуру теста.
- Требуется PostgreSQL (Testcontainers) для интеграционных тестов.
