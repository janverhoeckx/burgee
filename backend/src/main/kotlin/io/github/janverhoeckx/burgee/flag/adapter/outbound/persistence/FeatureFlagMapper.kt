package io.github.janverhoeckx.burgee.flag.adapter.outbound.persistence

import io.github.janverhoeckx.burgee.flag.domain.FeatureFlag

internal fun FeatureFlagRow.toDomain(): FeatureFlag = FeatureFlag(
    id = rowId,
    key = key,
    name = name,
    description = description,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun FeatureFlag.toRow(newRecord: Boolean): FeatureFlagRow = FeatureFlagRow(
    rowId = id,
    key = key,
    name = name,
    description = description,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
).also { it.newRecord = newRecord }
