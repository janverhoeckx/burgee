package io.github.janverhoeckx.burgee.user.adapter.inbound.web

import io.github.janverhoeckx.burgee.user.domain.IdentityProvider
import io.github.janverhoeckx.burgee.user.domain.Role
import io.github.janverhoeckx.burgee.user.domain.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class UserDtosTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val id = UUID.fromString("44444444-4444-4444-4444-444444444444")

    private val user = User(
        id = id,
        subject = "jane",
        email = "jane@example.com",
        displayName = "Jane",
        role = Role.ADMIN,
        passwordHash = "secret-hash",
        provider = IdentityProvider.BASIC,
        createdAt = now,
        updatedAt = now.plusSeconds(60),
    )

    @Test
    fun `toResponse copies fields and omits the password hash`() {
        val response = user.toResponse()

        assertThat(response).isEqualTo(
            UserResponse(
                id = id,
                subject = "jane",
                email = "jane@example.com",
                displayName = "Jane",
                role = Role.ADMIN,
                provider = IdentityProvider.BASIC,
                createdAt = now,
                updatedAt = now.plusSeconds(60),
            ),
        )
    }

    @Test
    fun `CreateUserRequest builds command and uses the explicit provider`() {
        val command = CreateUserRequest(
            subject = "ops",
            email = "ops@example.com",
            displayName = "Ops",
            role = Role.ADMIN,
            provider = IdentityProvider.BASIC,
            password = "secret",
        ).toCommand(defaultProvider = IdentityProvider.JWT)

        assertThat(command.subject).isEqualTo("ops")
        assertThat(command.email).isEqualTo("ops@example.com")
        assertThat(command.displayName).isEqualTo("Ops")
        assertThat(command.role).isEqualTo(Role.ADMIN)
        assertThat(command.provider).isEqualTo(IdentityProvider.BASIC)
        assertThat(command.password).isEqualTo("secret")
    }

    @Test
    fun `CreateUserRequest falls back to the default provider when none given`() {
        val command = CreateUserRequest(subject = "ops").toCommand(defaultProvider = IdentityProvider.JWT)

        assertThat(command.provider).isEqualTo(IdentityProvider.JWT)
        assertThat(command.role).isEqualTo(Role.USER)
    }

    @Test
    fun `CreateUserRequest normalizes blank email and displayName to null`() {
        val command = CreateUserRequest(
            subject = "ops",
            email = "   ",
            displayName = "",
        ).toCommand(defaultProvider = IdentityProvider.BASIC)

        assertThat(command.email).isNull()
        assertThat(command.displayName).isNull()
    }

    @Test
    fun `UpdateUserRequest builds command with the provided id and blanks normalized`() {
        val command = UpdateUserRequest(
            email = "  ",
            displayName = "Renamed",
            role = Role.USER,
            password = null,
        ).toCommand(id)

        assertThat(command.id).isEqualTo(id)
        assertThat(command.email).isNull()
        assertThat(command.displayName).isEqualTo("Renamed")
        assertThat(command.role).isEqualTo(Role.USER)
        assertThat(command.password).isNull()
    }
}
