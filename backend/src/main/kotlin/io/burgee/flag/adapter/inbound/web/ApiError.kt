package io.burgee.flag.adapter.inbound.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.Instant

data class ApiError(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val fieldErrors: Map<String, String?> = emptyMap(),
)

internal fun apiError(status: HttpStatus, message: String): ResponseEntity<ApiError> =
    ResponseEntity.status(status).body(
        ApiError(status = status.value(), error = status.reasonPhrase, message = message),
    )

internal fun notFound(message: String): ResponseEntity<ApiError> =
    apiError(HttpStatus.NOT_FOUND, message)
