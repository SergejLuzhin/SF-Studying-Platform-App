package com.studying.platform.dto;

import java.time.LocalDateTime;

/**
 * DTO для единого формата ответа при ошибках REST API.
 * <p>
 * Используется глобальным обработчиком ошибок {@code GlobalExceptionHandler}
 * для возвращения структурированной информации об ошибке в теле HTTP-ответа.
 * Содержит метку времени, код состояния, тип ошибки, сообщение и путь запроса.
 * </p>
 */
public class ErrorResponseDto {

    /**
     * Метка времени возникновения ошибки.
     */
    private LocalDateTime timestamp;

    /**
     * Код состояния HTTP.
     */
    private int status;

    /**
     * Краткое описание типа ошибки.
     */
    private String error;

    /**
     * Подробное сообщение об ошибке.
     */
    private String message;

    /**
     * Путь запроса, вызвавшего ошибку.
     */
    private String path;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public ErrorResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param timestamp метка времени
     * @param status    код состояния HTTP
     * @param error     тип ошибки
     * @param message   сообщение об ошибке
     * @param path      путь запроса
     */
    public ErrorResponseDto(LocalDateTime timestamp, int status, String error,
                            String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    /**
     * Возвращает метку времени возникновения ошибки.
     *
     * @return метка времени
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Устанавливает метку времени возникновения ошибки.
     *
     * @param timestamp метка времени
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Возвращает код состояния HTTP.
     *
     * @return код состояния
     */
    public int getStatus() {
        return status;
    }

    /**
     * Устанавливает код состояния HTTP.
     *
     * @param status код состояния
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Возвращает краткое описание типа ошибки.
     *
     * @return тип ошибки
     */
    public String getError() {
        return error;
    }

    /**
     * Устанавливает краткое описание типа ошибки.
     *
     * @param error тип ошибки
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Возвращает подробное сообщение об ошибке.
     *
     * @return сообщение об ошибке
     */
    public String getMessage() {
        return message;
    }

    /**
     * Устанавливает подробное сообщение об ошибке.
     *
     * @param message сообщение об ошибке
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Возвращает путь запроса, вызвавшего ошибку.
     *
     * @return путь запроса
     */
    public String getPath() {
        return path;
    }

    /**
     * Устанавливает путь запроса, вызвавшего ошибку.
     *
     * @param path путь запроса
     */
    public void setPath(String path) {
        this.path = path;
    }
}
