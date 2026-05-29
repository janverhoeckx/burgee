package io.github.janverhoeckx.burgee.user.domain

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val subject: String,
    val email: String?,
    val displayName: String?,
    val role: Role,
    val passwordHash: String?,
    val provider: IdentityProvider,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun withRole(role: Role, now: Instant): User =
        copy(role = role, updatedAt = now)

    fun withProfile(email: String?, displayName: String?, now: Instant): User =
        copy(email = email, displayName = displayName, updatedAt = now)

    fun withPasswordHash(passwordHash: String?, now: Instant): User =
        copy(passwordHash = passwordHash, updatedAt = now)

    companion object {
        fun create(
            subject: String,
            email: String?,
            displayName: String?,
            role: Role,
            provider: IdentityProvider,
            passwordHash: String? = null,
            now: Instant = Instant.now(),
            id: UUID = UUID.randomUUID(),
        ): User = User(
            id = id,
            subject = subject,
            email = email,
            displayName = displayName,
            role = role,
            passwordHash = passwordHash,
            provider = provider,
            createdAt = now,
            updatedAt = now,
        )
    }
}
