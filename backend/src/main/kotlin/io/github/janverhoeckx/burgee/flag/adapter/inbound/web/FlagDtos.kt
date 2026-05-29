package io.github.janverhoeckx.burgee.flag.adapter.inbound.web

import io.github.janverhoeckx.burgee.flag.application.port.inbound.CreateFlagUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.UpdateFlagUseCase
import io.github.janverhoeckx.burgee.flag.domain.FeatureFlag
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class FeatureFlagResponse(
    val id: UUID,
    val key: String,
    val name: String,
    val description: String?,
    val enabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class PublicFlagResponse(
    val key: String,
    val enabled: Boolean,
)

data class CreateFeatureFlagRequest(
    @field:NotBlank
    @field:Size(max = 128)
    @field:Pattern(regexp = "^[a-z0-9][a-z0-9._-]*$", message = "key must be lowercase alphanumeric with . _ -")
    val key: String,

    @field:NotBlank
    @field:Size(max = 256)
    val name: String,

    @field:Size(max = 4000)
    val description: String? = null,

    val enabled: Boolean = false,
) {
    fun toCommand() = CreateFlagUseCase.Command(
        key = key,
        name = name,
        description = description,
        enabled = enabled,
    )
}

data class UpdateFeatureFlagRequest(
    @field:NotBlank
    @field:Size(max = 256)
    val name: String,

    @field:Size(max = 4000)
    val description: String? = null,

    val enabled: Boolean,
) {
    fun toCommand(id: UUID) = UpdateFlagUseCase.Command(
        id = id,
        name = name,
        description = description,
        enabled = enabled,
    )
}

fun FeatureFlag.toResponse() = FeatureFlagResponse(
    id = id,
    key = key,
    name = name,
    description = description,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun FeatureFlag.toPublic() = PublicFlagResponse(key = key, enabled = enabled)
