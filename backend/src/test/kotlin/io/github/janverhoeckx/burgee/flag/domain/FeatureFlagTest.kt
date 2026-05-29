package io.github.janverhoeckx.burgee.flag.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class FeatureFlagTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val later = now.plusSeconds(60)
    private val id = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `create populates fields and stamps both timestamps`() {
        val flag = FeatureFlag.create(
            key = "checkout-v2",
            name = "Checkout v2",
            description = "Switch to new checkout flow",
            enabled = false,
            now = now,
            id = id,
        )

        assertThat(flag.id).isEqualTo(id)
        assertThat(flag.key).isEqualTo("checkout-v2")
        assertThat(flag.name).isEqualTo("Checkout v2")
        assertThat(flag.description).isEqualTo("Switch to new checkout flow")
        assertThat(flag.enabled).isFalse()
        assertThat(flag.createdAt).isEqualTo(now)
        assertThat(flag.updatedAt).isEqualTo(now)
    }

    @Test
    fun `create allows null description`() {
        val flag = FeatureFlag.create("k", "n", description = null, enabled = true, now = now)

        assertThat(flag.description).isNull()
        assertThat(flag.enabled).isTrue()
    }

    @Test
    fun `withDetails updates fields and refreshes updatedAt while preserving createdAt`() {
        val original = FeatureFlag.create("k", "n", "d", false, now, id)

        val updated = original.withDetails(
            name = "Renamed",
            description = "New description",
            enabled = true,
            now = later,
        )

        assertThat(updated.id).isEqualTo(original.id)
        assertThat(updated.key).isEqualTo(original.key)
        assertThat(updated.name).isEqualTo("Renamed")
        assertThat(updated.description).isEqualTo("New description")
        assertThat(updated.enabled).isTrue()
        assertThat(updated.createdAt).isEqualTo(now)
        assertThat(updated.updatedAt).isEqualTo(later)
    }

    @Test
    fun `withDetails leaves the original instance unchanged`() {
        val original = FeatureFlag.create("k", "n", "d", false, now, id)

        original.withDetails("Renamed", null, true, later)

        assertThat(original.name).isEqualTo("n")
        assertThat(original.description).isEqualTo("d")
        assertThat(original.enabled).isFalse()
        assertThat(original.updatedAt).isEqualTo(now)
    }

    @Test
    fun `toggled flips enabled and refreshes updatedAt`() {
        val flag = FeatureFlag.create("k", "n", null, enabled = false, now = now, id = id)

        val toggled = flag.toggled(later)

        assertThat(toggled.enabled).isTrue()
        assertThat(toggled.updatedAt).isEqualTo(later)
        assertThat(toggled.createdAt).isEqualTo(now)
    }

    @Test
    fun `toggled twice returns original enabled state`() {
        val flag = FeatureFlag.create("k", "n", null, enabled = true, now = now, id = id)

        val twice = flag.toggled(later).toggled(later.plusSeconds(1))

        assertThat(twice.enabled).isTrue()
    }
}
