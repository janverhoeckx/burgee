package io.github.janverhoeckx.burgee.audit.application.service

import io.github.janverhoeckx.burgee.audit.application.port.inbound.RecordAuditEntryUseCase
import io.github.janverhoeckx.burgee.audit.application.port.outbound.ActorProvider
import io.github.janverhoeckx.burgee.audit.application.port.outbound.AuditTrailRepositoryPort
import io.github.janverhoeckx.burgee.audit.domain.AuditAction
import io.github.janverhoeckx.burgee.audit.domain.AuditEntry
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class AuditTrailServiceTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val repository = mockk<AuditTrailRepositoryPort>()
    private val actorProvider = mockk<ActorProvider>()
    private val service = AuditTrailService(repository, actorProvider, clock)

    private val flagId = UUID.fromString("22222222-2222-2222-2222-222222222222")

    @Test
    fun `record stamps actor and timestamp and persists`() {
        every { actorProvider.currentActor() } returns "alice"
        val captured = slot<AuditEntry>()
        every { repository.save(capture(captured)) } answers { captured.captured }

        service.record(
            RecordAuditEntryUseCase.Command(
                action = AuditAction.TOGGLE,
                flagId = flagId,
                flagKey = "checkout-v2",
                detail = "enabled: false → true",
            ),
        )

        verify { repository.save(any()) }
        assertThat(captured.captured.actor).isEqualTo("alice")
        assertThat(captured.captured.occurredAt).isEqualTo(now)
        assertThat(captured.captured.action).isEqualTo(AuditAction.TOGGLE)
        assertThat(captured.captured.flagKey).isEqualTo("checkout-v2")
        assertThat(captured.captured.detail).isEqualTo("enabled: false → true")
    }

    @Test
    fun `list delegates to repository`() {
        val entry = AuditEntry.create(AuditAction.CREATE, flagId, "k", "alice", null, now)
        every { repository.findAll() } returns listOf(entry)

        assertThat(service.list()).containsExactly(entry)
    }

    @Test
    fun `listForFlag delegates to repository`() {
        val entry = AuditEntry.create(AuditAction.CREATE, flagId, "k", "alice", null, now)
        every { repository.findByFlagId(flagId) } returns listOf(entry)

        assertThat(service.listForFlag(flagId)).containsExactly(entry)
    }
}
