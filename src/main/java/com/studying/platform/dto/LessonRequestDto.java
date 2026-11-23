package com.studying.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для создания или обновления урока модуля через REST API.
 * <p>
 * Содержит обязательное поле «название». Содержимое и ссылка на видео
 * являются опциональными. Валидация выполняется с помощью Jakarta Bean Validation.
 * </p>
 */
public class LessonRequestDto {

    /**
     * Название урока. Обязательно для заполнения.
     */
    @NotBlank(message = "Название урока не может быть пустым")
    @Size(max = 200, message = "Название урока не должно превышать 200 символов")
    private String title;

    /**
     * Текстовое содержание урока. Может быть пустым или null.
     */
    private String content;

    /**
     * Ссылка на видео-материал. Может быть null.
     */
    private String videoUrl;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public LessonRequestDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param title    название урока
     * @param content  содержание урока
     * @param videoUrl ссылка на видео
     */
    public LessonRequestDto(String title, String content, String videoUrl) {
        this.title = title;
        this.content = content;
        this.videoUrl = videoUrl;
    }

    /**
     * Возвращает название урока.
     *
     * @return название урока
     */
    public String getTitle() {
        return title;
    }

    /**
     * Устанавливает название урока.
     *
     * @param title название урока
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Возвращает содержание урока.
     *
     * @return содержание
     */
    public String getContent() {
        return content;
    }

    /**
     * Устанавливает содержание урока.
     *
     * @param content содержание
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Возвращает ссылку на видео.
     *
     * @return ссылка на видео
     */
    public String getVideoUrl() {
        return videoUrl;
    }

    /**
     * Устанавливает ссылку на видео.
     *
     * @param videoUrl ссылка на видео
     */
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
