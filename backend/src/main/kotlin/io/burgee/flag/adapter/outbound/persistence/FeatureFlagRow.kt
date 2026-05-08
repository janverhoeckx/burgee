package io.burgee.flag.adapter.outbound.persistence

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("feature_flags")
data class FeatureFlagRow(
    @Id
    override val id: UUID,
    val key: String,
    val name: String,
    val description: String?,
    val enabled: Boolean,
    @Column("created_at")
    val createdAt: Instant,
    @Column("updated_at")
    val updatedAt: Instant,
    @Transient
    val newRecord: Boolean = false,
) : Persistable<UUID> {
    override fun isNew(): Boolean = newRecord
}
