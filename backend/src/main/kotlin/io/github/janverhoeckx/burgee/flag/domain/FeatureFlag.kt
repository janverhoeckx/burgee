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
    val targetingRule: TargetingRule = TargetingRule(),
) {
    fun withDetails(
        name: String,
        description: String?,
        enabled: Boolean,
        targetingRule: TargetingRule,
        now: Instant,
    ): FeatureFlag =
        copy(name = name, description = description, enabled = enabled, targetingRule = targetingRule, updatedAt = now)

    fun toggled(now: Instant): FeatureFlag =
        copy(enabled = !enabled, updatedAt = now)

    /** Evaluation: `enabled AND the Targeting Rule matches` for one Evaluation Context. */
    fun evaluate(context: EvaluationContext): Evaluation =
        Evaluation(flagKey = key, result = enabled && targetingRule.matches(context))

    companion object {
        fun create(
            key: String,
            name: String,
            description: String?,
            enabled: Boolean,
            now: Instant = Instant.now(),
            id: UUID = UUID.randomUUID(),
            targetingRule: TargetingRule = TargetingRule(),
        ): FeatureFlag = FeatureFlag(
            id = id,
            key = key,
            name = name,
            description = description,
            enabled = enabled,
            createdAt = now,
            updatedAt = now,
            targetingRule = targetingRule,
        )
    }
}
