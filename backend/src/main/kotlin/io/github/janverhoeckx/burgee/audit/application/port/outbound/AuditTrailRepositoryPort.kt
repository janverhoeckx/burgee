package io.github.janverhoeckx.burgee.audit.application.port.outbound

import io.github.janverhoeckx.burgee.audit.domain.AuditEntry
import java.util.UUID

interface AuditTrailRepositoryPort {
    fun save(entry: AuditEntry): AuditEntry
    fun findAll(): List<AuditEntry>
    fun findByFlagId(flagId: UUID): List<AuditEntry>
}
