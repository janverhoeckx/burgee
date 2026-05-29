package io.github.janverhoeckx.burgee.flag.adapter.outbound.persistence

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface FeatureFlagJdbcRepository : CrudRepository<FeatureFlagRow, UUID> {

    fun findByKey(key: String): FeatureFlagRow?

    fun existsByKey(key: String): Boolean

    @Query("SELECT * FROM feature_flags ORDER BY key ASC")
    fun findAllOrderedByKey(): List<FeatureFlagRow>
}
