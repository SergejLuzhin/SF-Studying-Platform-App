# Текущее состояние проекта

## Текущий этап
Этап 02 — не начат.

## Выполненные этапы
- **Этап 01 (Проектирование модели):** completed. Создан `docs/data-model.md` с описанием 15 JPA-сущностей, связей, каскадов, стратегий загрузки и защиты учебной истории. Все 6 критериев приёмки: PASS.

## Блокирующие проблемы
Нет.

## Затронутые файлы
- `docs/data-model.md` — создан (27 КБ, 602 строки)

## Ключевые решения
- 15 сущностей: User, Profile, Course, Category, Enrollment, Module, Lesson, Assignment, Submission, Quiz, Question, AnswerOption, QuizSubmission, CourseReview, Tag
- course_tag — join table, не отдельная JPA-сущность
- Все коллекции — LAZY
- Записи с учебной историей защищены от удаления (Enrollment DROPPED, Submission, QuizSubmission)
- Курс без записей и истории удаляется каскадно; при наличии истории — запрещено

## Проверки
- Критерий 1 (файл существует): PASS
- Критерий 2 (15 сущностей): PASS
- Критерий 3 (M:M Course-Tag via JoinTable): PASS
- Критерий 4 (M:M User-Course via Enrollment): PASS
- Критерий 5 (LAZY для всех коллекций): PASS
- Критерий 6 (защита истории): PASS
- `git status --short`: PASS (только новый файл)
- `git diff --check`: PASS (нет проблем форматирования)

## Следующее действие
Начать выполнение Этапа 02 (Основа Spring Boot) из plan.md.
