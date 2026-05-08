package io.burgee.flag

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FeatureFlagRepository : JpaRepository<FeatureFlag, UUID> {
    fun findByKey(key: String): FeatureFlag?
    fun existsByKey(key: String): Boolean
    fun findAllByOrderByKeyAsc(): List<FeatureFlag>
}
