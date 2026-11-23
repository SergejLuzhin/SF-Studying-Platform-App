# Checkpoint

## Текущее состояние

Пункты 1–3 `refactor-after-part2.md` завершены. Оценивание доступно только пользователю с ролью преподавателя; `null` и оценки вне диапазона отклоняются до изменения решения. Тесты проверяют сохранность данных после ошибок в новой транзакции. Этапы 11, 13 и 15 в `plan.md` остаются `in_progress` до повторной приёмки части II.

## Проверки

- **PASS:** `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn -Dtest=SubmissionTests,SubmissionGradingTransactionTests test` — 39 тестов, ошибок и пропусков нет.
- **PASS:** `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn verify` — 282 теста, ошибок и пропусков нет; PostgreSQL Testcontainers.
- **PASS:** `git diff --check`.

## Следующий шаг

Выполнить пункт 4 `refactor-after-part2.md`: хранение длинного комментария преподавателя в PostgreSQL. Рабочую базу `studying_platform_v2` для тестов не использовать; изменение её схемы отдельно согласовать с пользователем.
