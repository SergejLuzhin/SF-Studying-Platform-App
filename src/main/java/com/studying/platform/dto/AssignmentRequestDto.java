package com.studying.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO для создания или обновления задания через REST API.
 * <p>
 * Содержит необязательный идентификатор урока, название, описание, срок выполнения
 * и максимальный балл. При создании идентификатор задаётся путём запроса;
 * если он дополнительно указан в теле, он должен совпадать с путём.
 * При обновлении идентификатор из тела игнорируется и родитель не меняется.
 * Валидация выполняется с помощью Jakarta Bean
 * Validation до момента передачи данных в сервисный слой.
 * </p>
 */
public class AssignmentRequestDto {

    /**
     * Необязательный идентификатор урока для совместимости существующих запросов.
     */
    private Long lessonId;

    /**
     * Название задания. Обязательно для заполнения.
     */
    @NotBlank(message = "Название задания не может быть пустым")
    @Size(max = 200, message = "Название задания не должно превышать 200 символов")
    private String title;

    /**
     * Подробное описание задания и требования к выполнению.
     * Может быть пустым или null.
     */
    private String description;

    /**
     * Дата окончания выполнения задания. Необязательно.
     */
    private LocalDate dueDate;

    /**
     * Максимальный балл за выполнение задания. Необязательно.
     */
    private Integer maxScore;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public AssignmentRequestDto() {
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param lessonId    идентификатор урока
     * @param title       название задания
     * @param description описание задания
     * @param dueDate     срок выполнения
     * @param maxScore    максимальный балл
     */
    public AssignmentRequestDto(Long lessonId, String title, String description,
                                LocalDate dueDate, Integer maxScore) {
        this.lessonId = lessonId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.maxScore = maxScore;
    }

    /**
     * Возвращает идентификатор урока.
     *
     * @return идентификатор урока
     */
    public Long getLessonId() {
        return lessonId;
    }

    /**
     * Устанавливает идентификатор урока.
     *
     * @param lessonId идентификатор урока
     */
    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    /**
     * Возвращает название задания.
     *
     * @return название задания
     */
    public String getTitle() {
        return title;
    }

    /**
     * Устанавливает название задания.
     *
     * @param title название задания
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Возвращает описание задания.
     *
     * @return описание задания
     */
    public String getDescription() {
        return description;
    }

    /**
     * Устанавливает описание задания.
     *
     * @param description описание задания
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Возвращает дату окончания выполнения задания.
     *
     * @return дата окончания
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Устанавливает дату окончания выполнения задания.
     *
     * @param dueDate дата окончания
     */
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Возвращает максимальный балл за задание.
     *
     * @return максимальный балл
     */
    public Integer getMaxScore() {
        return maxScore;
    }

    /**
     * Устанавливает максимальный балл за задание.
     *
     * @param maxScore максимальный балл
     */
    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }
}
