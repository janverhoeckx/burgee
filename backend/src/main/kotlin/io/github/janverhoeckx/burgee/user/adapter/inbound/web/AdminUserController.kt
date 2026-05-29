package io.github.janverhoeckx.burgee.user.adapter.inbound.web

import io.github.janverhoeckx.burgee.flag.adapter.inbound.web.ApiError
import io.github.janverhoeckx.burgee.security.AuthProperties
import io.github.janverhoeckx.burgee.user.application.port.inbound.CreateUserUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.DeleteUserUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.GetUserByIdUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.ListUsersUseCase
import io.github.janverhoeckx.burgee.user.application.port.inbound.UpdateUserUseCase
import io.github.janverhoeckx.burgee.user.domain.IdentityProvider
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/users")
class AdminUserController(
    private val listUsers: ListUsersUseCase,
    private val getUserById: GetUserByIdUseCase,
    private val createUser: CreateUserUseCase,
    private val updateUser: UpdateUserUseCase,
    private val deleteUser: DeleteUserUseCase,
    private val authProperties: AuthProperties,
) {

    @GetMapping
    fun list(): List<UserResponse> = listUsers.list().map { it.toResponse() }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<*> =
        when (val result = getUserById.getById(id)) {
            is GetUserByIdUseCase.Result.Found -> ResponseEntity.ok(result.user.toResponse())
            GetUserByIdUseCase.Result.NotFound -> notFound("User $id not found")
        }

    @PostMapping
    fun create(@RequestBody @Valid request: CreateUserRequest): ResponseEntity<*> =
        when (val result = createUser.create(request.toCommand(defaultProvider()))) {
            is CreateUserUseCase.Result.Created ->
                ResponseEntity.status(HttpStatus.CREATED).body(result.user.toResponse())
            is CreateUserUseCase.Result.DuplicateSubject ->
                apiError(HttpStatus.CONFLICT, "User with id '${result.subject}' already exists")
        }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdateUserRequest,
    ): ResponseEntity<*> =
        when (val result = updateUser.update(request.toCommand(id))) {
            is UpdateUserUseCase.Result.Updated -> ResponseEntity.ok(result.user.toResponse())
            UpdateUserUseCase.Result.NotFound -> notFound("User $id not found")
        }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<*> =
        when (deleteUser.delete(id)) {
            DeleteUserUseCase.Result.Deleted -> ResponseEntity.noContent().build<Void>()
            DeleteUserUseCase.Result.NotFound -> notFound("User $id not found")
        }

    private fun defaultProvider(): IdentityProvider =
        IdentityProvider.valueOf(authProperties.method.name)

    private fun notFound(message: String): ResponseEntity<ApiError> =
        apiError(HttpStatus.NOT_FOUND, message)

    private fun apiError(status: HttpStatus, message: String): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(
            ApiError(status = status.value(), error = status.reasonPhrase, message = message),
        )
}
