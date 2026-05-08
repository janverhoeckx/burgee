package io.burgee.web

import io.burgee.flag.DuplicateFlagKeyException
import io.burgee.flag.FlagNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class ApiError(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val fieldErrors: Map<String, String?> = emptyMap(),
)

@RestControllerAdvice
class RestExceptionHandler {

    @ExceptionHandler(FlagNotFoundException::class)
    fun handleNotFound(ex: FlagNotFoundException): ResponseEntity<ApiError> =
        error(HttpStatus.NOT_FOUND, ex.message ?: "Not found")

    @ExceptionHandler(DuplicateFlagKeyException::class)
    fun handleDuplicate(ex: DuplicateFlagKeyException): ResponseEntity<ApiError> =
        error(HttpStatus.CONFLICT, ex.message ?: "Conflict")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        val body = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Validation failed",
            fieldErrors = fieldErrors,
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    private fun error(status: HttpStatus, message: String): ResponseEntity<ApiError> {
        val body = ApiError(status = status.value(), error = status.reasonPhrase, message = message)
        return ResponseEntity.status(status).body(body)
    }
}
