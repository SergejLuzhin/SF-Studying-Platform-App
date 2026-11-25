package com.studying.platform.controller;

import com.studying.platform.dto.AnswerOptionRequestDto;
import com.studying.platform.dto.AnswerOptionResponseDto;
import com.studying.platform.entity.AnswerOption;
import com.studying.platform.service.AnswerOptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Управляет отдельными вариантами ответа. Признак правильности доступен в этом API;
 * аутентификация и авторизация в приложении не реализованы.
 */
@RestController
@RequestMapping("/api/options")
public class OptionController {
    /** Сервис управления вариантами ответов. */
    private final AnswerOptionService answerOptionService;

    /**
     * Создаёт контроллер управления вариантами.
     *
     * @param answerOptionService сервис вариантов ответа
     */
    public OptionController(AnswerOptionService answerOptionService) {
        this.answerOptionService = answerOptionService;
    }

    /**
     * Возвращает вариант ответа с признаком правильности или 404 при отсутствии.
     *
     * @param optionId идентификатор варианта
     * @return HTTP 200 с данными варианта
     */
    @GetMapping("/{optionId}")
    public ResponseEntity<AnswerOptionResponseDto> getOption(@PathVariable Long optionId) {
        return ResponseEntity.ok(toResponse(answerOptionService.findAnswerOption(optionId)));
    }

    /**
     * Обновляет обязательные текст и признак правильности варианта.
     *
     * @param optionId идентификатор варианта
     * @param dto новые текст и признак правильности
     * @return HTTP 200 с сохранёнными данными
     */
    @PutMapping("/{optionId}")
    public ResponseEntity<AnswerOptionResponseDto> updateOption(@PathVariable Long optionId,
                                                                 @Valid @RequestBody AnswerOptionRequestDto dto) {
        return ResponseEntity.ok(toResponse(answerOptionService.updateAnswerOption(optionId, dto.getText(), dto.getIsCorrect())));
    }

    /**
     * Удаляет вариант при отсутствии результатов родительского теста.
     *
     * @param optionId идентификатор варианта
     * @return HTTP 204 после удаления; при наличии результатов возвращается 400
     */
    @DeleteMapping("/{optionId}")
    public ResponseEntity<Void> deleteOption(@PathVariable Long optionId) {
        answerOptionService.deleteAnswerOption(optionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Преобразует вариант в DTO управления с идентификатором вопроса.
     *
     * @param option вариант ответа
     * @return данные варианта для административного API
     */
    private AnswerOptionResponseDto toResponse(AnswerOption option) {
        return new AnswerOptionResponseDto(option.getId(), option.getQuestion().getId(), option.getText(), option.getIsCorrect());
    }
}
