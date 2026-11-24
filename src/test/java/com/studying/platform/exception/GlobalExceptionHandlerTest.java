package com.studying.platform.exception;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Проверяет безопасное представление неожиданных ошибок контроллера.
 */
class GlobalExceptionHandlerTest {
    /**
     * Проверяет HTTP 500 и отсутствие внутреннего текста исключения в JSON.
     *
     * @throws Exception если выполнение запроса завершилось ошибкой
     */
    @Test
    void unexpectedExceptionReturnsSafeServerError() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/test-error")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Внутренняя ошибка сервера"))
                .andExpect(jsonPath("$.path").value("/test-error"));
    }

    /**
     * Тестовый контроллер, имитирующий неожиданную ошибку приложения.
     */
    @RestController
    static class FailingController {
        /**
         * Выбрасывает исключение с внутренними подробностями.
         *
         * @throws UnsupportedOperationException всегда, для проверки обработчика
         */
        @GetMapping("/test-error")
        public void fail() {
            throw new UnsupportedOperationException("секретные внутренние данные");
        }
    }
}
