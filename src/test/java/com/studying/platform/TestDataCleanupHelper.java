package com.studying.platform;

import jakarta.persistence.EntityManager;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Общий помощник для безопасной очистки тестовых данных в интеграционных тестах.
 * <p>
 * Удаляет созданные тестовые данные в правильном порядке зависимостей
 * с использованием JPQL bulk-удаления, что гарантирует отсутствие
 * каскадных операций JPA и корректную работу вне транзакции теста.
 * </p>
 * <p>
 * Порядок удаления: отзывы → записи → уроки → модули → курсы → категории → пользователи.
 * Для пользователей удаление производится по префиксу email,
 * что гарантирует адресное удаление только данных текущего теста
 * и не затрагивает посторонние тестовые данные.
 * Для записей на курс удаление производится по ID курсов
 * и email пользователей, что позволяет удалять записи,
 * когда их собственные ID не отслеживаются.
 * </p>
 * <p>
 * Каждая операция удаления проверяет, что соответствующая коллекция ID
 * не пуста, чтобы избежать ненужных запросов к БД при пропуске этапа создания данных.
 * </p>
 *
 * @see EntityManager
 * @see TransactionTemplate
 */
@Component
class TestDataCleanupHelper {

    /**
     * Очищает тестовые данные в порядке зависимостей.
     * <p>
     * Удаляет созданные тестовые данные в обратном порядке зависимостей:
     * отзывы, записи, уроки, модули, курсы, категории и пользователи.
     * Для пользователей удаление производится по префиксу email.
     * Для записей на курс удаление производится по ID курсов
     * и email пользователей, что позволяет удалять записи,
     * когда их собственные ID не отслеживаются.
     * </p>
     * <p>
     * Каждая операция пропускается, если соответствующая коллекция ID
     * пуста или null.
     * </p>
     *
     * @param entityManager менеджер сущностей для выполнения JPQL-удалений
     * @param transactionTemplate шаблон транзакции для оборачивания очистки
     * @param reviewCourseIds идентификаторы курсов, на которых оставлены отзывы
     * @param enrollmentCourseIds идентификаторы курсов, на которых есть записи
     * @param enrollmentUserEmails email пользователей с записями
     * @param lessonIds идентификаторы уроков
     * @param moduleIds идентификаторы модулей
     * @param courseIds идентификаторы курсов
     * @param categoryIds идентификаторы категорий
     * @param userEmailPrefix префикс email для удаления пользователей текущего теста
     */
    void clearTestEntities(
            EntityManager entityManager,
            TransactionTemplate transactionTemplate,
            Set<Long> reviewCourseIds,
            Set<Long> enrollmentCourseIds,
            Set<String> enrollmentUserEmails,
            Set<Long> lessonIds,
            Set<Long> moduleIds,
            Set<Long> courseIds,
            Set<Long> categoryIds,
            String userEmailPrefix) {

        transactionTemplate.executeWithoutResult(status -> {
            // 1. Удаляем отзывы о курсах (зависят от course)
            if (reviewCourseIds != null && !reviewCourseIds.isEmpty()) {
                entityManager.createQuery("DELETE FROM CourseReview cr WHERE cr.course.id IN (:ids)")
                        .setParameter("ids", reviewCourseIds)
                        .executeUpdate();
            }

            // 2. Удаляем записи на курс по курсам и email пользователей
            if (enrollmentCourseIds != null && !enrollmentCourseIds.isEmpty()) {
                entityManager.createQuery("DELETE FROM Enrollment e WHERE e.course.id IN (:courseIds)")
                        .setParameter("courseIds", enrollmentCourseIds)
                        .executeUpdate();
            }
            if (enrollmentUserEmails != null && !enrollmentUserEmails.isEmpty()) {
                entityManager.createQuery("DELETE FROM Enrollment e WHERE e.user.email IN (:emails)")
                        .setParameter("emails", enrollmentUserEmails)
                        .executeUpdate();
            }

            // 3. Удаляем уроки (зависят от module)
            if (lessonIds != null && !lessonIds.isEmpty()) {
                entityManager.createQuery("DELETE FROM Lesson l WHERE l.id IN (:ids)")
                        .setParameter("ids", lessonIds)
                        .executeUpdate();
            }

            // 4. Удаляем модули (зависят от course)
            if (moduleIds != null && !moduleIds.isEmpty()) {
                entityManager.createQuery("DELETE FROM Module m WHERE m.id IN (:ids)")
                        .setParameter("ids", moduleIds)
                        .executeUpdate();
            }

            // 5. Удаляем курсы (зависят от teacher и category)
            if (courseIds != null && !courseIds.isEmpty()) {
                entityManager.createQuery("DELETE FROM Course c WHERE c.id IN (:ids)")
                        .setParameter("ids", courseIds)
                        .executeUpdate();
            }

            // 6. Удаляем категории
            if (categoryIds != null && !categoryIds.isEmpty()) {
                entityManager.createQuery("DELETE FROM Category cat WHERE cat.id IN (:ids)")
                        .setParameter("ids", categoryIds)
                        .executeUpdate();
            }

            // 7. Удаляем пользователей по префиксу email
            if (userEmailPrefix != null && !userEmailPrefix.isEmpty()) {
                entityManager.createQuery("DELETE FROM User u WHERE u.email LIKE :prefix")
                        .setParameter("prefix", userEmailPrefix + "%")
                        .executeUpdate();
            }
        });
    }
}
