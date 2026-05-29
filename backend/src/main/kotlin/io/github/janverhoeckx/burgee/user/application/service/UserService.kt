package io.github.janverhoeckx.burgee.user.application.service

import io.github.janverhoeckx.burgee.user.application.port.inbound.CreateUserUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.DeleteUserUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.EnsureBootstrapAdminUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.FindUserBySubjectUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.GetUserByIdUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.ListUsersUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.ResolveOrProvisionUserUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.UpdateUserUseCase
import io.github.janverhoeckx.burgee.user.application.port.outbound.PasswordHasher
import io.github.janverhoeckx.burgee.user.application.port.outbound.UserRepositoryPort
import io.github.janverhoeckx.burgee.user.domain.IdentityProvider
import io.github.janverhoeckx.burgee.user.domain.Role
import io.github.janverhoeckx.burgee.user.domain.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class UserService(
    private val repository: UserRepositoryPort,
    private val passwordHasher: PasswordHasher,
    private val clock: Clock,
) : ListUsersUseCase,
    GetUserByIdUseCase,
    CreateUserUseCase,
    UpdateUserUseCase,
    DeleteUserUseCase,
    ResolveOrProvisionUserUseCase,
    FindUserBySubjectUseCase,
    EnsureBootstrapAdminUseCase {

    @Transactional(readOnly = true)
    override fun list(): List<User> = repository.findAll()

    @Transactional(readOnly = true)
    override fun getById(id: UUID): GetUserByIdUseCase.Result =
        repository.findById(id)
            ?.let { GetUserByIdUseCase.Result.Found(it) }
            ?: GetUserByIdUseCase.Result.NotFound

    @Transactional(readOnly = true)
    override fun findBySubject(subject: String): User? = repository.findBySubject(subject)

    override fun create(command: CreateUserUseCase.Command): CreateUserUseCase.Result {
        if (repository.existsBySubject(command.subject)) {
            return CreateUserUseCase.Result.DuplicateSubject(command.subject)
        }
        val user = User.create(
            subject = command.subject,
            email = command.email,
            displayName = command.displayName,
            role = command.role,
            provider = command.provider,
            passwordHash = command.password?.takeIf { it.isNotBlank() }?.let { passwordHasher.hash(it) },
            now = now(),
        )
        return CreateUserUseCase.Result.Created(repository.save(user))
    }

    override fun update(command: UpdateUserUseCase.Command): UpdateUserUseCase.Result {
        val existing = repository.findById(command.id)
            ?: return UpdateUserUseCase.Result.NotFound
        val now = now()
        var updated = existing
            .withProfile(command.email, command.displayName, now)
            .withRole(command.role, now)
        command.password?.takeIf { it.isNotBlank() }?.let {
            updated = updated.withPasswordHash(passwordHasher.hash(it), now)
        }
        return UpdateUserUseCase.Result.Updated(repository.save(updated))
    }

    override fun delete(id: UUID): DeleteUserUseCase.Result {
        if (repository.findById(id) == null) return DeleteUserUseCase.Result.NotFound
        repository.deleteById(id)
        return DeleteUserUseCase.Result.Deleted
    }

    override fun resolveOrProvision(command: ResolveOrProvisionUserUseCase.Command): User {
        val existing = repository.findBySubject(command.subject)
        if (existing != null) {
            val refreshed = existing.withProfile(command.email, command.displayName, now())
            return if (refreshed.email == existing.email && refreshed.displayName == existing.displayName) {
                existing
            } else {
                repository.save(refreshed)
            }
        }
        val user = User.create(
            subject = command.subject,
            email = command.email,
            displayName = command.displayName,
            role = Role.NEW,
            provider = command.provider,
            now = now(),
        )
        return repository.save(user)
    }

    override fun ensureBootstrapAdmin(command: EnsureBootstrapAdminUseCase.Command) {
        when (command.provider) {
            IdentityProvider.BASIC -> ensureBasicAdmin(command.basicUsername, command.basicPassword)
            else -> ensureIdpAdmin(command.provider, command.idpAdminSubject)
        }
    }

    private fun ensureBasicAdmin(username: String?, password: String?) {
        val name = username?.takeIf { it.isNotBlank() } ?: return
        val now = now()
        val existing = repository.findBySubject(name)
        val hash = password?.takeIf { it.isNotBlank() }?.let { passwordHasher.hash(it) }
        val user = if (existing == null) {
            User.create(
                subject = name,
                email = null,
                displayName = name,
                role = Role.ADMIN,
                provider = IdentityProvider.BASIC,
                passwordHash = hash,
                now = now,
            )
        } else {
            // Keep the configured credentials authoritative so the operator can always sign in.
            existing
                .withRole(Role.ADMIN, now)
                .withPasswordHash(hash ?: existing.passwordHash, now)
        }
        repository.save(user)
    }

    private fun ensureIdpAdmin(
        provider: IdentityProvider,
        subject: String?,
    ) {
        val adminSubject = subject?.takeIf { it.isNotBlank() } ?: return
        val now = now()
        // Keep the configured subject authoritative so the first admin is always promoted,
        // even if the account was already auto-provisioned as NEW on an earlier login.
        val existing = repository.findBySubject(adminSubject)
        val user = existing?.withRole(Role.ADMIN, now)
            ?: User.create(
                subject = adminSubject,
                email = null,
                displayName = null,
                role = Role.ADMIN,
                provider = provider,
                now = now,
            )
        repository.save(user)
    }

    private fun now(): Instant = Instant.now(clock)
}
