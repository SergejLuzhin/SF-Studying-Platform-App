package com.studying.platform.dto;

/**
 * DTO для ответа REST API с данными урока модуля.
 * <p>
 * Содержит идентификатор, название, содержание, ссылку на видео
 * и идентификатор модуля. Используется в телах успешных ответов GET-запросов.
 * </p>
 */
public class LessonResponseDto {

    /**
     * Идентификатор урока.
     */
    private Long id;

    /**
     * Название урока.
     */
    private String title;

    /**
     * Текстовое содержание урока.
     */
    private String content;

    /**
     * Ссылка на видео-материал.
     */
    private String videoUrl;

    /**
     * Идентификатор модуля, которому принадлежит урок.
     */
    private Long moduleId;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public LessonResponseDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id        идентификатор урока
     * @param title     название урока
     * @param content   содержание урока
     * @param videoUrl  ссылка на видео
     * @param moduleId  идентификатор модуля
     */
    public LessonResponseDto(Long id, String title, String content,
                             String videoUrl, Long moduleId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.videoUrl = videoUrl;
        this.moduleId = moduleId;
    }

    /**
     * Возвращает идентификатор урока.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор урока.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
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

    /**
     * Возвращает идентификатор модуля.
     *
     * @return идентификатор модуля
     */
    public Long getModuleId() {
        return moduleId;
    }

    /**
     * Устанавливает идентификатор модуля.
     *
     * @param moduleId идентификатор модуля
     */
    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
    }
}
