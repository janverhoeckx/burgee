package io.github.janverhoeckx.burgee.audit.adapter.outbound.persistence

import io.github.janverhoeckx.burgee.audit.application.port.outbound.AuditTrailRepositoryPort
import io.github.janverhoeckx.burgee.audit.domain.AuditEntry
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class AuditTrailPersistenceAdapter(
    private val jdbc: AuditEntryJdbcRepository,
) : AuditTrailRepositoryPort {

    override fun save(entry: AuditEntry): AuditEntry =
        jdbc.save(entry.toRow()).toDomain()

    override fun findAll(): List<AuditEntry> =
        jdbc.findAllOrderedByOccurredAt().map { it.toDomain() }

    override fun findByFlagId(flagId: UUID): List<AuditEntry> =
        jdbc.findByFlagIdOrderedByOccurredAt(flagId).map { it.toDomain() }
}
