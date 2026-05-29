package io.github.janverhoeckx.burgee.user.application.port.inbound

import io.github.janverhoeckx.burgee.user.domain.IdentityProvider
import io.github.janverhoeckx.burgee.user.domain.User

/**
 * Resolves the local user for an authenticated identity-provider principal,
 * creating one with the default (no-permission) role on first sight.
 */
interface ResolveOrProvisionUserUseCase {
    data class Command(
        val subject: String,
        val provider: IdentityProvider,
        val email: String?,
        val displayName: String?,
    )

    fun resolveOrProvision(command: Command): User
}

/** Looks up a user by their authentication subject (used by basic-auth login). */
fun interface FindUserBySubjectUseCase {
    fun findBySubject(subject: String): User?
}

/**
 * Guarantees a bootstrap admin exists at startup so a first admin can be created
 * from configuration without manual database changes.
 */
interface EnsureBootstrapAdminUseCase {
    data class Command(
        val provider: IdentityProvider,
        val basicUsername: String?,
        val basicPassword: String?,
        val idpAdminSubject: String?,
    )

    fun ensureBootstrapAdmin(command: Command)
}
