package io.github.janverhoeckx.burgee.flag.adapter.inbound.web

import io.github.janverhoeckx.burgee.flag.application.port.inbound.GetFlagByKeyUseCase
import io.github.janverhoeckx.burgee.flag.application.port.inbound.ListFlagsUseCase
import io.github.janverhoeckx.burgee.flag.domain.FeatureFlag
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID

class PublicFlagControllerTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val flag = FeatureFlag(
        id = UUID.randomUUID(),
        key = "checkout-v2",
        name = "Checkout v2",
        description = null,
        enabled = true,
        createdAt = now,
        updatedAt = now,
    )

    private val listFlags = mockk<ListFlagsUseCase>()
    private val getByKey = mockk<GetFlagByKeyUseCase>()
    private val controller = PublicFlagController(listFlags, getByKey)

    @Test
    fun `list returns only key and enabled`() {
        every { listFlags.list() } returns listOf(flag)

        val result = controller.list()

        assertThat(result).containsExactly(PublicFlagResponse("checkout-v2", true))
    }

    @Test
    fun `get returns 200 and public projection when found`() {
        every { getByKey.getByKey("checkout-v2") } returns
            GetFlagByKeyUseCase.Result.Found(flag)

        val response = controller.get("checkout-v2")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(PublicFlagResponse("checkout-v2", true))
    }

    @Test
    fun `get returns 404 with ApiError body when missing`() {
        every { getByKey.getByKey("missing") } returns
            GetFlagByKeyUseCase.Result.NotFound

        val response = controller.get("missing")

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        val body = response.body as ApiError
        assertThat(body.status).isEqualTo(404)
        assertThat(body.message).contains("missing")
    }
}
