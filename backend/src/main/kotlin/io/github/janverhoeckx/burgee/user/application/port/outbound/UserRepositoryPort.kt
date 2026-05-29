package io.github.janverhoeckx.burgee.user.application.port.outbound

import io.github.janverhoeckx.burgee.user.domain.User
import java.util.UUID

interface UserRepositoryPort {
    fun findAll(): List<User>
    fun findById(id: UUID): User?
    fun findBySubject(subject: String): User?
    fun existsBySubject(subject: String): Boolean
    fun save(user: User): User
    fun deleteById(id: UUID)
}
