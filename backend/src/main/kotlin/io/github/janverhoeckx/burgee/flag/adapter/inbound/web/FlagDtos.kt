package io.github.janverhoeckx.burgee.flag.adapter.inbound.web

import io.github.janverhoeckx.burgee.flag.application.port.inbound.CreateFlagUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.UpdateFlagUseCase
import io.github.janverhoeckx.burgee.flag.domain.Condition
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
    val conditions: List<ConditionDto>,
)

/**
 * A Condition on the admin API: `{ "attribute": "organisationId", "operator": "IN", "values": ["acme"] }`.
 * Fields are nullable so that missing or `null` input reaches the domain (see `TargetingRule.parse`),
 * whose violations come back as 400 field errors instead of a JSON parse error.
 */
data class ConditionDto(
    val attribute: String?,
    val operator: String?,
    val values: List<String?>?,
) {
    fun toInput() = Condition.Input(attribute = attribute, operator = operator, values = values)
}

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

    /** The Targeting Rule. Optional; defaults to no Conditions (on for everyone when enabled). */
    val conditions: List<ConditionDto> = emptyList(),
) {
    fun toCommand() = CreateFlagUseCase.Command(
        key = key,
        name = name,
        description = description,
        enabled = enabled,
        conditions = conditions.map { it.toInput() },
    )
}

data class UpdateFeatureFlagRequest(
    @field:NotBlank
    @field:Size(max = 256)
    val name: String,

    @field:Size(max = 4000)
    val description: String? = null,

    val enabled: Boolean,

    /** The new Targeting Rule. Replaces the current one entirely; omitting it clears all Conditions. */
    val conditions: List<ConditionDto> = emptyList(),
) {
    fun toCommand(id: UUID) = UpdateFlagUseCase.Command(
        id = id,
        name = name,
        description = description,
        enabled = enabled,
        conditions = conditions.map { it.toInput() },
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
    conditions = targetingRule.conditions.map { it.toDto() },
)

fun Condition.toDto() = ConditionDto(attribute = attribute, operator = operator.name, values = values)
