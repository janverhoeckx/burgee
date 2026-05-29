package io.github.janverhoeckx.burgee.user.adapter.outbound.persistence

import io.github.janverhoeckx.burgee.user.application.port.outbound.UserRepositoryPort
import io.github.janverhoeckx.burgee.user.domain.User
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UserPersistenceAdapter(
    private val jdbc: UserJdbcRepository,
) : UserRepositoryPort {

    override fun findAll(): List<User> =
        jdbc.findAllOrderedBySubject().map { it.toDomain() }

    override fun findById(id: UUID): User? =
        jdbc.findById(id).orElse(null)?.toDomain()

    override fun findBySubject(subject: String): User? =
        jdbc.findBySubject(subject)?.toDomain()

    override fun existsBySubject(subject: String): Boolean = jdbc.existsBySubject(subject)

    override fun save(user: User): User {
        val newRecord = !jdbc.existsById(user.id)
        return jdbc.save(user.toRow(newRecord = newRecord)).toDomain()
    }

    override fun deleteById(id: UUID) {
        jdbc.deleteById(id)
    }
}
