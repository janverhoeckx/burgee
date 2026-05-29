package io.github.janverhoeckx.burgee.user.adapter.outbound.persistence

import io.github.janverhoeckx.burgee.user.domain.IdentityProvider
import io.github.janverhoeckx.burgee.user.domain.Role
import io.github.janverhoeckx.burgee.user.domain.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional
import java.util.UUID

class UserPersistenceAdapterTest {

    private val jdbc = mockk<UserJdbcRepository>()
    private val adapter = UserPersistenceAdapter(jdbc)

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val id = UUID.fromString("33333333-3333-3333-3333-333333333333")
    private val user = User(
        id = id,
        subject = "jane",
        email = "jane@example.com",
        displayName = "Jane",
        role = Role.USER,
        passwordHash = null,
        provider = IdentityProvider.OAUTH2,
        createdAt = now,
        updatedAt = now,
    )
    private val row = user.toRow(newRecord = false)

    @Test
    fun `findAll maps rows to domain in repository order`() {
        val other = user.copy(id = UUID.randomUUID(), subject = "amy").toRow(newRecord = false)
        every { jdbc.findAllOrderedBySubject() } returns listOf(other, row)

        val result = adapter.findAll()

        assertThat(result).extracting<String> { it.subject }.containsExactly("amy", "jane")
    }

    @Test
    fun `findById returns mapped domain when present`() {
        every { jdbc.findById(id) } returns Optional.of(row)

        assertThat(adapter.findById(id)).isEqualTo(user)
    }

    @Test
    fun `findById returns null when absent`() {
        every { jdbc.findById(id) } returns Optional.empty()

        assertThat(adapter.findById(id)).isNull()
    }

    @Test
    fun `findBySubject returns mapped domain when present`() {
        every { jdbc.findBySubject("jane") } returns row

        assertThat(adapter.findBySubject("jane")).isEqualTo(user)
    }

    @Test
    fun `findBySubject returns null when absent`() {
        every { jdbc.findBySubject("missing") } returns null

        assertThat(adapter.findBySubject("missing")).isNull()
    }

    @Test
    fun `existsBySubject delegates to repository`() {
        every { jdbc.existsBySubject("jane") } returns true

        assertThat(adapter.existsBySubject("jane")).isTrue()
    }

    @Test
    fun `save marks row as new when id is unknown`() {
        every { jdbc.existsById(id) } returns false
        val captured = slot<UserRow>()
        every { jdbc.save(capture(captured)) } answers { captured.captured }

        val result = adapter.save(user)

        assertThat(captured.captured.isNew()).isTrue()
        assertThat(result).isEqualTo(user)
    }

    @Test
    fun `save marks row as existing when id is known`() {
        every { jdbc.existsById(id) } returns true
        val captured = slot<UserRow>()
        every { jdbc.save(capture(captured)) } answers { captured.captured }

        adapter.save(user)

        assertThat(captured.captured.isNew()).isFalse()
    }

    @Test
    fun `deleteById delegates to repository`() {
        every { jdbc.deleteById(id) } returns Unit

        adapter.deleteById(id)

        verify { jdbc.deleteById(id) }
    }
}
