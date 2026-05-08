package io.burgee.flag.adapter.outbound.persistence

import io.burgee.flag.domain.FeatureFlag

internal fun FeatureFlagRow.toDomain(): FeatureFlag = FeatureFlag(
    id = id,
    key = key,
    name = name,
    description = description,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun FeatureFlag.toRow(newRecord: Boolean): FeatureFlagRow = FeatureFlagRow(
    id = id,
    key = key,
    name = name,
    description = description,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
    newRecord = newRecord,
)
