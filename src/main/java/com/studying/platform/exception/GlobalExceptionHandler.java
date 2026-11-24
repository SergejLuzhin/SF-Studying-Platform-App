package com.studying.platform.exception;

import com.studying.platform.dto.ErrorResponseDto;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Глобальный обработчик ошибок REST API.
 * <p>
 * Перехватывает исключения, выбрасываемые контроллерами и сервисами,
 * и преобразует их в стандартизированные ответы JSON через {@link ErrorResponseDto}.
 * Обрабатывает ошибки ресурсов, входных данных, бизнес-правил и сервера:
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

    /** Журнал непредвиденных ошибок сервера. */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Возвращает безопасное сообщение при некорректном или отсутствующем теле запроса.
     *
     * @param exception ошибка чтения тела
     * @param request контекст запроса
     * @return HTTP 400 в едином формате
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleUnreadableBody(
            HttpMessageNotReadableException exception, WebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Некорректное тело запроса", request);
    }

    /**
     * Обрабатывает неверный тип параметра или отсутствие обязательного параметра запроса.
     *
     * @param exception ошибка параметра
     * @param request контекст запроса
     * @return HTTP 400 в едином формате
     */
    @ExceptionHandler({TypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponseDto> handleInvalidParameter(Exception exception, WebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Некорректный или отсутствующий параметр запроса", request);
    }

    /**
     * Обрабатывает отсутствие маршрута или статического ресурса Spring.
     *
     * @param exception ошибка поиска маршрута или ресурса
     * @param request контекст запроса
     * @return HTTP 404 в едином формате
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponseDto> handleMissingRoute(Exception exception, WebRequest request) {
        return error(HttpStatus.NOT_FOUND, "Ресурс не найден", request);
    }

    /**
     * Создаёт стандартный безопасный ответ об ошибке.
     *
     * @param status HTTP-статус ошибки
     * @param message сообщение для клиента
     * @param request контекст запроса
     * @return ответ с указанным статусом и стандартным телом
     */
    private ResponseEntity<ErrorResponseDto> error(HttpStatus status, String message, WebRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponseDto(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), message,
                request.getDescription(false).replace("uri=", "")));
    }

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
     * Обработка исключения нарушения целостности данных.
     * <p>
     * Возвращает HTTP 400 с сообщением о нарушении ограничений базы данных
     * (например, дублирование уникального поля).
     * </p>
     *
     * @param exception исключение нарушения целостности
     * @param request   контекст запроса, содержащий путь
     * @return HTTP-ответ с кодом 400 и телом ErrorResponseDto
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception, WebRequest request) {

        return error(HttpStatus.BAD_REQUEST, "Нарушение целостности данных", request);
    }

    /**
     * Обработка общих необработанных исключений.
     * <p>
     * Сохраняет стандартные клиентские статусы Spring, а неожиданные ошибки
     * журналирует и возвращает как HTTP 500 без внутренних подробностей.
     * </p>
     *
     * @param exception любое необработанное исключение
     * @param request   контекст запроса, содержащий путь
     * @return HTTP-ответ с клиентским статусом Spring или кодом 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(
            Exception exception, WebRequest request) {

        if (exception instanceof ErrorResponse springError && springError.getStatusCode().is4xxClientError()) {
            HttpStatus status = HttpStatus.resolve(springError.getStatusCode().value());
            if (status != null) {
                return error(status, "Некорректный запрос", request);
            }
        }
        log.error("Необработанная ошибка при обработке {}", request.getDescription(false), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", request);
    }
}
