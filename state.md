# Checkpoint

## Текущее состояние

Пункты 1–2 `refactor-after-part2.md` завершены. Удаление курса без учебной истории удаляет задания и структуру тестов до каскадного удаления курса; история и общие объекты сохраняются. Этапы 11, 13 и 15 в `plan.md` остаются `in_progress` до повторной приёмки части II.

## Проверки

- **PASS:** `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn -Dtest=CourseDeletionTransactionTests,CourseDeletionTests,PostgresSchemaTests test` — 38 тестов, ошибок и пропусков нет.
- **PASS:** `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn verify` — 273 теста, ошибок и пропусков нет; PostgreSQL Testcontainers.
- **PASS:** `git diff --check`.

## Следующий шаг

Выполнить пункт 3 `refactor-after-part2.md`: проверка роли преподавателя и корректности оценки. Рабочую базу `studying_platform_v2` для тестов не использовать. Остаются открыты пункты 3–8 плана исправлений, включая вопрос обновления рабочей схемы для длинных комментариев.
