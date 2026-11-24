package com.studying.platform.dto;

/**
 * Краткие сведения о курсе из списка активных курсов студента.
 *
 * @param id идентификатор курса, не идентификатор записи на курс
 * @param title название курса
 * @param description описание курса, при отсутствии {@code null}
 */
public record CourseSummaryDto(Long id, String title, String description) {
}
