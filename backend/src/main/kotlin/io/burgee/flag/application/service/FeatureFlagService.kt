package io.burgee.flag.application.service

import io.burgee.flag.application.port.inbound.CreateFlagUseCase
import io.burgee.flag.application.port.inbound.DeleteFlagUseCase
import io.burgee.flag.application.port.inbound.GetFlagByIdUseCase
import io.burgee.flag.application.port.inbound.GetFlagByKeyUseCase
import io.burgee.flag.application.port.inbound.ListFlagsUseCase
import io.burgee.flag.application.port.inbound.ToggleFlagUseCase
import io.burgee.flag.application.port.inbound.UpdateFlagUseCase
import io.burgee.audit.application.port.inbound.RecordAuditEntryUseCase
import io.burgee.audit.domain.AuditAction
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
    private val auditTrail: RecordAuditEntryUseCase,
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
        val saved = repository.save(flag)
        recordAudit(AuditAction.CREATE, saved, "Created flag (enabled=${saved.enabled})")
        return CreateFlagUseCase.Result.Created(saved)
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
        val saved = repository.save(updated)
        recordAudit(AuditAction.UPDATE, saved, describeChanges(existing, saved))
        return UpdateFlagUseCase.Result.Updated(saved)
    }

    override fun toggle(id: UUID): ToggleFlagUseCase.Result {
        val existing = repository.findById(id)
            ?: return ToggleFlagUseCase.Result.NotFound
        val saved = repository.save(existing.toggled(now()))
        recordAudit(AuditAction.TOGGLE, saved, "enabled: ${existing.enabled} → ${saved.enabled}")
        return ToggleFlagUseCase.Result.Toggled(saved)
    }

    override fun delete(id: UUID): DeleteFlagUseCase.Result {
        val existing = repository.findById(id) ?: return DeleteFlagUseCase.Result.NotFound
        repository.deleteById(id)
        recordAudit(AuditAction.DELETE, existing, "Deleted flag '${existing.key}'")
        return DeleteFlagUseCase.Result.Deleted
    }

    private fun recordAudit(action: AuditAction, flag: FeatureFlag, detail: String?) {
        auditTrail.record(
            RecordAuditEntryUseCase.Command(
                action = action,
                flagId = flag.id,
                flagKey = flag.key,
                detail = detail,
            ),
        )
    }

    private fun describeChanges(before: FeatureFlag, after: FeatureFlag): String {
        val changes = buildList {
            if (before.name != after.name) add("name: '${before.name}' → '${after.name}'")
            if (before.description != after.description) {
                add("description: ${quote(before.description)} → ${quote(after.description)}")
            }
            if (before.enabled != after.enabled) add("enabled: ${before.enabled} → ${after.enabled}")
        }
        return if (changes.isEmpty()) "No changes" else changes.joinToString("; ")
    }

    private fun quote(value: String?): String = if (value == null) "∅" else "'$value'"

    private fun now(): Instant = Instant.now(clock)
}
