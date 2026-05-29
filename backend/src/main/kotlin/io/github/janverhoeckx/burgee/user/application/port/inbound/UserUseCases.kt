package io.github.janverhoeckx.burgee.user.application.port.inbound

import io.github.janverhoeckx.burgee.user.domain.IdentityProvider
import io.github.janverhoeckx.burgee.user.domain.Role
import io.github.janverhoeckx.burgee.user.domain.User
import java.util.UUID

fun interface ListUsersUseCase {
    fun list(): List<User>
}

interface GetUserByIdUseCase {
    sealed interface Result {
        data class Found(val user: User) : Result
        data object NotFound : Result
    }

    fun getById(id: UUID): Result
}

interface CreateUserUseCase {
    data class Command(
        val subject: String,
        val email: String?,
        val displayName: String?,
        val role: Role,
        val provider: IdentityProvider,
        val password: String?,
    )

    sealed interface Result {
        data class Created(val user: User) : Result
        data class DuplicateSubject(val subject: String) : Result
    }

    fun create(command: Command): Result
}

interface UpdateUserUseCase {
    data class Command(
        val id: UUID,
        val email: String?,
        val displayName: String?,
        val role: Role,
        val password: String?,
    )

    sealed interface Result {
        data class Updated(val user: User) : Result
        data object NotFound : Result
    }

    fun update(command: Command): Result
}

interface DeleteUserUseCase {
    sealed interface Result {
        data object Deleted : Result
        data object NotFound : Result
    }

    fun delete(id: UUID): Result
}
