package io.github.janverhoeckx.burgee.user.adapter.inbound.web

import io.github.janverhoeckx.burgee.user.application.port.inbound.CreateUserUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.UpdateUserUseCase
import io.github.janverhoeckx.burgee.user.domain.IdentityProvider
import io.github.janverhoeckx.burgee.user.domain.Role
import io.github.janverhoeckx.burgee.user.domain.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val subject: String,
    val email: String?,
    val displayName: String?,
    val role: Role,
    val provider: IdentityProvider,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateUserRequest(
    @field:NotBlank
    @field:Size(max = 256)
    val subject: String,

    @field:Email
    @field:Size(max = 256)
    val email: String? = null,

    @field:Size(max = 256)
    val displayName: String? = null,

    val role: Role = Role.USER,

    val provider: IdentityProvider? = null,

    @field:Size(max = 256)
    val password: String? = null,
) {
    fun toCommand(defaultProvider: IdentityProvider) = CreateUserUseCase.Command(
        subject = subject,
        email = email?.takeIf { it.isNotBlank() },
        displayName = displayName?.takeIf { it.isNotBlank() },
        role = role,
        provider = provider ?: defaultProvider,
        password = password,
    )
}

data class UpdateUserRequest(
    @field:Email
    @field:Size(max = 256)
    val email: String? = null,

    @field:Size(max = 256)
    val displayName: String? = null,

    val role: Role,

    @field:Size(max = 256)
    val password: String? = null,
) {
    fun toCommand(id: UUID) = UpdateUserUseCase.Command(
        id = id,
        email = email?.takeIf { it.isNotBlank() },
        displayName = displayName?.takeIf { it.isNotBlank() },
        role = role,
        password = password,
    )
}

fun User.toResponse() = UserResponse(
    id = id,
    subject = subject,
    email = email,
    displayName = displayName,
    role = role,
    provider = provider,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
