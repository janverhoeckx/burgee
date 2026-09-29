package io.github.janverhoeckx.burgee.flag.application.service

import io.github.janverhoeckx.burgee.flag.application.port.inbound.CreateFlagUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.DeleteFlagUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.EvaluateAllFlagsUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.EvaluateFlagUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.GetFlagByIdUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.InvalidTargetingRule
import io.github.janverhoeckx.burgee.flag.application.port.inbound.ListFlagsUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.ToggleFlagUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.UpdateFlagUseCase
import io.github.janverhoeckx.burgee.audit.application.port.inbound.RecordAuditEntryUseCase
import io.github.janverhoeckx.burgee.audit.domain.AuditAction
import io.github.janverhoeckx.burgee.flag.application.port.outbound.FeatureFlagRepositoryPort
import io.github.janverhoeckx.burgee.flag.domain.Evaluation
import io.github.janverhoeckx.burgee.flag.domain.EvaluationContext
import io.github.janverhoeckx.burgee.flag.domain.FeatureFlag
import io.github.janverhoeckx.burgee.flag.domain.TargetingRule
import io.github.janverhoeckx.burgee.flag.domain.getOrElse
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
    CreateFlagUseCase,
    UpdateFlagUseCase,
    ToggleFlagUseCase,
    DeleteFlagUseCase,
    EvaluateFlagUseCase,
    EvaluateAllFlagsUseCase {

    @Transactional(readOnly = true)
    override fun list(): List<FeatureFlag> = repository.findAll()

    @Transactional(readOnly = true)
    override fun getById(id: UUID): GetFlagByIdUseCase.Result =
        repository.findById(id)
            ?.let { GetFlagByIdUseCase.Result.Found(it) }
            ?: GetFlagByIdUseCase.Result.NotFound

    @Transactional(readOnly = true)
    override fun evaluate(key: String, context: EvaluationContext): EvaluateFlagUseCase.Result =
        repository.findByKey(key)
            ?.let { EvaluateFlagUseCase.Result.Evaluated(it.evaluate(context)) }
            ?: EvaluateFlagUseCase.Result.NotFound

    @Transactional(readOnly = true)
    override fun evaluateAll(context: EvaluationContext): List<Evaluation> =
        repository.findAll().map { it.evaluate(context) }

    override fun create(command: CreateFlagUseCase.Command): CreateFlagUseCase.Result {
        if (repository.existsByKey(command.key)) {
            return CreateFlagUseCase.Result.DuplicateKey(command.key)
        }
        val targetingRule = TargetingRule.parse(command.conditions).getOrElse { return InvalidTargetingRule(it) }
        val flag = FeatureFlag.create(
            key = command.key,
            name = command.name,
            description = command.description,
            enabled = command.enabled,
            now = now(),
            targetingRule = targetingRule,
        )
        val saved = repository.save(flag)
        recordAudit(AuditAction.CREATE, saved, FlagAuditDetails.creation(saved))
        return CreateFlagUseCase.Result.Created(saved)
    }

    override fun update(command: UpdateFlagUseCase.Command): UpdateFlagUseCase.Result {
        val existing = repository.findById(command.id)
            ?: return UpdateFlagUseCase.Result.NotFound
        val targetingRule = TargetingRule.parse(command.conditions).getOrElse { return InvalidTargetingRule(it) }
        val updated = existing.withDetails(
            name = command.name,
            description = command.description,
            enabled = command.enabled,
            targetingRule = targetingRule,
            now = now(),
        )
        val saved = repository.save(updated)
        recordAudit(AuditAction.UPDATE, saved, FlagAuditDetails.changes(existing, saved))
        return UpdateFlagUseCase.Result.Updated(saved)
    }

    override fun toggle(id: UUID): ToggleFlagUseCase.Result {
        val existing = repository.findById(id)
            ?: return ToggleFlagUseCase.Result.NotFound
        val saved = repository.save(existing.toggled(now()))
        recordAudit(AuditAction.TOGGLE, saved, FlagAuditDetails.toggle(existing, saved))
        return ToggleFlagUseCase.Result.Toggled(saved)
    }

    override fun delete(id: UUID): DeleteFlagUseCase.Result {
        val existing = repository.findById(id) ?: return DeleteFlagUseCase.Result.NotFound
        repository.deleteById(id)
        recordAudit(AuditAction.DELETE, existing, FlagAuditDetails.deletion(existing))
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

    private fun now(): Instant = Instant.now(clock)
}
