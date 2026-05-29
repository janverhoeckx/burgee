package io.github.janverhoeckx.burgee.flag.adapter.inbound.web

import io.github.janverhoeckx.burgee.flag.domain.FeatureFlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class FlagDtosTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val id = UUID.fromString("44444444-4444-4444-4444-444444444444")

    private val flag = FeatureFlag(
        id = id,
        key = "checkout-v2",
        name = "Checkout v2",
        description = "desc",
        enabled = true,
        createdAt = now,
        updatedAt = now.plusSeconds(60),
    )

    @Test
    fun `toResponse copies every field`() {
        val response = flag.toResponse()

        assertThat(response).isEqualTo(
            FeatureFlagResponse(
                id = id,
                key = "checkout-v2",
                name = "Checkout v2",
                description = "desc",
                enabled = true,
                createdAt = now,
                updatedAt = now.plusSeconds(60),
            ),
        )
    }

    @Test
    fun `toPublic exposes only key and enabled`() {
        assertThat(flag.toPublic()).isEqualTo(PublicFlagResponse("checkout-v2", true))
    }

    @Test
    fun `CreateFeatureFlagRequest builds command verbatim`() {
        val command = CreateFeatureFlagRequest(
            key = "k",
            name = "n",
            description = "d",
            enabled = true,
        ).toCommand()

        assertThat(command.key).isEqualTo("k")
        assertThat(command.name).isEqualTo("n")
        assertThat(command.description).isEqualTo("d")
        assertThat(command.enabled).isTrue()
    }

    @Test
    fun `UpdateFeatureFlagRequest builds command with provided id`() {
        val command = UpdateFeatureFlagRequest(name = "n", description = null, enabled = false)
            .toCommand(id)

        assertThat(command.id).isEqualTo(id)
        assertThat(command.name).isEqualTo("n")
        assertThat(command.description).isNull()
        assertThat(command.enabled).isFalse()
    }
}
