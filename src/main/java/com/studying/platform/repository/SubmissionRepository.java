package com.studying.platform.repository;

import com.studying.platform.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для сущности «Решение» {@link Submission}.
 * <p>
 * Обеспечивает стандартные CRUD-операции и кастомные выборки решений
 * по заданию, студенту и комбинации задание + студент.
 * </p>
 *
 * @see Submission
 */
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /**
     * Находит все решения заданного задания.
     *
     * @param assignmentId идентификатор задания
     * @return список решений задания
     */
    List<Submission> findByAssignmentId(Long assignmentId);

    /**
     * Находит все решения данного студента.
     *
     * @param studentId идентификатор студента
     * @return список решений студента
     */
    List<Submission> findByStudentId(Long studentId);

    /**
     * Находит решение конкретного студента для конкретного задания.
     * <p>
     * Используется для проверки наличия уже отправленного решения
     * перед новой отправкой. Возвращает пустое значение, если решение не найдено.
     * </p>
     *
     * @param assignmentId идентификатор задания
     * @param studentId    идентификатор студента
     * @return обязательное значение с решением, если найдено; пустое значение — если нет
     */
    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
}
