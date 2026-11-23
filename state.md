# Checkpoint

## Текущий этап

Следующий этап — 14 «Отзывы и теги» (`pending`).

## Последние изменения

### Этап 13. Прохождение тестов (completed)

- Создана сущность JPA: `QuizSubmission` (`id`, `quiz` ManyToOne, `student` ManyToOne, `score` Integer, `takenAt` LocalDateTime). Каскадное удаление не настроено.
- Создан репозиторий: `QuizSubmissionRepository` (с методами `findByStudentId` и `findByQuizId`).
- Создан сервис: `QuizSubmissionService` с методами `startQuiz` и `submitAnswers`.
  - Реализована проверка активной записи на курс (`EnrollmentStatus.ACTIVE`).
  - Вычисление балла по точному совпадению множеств выбранных и правильных ID вариантов ответа.
  - Поддержка множественных попыток (каждый вызов создаёт новую запись).
- Обновлён `PostgresSchemaTests`: добавлены проверки таблицы `quizsubmission`, её PK и FK.
- Добавлены интеграционные тесты в `QuizSubmissionTests.java` (23 теста): позитивные сценарии, проверка записи на курс, точное совпадение множеств, частичное совпадение, лишние ID, множественные попытки, выборки, ограничения NOT NULL.
- Обновлены `QuizRepository` и `QuestionRepository` с JPQL-запросами для предварительной загрузки вопросов и вариантов ответов.

## Проверки

- **PASS:** компиляция проекта и тестов (`mvn clean compile test-compile`).
- **PASS:** `git diff --check` — без ошибок форматирования.
- **PASS:** полный прогон всех тестов: 232 теста, 0 ошибок, 0 пропусков.

## База данных

Использовать только `studying_platform_v2` для приложения и `platform_test` либо Testcontainers для тестов. Миграция прежних данных не требуется.
