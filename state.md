# Checkpoint

## Текущее состояние

Завершён Этап 21: «Демонстрационные данные». Все предыдущие этапы (1–20) подтверждены.

## Что реализовано в Этапе 21

- **`DemoDataSeeder`** (`com.studying.platform.seeder`) — компонент, запускаемый через профиль `demo`.
  Создает: 1 категорию, 3 тега, 1 преподавателя, 2 студентов, 1 курс, 2 модуля, 4 урока, 4 задания, 2 записи, 1 викторину с 2 вопросами (SINGLE_CHOICE + MULTIPLE_CHOICE) и 6 вариантами ответов.
  Защита от дублей — проверка `existsByEmail(DEMO_TEACHER_EMAIL)`.
- **Конфигурация:** профиль `demo` добавлен в `application-dev.yml` как `on-profile`.
- **Репозитории:** добавлены `existsByEmail` в `UserRepository` и `findByName` в `TagRepository`.
- **Тесты:** 10 интеграционных тестов в `DemoDataSeederTest` (создание, связи, повторный запуск, SQL-проверка).

## Итоговые проверки

- **PASS:** `mvn test` — 466 тестов, 0 ошибок, 0 пропусков
- **PASS:** `mvn javadoc:javadoc` — нет предупреждений JavaDoc для `DemoDataSeeder`
- **PASS:** `git diff --check` — нет проблем с форматированием
- **PASS:** независимая проверка `stage-tester` — STATUS: PASS

## Затронутые файлы

- NEW: `src/main/java/com/studying/platform/seeder/DemoDataSeeder.java`
- NEW: `src/test/java/com/studying/platform/DemoDataSeederTest.java`
- NEW: `stages/stage-21.md`
- MOD: `src/main/java/com/studying/platform/repository/UserRepository.java`
- MOD: `src/main/java/com/studying/platform/repository/TagRepository.java`
- MOD: `src/main/resources/application-dev.yml`

## Следующий шаг

Этап 22: Docker и автоматизация — `pending`.
