package io.github.janverhoeckx.burgee.flag.adapter.outbound.persistence

import io.github.janverhoeckx.burgee.flag.domain.FeatureFlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class FeatureFlagMapperTest {

    private val createdAt = Instant.parse("2026-01-01T12:00:00Z")
    private val updatedAt = createdAt.plusSeconds(60)
    private val id = UUID.fromString("22222222-2222-2222-2222-222222222222")

    private val domain = FeatureFlag(
        id = id,
        key = "k",
        name = "n",
        description = "d",
        enabled = true,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    @Test
    fun `toRow with newRecord flag preserves all fields`() {
        val row = domain.toRow(newRecord = true)

        assertThat(row.id).isEqualTo(id)
        assertThat(row.key).isEqualTo("k")
        assertThat(row.name).isEqualTo("n")
        assertThat(row.description).isEqualTo("d")
        assertThat(row.enabled).isTrue()
        assertThat(row.createdAt).isEqualTo(createdAt)
        assertThat(row.updatedAt).isEqualTo(updatedAt)
        assertThat(row.isNew()).isTrue()
    }

    @Test
    fun `toRow with newRecord false marks as existing`() {
        assertThat(domain.toRow(newRecord = false).isNew()).isFalse()
    }

    @Test
    fun `toDomain reverses toRow`() {
        val roundTripped = domain.toRow(newRecord = false).toDomain()

        assertThat(roundTripped).isEqualTo(domain)
    }

    @Test
    fun `null description maps both directions`() {
        val withoutDesc = domain.copy(description = null)

        val row = withoutDesc.toRow(newRecord = false)
        assertThat(row.description).isNull()

        assertThat(row.toDomain().description).isNull()
    }
}
