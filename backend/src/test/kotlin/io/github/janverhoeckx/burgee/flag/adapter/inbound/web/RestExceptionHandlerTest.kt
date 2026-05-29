package io.github.janverhoeckx.burgee.flag.adapter.inbound.web

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

class RestExceptionHandlerTest {

    private val handler = RestExceptionHandler()

    @Test
    fun `validation errors map to 400 with field errors`() {
        val bindingResult = BeanPropertyBindingResult(Any(), "request").apply {
            addError(FieldError("request", "key", "must not be blank"))
            addError(FieldError("request", "name", "must not be blank"))
        }
        val ex = mockk<MethodArgumentNotValidException>()
        every { ex.bindingResult } returns bindingResult

        val response = handler.handleValidation(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.fieldErrors).containsEntry("key", "must not be blank")
        assertThat(response.body?.fieldErrors).containsEntry("name", "must not be blank")
    }
}
