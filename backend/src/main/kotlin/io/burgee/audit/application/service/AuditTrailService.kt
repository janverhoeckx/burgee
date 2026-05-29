package io.burgee.audit.application.service

import io.burgee.audit.application.port.inbound.ListAuditTrailUseCase
import io.burgee.audit.application.port.inbound.RecordAuditEntryUseCase
import io.burgee.audit.application.port.outbound.ActorProvider
import io.burgee.audit.application.port.outbound.AuditTrailRepositoryPort
import io.burgee.audit.domain.AuditEntry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class AuditTrailService(
    private val repository: AuditTrailRepositoryPort,
    private val actorProvider: ActorProvider,
    private val clock: Clock,
) : RecordAuditEntryUseCase,
    ListAuditTrailUseCase {

    override fun record(command: RecordAuditEntryUseCase.Command) {
        val entry = AuditEntry.create(
            action = command.action,
            flagId = command.flagId,
            flagKey = command.flagKey,
            actor = actorProvider.currentActor(),
            detail = command.detail,
            now = Instant.now(clock),
        )
        repository.save(entry)
    }

    @Transactional(readOnly = true)
    override fun list(): List<AuditEntry> = repository.findAll()

    @Transactional(readOnly = true)
    override fun listForFlag(flagId: UUID): List<AuditEntry> = repository.findByFlagId(flagId)
}
