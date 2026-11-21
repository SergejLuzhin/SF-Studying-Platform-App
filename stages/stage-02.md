# Этап 02: Настройка проекта, Spring Boot и БД

## 1. Цель и границы этапа
Настроить базовую инфраструктуру проекта: инициализация `pom.xml` (Maven, Java 23, Spring Boot), создание точек входа, настройка профилей (`dev`, `test`), подключение Spring Data JPA и PostgreSQL. Обеспечить управление чувствительными параметрами через переменные окружения. Подготовить инфраструктуру для запуска PostgreSQL через Testcontainers в тестовом профиле.

**Границы:**
- Только настройка конфигурации и зависимостей.
- Не реализуем бизнес-логику, сущности или контроллеры.
- Не настраиваем аутентификацию, email и аналитику.
- Не пишем ADR (архитектурное решение тривиально и укладывается в план).

## 2. Связанные файлы

### Исходные файлы (создаются/изменяются)
1. `pom.xml` — обновление зависимостей Spring Boot, плагинов и профиля сборки.
2. `src/main/java/com/studying/platform/StudyingPlatformApplication.java` — точка входа (`@SpringBootApplication`).
3. `src/test/java/com/studying/platform/StudyingPlatformApplicationTests.java` — базовый тест контекста и Testcontainers.

### Конфигурационные файлы (создаются)
4. `src/main/resources/application.yml` — базовая конфигурация (общие параметры, профили).
5. `src/main/resources/application-dev.yml` — параметры для локальной разработки (`ddl-auto: update`).
6. `src/main/resources/application-test.yml` — параметры для тестирования (подключение к Testcontainers).

### Документация
- `docs/adr/README.md` — **не изменяется** (нет новых существенных архитектурных решений).

## 3. Порядок действий

### Шаг 1. Обновление `pom.xml`
- Установить родителя `spring-boot-starter-parent` (версия 3.2.x или новее, совместимая с Java 23).
- Указать `<java.version>23</java.version>`.
- Добавить зависимости:
  - `spring-boot-starter-web` (основа для будущего REST API).
  - `spring-boot-starter-data-jpa` (работа с БД).
  - `postgresql` (драйвер PostgreSQL).
  - `spring-boot-starter-test` (JUnit 5, Mockito, Testcontainers).
- Убедиться, что плагин `maven-compiler-plugin` настроен на Java 23 (или наследуется от родителя).
- Добавить плагин `spring-boot-maven-plugin` для удобного запуска `mvn spring-boot:run`.

### Шаг 2. Создание точки входа
- Создать пакет `com.studying.platform` в `src/main/java`.
- Создать класс `StudyingPlatformApplication` с аннотацией `@SpringBootApplication`.
- Добавить метод `main` для запуска.
- Добавить JavaDoc на русском языке.

### Шаг 3. Создание конфигурации профилей (`application.yml`)
- Определить профиль по умолчанию (`dev`), если приложение запускается локально без явного указания.
- Указать базовые настройки логирования (например, `info` уровень).
- Использовать переменные окружения для URL, логина и пароля БД:
  ```yaml
  spring:
    datasource:
      url: ${DB_URL:jdbc:postgresql://localhost:5432/studying_platform}
      username: ${DB_USER:postgres}
      password: ${DB_PASSWORD:postgres}
  ```

### Шаг 4. Создание профиля `dev` (`application-dev.yml`)
- Настроить `spring.jpa.hibernate.ddl-auto: update` (для локальной разработки и автосоздания схемы).
- Включить `show-sql: true` (опционально, для отладки).
- Убедиться, что URL БД по умолчанию указывает на `localhost:5432`.

### Шаг 5. Создание профиля `test` (`application-test.yml`)
- Настроить подключение к БД, управляемой Testcontainers.
- Spring Boot автоматически подхватывает контейнеры, если настроен `@Testcontainers` в тестах, но параметры DataSource должны указывать на динамически созданные хост и порт:
  ```yaml
  spring:
    datasource:
      url: jdbc:tc:postgresql:16:///studying_platform_test
      username: postgres
      password: postgres
    jpa:
      hibernate:
        ddl-auto: create-drop # В тестах схема уничтожается при остановке
      show-sql: true
  ```
*Примечание:* Использование JDBC-URL `jdbc:tc:...` — это современный способ запуска Testcontainers PostgreSQL напрямую через Spring Boot без дополнительного кода конфигурации в тестах для базовых сценариев.

### Шаг 6. Создание теста `StudyingPlatformApplicationTests.java`
- Использовать аннотации `@SpringBootTest`, `@ActiveProfiles("test")`.
- Добавить `@Testcontainers` и `@DockerComposeOption.NONE` (так как используем `jdbc:tc:`).
- Написать тест на инициализацию контекста (Spring Boot запустит контейнер, создаст схему и остановит контейнер).
- Добавить JavaDoc.

## 4. Критерии приёмки

| # | Критерий | PASS / FAIL |
|---|---|---|
| 1 | `mvn clean compile` завершается без ошибок | [ ] |
| 2 | `mvn test` запускается и все тесты проходят успешно | [ ] |
| 3 | Конфигурация профилей читается корректно (Dev/Test) | [ ] |
| 4 | Чувствительные данные (пароли) не зашиты в YAML-файлы, используются переменные окружения | [ ] |
| 5 | PostgreSQL поднимается через Testcontainers при запуске `mvn test` | [ ] |
| 6 | `spring.jpa.hibernate.ddl-auto: update` работает в `dev` (проверяется запуском приложения в dev-профиле, если доступен локальный PostgreSQL) | [ ] |

## 5. Команды проверки
Все команды выполняются в корне проекта (`Studying-Platform`):
1. Проверка сборки:
   ```bash
   mvn clean compile
   ```
2. Запуск тестов (включая Testcontainers):
   ```bash
   mvn clean test
   ```
3. Проверка запуска приложения в dev-профиле (требует запущенный локальный PostgreSQL на порту 5432):
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

## 6. Ограничения, риски и блокеры

- **Риск:** Отсутствие Docker на машине исполнителя. Testcontainers критически зависят от Docker. Если Docker недоступен, тесты завершатся с ошибкой `DockerException`.
  *Решение:* Проверить наличие Docker перед запуском `mvn test`.
- **Риск:** Несовместимость версий Spring Boot и Java 23.
  *Решение:* Использовать Spring Boot 3.2.4 или новее.
- **ADR_REQUIRED:** Нет. Настройка Maven и Spring Boot не является существенным архитектурным решением, требующим документирования ADR.
