package io.github.janverhoeckx.burgee.flag.adapter.inbound.web

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        return validationFailed(fieldErrors)
    }

    @ExceptionHandler(InvalidEvaluationContextException::class)
    fun handleInvalidEvaluationContext(ex: InvalidEvaluationContextException): ResponseEntity<ApiError> =
        validationFailed(ex.fieldErrors)
}
