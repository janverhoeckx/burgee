package io.github.janverhoeckx.burgee.user.adapter.outbound.persistence

import io.github.janverhoeckx.burgee.user.domain.IdentityProvider
import io.github.janverhoeckx.burgee.user.domain.Role
import io.github.janverhoeckx.burgee.user.domain.User

internal fun UserRow.toDomain(): User = User(
    id = rowId,
    subject = subject,
    email = email,
    displayName = displayName,
    role = Role.valueOf(role),
    passwordHash = passwordHash,
    provider = IdentityProvider.valueOf(provider),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun User.toRow(newRecord: Boolean): UserRow = UserRow(
    rowId = id,
    subject = subject,
    email = email,
    displayName = displayName,
    role = role.name,
    passwordHash = passwordHash,
    provider = provider.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
).also { it.newRecord = newRecord }
