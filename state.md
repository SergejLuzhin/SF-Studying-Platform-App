# Checkpoint

## Текущее состояние

Этап 19 «API тестов, отзывов и тегов» — **`completed`**. Все 3 отключённых `@Disabled` теста исправлены и проходят.

### Исправления в рамках этапа 19

1. **`QuizService.createQuestion`** — добавлена поддержка двунаправленной связи: `quiz.getQuestions().add(question)`.
2. **`AnswerOptionService.createAnswerOption`** — добавлена поддержка двунаправленной связи: `question.getOptions().add(option)`.
3. **`AnswerOptionService.getOptionsByQuestion`** — добавлен новый метод для получения списка вариантов вопроса.
4. **`QuestionController`** — добавлен `GET /api/question/{questionId}/options`; перемещены GET/PUT/DELETE вариантов с конфликтующего пути `/api/question/{id}/options` на отдельный `OptionController` (существующий), в самом контроллере оставлены только POST и GET для создания и получения списка.
5. **`QuizReviewTagControllerTest.java`** — удалены 3 `@Disabled` аннотации; исправлены тестовые данные (`isCorrect = true` вместо `false` для SINGLE_CHOICE); удалено дублирование `DELETE FROM modules` в `cleanUp()`.

## Проверки

- **PASS:** `mvn test` — 423 теста, 0 ошибок, 0 пропусков.
- **PASS:** `git diff --check` — без проблем форматирования.
- **ADR:** изменений нет, индекс пуст.

## Изменённые файлы

1. `src/main/java/…/service/QuizService.java` — `createQuestion`: добавлена поддержка bidirectional (quiz.getQuestions().add(question)).
2. `src/main/java/…/service/AnswerOptionService.java` — `createAnswerOption`: добавлена поддержка bidirectional (question.getOptions().add(option)); добавлен `getOptionsByQuestion`.
3. `src/main/java/…/controller/QuestionController.java` — добавлен `GET /api/question/{id}/options`; удалены GET/PUT/DELETE для отдельных вариантов (конфликт URL).
4. `src/test/java/…/QuizReviewTagControllerTest.java` — удалены 3 `@Disabled`; исправлены тестовые данные вариантов; удалено дублирование cleanUp.

## План этапа

- `stages/stage-19.md` — детальный план реализации (исправлено).

## Ключевые решения

- Двунаправленные связи JPA поддерживаются в методах создания сущностей.
- Эндпоинт `GET /api/question/{questionId}/options` возвращает список вариантов ответа.
- Все 34 HTTP-теста проходят.

## Открытые проблемы и следующий шаг

**Нет открытых проблем.** Этап 19 завершён.

**Следующий шаг:** Этап 20 «Ленивая загрузка и CRUD» — `pending`.
