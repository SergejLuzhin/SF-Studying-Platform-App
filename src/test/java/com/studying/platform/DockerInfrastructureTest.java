package com.studying.platform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные проверки инфраструктуры Docker и CI.
 * <p>
 * Верифицирует существование и корректность файлов контейнеризации:
 * Dockerfile, docker-compose.yml, профиля docker, CI-конфига GitHub Actions
 * и отсутствие конфликтов с .gitignore.
 */
class DockerInfrastructureTest {

    private static final Path ROOT = Paths.get(System.getProperty("user.dir"));

    // -----------------------------------------------------------------------
    // Dockerfile
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Dockerfile должен существовать в корне проекта")
    void dockerfileShouldExist() {
        Path dockerfile = ROOT.resolve("Dockerfile");
        assertTrue(Files.exists(dockerfile), "Dockerfile не найден в корне проекта");
        assertTrue(Files.isRegularFile(dockerfile), "Dockerfile не является обычным файлом");
    }

    @Test
    @DisplayName("Dockerfile должен содержать базовый образ eclipse-temurin:23-jre")
    void dockerfileShouldContainBaseImage() throws IOException {
        String content = Files.readString(ROOT.resolve("Dockerfile"));
        assertTrue(content.contains("eclipse-temurin:23-jre"),
                "Dockerfile должен содержать образ eclipse-temurin:23-jre");
    }

    @Test
    @DisplayName("Dockerfile должен содержать EXPOSE 8080")
    void dockerfileShouldExposePort() throws IOException {
        String content = Files.readString(ROOT.resolve("Dockerfile"));
        assertTrue(content.contains("EXPOSE 8080"),
                "Dockerfile должен содержать EXPOSE 8080");
    }

    @Test
    @DisplayName("Dockerfile должен содержать ENTRYPOINT java -jar")
    void dockerfileShouldContainEntrypoint() throws IOException {
        String content = Files.readString(ROOT.resolve("Dockerfile"));
        assertTrue(content.contains("ENTRYPOINT"),
                "Dockerfile должен содержать ENTRYPOINT");
        assertTrue(content.contains("-jar"),
                "Dockerfile должен содержать -jar в ENTRYPOINT");
    }

    @Test
    @DisplayName("Dockerfile должен содержать HEALTHCHECK")
    void dockerfileShouldContainHealthcheck() throws IOException {
        String content = Files.readString(ROOT.resolve("Dockerfile"));
        assertTrue(content.contains("HEALTHCHECK"),
                "Dockerfile должен содержать HEALTHCHECK");
    }

    // -----------------------------------------------------------------------
    // docker-compose.yml
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("docker-compose.yml должен существовать в корне проекта")
    void dockerComposeShouldExist() {
        Path compose = ROOT.resolve("docker-compose.yml");
        assertTrue(Files.exists(compose), "docker-compose.yml не найден в корне проекта");
    }

    @Test
    @DisplayName("docker-compose.yml должен содержать сервис postgres с образом postgres:16")
    void dockerComposeShouldContainPostgres() throws IOException {
        String content = Files.readString(ROOT.resolve("docker-compose.yml"));
        assertTrue(content.contains("postgres:16"),
                "docker-compose.yml должен содержать образ postgres:16");
        assertTrue(content.contains("healthcheck"),
                "docker-compose.yml должен содержать healthcheck для postgres");
    }

    @Test
    @DisplayName("docker-compose.yml должен содержать сервис app с build и портом 8080")
    void dockerComposeShouldContainAppService() throws IOException {
        String content = Files.readString(ROOT.resolve("docker-compose.yml"));
        assertTrue(content.contains("build:"),
                "docker-compose.yml должен содержать build для app");
        assertTrue(content.contains("8080:8080"),
                "docker-compose.yml должен пробрасывать порт 8080:8080");
        assertTrue(content.contains("depends_on"),
                "docker-compose.yml должен содержать depends_on");
        assertTrue(content.contains("service_healthy"),
                "docker-compose.yml должен использовать condition: service_healthy");
    }

    // -----------------------------------------------------------------------
    // application-docker.yml
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("application-docker.yml должен существовать в resources")
    void dockerProfileShouldExist() {
        Path profile = ROOT.resolve("src/main/resources/application-docker.yml");
        assertTrue(Files.exists(profile), "application-docker.yml не найден в src/main/resources/");
    }

    @Test
    @DisplayName("application-docker.yml должен содержать URL БД с хостом postgres")
    void dockerProfileShouldContainPostgresHost() throws IOException {
        Path profile = ROOT.resolve("src/main/resources/application-docker.yml");
        String content = Files.readString(profile);
        assertTrue(content.contains("postgres:5432"),
                "application-docker.yml должен содержать URL jdbc:postgresql://postgres:5432/");
    }

    @Test
    @DisplayName("application-docker.yml должен содержать ddl-auto: update")
    void dockerProfileShouldContainDdlUpdate() throws IOException {
        Path profile = ROOT.resolve("src/main/resources/application-docker.yml");
        String content = Files.readString(profile);
        assertTrue(content.contains("update"),
                "application-docker.yml должен содержать ddl-auto: update");
    }

    // -----------------------------------------------------------------------
    // CI configuration
    // -----------------------------------------------------------------------

    @Test
    @DisplayName(".github/workflows/ci.yml должен существовать")
    void ciConfigShouldExist() {
        Path ci = ROOT.resolve(".github/workflows/ci.yml");
        assertTrue(Files.exists(ci), ".github/workflows/ci.yml не найден");
    }

    @Test
    @DisplayName("ci.yml должен содержать шаг setup-java с JDK 23")
    void ciShouldSetupJava23() throws IOException {
        Path ci = ROOT.resolve(".github/workflows/ci.yml");
        String content = Files.readString(ci);
        assertTrue(content.contains("java-version: '23'"),
                "ci.yml должен устанавливать JDK 23");
        assertTrue(content.contains("temurin"),
                "ci.yml должен использовать дистрибуцию temurin");
    }

    @Test
    @DisplayName("ci.yml должен запускать mvn test с профилем test")
    void ciShouldRunTests() throws IOException {
        Path ci = ROOT.resolve(".github/workflows/ci.yml");
        String content = Files.readString(ci);
        assertTrue(content.contains("test"),
                "ci.yml должен запускать тесты Maven");
        assertTrue(content.contains("-Ptest"),
                "ci.yml должен активировать профиль test для Testcontainers");
    }

    // -----------------------------------------------------------------------
    // .gitignore
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Dockerfile, docker-compose.yml и .github не должны быть в .gitignore")
    void infrastructureFilesNotIgnored() throws IOException {
        Path gitignore = ROOT.resolve(".gitignore");
        if (!Files.exists(gitignore)) {
            fail(".gitignore не найден — невозможно проверить игнорирование файлов");
            return;
        }

        String gitignoreContent = Files.readString(gitignore);

        // Проверяем, что имена файлов не указаны как шаблоны игнорирования
        // (строки, начинающиеся с / или без спецсимволов, которые совпадают с именем файла)
        assertTrue(gitignoreContent.lines()
                        .filter(line -> !line.trim().startsWith("#") && !line.trim().isEmpty())
                        .noneMatch(pattern -> pattern.equals("Dockerfile")
                                || pattern.equals("docker-compose.yml")
                                || pattern.equals(".github/")
                                || pattern.equals(".github")),
                "Dockerfile, docker-compose.yml и .github не должны быть в .gitignore");
    }

    // -----------------------------------------------------------------------
    // Maven build compatibility
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Сборка Maven должна успешно завершаться (clean compile)")
    void mavenCompileShouldSucceed() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "mvn", "-B", "clean", "compile");
        pb.directory(ROOT.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        assertEquals(0, exitCode,
                "Maven compile завершился с ошибкой. Вывод:\n" +
                        drainStream(process.getInputStream()));
    }

    private String drainStream(java.io.InputStream is) throws IOException {
        return new String(is.readAllBytes());
    }
}
