package com.studying.platform.exception;

/**
 * Исключение, выбрасываемое при отсутствии запрашиваемого ресурса.
 * <p>
 * Используется в сервисном слое для сигнализации об отсутствии
 * сущности по заданному идентификатору (например, курс, модуль, пользователь не найден).
 * Является проверенной ошибкой времени выполнения и наследуется от {@code RuntimeException}.
 * </p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Создаёт новое исключение с заданным сообщением.
     *
     * @param message описание причины исключения
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Создаёт новое исключение с заданным сообщением и причиной.
     *
     * @param message описание причины исключения
     * @param cause   основная причина исключения
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
