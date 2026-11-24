# Учебная платформа

Spring Boot-приложение для управления курсами, учебными материалами, заданиями и тестами через JSON REST API. Данные хранятся в PostgreSQL.

> **Безопасность:** аутентификация и авторизация отсутствуют. Все API, включая административные операции и просмотр правильных ответов, доступны любому клиенту, имеющему доступ к серверу. Проверки ролей — бизнес-правила, а не защита доступа. Конфигурация и пароли ниже предназначены только для изолированной демонстрации, не для production.

## Требования

- JDK 23 и Maven, PostgreSQL 16; для интеграционных тестов нужен работающий Docker и доступ к образу `postgres:16`.
- Для примеров API нужны `curl` и `jq`. Docker Compose нужен для контейнерного запуска.
- По умолчанию сервер слушает порт 8080. База должна существовать заранее; Hibernate обновляет схему при старте (`ddl-auto: update`).

## Локальный запуск

Профиль по умолчанию — `dev`. Его значения по умолчанию: `jdbc:postgresql://localhost:5432/studying_platform_v2`, пользователь и пароль `postgres`. **Не запускайте пример на рабочей базе**: явно задайте отдельную пустую базу и её учётные данные. Например, для изолированной PostgreSQL в Docker (порт 55432 должен быть свободен):

```bash
docker run -d --name studying-demo-db -p 127.0.0.1:55432:5432 \
  -e POSTGRES_DB=studying_demo -e POSTGRES_USER=demo -e POSTGRES_PASSWORD=demo postgres:16
export DB_URL=jdbc:postgresql://localhost:55432/studying_demo
export DB_USER=demo DB_PASSWORD=demo
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

Дождитесь готовности PostgreSQL перед запуском Maven. При завершении остановите приложение через Ctrl+C. Контейнер можно остановить командой `docker stop studying-demo-db`; его данные остаются в контейнере. Не удаляйте чужие контейнеры или данные.

## Docker Compose

`Dockerfile` копирует уже собранный JAR; сборка внутри Docker не выполняется:

```bash
mvn -B package
docker compose config
docker compose up --build
```

`package` запускает тесты, которым нужен Docker. Compose запускает PostgreSQL и приложение на `localhost:8080`; профиль `docker` использует адрес `postgres:5432`, базу `studying_platform`, пользователя и пароль `postgres`, заданные в конфигурации. Переменные `DB_*` профилем `docker` не используются. Постоянный том в Compose не объявлен: данные зависят от жизненного цикла контейнера PostgreSQL. Для остановки используйте Ctrl+C или `docker compose stop`; не удаляйте данные без необходимости.

## Тестирование

```bash
mvn -B clean compile
mvn -B test
```

Интеграционные тесты используют Spring и изолированную PostgreSQL Testcontainers с профилем `test` и `create-drop`, а не рабочую базу. Требуются работающий Docker daemon и доступный образ. CI выполняет также `mvn -B test -Ptest`; отдельный Maven-профиль `test` в `pom.xml` не объявлен.

## Демонстрационный сценарий

Используйте **новую изолированную** базу, как в локальной инструкции. Для явного включения заполнения остановите сервер, задайте `SPRING_PROFILES_ACTIVE=dev,demo` и снова выполните `mvn spring-boot:run`. При старте создаются категория `Programming`, три тега, преподаватель, два студента, курс `Intro to Java`, два модуля, четыре урока и задания, две записи и тест с двумя вопросами. Без профиля `demo` заполнение не запускается.

При следующем запуске с `dev,demo` наличие `demo-teacher@platform.test` пропускает заполнение целиком: дубликаты не создаются, но частично удалённый набор не восстанавливается. На непустой базе возможны конфликты уникальных данных. ID зависят от базы и не фиксированы.

Для следующего примера получите ID из **изолированной демонстрационной базы** (команда предполагает контейнер из примера выше):

```bash
sql() { docker exec studying-demo-db psql -U demo -d studying_demo -Atc "$1"; }
COURSE_ID=$(sql "select id from courses where title = 'Intro to Java'")
STUDENT_ID=$(sql "select id from users where login = 'demo-student-one@platform.test'")
QUIZ_ID=$(sql "select id from quiz where title = 'Module 2 Quiz'")
curl -s "http://localhost:8080/api/courses/$COURSE_ID/structure" | jq .
curl -s "http://localhost:8080/api/enrollments/courses/$COURSE_ID/students" | jq .
```

Выберите ID урока из поля `modules[].lessons[].id` структуры и просмотрите его задания:

```bash
LESSON_ID=ИД_УРОКА
curl -s "http://localhost:8080/api/assignments/lessons/$LESSON_ID/assignments" | jq .
curl -s "http://localhost:8080/api/quiz/$QUIZ_ID/start" | jq .
```

`/start` возвращает ID и тексты вопросов и вариантов, но не признак правильности. Для демонстрации попытки без выбранных ответов (она сохраняется с нулевым результатом):

```bash
curl -s -X POST "http://localhost:8080/api/quiz/$QUIZ_ID/submit" \
  -H 'Content-Type: application/json' \
  -d "{\"studentId\":$STUDENT_ID,\"answers\":{}}" | jq .
curl -s "http://localhost:8080/api/submissions/quiz/student/$STUDENT_ID" | jq .
```

Ответ создания попытки имеет HTTP 201 и содержит `id`, `quizId`, `studentId`, `score`, `takenAt`. В обычной попытке `answers` — объект, где ключи — ID вопросов в виде строк, а значения — массивы ID выбранных вариантов, например `{"studentId":123,"answers":{"456":[789]}}`; используйте реальные ID из `/start`. Вопрос засчитывается только при точном совпадении набора с правильным. Тест можно проходить повторно. До прохождения каждый вопрос должен иметь варианты и корректное количество правильных ответов; неготовый тест не сохраняет результат.

## REST API

Базовый адрес `http://localhost:8080`. Тела запросов — JSON, для записи нужен заголовок `Content-Type: application/json`. Успешное создание обычно возвращает 201, чтение и изменение 200, удаление 204 без тела. Списки — обычные JSON-массивы, в том числе `[]`, без оболочки пагинации.

| Ресурс | Основные маршруты |
|---|---|
| Пользователи | `POST /api/users`, `GET`, `PUT`, `DELETE /api/users/{id}` |
| Категории | `POST`, `GET /api/categories`; `GET`, `PUT`, `DELETE /api/categories/{id}` |
| Курсы | `POST /api/courses`; `GET`, `PUT`, `DELETE /api/courses/{id}`; `GET /api/courses/{id}/structure` |
| Модули и уроки | `POST /api/courses/{id}/modules`; `GET`, `PUT`, `DELETE /api/modules/{id}`; `POST /api/lessons/modules/{id}/lessons`; `GET`, `PUT`, `DELETE /api/lessons/{id}` |
| Записи | `POST /api/enrollments`; `DELETE /api/enrollments?studentId=...&courseId=...`; `GET /api/enrollments/courses/{id}/students`, `GET /api/enrollments/users/{id}/courses` |
| Задания | `POST`, `GET /api/assignments/lessons/{lessonId}/assignments`; `GET`, `PUT`, `DELETE /api/assignments/{id}` |
| Решения | `POST /api/submissions`; `PATCH /api/submissions/{id}?teacherId=...`; `GET /api/submissions/assignments/{id}/submissions`, `GET /api/submissions/users/{id}/submissions` |
| Тесты | `POST /api/quiz`; `GET`, `PUT`, `DELETE /api/quiz/{id}`; `GET /api/quiz/{id}/start`; `POST /api/quiz/{id}/submit`; `GET`, `POST /api/quiz/{id}/questions` |
| Вопросы и варианты | `POST /api/question/quiz/{quizId}/questions`; `GET`, `PUT`, `DELETE /api/question/{id}`; `GET`, `POST /api/question/{id}/options`; `GET`, `PUT`, `DELETE /api/options/{id}` |
| Результаты и отзывы | `GET /api/submissions/quiz/{id}`, `/api/submissions/quiz/student/{id}`, `/api/submissions/quiz/quiz/{id}`; `POST /api/course-reviews`, `GET /api/course-reviews/course/{id}`, `/api/course-reviews/student/{id}` |
| Теги | `POST`, `GET /api/tags`; `GET`, `PUT`, `DELETE /api/tags/{id}`; связи описаны ниже |

Списки студентов курса содержат `id`, `email`, `role` пользователя; списки курсов студента — `id`, `title`, `description` курса. Возвращаются только активные записи. В ответе создания записи `id` обозначает саму запись.

При создании задания необязательный `lessonId` в теле должен совпадать с ID в пути. При `PUT /api/assignments/{id}` поле `lessonId` игнорируется и не переносит задание. Например, тело создания: `{"title":"Практика","description":"Задание","dueDate":"2030-01-01","maxScore":100}`.

Управление тегами: создание, например, `POST /api/tags` с `{"name":"Java"}`. Добавление и удаление **связей** — `POST` и `DELETE /api/courses/{courseId}/tags` с JSON-массивом ID в теле, например `[1,2]`; успех — 204. Открепление не удаляет общие теги. `DELETE /api/tags/{tagId}` удаляет тег и его связи, но не курсы.

`PUT /api/users/{userId}` требует оба поля: `{"email":"student@example.com","role":"STUDENT"}`. Email должен быть корректным, уникальным и не длиннее 50 символов. Для `PUT /api/courses/{courseId}` обязательны `title`, `duration` (не менее 1), `startDate`, `teacherId`, `categoryId`; `description` может быть `null`. Преподаватель должен иметь роль `TEACHER`.

При ошибке сервер возвращает объект `timestamp`, `status`, `error`, `message`, `path`. Невалидные данные и нарушения бизнес-правил обычно дают 400, отсутствующий ресурс — 404, непредвиденная ошибка — 500 с безопасным сообщением. Конкретный текст и порядок ошибок валидации не фиксированы. Некоторые проверки записи в сервисах могут возвращать 404.

## Модель и целостность

Контроллеры принимают и возвращают DTO; сервисы выполняют бизнес-операции в транзакциях; репозитории Spring Data JPA работают с PostgreSQL через Hibernate. Модель включает 15 сущностей: пользователей и профили, категории и курсы, записи, модули и уроки, задания и решения, тесты, вопросы, варианты и результаты, отзывы и теги. Присутствуют связи 1:1, 1:N и M:N; теги общие для курсов.

Коллекции загружаются лениво. Обращение к незагруженной коллекции после закрытия сессии может вызвать `LazyInitializationException`; сервис получает структуру внутри транзакции и возвращает DTO через `/api/courses/{id}/structure`. Не следует полагаться на открытую сессию при сериализации.

Записаться может только студент, повторная активная запись запрещена; решение, прохождение теста и отзыв требуют активной записи. На задание допускается одно решение. Пустой курс без записей и учебной истории можно удалить вместе с содержимым. При наличии записей или сохранённых решений, результатов тестов или отзывов удаление запрещено даже после отписки. Отписка не удаляет историю; общие пользователи, категории и теги при удалении курса сохраняются.
