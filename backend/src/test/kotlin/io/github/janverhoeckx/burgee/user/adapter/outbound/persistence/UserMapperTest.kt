package io.github.janverhoeckx.burgee.user.adapter.outbound.persistence

import io.github.janverhoeckx.burgee.user.domain.IdentityProvider
import io.github.janverhoeckx.burgee.user.domain.Role
import io.github.janverhoeckx.burgee.user.domain.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class UserMapperTest {

    private val createdAt = Instant.parse("2026-01-01T12:00:00Z")
    private val updatedAt = createdAt.plusSeconds(60)
    private val id = UUID.fromString("22222222-2222-2222-2222-222222222222")

    private val domain = User(
        id = id,
        subject = "jane",
        email = "jane@example.com",
        displayName = "Jane",
        role = Role.ADMIN,
        passwordHash = "hash",
        provider = IdentityProvider.BASIC,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    @Test
    fun `toRow with newRecord flag stores enum names and preserves fields`() {
        val row = domain.toRow(newRecord = true)

        assertThat(row.rowId).isEqualTo(id)
        assertThat(row.subject).isEqualTo("jane")
        assertThat(row.email).isEqualTo("jane@example.com")
        assertThat(row.displayName).isEqualTo("Jane")
        assertThat(row.role).isEqualTo("ADMIN")
        assertThat(row.passwordHash).isEqualTo("hash")
        assertThat(row.provider).isEqualTo("BASIC")
        assertThat(row.createdAt).isEqualTo(createdAt)
        assertThat(row.updatedAt).isEqualTo(updatedAt)
        assertThat(row.isNew()).isTrue()
    }

    @Test
    fun `toRow with newRecord false marks as existing`() {
        assertThat(domain.toRow(newRecord = false).isNew()).isFalse()
    }

    @Test
    fun `toDomain reverses toRow`() {
        val roundTripped = domain.toRow(newRecord = false).toDomain()

        assertThat(roundTripped).isEqualTo(domain)
    }

    @Test
    fun `toDomain parses role and provider enums`() {
        val row = domain.copy(role = Role.NEW, provider = IdentityProvider.FIREBASE).toRow(newRecord = false)

        val mapped = row.toDomain()

        assertThat(mapped.role).isEqualTo(Role.NEW)
        assertThat(mapped.provider).isEqualTo(IdentityProvider.FIREBASE)
    }

    @Test
    fun `null email displayName and passwordHash map both directions`() {
        val sparse = domain.copy(email = null, displayName = null, passwordHash = null)

        val row = sparse.toRow(newRecord = false)
        assertThat(row.email).isNull()
        assertThat(row.displayName).isNull()
        assertThat(row.passwordHash).isNull()

        assertThat(row.toDomain()).isEqualTo(sparse)
    }
}
