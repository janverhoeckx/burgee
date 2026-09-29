package io.github.janverhoeckx.burgee.flag.domain

import java.time.Instant
import java.util.UUID

data class FeatureFlag(
    val id: UUID,
    val key: String,
    val name: String,
    val description: String?,
    val enabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** The Targeting Rule: every Condition must match for an enabled flag to evaluate to true. */
    val conditions: List<Condition> = emptyList(),
) {
    init {
        val attributes = conditions.map { it.attribute }
        require(attributes.size == attributes.toSet().size) {
            "A Targeting Rule cannot hold two Conditions on the same Attribute: $attributes"
        }
    }

    fun withDetails(
        name: String,
        description: String?,
        enabled: Boolean,
        now: Instant,
        conditions: List<Condition> = this.conditions,
    ): FeatureFlag =
        copy(name = name, description = description, enabled = enabled, conditions = conditions, updatedAt = now)

    fun toggled(now: Instant): FeatureFlag =
        copy(enabled = !enabled, updatedAt = now)

    /** Evaluation: the flag's true/false result for one Evaluation Context. */
    fun evaluate(context: EvaluationContext): Boolean =
        enabled && conditions.all { it.matches(context) }

    companion object {
        fun create(
            key: String,
            name: String,
            description: String?,
            enabled: Boolean,
            now: Instant = Instant.now(),
            id: UUID = UUID.randomUUID(),
            conditions: List<Condition> = emptyList(),
        ): FeatureFlag = FeatureFlag(
            id = id,
            key = key,
            name = name,
            description = description,
            enabled = enabled,
            createdAt = now,
            updatedAt = now,
            conditions = conditions,
        )
    }
}
