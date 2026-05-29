package io.github.janverhoeckx.burgee.audit.application.port.inbound

import io.github.janverhoeckx.burgee.audit.domain.AuditAction
import io.github.janverhoeckx.burgee.audit.domain.AuditEntry
import java.util.UUID

interface RecordAuditEntryUseCase {
    data class Command(
        val action: AuditAction,
        val flagId: UUID,
        val flagKey: String,
        val detail: String?,
    )

    fun record(command: Command)
}

interface ListAuditTrailUseCase {
    fun list(): List<AuditEntry>

    fun listForFlag(flagId: UUID): List<AuditEntry>
}
