package io.burgee.flag.adapter.outbound.persistence

import io.burgee.flag.domain.FeatureFlag
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional
import java.util.UUID

class FeatureFlagPersistenceAdapterTest {

    private val jdbc = mockk<FeatureFlagJdbcRepository>()
    private val adapter = FeatureFlagPersistenceAdapter(jdbc)

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val id = UUID.fromString("33333333-3333-3333-3333-333333333333")
    private val flag = FeatureFlag(
        id = id,
        key = "k",
        name = "n",
        description = "d",
        enabled = true,
        createdAt = now,
        updatedAt = now,
    )
    private val row = flag.toRow(newRecord = false)

    @Test
    fun `findAll maps rows to domain in order`() {
        val other = flag.copy(id = UUID.randomUUID(), key = "a").toRow(newRecord = false)
        every { jdbc.findAllOrderedByKey() } returns listOf(other, row)

        val result = adapter.findAll()

        assertThat(result).extracting<String> { it.key }.containsExactly("a", "k")
    }

    @Test
    fun `findById returns mapped domain when present`() {
        every { jdbc.findById(id) } returns Optional.of(row)

        assertThat(adapter.findById(id)).isEqualTo(flag)
    }

    @Test
    fun `findById returns null when absent`() {
        every { jdbc.findById(id) } returns Optional.empty()

        assertThat(adapter.findById(id)).isNull()
    }

    @Test
    fun `findByKey returns mapped domain when present`() {
        every { jdbc.findByKey("k") } returns row

        assertThat(adapter.findByKey("k")).isEqualTo(flag)
    }

    @Test
    fun `findByKey returns null when absent`() {
        every { jdbc.findByKey("missing") } returns null

        assertThat(adapter.findByKey("missing")).isNull()
    }

    @Test
    fun `existsByKey delegates to repository`() {
        every { jdbc.existsByKey("k") } returns true

        assertThat(adapter.existsByKey("k")).isTrue()
    }

    @Test
    fun `existsById delegates to repository`() {
        every { jdbc.existsById(id) } returns true

        assertThat(adapter.existsById(id)).isTrue()
    }

    @Test
    fun `save marks row as new when id is unknown`() {
        every { jdbc.existsById(id) } returns false
        val captured = slot<FeatureFlagRow>()
        every { jdbc.save(capture(captured)) } answers { captured.captured }

        val result = adapter.save(flag)

        assertThat(captured.captured.isNew()).isTrue()
        assertThat(result).isEqualTo(flag)
    }

    @Test
    fun `save marks row as existing when id is known`() {
        every { jdbc.existsById(id) } returns true
        val captured = slot<FeatureFlagRow>()
        every { jdbc.save(capture(captured)) } answers { captured.captured }

        adapter.save(flag)

        assertThat(captured.captured.isNew()).isFalse()
    }

    @Test
    fun `deleteById delegates to repository`() {
        every { jdbc.deleteById(id) } returns Unit

        adapter.deleteById(id)

        verify { jdbc.deleteById(id) }
    }
}
