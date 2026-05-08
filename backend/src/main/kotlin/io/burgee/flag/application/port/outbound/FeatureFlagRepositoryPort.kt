package io.burgee.flag.application.port.outbound

import io.burgee.flag.domain.FeatureFlag
import java.util.UUID

interface FeatureFlagRepositoryPort {
    fun findAll(): List<FeatureFlag>
    fun findById(id: UUID): FeatureFlag?
    fun findByKey(key: String): FeatureFlag?
    fun existsByKey(key: String): Boolean
    fun existsById(id: UUID): Boolean
    fun save(flag: FeatureFlag): FeatureFlag
    fun deleteById(id: UUID)
}
