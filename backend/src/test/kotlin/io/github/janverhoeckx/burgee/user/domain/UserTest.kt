package io.github.janverhoeckx.burgee.user.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class UserTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val later = now.plusSeconds(60)
    private val id = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `create populates fields and stamps both timestamps`() {
        val user = User.create(
            subject = "google-sub",
            email = "a@example.com",
            displayName = "Ada",
            role = Role.NEW,
            provider = IdentityProvider.JWT,
            now = now,
            id = id,
        )

        assertThat(user.id).isEqualTo(id)
        assertThat(user.subject).isEqualTo("google-sub")
        assertThat(user.email).isEqualTo("a@example.com")
        assertThat(user.displayName).isEqualTo("Ada")
        assertThat(user.role).isEqualTo(Role.NEW)
        assertThat(user.provider).isEqualTo(IdentityProvider.JWT)
        assertThat(user.passwordHash).isNull()
        assertThat(user.createdAt).isEqualTo(now)
        assertThat(user.updatedAt).isEqualTo(now)
    }

    @Test
    fun `withRole changes role and refreshes updatedAt while preserving createdAt`() {
        val original = User.create("s", null, null, Role.NEW, IdentityProvider.JWT, now = now, id = id)

        val promoted = original.withRole(Role.ADMIN, later)

        assertThat(promoted.role).isEqualTo(Role.ADMIN)
        assertThat(promoted.createdAt).isEqualTo(now)
        assertThat(promoted.updatedAt).isEqualTo(later)
    }

    @Test
    fun `withProfile replaces email and displayName and refreshes updatedAt`() {
        val original = User.create("s", "old@example.com", "Old", Role.USER, IdentityProvider.JWT, now = now, id = id)

        val refreshed = original.withProfile("new@example.com", "New", later)

        assertThat(refreshed.email).isEqualTo("new@example.com")
        assertThat(refreshed.displayName).isEqualTo("New")
        assertThat(refreshed.role).isEqualTo(Role.USER)
        assertThat(refreshed.updatedAt).isEqualTo(later)
    }

    @Test
    fun `withPasswordHash sets the hash and refreshes updatedAt`() {
        val original = User.create("s", null, null, Role.ADMIN, IdentityProvider.BASIC, now = now, id = id)

        val secured = original.withPasswordHash("hashed", later)

        assertThat(secured.passwordHash).isEqualTo("hashed")
        assertThat(secured.updatedAt).isEqualTo(later)
    }

    @Test
    fun `copy helpers leave the original instance unchanged`() {
        val original = User.create("s", "e@example.com", "Name", Role.NEW, IdentityProvider.JWT, now = now, id = id)

        original.withRole(Role.ADMIN, later)
        original.withProfile("other@example.com", "Other", later)
        original.withPasswordHash("h", later)

        assertThat(original.role).isEqualTo(Role.NEW)
        assertThat(original.email).isEqualTo("e@example.com")
        assertThat(original.passwordHash).isNull()
        assertThat(original.updatedAt).isEqualTo(now)
    }
}
