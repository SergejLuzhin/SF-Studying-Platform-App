package com.studying.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Сущность «Курс» учебной платформы.
 * <p>
 * Представляет учебный курс, содержащий информацию о названии, описании,
 * продолжительности и дате начала. Курс привязан к преподавателю
 * (пользователю с ролью TEACHER) и категории. Содержит структурированное
 * содержание в виде модулей, уроков и заданий.
 * </p>
 */
@Entity
@Table(name = "courses")
public class Course {

    /**
     * Идентификатор курса. Генерируется автоматически базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название курса. Обязательно для заполнения.
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Подробное описание курса. Хранится в формате TEXT.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Продолжительность курса в часах. Должна быть не менее 1.
     * Валидация диапазона выполняется на уровне бизнес-логики.
     */
    @Column(nullable = false)
    private Integer duration;

    /**
     * Дата начала обучения. Обязательно для заполнения.
     */
    @Column(nullable = false)
    private LocalDate startDate;

    /**
     * Преподаватель курса. Владелец связи «многие-к-одному» с {@link User}.
     * Загружается лениво для оптимизации производительности.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_course_teacher"))
    private User teacher;

    /**
     * Категория курса. Владелец связи «многие-к-одному» с {@link Category}.
     * Загружается сразу (EAGER), поскольку категория — небольшая справочная сущность.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_course_category"))
    private Category category;

    /**
     * Модули курса. Загружаются лениво, каскадные операции PERSIST, MERGE, REMOVE включены.
     * Владелец обратной связи — сущность {@link Module}.
     */
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY,
            cascade = {jakarta.persistence.CascadeType.PERSIST,
                    jakarta.persistence.CascadeType.MERGE,
                    jakarta.persistence.CascadeType.REMOVE},
            orphanRemoval = true)
    private Set<Module> modules = new HashSet<>();

    /**
     * Записи студентов на курс. Загружаются лениво.
     * Владелец обратной связи — сущность {@link Enrollment}.
     */
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private Set<Enrollment> enrollments = new HashSet<>();

    /**
     * Отзывы о курсе. Загружаются лениво.
     * Владелец обратной связи — сущность {@link CourseReview}.
     */
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private Set<CourseReview> reviews = new HashSet<>();

    /**
     * Теги курса. Загружаются лениво, каскадное сохранение и обновление включено.
     * Связь реализуется через вспомогательную таблицу {@code course_tag}.
     */
    @ManyToMany(fetch = FetchType.LAZY,
            cascade = {jakarta.persistence.CascadeType.PERSIST,
                    jakarta.persistence.CascadeType.MERGE})
    @JoinTable(name = "course_tag",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    /**
     * Публичный конструктор по умолчанию, требуемый JPA-провайдером.
     */
    public Course() {
    }

    /**
     * Создаёт курс с заданными параметрами.
     *
     * @param title       название курса
     * @param description описание курса
     * @param duration    продолжительность в часах
     * @param startDate   дата начала курса
     * @param teacher     преподаватель, ведущий курс
     * @param category    категория курса
     */
    public Course(String title, String description, Integer duration,
                  LocalDate startDate, User teacher, Category category) {
        this.title = title;
        this.description = description;
        this.duration = duration;
        this.startDate = startDate;
        this.teacher = teacher;
        this.category = category;
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
     * Возвращает преподавателя курса.
     *
     * @return преподаватель
     */
    public User getTeacher() {
        return teacher;
    }

    /**
     * Устанавливает преподавателя курса.
     *
     * @param teacher преподаватель
     */
    public void setTeacher(User teacher) {
        this.teacher = teacher;
    }

    /**
     * Возвращает категорию курса.
     *
     * @return категория курса
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Устанавливает категорию курса.
     *
     * @param category категория курса
     */
    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * Возвращает коллекцию модулей курса.
     *
     * @return коллекция модулей
     */
    public Set<Module> getModules() {
        return modules;
    }

    /**
     * Устанавливает коллекцию модулей курса.
     *
     * @param modules коллекция модулей
     */
    public void setModules(Set<Module> modules) {
        this.modules = modules;
    }

    /**
     * Возвращает коллекцию записей студентов на курс.
     *
     * @return коллекция записей
     */
    public Set<Enrollment> getEnrollments() {
        return enrollments;
    }

    /**
     * Устанавливает коллекцию записей студентов на курс.
     *
     * @param enrollments коллекция записей
     */
    public void setEnrollments(Set<Enrollment> enrollments) {
        this.enrollments = enrollments;
    }

    /**
     * Возвращает коллекцию отзывов о курсе.
     *
     * @return коллекция отзывов
     */
    public Set<CourseReview> getReviews() {
        return reviews;
    }

    /**
     * Устанавливает коллекцию отзывов о курсе.
     *
     * @param reviews коллекция отзывов
     */
    public void setReviews(Set<CourseReview> reviews) {
        this.reviews = reviews;
    }

    /**
     * Возвращает коллекцию тегов курса.
     *
     * @return коллекция тегов
     */
    public Set<Tag> getTags() {
        return tags;
    }

    /**
     * Устанавливает коллекцию тегов курса.
     *
     * @param tags коллекция тегов
     */
    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }
}
