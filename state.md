# Checkpoint

## Текущее состояние

Пункты 1–7 `refactor-after-part2.md` завершены; пункт 8 `blocked`: рабочая схема пока не содержит таблицы заданий и решений, а пользователь решил оставить её без изменений. Сквозная приёмка изолированной PostgreSQL прошла: учебная история защищена, курс без истории удаляется с содержимым, общие объекты сохраняются. Этапы 13 и 15 в `plan.md` подтверждены как `completed`, этап 11 остаётся `in_progress` до решения вопроса рабочей схемы.

## Проверки

- **PASS:** `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn -Dtest=PartTwoAcceptanceTests,CourseDeletionTransactionTests,SubmissionGradingTransactionTests,QuizReadinessTransactionTests,QuizServiceTransactionTests test` — 33 теста, ошибок и пропусков нет.
- **PASS:** `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn verify` — 297 тестов, ошибок и пропусков нет; профиль test и PostgreSQL Testcontainers.
- **PASS:** отдельные `mvn -o -DskipTests -Dshow=private javadoc:javadoc` и `javadoc:test-javadoc` на Java 23 — без ошибок, с предупреждениями о недокументированных элементах.
- **PASS:** `git diff --check`.
- **PASS:** проверка метаданных `studying_platform_v2` только на чтение: таблицы `assignments` и `submissions` отсутствуют.
- **NOT_RUN:** изменение рабочей схемы; пользователь решил оставить её как есть.

## Открытые вопросы и следующий шаг

Не менять рабочую схему без нового согласования пользователя. Для использования заданий и решений в `studying_platform_v2` потребуется отдельно согласованное обновление схемы; запуск приложения с профилем `dev` может её изменить из-за `ddl-auto=update`. Не запускать тестовый профиль и `create-drop` на рабочей базе. После решения вопроса пересмотреть пункт 8 и статус этапа 11. Для будущего REST использовать представление теста без признаков правильности ответов.
