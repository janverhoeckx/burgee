package io.github.janverhoeckx.burgee.user.adapter.outbound.persistence

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface UserJdbcRepository : CrudRepository<UserRow, UUID> {

    fun findBySubject(subject: String): UserRow?

    fun existsBySubject(subject: String): Boolean

    @Query("SELECT * FROM users ORDER BY subject ASC")
    fun findAllOrderedBySubject(): List<UserRow>
}
