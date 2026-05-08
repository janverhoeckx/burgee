package io.burgee.flag.application.service

import io.burgee.flag.application.port.inbound.CreateFlagUseCase
import io.burgee.flag.application.port.inbound.DeleteFlagUseCase
import io.burgee.flag.application.port.inbound.GetFlagByIdUseCase
import io.burgee.flag.application.port.inbound.GetFlagByKeyUseCase
import io.burgee.flag.application.port.inbound.ToggleFlagUseCase
import io.burgee.flag.application.port.inbound.UpdateFlagUseCase
import io.burgee.flag.application.port.outbound.FeatureFlagRepositoryPort
import io.burgee.flag.domain.FeatureFlag
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
    private val service = FeatureFlagService(repository, clock)

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
    fun `getByKey returns NotFound when missing`() {
        every { repository.findByKey("missing") } returns null

        assertThat(service.getByKey("missing"))
            .isEqualTo(GetFlagByKeyUseCase.Result.NotFound)
    }

    @Test
    fun `getByKey returns Found when present`() {
        every { repository.findByKey("checkout-v2") } returns existing

        assertThat(service.getByKey("checkout-v2"))
            .isEqualTo(GetFlagByKeyUseCase.Result.Found(existing))
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
        every { repository.existsById(id) } returns true
        every { repository.deleteById(id) } returns Unit

        assertThat(service.delete(id)).isEqualTo(DeleteFlagUseCase.Result.Deleted)
        verify { repository.deleteById(id) }
    }

    @Test
    fun `delete returns NotFound when missing`() {
        every { repository.existsById(id) } returns false

        assertThat(service.delete(id)).isEqualTo(DeleteFlagUseCase.Result.NotFound)
        verify(exactly = 0) { repository.deleteById(any()) }
    }
}
