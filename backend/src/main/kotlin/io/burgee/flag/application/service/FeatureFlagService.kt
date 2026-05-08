package io.burgee.flag.application.service

import io.burgee.flag.application.port.inbound.CreateFlagUseCase
import io.burgee.flag.application.port.inbound.DeleteFlagUseCase
import io.burgee.flag.application.port.inbound.GetFlagByIdUseCase
import io.burgee.flag.application.port.inbound.GetFlagByKeyUseCase
import io.burgee.flag.application.port.inbound.ListFlagsUseCase
import io.burgee.flag.application.port.inbound.ToggleFlagUseCase
import io.burgee.flag.application.port.inbound.UpdateFlagUseCase
import io.burgee.flag.application.port.outbound.FeatureFlagRepositoryPort
import io.burgee.flag.domain.FeatureFlag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class FeatureFlagService(
    private val repository: FeatureFlagRepositoryPort,
    private val clock: Clock,
) : ListFlagsUseCase,
    GetFlagByIdUseCase,
    GetFlagByKeyUseCase,
    CreateFlagUseCase,
    UpdateFlagUseCase,
    ToggleFlagUseCase,
    DeleteFlagUseCase {

    @Transactional(readOnly = true)
    override fun list(): List<FeatureFlag> = repository.findAll()

    @Transactional(readOnly = true)
    override fun getById(id: UUID): GetFlagByIdUseCase.Result =
        repository.findById(id)
            ?.let { GetFlagByIdUseCase.Result.Found(it) }
            ?: GetFlagByIdUseCase.Result.NotFound

    @Transactional(readOnly = true)
    override fun getByKey(key: String): GetFlagByKeyUseCase.Result =
        repository.findByKey(key)
            ?.let { GetFlagByKeyUseCase.Result.Found(it) }
            ?: GetFlagByKeyUseCase.Result.NotFound

    override fun create(command: CreateFlagUseCase.Command): CreateFlagUseCase.Result {
        if (repository.existsByKey(command.key)) {
            return CreateFlagUseCase.Result.DuplicateKey(command.key)
        }
        val flag = FeatureFlag.create(
            key = command.key,
            name = command.name,
            description = command.description,
            enabled = command.enabled,
            now = now(),
        )
        return CreateFlagUseCase.Result.Created(repository.save(flag))
    }

    override fun update(command: UpdateFlagUseCase.Command): UpdateFlagUseCase.Result {
        val existing = repository.findById(command.id)
            ?: return UpdateFlagUseCase.Result.NotFound
        val updated = existing.withDetails(
            name = command.name,
            description = command.description,
            enabled = command.enabled,
            now = now(),
        )
        return UpdateFlagUseCase.Result.Updated(repository.save(updated))
    }

    override fun toggle(id: UUID): ToggleFlagUseCase.Result {
        val existing = repository.findById(id)
            ?: return ToggleFlagUseCase.Result.NotFound
        return ToggleFlagUseCase.Result.Toggled(repository.save(existing.toggled(now())))
    }

    override fun delete(id: UUID): DeleteFlagUseCase.Result {
        if (!repository.existsById(id)) return DeleteFlagUseCase.Result.NotFound
        repository.deleteById(id)
        return DeleteFlagUseCase.Result.Deleted
    }

    private fun now(): Instant = Instant.now(clock)
}
