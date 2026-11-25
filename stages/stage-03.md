# Этап 03. Инфраструктура интеграционных тестов

**Ссылка на общий план:** [`plan.md`](../../plan.md) — Этап 03, «Инфраструктура интеграционных тестов».
**Связанные документы:**
- [`task.md`](../../task.md) — требования к интеграционным тестам (разд. 2.1, абзац про интеграционные тесты).
- [`state.md`](../../state.md) — этапы 01 и 02 завершены; профили `dev`/`test` настроены, Testcontainers с PostgreSQL уже подключён.
- [`docs/adr/README.md`](../../docs/adr/README.md) — индекс ADR пуст; значимых архитектурных решений для этого этапа не требуется.

---

## 1. Цель и границы этапа

**Цель:** убедиться, что инфраструктура интеграционных тестов полностью рабочая: Spring Boot загружает контекст с профилем `test`, Testcontainers автоматически поднимает изолированную PostgreSQL, JPA-слой инициализируется (DataSource + EntityManagerFactory), и тестовая база полностью независима от локальной рабочей базы (`dev`-профиль).

**Границы:**
- **Входит:** верификация конфигурации тестов, запуск Testcontainers PostgreSQL, проверка подключения Spring-контекста к реальной PostgreSQL, проверка работы JPA EntityManagerFactory, проверка изоляции от `dev`-базы.
- **Не входит:** создание JPA-сущностей (этап 04+), написание CRUD-тестов, REST API, миграции Flyway/Liquibase.

---

## 2. Связанные файлы

| Файл | Роль |
|---|---|
| `pom.xml` | Зависимости: `spring-boot-starter-test`, `org.testcontainers:jdbc`, `org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter` — **уже присутствуют**. |
| `src/main/resources/application.yml` | Базовая конфигурация, `spring.profiles.active: dev`. |
| `src/main/resources/application-test.yml` | Профиль `test`: `jdbc:tc:postgresql:16:///studying_platform_test`, `ddl-auto: create-drop`. |
| `src/main/resources/application-dev.yml` | Профиль `dev`: локальная PostgreSQL, `ddl-auto: update`. |
| `src/test/java/com/studying/platform/StudyingPlatformApplicationTests.java` | Текущий пустой `@SpringBootTest` — требует расширения. |

**Применимые ADR:** Нет утверждённых ADR (индекс пуст). Данный этап не требует новых архитектурных решений.

---

## 3. Предварительный анализ текущей конфигурации

### 3.1. `application-test.yml` — анализ

Текущая конфигурация профиля `test`:

```yaml
spring:
  config:
    activate:
      on-profile: test
  datasource:
    url: jdbc:tc:postgresql:16:///studying_platform_test
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

**Выводы:**
- `jdbc:tc:postgresql:16:///studying_platform_test` — стандартный формат Testcontainers JDBC URL. При первом подключении Docker-образ `postgres:16` скачивается и контейнер запускается автоматически.
- Имя базы `studying_platform_test` — уникальное, не конфликтует с `dev`-базой `studying_platform`.
- `ddl-auto: create-drop` — при загрузке контекста схема создаётся, при выгрузке — удаляется. Идеально для изолированных тестов.
- Логирование SQL включено — полезно для отладки.

**Потенциальная проблема:** `@SpringBootTest` по умолчанию кэширует контекст между тестовыми методами (Spring Boot Test Context Framework). При `create-drop` контекст загружается один раз при первом тесте, схема создаётся и остаётся до выгрузки контекста. Это корректное поведение: все тесты внутри одного метода `@SpringBootTest` делят один контекст и одну схему.

### 3.2. `StudyingPlatformApplicationTests.java` — анализ

Текущий тест:

```java
@SpringBootTest
@ActiveProfiles("test")
class StudyingPlatformApplicationTests {

    @Test
    void contextLoads(@Autowired DataSource dataSource) {
        org.junit.jupiter.api.Assertions.assertNotNull(dataSource);
    }
}
```

**Недостатки:**
- Проверяет только `@Autowired DataSource`, но не проверяет EntityManagerFactory (JPA-слой).
- Не проверяет реальную связь с контейнеризированной PostgreSQL.
- Нет тестов на изоляцию от `dev`-базы.

---

## 4. Действия

### Действие 3.1. Расширить `StudyingPlatformApplicationTests.java`

Добавить интеграционный тест, проверяющий:
1. Загрузку Spring-контекста с профилем `test`.
2. Наличие настроенного `EntityManagerFactory` (JPA-слой инициализирован).
3. Возможность получить соединение из DataSource и выполнить запрос к реальной PostgreSQL (проверка живости контейнера).

**Требования к тесту:**
- Использовать `@SpringBootTest` + `@ActiveProfiles("test")` (как есть).
- Внедрить `EntityManagerFactory emf` и `DataSource ds` через `@Autowired`.
- Проверить `emf != null` и `ds != null`.
- Получить `Connection` из `ds` и выполнить `SELECT 1` для подтверждения живого соединения с PostgreSQL.
- Закрыть соединение в `finally`-блоке.
- JavaDoc на русском для класса и каждого метода.

**Редактируемый файл:** только `src/test/java/com/studying/platform/StudyingPlatformApplicationTests.java`.

### Действие 3.2. Добавить тест изоляции от `dev`-базы

Добавить тестовый метод, проверяющий:
1. Что активный профиль — `test` (через `Environment.getActiveProfiles()`).
2. Что URL DataSource содержит `studying_platform_test` (а не `studying_platform`).
3. Что `ddl-auto` установлен в `create-drop`.

Это гарантирует, что тесты не могут случайно подключиться к рабочей базе.

### Действие 3.3. Проверить компиляцию и запуск тестов

После изменения файла теста:
- Выполнить `mvn clean compile` — убедиться, что проект компилируется.
- Выполнить `mvn test` — запустить все тесты и проверить, что они проходят.

---

## 5. Детальный план действий (порядок)

| № | Действие | Описание |
|---|---|---|
| 3.1 | Изменить `StudyingPlatformApplicationTests.java` | Расширить класс: добавить внедрение `EntityManagerFactory` и `Environment`, написать тесты на JPA-слой, живость PostgreSQL и изоляцию от `dev`. |
| 3.2 | Проверить компиляцию | Выполнить `mvn clean compile -q` из корня проекта. |
| 3.3 | Проверить запуск тестов | Выполнить `mvn test -q` из корня проекта. |
| 3.4 | Зафиксировать результат | Записать результаты проверок в отчёт. |

---

## 6. Критерии приёмки

| # | Критерий | Как проверяется |
|---|---|---|
| 6.1 | Docker запускает тестовую PostgreSQL через Testcontainers | Тестовый метод получает `Connection` и выполняет `SELECT 1` без исключений. В логах Maven — появление строки `postgres:16` (или аналогичный образ) и успешное создание контейнера. |
| 6.2 | Spring Boot-контекст загружается с профилем `test` | Метод `contextLoads` проходит без исключений; `@ActiveProfiles("test")` активируется. |
| 6.3 | JPA EntityManagerFactory настроен и не null | Тест внедряет `EntityManagerFactory` и проверяет `assertNotNull`. |
| 6.4 | Схема создаётся автоматически | `ddl-auto: create-drop` в `application-test.yml` гарантирует создание при загрузке контекста и удаление при выгрузке. Без сущей на данном этапе проверка — отсутствие ошибок при загрузке контекста. |
| 6.5 | Тестовая база изолирована от рабочей (`dev`) | Тест проверяет, что активный профиль — `test`, а имя базы содержит `studying_platform_test` (не `studying_platform`). |
| 6.6 | Тестовая база не зависит от локальных рабочих данных | Контейнер Testcontainers создаёт новую базу с нуля; `create-drop` удаляет её при завершении. Локальная база `studying_platform` не затрагивается. |
| 6.7 | `mvn clean compile` проходит без ошибок | Команда выполняется из корня проекта. |
| 6.8 | `mvn test` проходит без ошибок | Все тесты (включая новые) проходят успешно. |

---

## 7. Тест-кейсы

### TC-01: Успешная загрузка контекста с PostgreSQL через Testcontainers
- **Дано:** Spring Boot с профилем `test`, Testcontainers JDBC URL.
- **Когда:** Запускается `@SpringBootTest`.
- **Ожидаемый результат:** Контекст загружается, `DataSource` и `EntityManagerFactory` не `null`.

### TC-02: Живое соединение с контейнеризированной PostgreSQL
- **Дано:** Загруженный контекст с профилем `test`.
- **Когда:** Получается `Connection` из `DataSource` и выполняется `SELECT 1`.
- **Ожидаемый результат:** Запрос возвращается успешно, `Connection` можно закрыть.

### TC-03: Проверка активного профиля
- **Дано:** Тестовый метод с внедрённым `Environment`.
- **Когда:** Запрашивается `getActiveProfiles()`.
- **Ожидаемый результат:** В массиве присутствует `"test"`.

### TC-04: Проверка имени тестовой базы данных
- **Дано:** Тестовый метод с внедрённым `DataSource`.
- **Когда:** Считывается `Connection.getMetaData().getURL()`.
- **Ожидаемый результат:** URL содержит `studying_platform_test` (не `studying_platform`).

### TC-05: Проверка ddl-auto = create-drop
- **Дано:** Тестовый метод с внедрённым `SpringEnvironment` или доступом к `LocalContainerEntityManagerFactoryBean`.
- **Когда:** Запрашивается свойство `hibernate.hbm2ddl.auto`.
- **Ожидаемый результат:** Значение — `create-drop`.

### TC-06: Изоляция от dev-базы (негативный)
- **Дано:** Активный профиль `test`, Testcontainers.
- **Когда:** Выполняется любой SQL-запрос к базе.
- **Ожидаемый результат:** Запрос выполняется к базе `studying_platform_test`, а не `studying_platform`. Локальная база не модифицируется.

---

## 8. Команды проверки

| № | Команда | Описание |
|---|---|---|
| P-1 | `mvn clean compile -q` | Проверка компиляции проекта. Ожидается: `BUILD SUCCESS`, 0 ошибок. |
| P-2 | `mvn test -q` | Запуск всех тестов (включая `StudyingPlatformApplicationTests`). Ожидается: `BUILD SUCCESS`, все тесты `PASS`. |
| P-3 | `mvn test -q -Dtest=StudyingPlatformApplicationTests` | Запуск только целевого тестового класса. Ожидается: `BUILD SUCCESS`, все методы `PASS`. |

---

## 9. Ограничения, риски и блокеры

### Риски

| Риск | Вероятность | Влияние | Митигация |
|---|---|---|---|
| Docker не запущен или недоступен на машине исполнителя | Средняя | Критическое — тесты не могут запуститься | Зафиксировать `FAIL` с сообщением; предложить запустить Docker Desktop / daemon. |
| Проблемы с сетью — не удаётся скачать образ `postgres:16` | Низкая | Критическое — контейнер не стартует | Увеличить таймаут Testcontainers (если используется), проверить интернет-соединение. |
| Конфликт портов — другой контейнер использует порт PostgreSQL | Низкая | Среднее | Testcontainers использует случайный свободный порт, конфликтов быть не должно. |
| Кэширование контекста Spring между запусками | Низкая | Среднее — старый контекст может скрыть ошибки | `mvn clean` перед каждым запуском; Spring Boot Test кэширует контекст только в рамках одного JVM-запуска. |

### Блокеры

Нет известных блокеров. Если Docker недоступен — этап помечается как `blocked` с указанием причины.

### ADR_REQUIRED

Нет. Данный этап не требует существенных архитектурных решений. Конфигурация через `application-test.yml` и Testcontainers JDBC URL — стандартный паттерн, уже описанный в `plan.md` и подтверждённый `state.md`.

---

## 10. Изменяемые файлы

| Файл | Изменение |
|---|---|
| `src/test/java/com/studying/platform/StudyingPlatformApplicationTests.java` | Расширение: внедрение `EntityManagerFactory`, `Environment`; добавление тестовых методов TC-01..TC-05. |

**Не изменяются:** `pom.xml` (зависимости уже все необходимы), `application.yml`, `application-test.yml`, `application-dev.yml`, исходный код `src/main/`.

---

STATUS: READY
Файл плана: `stages/stage-03.md`
Сводка: Этап 03 — инфраструктура интеграционных тестов. Требуется расширить существующий `StudyingPlatformApplicationTests.java`, добавив проверки загрузки JPA-слоя (EntityManagerFactory), живости PostgreSQL через Testcontainers (SELECT 1), активного профиля `test`, имени тестовой базы (`studying_platform_test`) и изоляции от `dev`. Никаких ADR не требуется. Все зависимости уже присутствуют в `pom.xml`. Единственный изменяемый файл — `StudyingPlatformApplicationTests.java`.
