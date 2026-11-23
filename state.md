# Checkpoint

## Текущее состояние

Пункты 1–4 `refactor-after-part2.md` завершены. `Submission.feedback` отображается в nullable PostgreSQL `TEXT`; длинный комментарий, его замена и `null` проверены через сервис, JPA и SQL после подтверждения транзакций. Этапы 11, 13 и 15 в `plan.md` остаются `in_progress` до повторной приёмки части II.

## Проверки

- **PASS:** `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn -Dtest=PostgresTextStorageTests,SubmissionTests,SubmissionGradingTransactionTests test` — 44 теста, ошибок и пропусков нет.
- **PASS:** `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn verify` — 284 теста, ошибок и пропусков нет; PostgreSQL Testcontainers.
- **PASS:** `git diff --check`.

## Открытые вопросы и следующий шаг

Рабочая схема `studying_platform_v2` не проверялась и не изменялась. Если `feedback` там имеет тип `VARCHAR(255)`, его изменение на `TEXT` требует отдельного согласования: `ddl-auto=update` может не изменить существующую колонку. Далее выполнить пункт 5 `refactor-after-part2.md`: загрузка структуры теста до окончания транзакции. Рабочую базу для тестов не использовать.
