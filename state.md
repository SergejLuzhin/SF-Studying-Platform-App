# Checkpoint

## Текущее состояние

Пункты 1–8 `refactor-after-part2.md` завершены; этапы 09–15 в `plan.md` имеют статус `completed`. Следующий этап — 16 «Основа REST API» (`pending`); перед выполнением подготовить его подробный план по правилам проекта.

## Проверки

- **PASS:** `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn -Dtest=PartTwoAcceptanceTests,CourseDeletionTransactionTests,SubmissionGradingTransactionTests,QuizReadinessTransactionTests,QuizServiceTransactionTests test` — 33 теста, ошибок и пропусков нет.
- **PASS:** повторный `JAVA_HOME=$(/usr/libexec/java_home -v 23) mvn verify` — 297 тестов, ошибок и пропусков нет; профиль `test` и PostgreSQL Testcontainers.
- **PASS:** отдельные `mvn -o -DskipTests -Dshow=private javadoc:javadoc` и `javadoc:test-javadoc` на Java 23.
- **PASS:** с согласия пользователя схема `studying_platform_v2` обновлена через `ddl-auto=update`; проверены 16 таблиц, 19 внешних ключей и `submissions.feedback` типа `text`. Во всех таблицах по 0 записей.

## Открытые вопросы и следующий шаг

Не запускать тестовый профиль и `create-drop` на рабочей базе; профиль `dev` использует `ddl-auto=update` и может изменять схему. Для будущего REST использовать представление теста без признаков правильности ответов. Аутентификация и авторизация вне согласованного объёма.
