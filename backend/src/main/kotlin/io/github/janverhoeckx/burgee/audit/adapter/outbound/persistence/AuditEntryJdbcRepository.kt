package io.github.janverhoeckx.burgee.audit.adapter.outbound.persistence

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface AuditEntryJdbcRepository : CrudRepository<AuditEntryRow, UUID> {

    @Query("SELECT * FROM audit_entries ORDER BY occurred_at DESC")
    fun findAllOrderedByOccurredAt(): List<AuditEntryRow>

    @Query("SELECT * FROM audit_entries WHERE flag_id = :flagId ORDER BY occurred_at DESC")
    fun findByFlagIdOrderedByOccurredAt(flagId: UUID): List<AuditEntryRow>
}
