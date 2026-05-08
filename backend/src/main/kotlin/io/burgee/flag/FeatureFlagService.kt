package io.burgee.flag

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class FeatureFlagService(private val repository: FeatureFlagRepository) {

    @Transactional(readOnly = true)
    fun listAll(): List<FeatureFlag> = repository.findAllByOrderByKeyAsc()

    @Transactional(readOnly = true)
    fun get(id: UUID): FeatureFlag =
        repository.findById(id).orElseThrow { FlagNotFoundException("Flag $id not found") }

    @Transactional(readOnly = true)
    fun getByKey(key: String): FeatureFlag =
        repository.findByKey(key) ?: throw FlagNotFoundException("Flag '$key' not found")

    fun create(request: CreateFeatureFlagRequest): FeatureFlag {
        if (repository.existsByKey(request.key)) {
            throw DuplicateFlagKeyException("Flag with key '${request.key}' already exists")
        }
        val flag = FeatureFlag(
            key = request.key,
            name = request.name,
            description = request.description,
            enabled = request.enabled,
        )
        return repository.save(flag)
    }

    fun update(id: UUID, request: UpdateFeatureFlagRequest): FeatureFlag {
        val flag = get(id)
        flag.name = request.name
        flag.description = request.description
        flag.enabled = request.enabled
        flag.updatedAt = Instant.now()
        return flag
    }

    fun toggle(id: UUID): FeatureFlag {
        val flag = get(id)
        flag.enabled = !flag.enabled
        flag.updatedAt = Instant.now()
        return flag
    }

    fun delete(id: UUID) {
        if (!repository.existsById(id)) throw FlagNotFoundException("Flag $id not found")
        repository.deleteById(id)
    }
}

class FlagNotFoundException(message: String) : RuntimeException(message)
class DuplicateFlagKeyException(message: String) : RuntimeException(message)
