package io.github.janverhoeckx.burgee.user.adapter.outbound.persistence

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("users")
data class UserRow(
    @Id
    @Column("id")
    val rowId: UUID,
    val subject: String,
    val email: String?,
    @Column("display_name")
    val displayName: String?,
    val role: String,
    @Column("password_hash")
    val passwordHash: String?,
    val provider: String,
    @Column("created_at")
    val createdAt: Instant,
    @Column("updated_at")
    val updatedAt: Instant,
) : Persistable<UUID> {
    @Transient
    var newRecord: Boolean = false

    override fun getId(): UUID = rowId

    override fun isNew(): Boolean = newRecord
}
