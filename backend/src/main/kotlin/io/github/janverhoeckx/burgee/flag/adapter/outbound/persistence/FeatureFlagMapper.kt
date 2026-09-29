package io.github.janverhoeckx.burgee.flag.adapter.outbound.persistence

import io.github.janverhoeckx.burgee.flag.domain.Condition
import io.github.janverhoeckx.burgee.flag.domain.ConditionOperator
import io.github.janverhoeckx.burgee.flag.domain.FeatureFlag

internal fun FeatureFlagRow.toDomain(): FeatureFlag = FeatureFlag(
    id = rowId,
    key = key,
    name = name,
    description = description,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
    conditions = conditions.map { it.toDomain() },
)

internal fun FeatureFlag.toRow(newRecord: Boolean): FeatureFlagRow = FeatureFlagRow(
    rowId = id,
    key = key,
    name = name,
    description = description,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
    conditions = conditions.map { it.toRow() },
).also { it.newRecord = newRecord }

private fun FlagConditionRow.toDomain(): Condition =
    Condition.of(attribute, ConditionOperator.valueOf(operator), values)

private fun Condition.toRow(): FlagConditionRow =
    FlagConditionRow(attribute = attribute, operator = operator.name, values = values)
