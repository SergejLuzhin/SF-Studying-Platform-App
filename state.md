# Checkpoint

## Текущее состояние

Пункты 1–5 `refactor-after-part2.md` завершены. `startQuiz` загружает вопросы и варианты внутри транзакции; их доступность после возврата и обычная ленивость JPA проверены. Этапы 11, 13 и 15 в `plan.md` остаются `in_progress` до повторной приёмки части II.

## Проверки

- **PASS:** `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn -Dtest=QuizServiceTransactionTests,QuizSubmissionTests test` — 27 тестов, ошибок и пропусков нет.
- **PASS:** `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn verify` — 288 тестов, ошибок и пропусков нет; PostgreSQL Testcontainers.
- **PASS:** `git diff --check`.

## Открытые вопросы и следующий шаг

Выполнить пункт 6 `refactor-after-part2.md`: проверка готовности теста перед сохранением результата. Сущность теста содержит признаки правильности ответов и не должна напрямую выдаваться студенту будущим REST API. Рабочая схема `studying_platform_v2` не проверялась и не изменялась: если `feedback` имеет тип `VARCHAR(255)`, переход на `TEXT` требует отдельного согласования; `ddl-auto=update` может не обновить колонку. Рабочую базу для тестов не использовать.
