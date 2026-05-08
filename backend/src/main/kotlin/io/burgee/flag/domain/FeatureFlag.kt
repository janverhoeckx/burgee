package io.burgee.flag.domain

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
) {
    fun withDetails(name: String, description: String?, enabled: Boolean, now: Instant): FeatureFlag =
        copy(name = name, description = description, enabled = enabled, updatedAt = now)

    fun toggled(now: Instant): FeatureFlag =
        copy(enabled = !enabled, updatedAt = now)

    companion object {
        fun create(
            key: String,
            name: String,
            description: String?,
            enabled: Boolean,
            now: Instant = Instant.now(),
            id: UUID = UUID.randomUUID(),
        ): FeatureFlag = FeatureFlag(
            id = id,
            key = key,
            name = name,
            description = description,
            enabled = enabled,
            createdAt = now,
            updatedAt = now,
        )
    }
}
