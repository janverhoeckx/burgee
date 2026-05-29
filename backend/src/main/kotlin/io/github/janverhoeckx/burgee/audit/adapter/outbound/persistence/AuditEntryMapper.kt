package io.github.janverhoeckx.burgee.audit.adapter.outbound.persistence

import io.github.janverhoeckx.burgee.audit.domain.AuditAction
import io.github.janverhoeckx.burgee.audit.domain.AuditEntry

internal fun AuditEntryRow.toDomain(): AuditEntry = AuditEntry(
    id = rowId,
    flagId = flagId,
    flagKey = flagKey,
    action = AuditAction.valueOf(action),
    actor = actor,
    detail = detail,
    occurredAt = occurredAt,
)

internal fun AuditEntry.toRow(): AuditEntryRow = AuditEntryRow(
    rowId = id,
    flagId = flagId,
    flagKey = flagKey,
    action = action.name,
    actor = actor,
    detail = detail,
    occurredAt = occurredAt,
).also { it.newRecord = true }
