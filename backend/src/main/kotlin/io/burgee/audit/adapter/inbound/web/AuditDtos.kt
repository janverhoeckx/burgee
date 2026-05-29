package io.burgee.audit.adapter.inbound.web

import io.burgee.audit.domain.AuditEntry
import java.time.Instant
import java.util.UUID

data class AuditEntryResponse(
    val id: UUID,
    val flagId: UUID,
    val flagKey: String,
    val action: String,
    val actor: String,
    val detail: String?,
    val occurredAt: Instant,
)

fun AuditEntry.toResponse() = AuditEntryResponse(
    id = id,
    flagId = flagId,
    flagKey = flagKey,
    action = action.name,
    actor = actor,
    detail = detail,
    occurredAt = occurredAt,
)
