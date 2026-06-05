package io.github.janverhoeckx.burgee.user.adapter.inbound.web

import io.github.janverhoeckx.burgee.flag.adapter.inbound.web.ApiError
import io.github.janverhoeckx.burgee.security.AuthProperties
import io.github.janverhoeckx.burgee.user.application.port.inbound.CreateUserUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.DeleteUserUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.GetUserByIdUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.ListUsersUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.UpdateUserUseCase
import io.github.janverhoeckx.burgee.user.domain.IdentityProvider
import io.github.janverhoeckx.burgee.user.domain.Role
import io.github.janverhoeckx.burgee.user.domain.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID

class AdminUserControllerTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val id = UUID.fromString("33333333-3333-3333-3333-333333333333")

    private val user = User(
        id = id,
        subject = "jane",
        email = "jane@example.com",
        displayName = "Jane",
        role = Role.USER,
        passwordHash = null,
        provider = IdentityProvider.JWT,
        createdAt = now,
        updatedAt = now,
    )

    private val listUsers = mockk<ListUsersUseCase>()
    private val getUserById = mockk<GetUserByIdUseCase>()
    private val createUser = mockk<CreateUserUseCase>()
    private val updateUser = mockk<UpdateUserUseCase>()
    private val deleteUser = mockk<DeleteUserUseCase>()
    private val authProperties = AuthProperties(method = AuthProperties.Method.JWT)

    private val controller = AdminUserController(
        listUsers = listUsers,
        getUserById = getUserById,
        createUser = createUser,
        updateUser = updateUser,
        deleteUser = deleteUser,
        authProperties = authProperties,
    )

    @Test
    fun `list maps all users to response`() {
        every { listUsers.list() } returns listOf(user)

        val result = controller.list()

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo(id)
    }

    @Test
    fun `get returns 200 with response when found`() {
        every { getUserById.getById(id) } returns GetUserByIdUseCase.Result.Found(user)

        val response = controller.get(id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(user.toResponse())
    }

    @Test
    fun `get returns 404 with ApiError when missing`() {
        every { getUserById.getById(id) } returns GetUserByIdUseCase.Result.NotFound

        val response = controller.get(id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat((response.body as ApiError).message).contains(id.toString())
    }

    @Test
    fun `create returns 201 with created user and falls back to configured provider`() {
        every { createUser.create(any()) } returns CreateUserUseCase.Result.Created(user)

        val response = controller.create(
            CreateUserRequest(subject = "jane", role = Role.ADMIN, password = "secret"),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body).isEqualTo(user.toResponse())
        verify {
            createUser.create(
                CreateUserUseCase.Command(
                    subject = "jane",
                    email = null,
                    displayName = null,
                    role = Role.ADMIN,
                    provider = IdentityProvider.JWT,
                    password = "secret",
                ),
            )
        }
    }

    @Test
    fun `create returns 409 with ApiError on duplicate subject`() {
        every { createUser.create(any()) } returns CreateUserUseCase.Result.DuplicateSubject("jane")

        val response = controller.create(CreateUserRequest(subject = "jane"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat((response.body as ApiError).message).contains("'jane'")
    }

    @Test
    fun `update returns 200 when user exists`() {
        every { updateUser.update(any()) } returns UpdateUserUseCase.Result.Updated(user)

        val response = controller.update(id, UpdateUserRequest(role = Role.ADMIN))

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(user.toResponse())
        verify {
            updateUser.update(
                UpdateUserUseCase.Command(
                    id = id,
                    email = null,
                    displayName = null,
                    role = Role.ADMIN,
                    password = null,
                ),
            )
        }
    }

    @Test
    fun `update returns 404 when user missing`() {
        every { updateUser.update(any()) } returns UpdateUserUseCase.Result.NotFound

        val response = controller.update(id, UpdateUserRequest(role = Role.USER))

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `delete returns 204 when user deleted`() {
        every { deleteUser.delete(id) } returns DeleteUserUseCase.Result.Deleted

        val response = controller.delete(id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `delete returns 404 when user missing`() {
        every { deleteUser.delete(id) } returns DeleteUserUseCase.Result.NotFound

        val response = controller.delete(id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
}
