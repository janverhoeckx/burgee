package io.github.janverhoeckx.burgee.audit.adapter.outbound.persistence

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("audit_entries")
data class AuditEntryRow(
    @Id
    @Column("id")
    val rowId: UUID,
    @Column("flag_id")
    val flagId: UUID,
    @Column("flag_key")
    val flagKey: String,
    val action: String,
    val actor: String,
    val detail: String?,
    @Column("occurred_at")
    val occurredAt: Instant,
) : Persistable<UUID> {
    @Transient
    var newRecord: Boolean = false

    override fun getId(): UUID = rowId

    override fun isNew(): Boolean = newRecord
}
