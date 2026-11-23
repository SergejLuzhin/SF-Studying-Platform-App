package com.studying.platform.exception;

import com.studying.platform.dto.ErrorResponseDto;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Глобальный обработчик ошибок REST API.
 * <p>
 * Перехватывает исключения, выбрасываемые контроллерами и сервисами,
 * и преобразует их в стандартизированные ответы JSON через {@link ErrorResponseDto}.
 * Обрабатывает четыре типа ошибок:
 * </p>
 * <ul>
 *   <li>ResourceNotFoundException — ошибка 404 Not Found</li>
 *   <li>MethodArgumentNotValidException — ошибка 400 Bad Request при невалидных входных данных</li>
 *   <li>IllegalStateException — ошибка 400 Bad Request при нарушении бизнес-правил</li>
 *   <li>Generic Exception — ошибка 500 Internal Server Error</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обработка исключения «ресурс не найден».
     * <p>
     * Возвращает HTTP 404 с сообщением об отсутствующем ресурсе.
     * </p>
     *
     * @param exception исключение, выброшенное при отсутствии ресурса
     * @param request   контекст запроса, содержащий путь
     * @return HTTP-ответ с кодом 404 и телом ErrorResponseDto
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(
            ResourceNotFoundException exception, WebRequest request) {

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                exception.getMessage(),
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Обработка исключения валидации входных аргументов.
     * <p>
     * Возвращает HTTP 400 с объединённым сообщением обо всех
     * нарушениях ограничений валидации.
     * </p>
     *
     * @param exception исключение валидации
     * @param request   контекст запроса, содержащий путь
     * @return HTTP-ответ с кодом 400 и телом ErrorResponseDto
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception, WebRequest request) {

        String messages = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                messages,
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Обработка исключения некорректных аргументов.
     * <p>
     * Возвращает HTTP 400 с сообщением о некорректном вводе,
     * который не был перехвачен валидацией полей (например,
     * некорректная продолжительность курса).
     * </p>
     *
     * @param exception исключение некорректных аргументов
     * @param request   контекст запроса, содержащий путь
     * @return HTTP-ответ с кодом 400 и телом ErrorResponseDto
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(
            IllegalArgumentException exception, WebRequest request) {

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Обработка исключения нарушения бизнес-правил.
     * <p>
     * Возвращает HTTP 400 с сообщением о нарушенном бизнес-правиле
     * (например, дублирование записи, некорректное состояние).
     * </p>
     *
     * @param exception исключение нарушения бизнес-правил
     * @param request   контекст запроса, содержащий путь
     * @return HTTP-ответ с кодом 400 и телом ErrorResponseDto
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalStateException(
            IllegalStateException exception, WebRequest request) {

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Обработка общих необработанных исключений.
     * <p>
     * Возвращает HTTP 500 с общим сообщением об ошибке сервера.
     * В продакшн-среде рекомендуется логировать исключение.
     * </p>
     *
     * @param exception любое необработанное исключение
     * @param request   контекст запроса, содержащий путь
     * @return HTTP-ответ с кодом 500 и телом ErrorResponseDto
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(
            Exception exception, WebRequest request) {

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Внутренняя ошибка сервера",
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
