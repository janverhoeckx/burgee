package io.github.janverhoeckx.burgee.flag.application.service

import io.github.janverhoeckx.burgee.flag.application.port.inbound.CreateFlagUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.DeleteFlagUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.EvaluateFlagUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.GetFlagByIdUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.ToggleFlagUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.UpdateFlagUseCase
import io.github.janverhoeckx.burgee.audit.application.port.inbound.RecordAuditEntryUseCase
import io.github.janverhoeckx.burgee.audit.domain.AuditAction
import io.github.janverhoeckx.burgee.flag.application.port.outbound.FeatureFlagRepositoryPort
import io.github.janverhoeckx.burgee.flag.domain.Condition
import io.github.janverhoeckx.burgee.flag.domain.ConditionOperator
import io.github.janverhoeckx.burgee.flag.domain.Evaluation
import io.github.janverhoeckx.burgee.flag.domain.EvaluationContext
import io.github.janverhoeckx.burgee.flag.domain.FeatureFlag
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

class FeatureFlagServiceTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val repository = mockk<FeatureFlagRepositoryPort>()
    private val auditTrail = mockk<RecordAuditEntryUseCase>(relaxed = true)
    private val service = FeatureFlagService(repository, auditTrail, clock)

    private val id = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val existing = FeatureFlag(
        id = id,
        key = "checkout-v2",
        name = "Checkout v2",
        description = "old",
        enabled = false,
        createdAt = now.minusSeconds(3600),
        updatedAt = now.minusSeconds(3600),
    )

    @Test
    fun `list returns repository contents`() {
        every { repository.findAll() } returns listOf(existing)

        assertThat(service.list()).containsExactly(existing)
    }

    @Test
    fun `getById returns NotFound when missing`() {
        every { repository.findById(id) } returns null

        assertThat(service.getById(id)).isEqualTo(GetFlagByIdUseCase.Result.NotFound)
    }

    @Test
    fun `getById returns Found when present`() {
        every { repository.findById(id) } returns existing

        assertThat(service.getById(id))
            .isEqualTo(GetFlagByIdUseCase.Result.Found(existing))
    }

    @Test
    fun `create returns DuplicateKey without saving when key exists`() {
        every { repository.existsByKey("checkout-v2") } returns true

        val result = service.create(
            CreateFlagUseCase.Command("checkout-v2", "name", null, false),
        )

        assertThat(result).isEqualTo(CreateFlagUseCase.Result.DuplicateKey("checkout-v2"))
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `create stamps timestamps from clock and returns Created`() {
        every { repository.existsByKey("brand-new") } returns false
        val captured = slot<FeatureFlag>()
        every { repository.save(capture(captured)) } answers { captured.captured }

        val result = service.create(
            CreateFlagUseCase.Command(
                key = "brand-new",
                name = "Brand new",
                description = "desc",
                enabled = true,
            ),
        )

        assertThat(captured.captured.key).isEqualTo("brand-new")
        assertThat(captured.captured.createdAt).isEqualTo(now)
        assertThat(captured.captured.updatedAt).isEqualTo(now)
        assertThat(result).isInstanceOf(CreateFlagUseCase.Result.Created::class.java)
        assertThat((result as CreateFlagUseCase.Result.Created).flag).isEqualTo(captured.captured)
    }

    @Test
    fun `update returns NotFound when flag missing`() {
        every { repository.findById(id) } returns null

        assertThat(service.update(UpdateFlagUseCase.Command(id, "x", null, true)))
            .isEqualTo(UpdateFlagUseCase.Result.NotFound)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `update returns Updated with new values and refreshed updatedAt`() {
        every { repository.findById(id) } returns existing
        val captured = slot<FeatureFlag>()
        every { repository.save(capture(captured)) } answers { captured.captured }

        val result = service.update(
            UpdateFlagUseCase.Command(id = id, name = "renamed", description = "new", enabled = true),
        )

        assertThat(captured.captured.name).isEqualTo("renamed")
        assertThat(captured.captured.description).isEqualTo("new")
        assertThat(captured.captured.enabled).isTrue()
        assertThat(captured.captured.key).isEqualTo(existing.key)
        assertThat(captured.captured.createdAt).isEqualTo(existing.createdAt)
        assertThat(captured.captured.updatedAt).isEqualTo(now)
        assertThat(result).isEqualTo(UpdateFlagUseCase.Result.Updated(captured.captured))
    }

    @Test
    fun `toggle returns NotFound when flag missing`() {
        every { repository.findById(id) } returns null

        assertThat(service.toggle(id)).isEqualTo(ToggleFlagUseCase.Result.NotFound)
    }

    @Test
    fun `toggle flips enabled and stamps updatedAt`() {
        every { repository.findById(id) } returns existing
        val captured = slot<FeatureFlag>()
        every { repository.save(capture(captured)) } answers { captured.captured }

        val result = service.toggle(id)

        assertThat(captured.captured.enabled).isTrue()
        assertThat(captured.captured.updatedAt).isEqualTo(now)
        assertThat(result).isEqualTo(ToggleFlagUseCase.Result.Toggled(captured.captured))
    }

    @Test
    fun `delete returns Deleted and removes when present`() {
        every { repository.findById(id) } returns existing
        every { repository.deleteById(id) } returns Unit

        assertThat(service.delete(id)).isEqualTo(DeleteFlagUseCase.Result.Deleted)
        verify { repository.deleteById(id) }
    }

    @Test
    fun `delete returns NotFound when missing`() {
        every { repository.findById(id) } returns null

        assertThat(service.delete(id)).isEqualTo(DeleteFlagUseCase.Result.NotFound)
        verify(exactly = 0) { repository.deleteById(any()) }
    }

    @Test
    fun `create records a CREATE audit entry`() {
        every { repository.existsByKey("brand-new") } returns false
        every { repository.save(any()) } answers { firstArg() }

        service.create(CreateFlagUseCase.Command("brand-new", "Brand new", "desc", true))

        val command = slot<RecordAuditEntryUseCase.Command>()
        verify { auditTrail.record(capture(command)) }
        assertThat(command.captured.action).isEqualTo(AuditAction.CREATE)
        assertThat(command.captured.flagKey).isEqualTo("brand-new")
    }

    @Test
    fun `update records an UPDATE audit entry describing the change`() {
        every { repository.findById(id) } returns existing
        every { repository.save(any()) } answers { firstArg() }

        service.update(UpdateFlagUseCase.Command(id, "renamed", existing.description, existing.enabled))

        val command = slot<RecordAuditEntryUseCase.Command>()
        verify { auditTrail.record(capture(command)) }
        assertThat(command.captured.action).isEqualTo(AuditAction.UPDATE)
        assertThat(command.captured.detail).contains("name:").contains("renamed")
    }

    @Test
    fun `toggle records a TOGGLE audit entry`() {
        every { repository.findById(id) } returns existing
        every { repository.save(any()) } answers { firstArg() }

        service.toggle(id)

        val command = slot<RecordAuditEntryUseCase.Command>()
        verify { auditTrail.record(capture(command)) }
        assertThat(command.captured.action).isEqualTo(AuditAction.TOGGLE)
        assertThat(command.captured.detail).isEqualTo("enabled: false → true")
    }

    @Test
    fun `delete records a DELETE audit entry`() {
        every { repository.findById(id) } returns existing
        every { repository.deleteById(id) } returns Unit

        service.delete(id)

        val command = slot<RecordAuditEntryUseCase.Command>()
        verify { auditTrail.record(capture(command)) }
        assertThat(command.captured.action).isEqualTo(AuditAction.DELETE)
        assertThat(command.captured.flagKey).isEqualTo(existing.key)
    }

    @Test
    fun `create does not record audit on duplicate key`() {
        every { repository.existsByKey("checkout-v2") } returns true

        service.create(CreateFlagUseCase.Command("checkout-v2", "name", null, false))

        verify(exactly = 0) { auditTrail.record(any()) }
    }

    @Test
    fun `evaluate returns NotFound for an unknown key`() {
        every { repository.findByKey("missing") } returns null

        assertThat(service.evaluate("missing", EvaluationContext.EMPTY))
            .isEqualTo(EvaluateFlagUseCase.Result.NotFound)
    }

    @Test
    fun `evaluate returns the Evaluation of the flag with that key`() {
        every { repository.findByKey("checkout-v2") } returns existing.copy(enabled = true)

        assertThat(service.evaluate("checkout-v2", EvaluationContext(mapOf("organisationId" to "acme"))))
            .isEqualTo(EvaluateFlagUseCase.Result.Evaluated(Evaluation("checkout-v2", true)))
    }

    @Test
    fun `evaluateAll returns an Evaluation for every flag, including disabled ones`() {
        val on = existing.copy(key = "a-on", enabled = true)
        val off = existing.copy(key = "b-off", enabled = false)
        every { repository.findAll() } returns listOf(on, off)

        assertThat(service.evaluateAll(EvaluationContext.EMPTY))
            .containsExactly(Evaluation("a-on", true), Evaluation("b-off", false))
    }

    private val orgIn = Condition.of("organisationId", ConditionOperator.IN, listOf("acme", "globex"))

    @Test
    fun `create saves the Targeting Rule with duplicate values removed`() {
        every { repository.existsByKey("targeted") } returns false
        every { repository.save(any()) } answers { firstArg() }

        val result = service.create(
            CreateFlagUseCase.Command(
                key = "targeted",
                name = "Targeted",
                description = null,
                enabled = true,
                conditions = listOf(Condition.Input("organisationId", "IN", listOf("acme", "globex", "acme"))),
            ),
        )

        assertThat((result as CreateFlagUseCase.Result.Created).flag.conditions).containsExactly(orgIn)
        verify { repository.save(match { it.conditions == listOf(orgIn) }) }
    }

    @Test
    fun `create rejects an invalid Targeting Rule without saving or auditing`() {
        every { repository.existsByKey("targeted") } returns false

        val result = service.create(
            CreateFlagUseCase.Command(
                key = "targeted",
                name = "Targeted",
                description = null,
                enabled = true,
                conditions = listOf(Condition.Input("organisationId", "IN", emptyList())),
            ),
        )

        assertThat(result).isEqualTo(
            CreateFlagUseCase.Result.InvalidTargetingRule(
                mapOf("conditions[0].values" to "must contain at least one value"),
            ),
        )
        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { auditTrail.record(any()) }
    }

    @Test
    fun `update replaces the whole Targeting Rule`() {
        every { repository.findById(id) } returns existing.copy(conditions = listOf(orgIn))
        every { repository.save(any()) } answers { firstArg() }

        val result = service.update(
            UpdateFlagUseCase.Command(
                id = id,
                name = existing.name,
                description = existing.description,
                enabled = true,
                conditions = listOf(Condition.Input("country", "IN", listOf("nl"))),
            ),
        )

        assertThat((result as UpdateFlagUseCase.Result.Updated).flag.conditions)
            .containsExactly(Condition.of("country", ConditionOperator.IN, listOf("nl")))
    }

    @Test
    fun `update without Conditions clears the Targeting Rule`() {
        every { repository.findById(id) } returns existing.copy(conditions = listOf(orgIn))
        every { repository.save(any()) } answers { firstArg() }

        val result = service.update(UpdateFlagUseCase.Command(id, existing.name, existing.description, true))

        assertThat((result as UpdateFlagUseCase.Result.Updated).flag.conditions).isEmpty()
    }

    @Test
    fun `update rejects an invalid Targeting Rule without saving or auditing`() {
        every { repository.findById(id) } returns existing

        val result = service.update(
            UpdateFlagUseCase.Command(
                id = id,
                name = existing.name,
                description = existing.description,
                enabled = true,
                conditions = listOf(
                    Condition.Input("organisationId", "IN", listOf("acme")),
                    Condition.Input("organisationId", "IN", listOf("globex")),
                ),
            ),
        )

        assertThat(result).isEqualTo(
            UpdateFlagUseCase.Result.InvalidTargetingRule(
                mapOf("conditions[1].attribute" to "duplicate attribute 'organisationId'"),
            ),
        )
        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { auditTrail.record(any()) }
    }

    private fun recordedDetail(): String? {
        val command = slot<RecordAuditEntryUseCase.Command>()
        verify { auditTrail.record(capture(command)) }
        return command.captured.detail
    }

    @Test
    fun `update records the Conditions before and after when the Targeting Rule changes`() {
        every { repository.findById(id) } returns existing.copy(
            conditions = listOf(Condition.of("organisationId", ConditionOperator.IN, listOf("acme"))),
        )
        every { repository.save(any()) } answers { firstArg() }

        service.update(
            UpdateFlagUseCase.Command(
                id = id,
                name = existing.name,
                description = existing.description,
                enabled = existing.enabled,
                conditions = listOf(Condition.Input("organisationId", "IN", listOf("acme", "globex"))),
            ),
        )

        assertThat(recordedDetail())
            .isEqualTo("conditions: [organisationId IN (acme)] → [organisationId IN (acme, globex)]")
    }

    @Test
    fun `update does not mention the Conditions when the Targeting Rule is unchanged`() {
        every { repository.findById(id) } returns existing.copy(conditions = listOf(orgIn))
        every { repository.save(any()) } answers { firstArg() }

        service.update(
            UpdateFlagUseCase.Command(
                id = id,
                name = "renamed",
                description = existing.description,
                enabled = existing.enabled,
                conditions = listOf(Condition.Input("organisationId", "IN", listOf("acme", "globex"))),
            ),
        )

        assertThat(recordedDetail()).isEqualTo("name: 'Checkout v2' → 'renamed'")
    }

    @Test
    fun `update that clears the Targeting Rule records it as empty`() {
        every { repository.findById(id) } returns existing.copy(conditions = listOf(orgIn))
        every { repository.save(any()) } answers { firstArg() }

        service.update(UpdateFlagUseCase.Command(id, existing.name, existing.description, existing.enabled))

        assertThat(recordedDetail()).isEqualTo("conditions: [organisationId IN (acme, globex)] → []")
    }

    @Test
    fun `create with Conditions records them in the CREATE audit entry`() {
        every { repository.existsByKey("targeted") } returns false
        every { repository.save(any()) } answers { firstArg() }

        service.create(
            CreateFlagUseCase.Command(
                key = "targeted",
                name = "Targeted",
                description = null,
                enabled = true,
                conditions = listOf(
                    Condition.Input("organisationId", "IN", listOf("acme", "globex")),
                    Condition.Input("country", "IN", listOf("nl")),
                ),
            ),
        )

        assertThat(recordedDetail()).isEqualTo(
            "Created flag (enabled=true, conditions=[organisationId IN (acme, globex) AND country IN (nl)])",
        )
    }

    @Test
    fun `create without Conditions keeps the CREATE audit detail short`() {
        every { repository.existsByKey("everyone") } returns false
        every { repository.save(any()) } answers { firstArg() }

        service.create(CreateFlagUseCase.Command("everyone", "Everyone", null, false))

        assertThat(recordedDetail()).isEqualTo("Created flag (enabled=false)")
    }

    private val targeted = existing.copy(key = "targeted", enabled = true, conditions = listOf(orgIn))

    @Test
    fun `evaluate applies the Targeting Rule to the Evaluation Context`() {
        every { repository.findByKey("targeted") } returns targeted

        assertThat(service.evaluate("targeted", EvaluationContext(mapOf("organisationId" to "acme"))))
            .isEqualTo(EvaluateFlagUseCase.Result.Evaluated(Evaluation("targeted", true)))
        assertThat(service.evaluate("targeted", EvaluationContext(mapOf("organisationId" to "initech"))))
            .isEqualTo(EvaluateFlagUseCase.Result.Evaluated(Evaluation("targeted", false)))
        assertThat(service.evaluate("targeted", EvaluationContext.EMPTY))
            .isEqualTo(EvaluateFlagUseCase.Result.Evaluated(Evaluation("targeted", false)))
    }

    @Test
    fun `evaluateAll applies each flag's Targeting Rule`() {
        val everyone = existing.copy(key = "everyone", enabled = true)
        val disabledTargeted = targeted.copy(key = "disabled-targeted", enabled = false)
        every { repository.findAll() } returns listOf(everyone, targeted, disabledTargeted)

        assertThat(service.evaluateAll(EvaluationContext(mapOf("organisationId" to "globex")))).containsExactly(
            Evaluation("everyone", true),
            Evaluation("targeted", true),
            Evaluation("disabled-targeted", false),
        )
        assertThat(service.evaluateAll(EvaluationContext(mapOf("country" to "nl")))).containsExactly(
            Evaluation("everyone", true),
            Evaluation("targeted", false),
            Evaluation("disabled-targeted", false),
        )
    }
}
