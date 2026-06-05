package io.github.janverhoeckx.burgee.user.application.service

import io.github.janverhoeckx.burgee.user.application.port.inbound.CreateUserUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.DeleteUserUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.EnsureBootstrapAdminUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.GetUserByIdUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.ResolveOrProvisionUserUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.UpdateUserUseCase
import io.github.janverhoeckx.burgee.user.application.port.outbound.PasswordHasher
import io.github.janverhoeckx.burgee.user.application.port.outbound.UserRepositoryPort
import io.github.janverhoeckx.burgee.user.domain.IdentityProvider
import io.github.janverhoeckx.burgee.user.domain.Role
import io.github.janverhoeckx.burgee.user.domain.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class UserServiceTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val repository = mockk<UserRepositoryPort>()
    private val passwordHasher = mockk<PasswordHasher>()
    private val service = UserService(repository, passwordHasher, clock)

    private val id = UUID.fromString("22222222-2222-2222-2222-222222222222")
    private val existing = User(
        id = id,
        subject = "jane",
        email = "jane@example.com",
        displayName = "Jane",
        role = Role.USER,
        passwordHash = null,
        provider = IdentityProvider.JWT,
        createdAt = now.minusSeconds(3600),
        updatedAt = now.minusSeconds(3600),
    )

    @Test
    fun `create returns DuplicateSubject without saving when subject exists`() {
        every { repository.existsBySubject("jane") } returns true

        val result = service.create(
            CreateUserUseCase.Command("jane", null, null, Role.USER, IdentityProvider.JWT, null),
        )

        assertThat(result).isEqualTo(CreateUserUseCase.Result.DuplicateSubject("jane"))
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `create hashes the password and stores the chosen role`() {
        every { repository.existsBySubject("ops") } returns false
        every { passwordHasher.hash("secret") } returns "hashed"
        val captured = slot<User>()
        every { repository.save(capture(captured)) } answers { captured.captured }

        val result = service.create(
            CreateUserUseCase.Command(
                subject = "ops",
                email = "ops@example.com",
                displayName = "Ops",
                role = Role.ADMIN,
                provider = IdentityProvider.BASIC,
                password = "secret",
            ),
        )

        assertThat(captured.captured.passwordHash).isEqualTo("hashed")
        assertThat(captured.captured.role).isEqualTo(Role.ADMIN)
        assertThat(captured.captured.createdAt).isEqualTo(now)
        assertThat(result).isInstanceOf(CreateUserUseCase.Result.Created::class.java)
    }

    @Test
    fun `update returns NotFound when user missing`() {
        every { repository.findById(id) } returns null

        assertThat(service.update(UpdateUserUseCase.Command(id, null, null, Role.ADMIN, null)))
            .isEqualTo(UpdateUserUseCase.Result.NotFound)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `update changes role and refreshes timestamp without touching password when blank`() {
        every { repository.findById(id) } returns existing
        val captured = slot<User>()
        every { repository.save(capture(captured)) } answers { captured.captured }

        service.update(UpdateUserUseCase.Command(id, "jane@example.com", "Jane", Role.ADMIN, null))

        assertThat(captured.captured.role).isEqualTo(Role.ADMIN)
        assertThat(captured.captured.passwordHash).isNull()
        assertThat(captured.captured.updatedAt).isEqualTo(now)
        verify(exactly = 0) { passwordHasher.hash(any()) }
    }

    @Test
    fun `update hashes a new password when provided`() {
        every { repository.findById(id) } returns existing
        every { passwordHasher.hash("new-pw") } returns "new-hash"
        val captured = slot<User>()
        every { repository.save(capture(captured)) } answers { captured.captured }

        service.update(UpdateUserUseCase.Command(id, null, null, Role.USER, "new-pw"))

        assertThat(captured.captured.passwordHash).isEqualTo("new-hash")
    }

    @Test
    fun `delete returns NotFound when missing`() {
        every { repository.findById(id) } returns null

        assertThat(service.delete(id)).isEqualTo(DeleteUserUseCase.Result.NotFound)
        verify(exactly = 0) { repository.deleteById(any()) }
    }

    @Test
    fun `delete removes the user when present`() {
        every { repository.findById(id) } returns existing
        every { repository.deleteById(id) } returns Unit

        assertThat(service.delete(id)).isEqualTo(DeleteUserUseCase.Result.Deleted)
        verify { repository.deleteById(id) }
    }

    @Test
    fun `getById returns Found when present`() {
        every { repository.findById(id) } returns existing

        assertThat(service.getById(id)).isEqualTo(GetUserByIdUseCase.Result.Found(existing))
    }

    @Test
    fun `resolveOrProvision creates a NEW user on first sight`() {
        every { repository.findBySubject("sub-new") } returns null
        val captured = slot<User>()
        every { repository.save(capture(captured)) } answers { captured.captured }

        val user = service.resolveOrProvision(
            ResolveOrProvisionUserUseCase.Command(
                subject = "sub-new",
                provider = IdentityProvider.JWT,
                email = "new@example.com",
                displayName = "New",
            ),
        )

        assertThat(user.role).isEqualTo(Role.NEW)
        assertThat(captured.captured.provider).isEqualTo(IdentityProvider.JWT)
        assertThat(captured.captured.subject).isEqualTo("sub-new")
    }

    @Test
    fun `resolveOrProvision keeps existing role and skips save when profile unchanged`() {
        every { repository.findBySubject("jane") } returns existing

        val user = service.resolveOrProvision(
            ResolveOrProvisionUserUseCase.Command("jane", IdentityProvider.JWT, "jane@example.com", "Jane"),
        )

        assertThat(user).isEqualTo(existing)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `resolveOrProvision refreshes profile when it changed`() {
        every { repository.findBySubject("jane") } returns existing
        val captured = slot<User>()
        every { repository.save(capture(captured)) } answers { captured.captured }

        service.resolveOrProvision(
            ResolveOrProvisionUserUseCase.Command("jane", IdentityProvider.JWT, "updated@example.com", "Jane R"),
        )

        assertThat(captured.captured.email).isEqualTo("updated@example.com")
        assertThat(captured.captured.role).isEqualTo(Role.USER)
    }

    @Test
    fun `ensureBootstrapAdmin creates basic admin with hashed password when absent`() {
        every { repository.findBySubject("admin") } returns null
        every { passwordHasher.hash("admin") } returns "hashed-admin"
        val captured = slot<User>()
        every { repository.save(capture(captured)) } answers { captured.captured }

        service.ensureBootstrapAdmin(
            EnsureBootstrapAdminUseCase.Command(IdentityProvider.BASIC, "admin", "admin", null),
        )

        assertThat(captured.captured.role).isEqualTo(Role.ADMIN)
        assertThat(captured.captured.provider).isEqualTo(IdentityProvider.BASIC)
        assertThat(captured.captured.passwordHash).isEqualTo("hashed-admin")
    }

    @Test
    fun `ensureBootstrapAdmin seeds idp admin subject when missing`() {
        every { repository.findBySubject("google-sub-123") } returns null
        val captured = slot<User>()
        every { repository.save(capture(captured)) } answers { captured.captured }

        service.ensureBootstrapAdmin(
            EnsureBootstrapAdminUseCase.Command(IdentityProvider.JWT, null, null, "google-sub-123"),
        )

        assertThat(captured.captured.subject).isEqualTo("google-sub-123")
        assertThat(captured.captured.role).isEqualTo(Role.ADMIN)
        assertThat(captured.captured.provider).isEqualTo(IdentityProvider.JWT)
    }

    @Test
    fun `ensureBootstrapAdmin promotes an already-provisioned idp admin subject to ADMIN`() {
        val provisioned = existing.copy(subject = "google-sub-123", role = Role.NEW)
        every { repository.findBySubject("google-sub-123") } returns provisioned
        val captured = slot<User>()
        every { repository.save(capture(captured)) } answers { captured.captured }

        service.ensureBootstrapAdmin(
            EnsureBootstrapAdminUseCase.Command(IdentityProvider.JWT, null, null, "google-sub-123"),
        )

        assertThat(captured.captured.id).isEqualTo(provisioned.id)
        assertThat(captured.captured.role).isEqualTo(Role.ADMIN)
    }

    @Test
    fun `ensureBootstrapAdmin does nothing for idp when no admin subject configured`() {
        service.ensureBootstrapAdmin(
            EnsureBootstrapAdminUseCase.Command(IdentityProvider.JWT, null, null, ""),
        )

        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { repository.findBySubject(any()) }
    }
}
