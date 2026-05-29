package io.github.janverhoeckx.burgee.flag.adapter.outbound.persistence

import io.github.janverhoeckx.burgee.flag.application.port.outbound.FeatureFlagRepositoryPort
import io.github.janverhoeckx.burgee.flag.domain.FeatureFlag
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class FeatureFlagPersistenceAdapter(
    private val jdbc: FeatureFlagJdbcRepository,
) : FeatureFlagRepositoryPort {

    override fun findAll(): List<FeatureFlag> =
        jdbc.findAllOrderedByKey().map { it.toDomain() }

    override fun findById(id: UUID): FeatureFlag? =
        jdbc.findById(id).orElse(null)?.toDomain()

    override fun findByKey(key: String): FeatureFlag? =
        jdbc.findByKey(key)?.toDomain()

    override fun existsByKey(key: String): Boolean = jdbc.existsByKey(key)

    override fun existsById(id: UUID): Boolean = jdbc.existsById(id)

    override fun save(flag: FeatureFlag): FeatureFlag {
        val newRecord = !jdbc.existsById(flag.id)
        return jdbc.save(flag.toRow(newRecord = newRecord)).toDomain()
    }

    override fun deleteById(id: UUID) {
        jdbc.deleteById(id)
    }
}
