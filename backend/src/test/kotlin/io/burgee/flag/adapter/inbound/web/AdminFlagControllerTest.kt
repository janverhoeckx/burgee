package io.burgee.flag.adapter.inbound.web

import io.burgee.flag.application.port.inbound.CreateFlagUseCase
import io.burgee.flag.application.port.inbound.DeleteFlagUseCase
import io.burgee.flag.application.port.inbound.GetFlagByIdUseCase
import io.burgee.flag.application.port.inbound.ListFlagsUseCase
import io.burgee.flag.application.port.inbound.ToggleFlagUseCase
import io.burgee.flag.application.port.inbound.UpdateFlagUseCase
import io.burgee.flag.domain.FeatureFlag
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID

class AdminFlagControllerTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val id = UUID.fromString("55555555-5555-5555-5555-555555555555")

    private val flag = FeatureFlag(
        id = id,
        key = "k",
        name = "n",
        description = "d",
        enabled = false,
        createdAt = now,
        updatedAt = now,
    )

    private val listFlags = mockk<ListFlagsUseCase>()
    private val getById = mockk<GetFlagByIdUseCase>()
    private val createFlag = mockk<CreateFlagUseCase>()
    private val updateFlag = mockk<UpdateFlagUseCase>()
    private val toggleFlag = mockk<ToggleFlagUseCase>()
    private val deleteFlag = mockk<DeleteFlagUseCase>()

    private val controller = AdminFlagController(
        listFlags = listFlags,
        getFlagById = getById,
        createFlag = createFlag,
        updateFlag = updateFlag,
        toggleFlag = toggleFlag,
        deleteFlag = deleteFlag,
    )

    @Test
    fun `list maps all flags to full response`() {
        every { listFlags.list() } returns listOf(flag)

        val result = controller.list()

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo(id)
    }

    @Test
    fun `get returns 200 with full response when found`() {
        every { getById.getById(id) } returns GetFlagByIdUseCase.Result.Found(flag)

        val response = controller.get(id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(flag.toResponse())
    }

    @Test
    fun `get returns 404 with ApiError when missing`() {
        every { getById.getById(id) } returns GetFlagByIdUseCase.Result.NotFound

        val response = controller.get(id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat((response.body as ApiError).message).contains(id.toString())
    }

    @Test
    fun `create returns 201 with created flag`() {
        every { createFlag.create(any()) } returns CreateFlagUseCase.Result.Created(flag)

        val response = controller.create(CreateFeatureFlagRequest("k", "n", "d", false))

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body).isEqualTo(flag.toResponse())
        verify {
            createFlag.create(
                CreateFlagUseCase.Command(key = "k", name = "n", description = "d", enabled = false),
            )
        }
    }

    @Test
    fun `create returns 409 with ApiError on duplicate key`() {
        every { createFlag.create(any()) } returns CreateFlagUseCase.Result.DuplicateKey("k")

        val response = controller.create(CreateFeatureFlagRequest("k", "n", "d", false))

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat((response.body as ApiError).message).contains("'k'")
    }

    @Test
    fun `update returns 200 when flag exists`() {
        every { updateFlag.update(any()) } returns UpdateFlagUseCase.Result.Updated(flag)

        val response = controller.update(id, UpdateFeatureFlagRequest("renamed", null, true))

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(flag.toResponse())
        verify {
            updateFlag.update(
                UpdateFlagUseCase.Command(id = id, name = "renamed", description = null, enabled = true),
            )
        }
    }

    @Test
    fun `update returns 404 when flag missing`() {
        every { updateFlag.update(any()) } returns UpdateFlagUseCase.Result.NotFound

        val response = controller.update(id, UpdateFeatureFlagRequest("renamed", null, true))

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `toggle returns 200 with toggled flag`() {
        every { toggleFlag.toggle(id) } returns ToggleFlagUseCase.Result.Toggled(flag)

        val response = controller.toggle(id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(flag.toResponse())
    }

    @Test
    fun `toggle returns 404 when flag missing`() {
        every { toggleFlag.toggle(id) } returns ToggleFlagUseCase.Result.NotFound

        val response = controller.toggle(id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `delete returns 204 when flag deleted`() {
        every { deleteFlag.delete(id) } returns DeleteFlagUseCase.Result.Deleted

        val response = controller.delete(id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `delete returns 404 when flag missing`() {
        every { deleteFlag.delete(id) } returns DeleteFlagUseCase.Result.NotFound

        val response = controller.delete(id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
}
