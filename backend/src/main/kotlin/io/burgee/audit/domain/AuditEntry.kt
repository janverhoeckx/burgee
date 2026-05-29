package io.burgee.audit.domain

import java.time.Instant
import java.util.UUID

data class AuditEntry(
    val id: UUID,
    val flagId: UUID,
    val flagKey: String,
    val action: AuditAction,
    val actor: String,
    val detail: String?,
    val occurredAt: Instant,
) {
    companion object {
        fun create(
            action: AuditAction,
            flagId: UUID,
            flagKey: String,
            actor: String,
            detail: String?,
            now: Instant = Instant.now(),
            id: UUID = UUID.randomUUID(),
        ): AuditEntry = AuditEntry(
            id = id,
            flagId = flagId,
            flagKey = flagKey,
            action = action,
            actor = actor,
            detail = detail,
            occurredAt = now,
        )
    }
}
