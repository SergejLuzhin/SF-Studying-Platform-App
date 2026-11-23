package com.studying.platform.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO для ответа REST API с полной структурой курса.
 * <p>
 * Содержит базовые данные курса и вложенные DTO модулей с уроками.
 * Используется эндпоинтом получения структуры курса.
 * </p>
 */
public class CourseStructureResponseDto {

    /**
     * Идентификатор курса.
     */
    private Long id;

    /**
     * Название курса.
     */
    private String title;

    /**
     * Описание курса.
     */
    private String description;

    /**
     * Продолжительность курса в часах.
     */
    private Integer duration;

    /**
     * Дата начала курса.
     */
    private LocalDate startDate;

    /**
     * Идентификатор преподавателя.
     */
    private Long teacherId;

    /**
     * Идентификатор категории.
     */
    private Long categoryId;

    /**
     * Список модулей курса.
     */
    private List<ModuleStructureDto> modules;

    /**
     * Публичный конструктор по умолчанию, требуемый десериализацией JSON.
     */
    public CourseStructureResponseDto() {
        this.modules = new ArrayList<>();
    }

    /**
     * Создаёт DTO с заданными параметрами.
     *
     * @param id            идентификатор курса
     * @param title         название курса
     * @param description   описание курса
     * @param duration      продолжительность в часах
     * @param startDate     дата начала курса
     * @param teacherId     идентификатор преподавателя
     * @param categoryId    идентификатор категории
     * @param modules       список модулей с уроками
     */
    public CourseStructureResponseDto(Long id, String title, String description, Integer duration,
                                      LocalDate startDate, Long teacherId, Long categoryId,
                                      List<ModuleStructureDto> modules) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.duration = duration;
        this.startDate = startDate;
        this.teacherId = teacherId;
        this.categoryId = categoryId;
        this.modules = modules != null ? modules : new ArrayList<>();
    }

    /**
     * Возвращает идентификатор курса.
     *
     * @return идентификатор
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор курса.
     *
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает название курса.
     *
     * @return название курса
     */
    public String getTitle() {
        return title;
    }

    /**
     * Устанавливает название курса.
     *
     * @param title название курса
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Возвращает описание курса.
     *
     * @return описание курса
     */
    public String getDescription() {
        return description;
    }

    /**
     * Устанавливает описание курса.
     *
     * @param description описание курса
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Возвращает продолжительность курса в часах.
     *
     * @return продолжительность
     */
    public Integer getDuration() {
        return duration;
    }

    /**
     * Устанавливает продолжительность курса в часах.
     *
     * @param duration продолжительность
     */
    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    /**
     * Возвращает дату начала курса.
     *
     * @return дата начала
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Устанавливает дату начала курса.
     *
     * @param startDate дата начала
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Возвращает идентификатор преподавателя.
     *
     * @return идентификатор преподавателя
     */
    public Long getTeacherId() {
        return teacherId;
    }

    /**
     * Устанавливает идентификатор преподавателя.
     *
     * @param teacherId идентификатор преподавателя
     */
    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    /**
     * Возвращает идентификатор категории.
     *
     * @return идентификатор категории
     */
    public Long getCategoryId() {
        return categoryId;
    }

    /**
     * Устанавливает идентификатор категории.
     *
     * @param categoryId идентификатор категории
     */
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * Возвращает список модулей курса.
     *
     * @return список модулей
     */
    public List<ModuleStructureDto> getModules() {
        return modules;
    }

    /**
     * Устанавливает список модулей курса.
     *
     * @param modules список модулей
     */
    public void setModules(List<ModuleStructureDto> modules) {
        this.modules = modules;
    }

    /**
     * Вложенный DTO для представления модуля с уроками в структуре курса.
     */
    public static class ModuleStructureDto {

        /**
         * Идентификатор модуля.
         */
        private Long id;

        /**
         * Название модуля.
         */
        private String title;

        /**
         * Описание модуля.
         */
        private String description;

        /**
         * Порядковый номер модуля в курсе.
         */
        private Integer orderIndex;

        /**
         * Список уроков модуля.
         */
        private List<LessonStructureDto> lessons;

        /**
         * Публичный конструктор по умолчанию.
         */
        public ModuleStructureDto() {
            this.lessons = new ArrayList<>();
        }

        /**
         * Создаёт DTO модуля с заданными параметрами.
         *
         * @param id          идентификатор модуля
         * @param title       название модуля
         * @param description описание модуля
         * @param orderIndex  порядковый номер
         * @param lessons     список уроков
         */
        public ModuleStructureDto(Long id, String title, String description,
                                  Integer orderIndex, List<LessonStructureDto> lessons) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.orderIndex = orderIndex;
            this.lessons = lessons != null ? lessons : new ArrayList<>();
        }

        /**
         * Возвращает идентификатор модуля.
         *
         * @return идентификатор
         */
        public Long getId() {
            return id;
        }

        /**
         * Устанавливает идентификатор модуля.
         *
         * @param id идентификатор
         */
        public void setId(Long id) {
            this.id = id;
        }

        /**
         * Возвращает название модуля.
         *
         * @return название модуля
         */
        public String getTitle() {
            return title;
        }

        /**
         * Устанавливает название модуля.
         *
         * @param title название модуля
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * Возвращает описание модуля.
         *
         * @return описание модуля
         */
        public String getDescription() {
            return description;
        }

        /**
         * Устанавливает описание модуля.
         *
         * @param description описание модуля
         */
        public void setDescription(String description) {
            this.description = description;
        }

        /**
         * Возвращает порядковый номер модуля.
         *
         * @return порядковый номер
         */
        public Integer getOrderIndex() {
            return orderIndex;
        }

        /**
         * Устанавливает порядковый номер модуля.
         *
         * @param orderIndex порядковый номер
         */
        public void setOrderIndex(Integer orderIndex) {
            this.orderIndex = orderIndex;
        }

        /**
         * Возвращает список уроков модуля.
         *
         * @return список уроков
         */
        public List<LessonStructureDto> getLessons() {
            return lessons;
        }

        /**
         * Устанавливает список уроков модуля.
         *
         * @param lessons список уроков
         */
        public void setLessons(List<LessonStructureDto> lessons) {
            this.lessons = lessons;
        }
    }

    /**
     * Вложенный DTO для представления урока в структуре модуля.
     */
    public static class LessonStructureDto {

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
         * Публичный конструктор по умолчанию.
         */
        public LessonStructureDto() {
        }

        /**
         * Создаёт DTO урока с заданными параметрами.
         *
         * @param id       идентификатор урока
         * @param title    название урока
         * @param content  содержание урока
         * @param videoUrl ссылка на видео
         */
        public LessonStructureDto(Long id, String title, String content, String videoUrl) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.videoUrl = videoUrl;
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
    }
}
